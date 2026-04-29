package com.auction.network.server;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.model.Auction;
import com.auction.model.Item.Item;
import com.auction.model.User.User;
import com.auction.network.protocol.SocketMessage;
import com.auction.network.protocol.SocketMessage.Action;
import com.auction.service.AuctionService;
import com.auction.service.BidService;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mỗi client kết nối được gán một ClientHandler chạy trên thread riêng.
 * Nhận SocketMessage → xử lý → trả SocketMessage.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;          // Tham chiếu đến server để broadcast

    private ObjectInputStream in;
    private ObjectOutputStream out;

    // Services & DAOs
    private final AuctionService auctionService = new AuctionService();
    private final BidService bidService         = new BidService();
    private final UserDAO userDAO               = new UserDAO();
    private final ItemDAO itemDAO               = new ItemDAO();

    // Thông tin client hiện tại (sau khi login)
    private User currentUser = null;
    private String auctionIdWatching = null;   // Phiên đang xem để nhận broadcast

    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    // ── Vòng lặp chính ────────────────────────────────────────────
    @Override
    public void run() {
        try {
            // ObjectOutputStream PHẢI tạo trước ObjectInputStream (tránh deadlock)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] Client kết nối: " + socket.getRemoteSocketAddress());

            SocketMessage msg;
            while ((msg = (SocketMessage) in.readObject()) != null) {
                SocketMessage response = handle(msg);
                sendResponse(response);
            }

        } catch (EOFException | java.net.SocketException e) {
            // Client ngắt kết nối bình thường
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ClientHandler] Lỗi: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ── Điều phối request ─────────────────────────────────────────
    private SocketMessage handle(SocketMessage msg) {
        System.out.printf("[ClientHandler] Nhận: %s từ %s%n",
                msg.getAction(), socket.getRemoteSocketAddress());

        return switch (msg.getAction()) {
            // Auth
            case LOGIN    -> handleLogin(msg);
            case LOGOUT   -> handleLogout();
            case REGISTER -> handleRegister(msg);

            // Auction
            case GET_ALL_AUCTIONS -> handleGetAllAuctions();
            case GET_AUCTION      -> handleGetAuction(msg);
            case CREATE_AUCTION   -> handleCreateAuction(msg);
            case START_AUCTION    -> handleStartAuction(msg);
            case FINISH_AUCTION   -> handleFinishAuction(msg);
            case CANCEL_AUCTION   -> handleCancelAuction(msg);
            case DELETE_AUCTION   -> handleDeleteAuction(msg);

            // Bid
            case PLACE_BID          -> handlePlaceBid(msg);
            case REGISTER_AUTO_BID  -> handleRegisterAutoBid(msg);
            case CANCEL_AUTO_BID    -> handleCancelAutoBid(msg);

            // Item
            case CREATE_ITEM         -> handleCreateItem(msg);
            case UPDATE_ITEM         -> handleUpdateItem(msg);
            case DELETE_ITEM         -> handleDeleteItem(msg);
            case GET_ITEMS_BY_SELLER -> handleGetItemsBySeller(msg);

            // User
            case GET_ALL_USERS -> handleGetAllUsers();
            case UPDATE_USER   -> handleUpdateUser(msg);
            case DELETE_USER   -> handleDeleteUser(msg);

            default -> SocketMessage.error(msg.getAction(), "Action không được hỗ trợ: " + msg.getAction());
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  AUTH
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handleLogin(SocketMessage msg) {
        String username = msg.getString("username");
        String password = msg.getString("password");

        if (username == null || password == null)
            return SocketMessage.error(Action.LOGIN, "Thiếu username hoặc password");

        User user = userDAO.login(username, password);
        if (user == null)
            return SocketMessage.error(Action.LOGIN, "Sai tên đăng nhập hoặc mật khẩu");

        this.currentUser = user;
        return SocketMessage.ok(Action.LOGIN, "Đăng nhập thành công")
                .put("user", user);
    }

    private SocketMessage handleLogout() {
        this.currentUser = null;
        this.auctionIdWatching = null;
        return SocketMessage.ok(Action.LOGOUT, "Đã đăng xuất");
    }

    private SocketMessage handleRegister(SocketMessage msg) {
        User user = (User) msg.get("user");
        if (user == null)
            return SocketMessage.error(Action.REGISTER, "Thiếu thông tin người dùng");

        boolean ok = userDAO.insert(user);
        return ok
                ? SocketMessage.ok(Action.REGISTER, "Đăng ký thành công")
                : SocketMessage.error(Action.REGISTER, "Đăng ký thất bại (username/email đã tồn tại?)");
    }

    // ══════════════════════════════════════════════════════════════
    //  AUCTION
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handleGetAllAuctions() {
        List<Auction> list = auctionService.getAllAuctions();
        return SocketMessage.ok(Action.GET_ALL_AUCTIONS, "OK")
                .put("auctions", list);
    }

    private SocketMessage handleGetAuction(SocketMessage msg) {
        String auctionId = msg.getString("auctionId");
        if (auctionId == null)
            return SocketMessage.error(Action.GET_AUCTION, "Thiếu auctionId");

        // Đánh dấu client đang xem phiên này để nhận broadcast
        this.auctionIdWatching = auctionId;

        Auction auction = auctionService.getAuctionById(auctionId);
        return auction != null
                ? SocketMessage.ok(Action.GET_AUCTION, "OK").put("auction", auction)
                : SocketMessage.error(Action.GET_AUCTION, "Không tìm thấy phiên: " + auctionId);
    }

    private SocketMessage handleCreateAuction(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.CREATE_AUCTION);

        Item item            = (Item) msg.get("item");
        LocalDateTime start  = (LocalDateTime) msg.get("startTime");
        LocalDateTime end    = (LocalDateTime) msg.get("endTime");
        boolean antiSniping  = msg.getBoolean("antiSnipingEnabled");
        int snipeWindow      = msg.getInt("snipeWindowSeconds");
        int snipeExtend      = msg.getInt("snipeExtendSeconds");

        if (item == null || start == null || end == null)
            return SocketMessage.error(Action.CREATE_AUCTION, "Thiếu thông tin auction");

        try {
            String id = auctionService.createAuction(item, start, end, antiSniping, snipeWindow, snipeExtend);
            return id != null
                    ? SocketMessage.ok(Action.CREATE_AUCTION, "Tạo phiên thành công").put("auctionId", id)
                    : SocketMessage.error(Action.CREATE_AUCTION, "Tạo phiên thất bại");
        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.CREATE_AUCTION, e.getMessage());
        }
    }

    private SocketMessage handleStartAuction(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.START_AUCTION);
        String id = msg.getString("auctionId");
        boolean ok = auctionService.startAuction(id);
        return ok
                ? SocketMessage.ok(Action.START_AUCTION, "Phiên đã bắt đầu")
                : SocketMessage.error(Action.START_AUCTION, "Không thể bắt đầu phiên");
    }

    private SocketMessage handleFinishAuction(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.FINISH_AUCTION);
        String id = msg.getString("auctionId");
        boolean ok = auctionService.finishAuction(id);
        if (ok) {
            // Broadcast cho tất cả client đang xem phiên này
            Auction finished = auctionService.getAuctionById(id);
            server.broadcastToWatchers(id,
                    SocketMessage.ok(Action.BROADCAST_AUCTION_END, "Phiên đấu giá đã kết thúc")
                            .put("auction", finished));
        }
        return ok
                ? SocketMessage.ok(Action.FINISH_AUCTION, "Phiên đã kết thúc")
                : SocketMessage.error(Action.FINISH_AUCTION, "Không thể kết thúc phiên");
    }

    private SocketMessage handleCancelAuction(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.CANCEL_AUCTION);
        String id = msg.getString("auctionId");
        boolean ok = auctionService.cancelAuction(id);
        return ok
                ? SocketMessage.ok(Action.CANCEL_AUCTION, "Phiên đã hủy")
                : SocketMessage.error(Action.CANCEL_AUCTION, "Không thể hủy phiên");
    }

    private SocketMessage handleDeleteAuction(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.DELETE_AUCTION);
        String id = msg.getString("auctionId");
        boolean ok = auctionService.deleteAuction(id);
        return ok
                ? SocketMessage.ok(Action.DELETE_AUCTION, "Đã xóa phiên")
                : SocketMessage.error(Action.DELETE_AUCTION, "Không thể xóa phiên");
    }

    // ══════════════════════════════════════════════════════════════
    //  BID
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handlePlaceBid(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.PLACE_BID);

        String auctionId  = msg.getString("auctionId");
        String bidderId   = msg.getString("bidderId");
        String bidderName = msg.getString("bidderName");
        double amount     = msg.getDouble("amount");

        try {
            String bidId = bidService.placeManualBid(auctionId, bidderId, bidderName, amount);
            if (bidId == null)
                return SocketMessage.error(Action.PLACE_BID, "Đặt giá thất bại");

            // Broadcast cho tất cả client đang xem phiên này
            Auction updated = auctionService.getAuctionById(auctionId);
            server.broadcastToWatchers(auctionId,
                    SocketMessage.ok(Action.BROADCAST_BID_UPDATE, "Có bid mới!")
                            .put("auction", updated)
                            .put("bidderName", bidderName)
                            .put("amount", amount));

            return SocketMessage.ok(Action.PLACE_BID, "Đặt giá thành công").put("bidId", bidId);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return SocketMessage.error(Action.PLACE_BID, e.getMessage());
        }
    }

    private SocketMessage handleRegisterAutoBid(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.REGISTER_AUTO_BID);

        String auctionId  = msg.getString("auctionId");
        String bidderId   = msg.getString("bidderId");
        String bidderName = msg.getString("bidderName");
        double maxBid     = msg.getDouble("maxBid");
        double increment  = msg.getDouble("increment");

        try {
            boolean ok = bidService.registerAutoBid(auctionId, bidderId, bidderName, maxBid, increment);
            return ok
                    ? SocketMessage.ok(Action.REGISTER_AUTO_BID, "Đăng ký auto bid thành công")
                    : SocketMessage.error(Action.REGISTER_AUTO_BID, "Đăng ký thất bại");
        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.REGISTER_AUTO_BID, e.getMessage());
        }
    }

    private SocketMessage handleCancelAutoBid(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.CANCEL_AUTO_BID);
        String auctionId = msg.getString("auctionId");
        String bidderId  = msg.getString("bidderId");
        boolean ok = bidService.cancelAutoBid(auctionId, bidderId);
        return ok
                ? SocketMessage.ok(Action.CANCEL_AUTO_BID, "Hủy auto bid thành công")
                : SocketMessage.error(Action.CANCEL_AUTO_BID, "Hủy thất bại");
    }

    // ══════════════════════════════════════════════════════════════
    //  ITEM
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handleCreateItem(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.CREATE_ITEM);
        Item item     = (Item) msg.get("item");
        String seller = msg.getString("sellerId");
        if (item == null || seller == null)
            return SocketMessage.error(Action.CREATE_ITEM, "Thiếu item hoặc sellerId");

        boolean ok = itemDAO.insert(item, seller);
        return ok
                ? SocketMessage.ok(Action.CREATE_ITEM, "Tạo item thành công").put("itemId", item.getId())
                : SocketMessage.error(Action.CREATE_ITEM, "Tạo item thất bại");
    }

    private SocketMessage handleUpdateItem(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.UPDATE_ITEM);
        Item item = (Item) msg.get("item");
        if (item == null) return SocketMessage.error(Action.UPDATE_ITEM, "Thiếu item");
        boolean ok = itemDAO.update(item);
        return ok
                ? SocketMessage.ok(Action.UPDATE_ITEM, "Cập nhật thành công")
                : SocketMessage.error(Action.UPDATE_ITEM, "Cập nhật thất bại");
    }

    private SocketMessage handleDeleteItem(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.DELETE_ITEM);
        String itemId = msg.getString("itemId");
        boolean ok = itemDAO.delete(itemId);
        return ok
                ? SocketMessage.ok(Action.DELETE_ITEM, "Xóa item thành công")
                : SocketMessage.error(Action.DELETE_ITEM, "Xóa thất bại");
    }

    private SocketMessage handleGetItemsBySeller(SocketMessage msg) {
        String sellerId = msg.getString("sellerId");
        List<Item> items = itemDAO.getItemsBySeller(sellerId);
        return SocketMessage.ok(Action.GET_ITEMS_BY_SELLER, "OK").put("items", items);
    }

    // ══════════════════════════════════════════════════════════════
    //  USER
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handleGetAllUsers() {
        if (!isLoggedIn()) return requireLogin(Action.GET_ALL_USERS);
        List<User> users = userDAO.getAll();
        return SocketMessage.ok(Action.GET_ALL_USERS, "OK").put("users", users);
    }

    private SocketMessage handleUpdateUser(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.UPDATE_USER);
        User user = (User) msg.get("user");
        if (user == null) return SocketMessage.error(Action.UPDATE_USER, "Thiếu user");
        boolean ok = userDAO.update(user);
        return ok
                ? SocketMessage.ok(Action.UPDATE_USER, "Cập nhật thành công")
                : SocketMessage.error(Action.UPDATE_USER, "Cập nhật thất bại");
    }

    private SocketMessage handleDeleteUser(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.DELETE_USER);
        String userId = msg.getString("userId");
        boolean ok = userDAO.delete(userId);
        return ok
                ? SocketMessage.ok(Action.DELETE_USER, "Xóa user thành công")
                : SocketMessage.error(Action.DELETE_USER, "Xóa thất bại");
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════════

    /** Gửi response về client, thread-safe */
    public synchronized void sendResponse(SocketMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // Quan trọng: tránh cache object cũ trong Java Serialization
        } catch (IOException e) {
            System.err.println("[ClientHandler] Gửi response lỗi: " + e.getMessage());
        }
    }

    public String getAuctionIdWatching() {
        return auctionIdWatching;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private boolean isLoggedIn() {
        return currentUser != null;
    }

    private SocketMessage requireLogin(Action action) {
        return SocketMessage.error(action, "Bạn cần đăng nhập để thực hiện thao tác này");
    }

    private void cleanup() {
        server.removeClient(this);
        try { if (in != null)  in.close();  } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
        System.out.println("[Server] Client ngắt kết nối: " + socket.getRemoteSocketAddress());
    }
}
