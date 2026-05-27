package com.auction.server.network;

import com.auction.server.service.ItemService;
import com.auction.server.service.UserService;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;
import com.auction.shared.network.protocol.SocketMessage.Action;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.server.service.BidService;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.exception.AuthenticationException;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final BidService bidService = new BidService();
    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();
    private final ItemService itemService = new ItemService();

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
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] Client kết nối: " + socket.getRemoteSocketAddress());

            SocketMessage msg;
            while ((msg = (SocketMessage) in.readObject()) != null) {
                SocketMessage response = handle(msg);
                sendResponse(response);
            }

        } catch (EOFException | java.net.SocketException e) {
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
            case LOGIN -> handleLogin(msg);
            case LOGOUT -> handleLogout();
            case REGISTER -> handleRegister(msg);

            // Auction
            case GET_ALL_AUCTIONS -> handleGetAllAuctions();
            case GET_MY_SELLER_AUCTIONS -> handleGetMySellerAuctions();
            case GET_AUCTION -> handleGetAuction(msg);
            case CREATE_AUCTION -> handleCreateAuction(msg);
            case START_AUCTION -> handleStartAuction(msg);
            case FINISH_AUCTION -> handleFinishAuction(msg);
            case CANCEL_AUCTION -> handleCancelAuction(msg);
            case DELETE_AUCTION -> handleDeleteAuction(msg);

            // Bid
            case PLACE_BID -> handlePlaceBid(msg);
            case REGISTER_AUTO_BID -> handleRegisterAutoBid(msg);
            case CANCEL_AUTO_BID -> handleCancelAutoBid(msg);
            case GET_MY_BIDS -> handleGetMyBids(msg);

            // Item
            case CREATE_ITEM -> handleCreateItem(msg);
            case UPDATE_ITEM -> handleUpdateItem(msg);
            case DELETE_ITEM -> handleDeleteItem(msg);
            case GET_ITEMS_BY_SELLER -> handleGetItemsBySeller(msg);

            // User
            case GET_ALL_USERS -> handleGetAllUsers();
            case UPDATE_USER -> handleUpdateUser(msg);
            case DELETE_USER -> handleDeleteUser(msg);
            case DEPOSIT_USER -> handleDepositUser(msg);

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

        try {
            User user = authService.login(username, password);
            if (user == null)
                return SocketMessage.error(Action.LOGIN, "Sai tên đăng nhập hoặc mật khẩu");

            this.currentUser = user;
            return SocketMessage.ok(Action.LOGIN, "Đăng nhập thành công")
                    .put("user", user);

        } catch (AuthenticationException e) {
            return SocketMessage.error(Action.LOGIN, e.getMessage());
        }
    }

    private SocketMessage handleLogout() {
        this.currentUser = null;
        this.auctionIdWatching = null;
        return SocketMessage.ok(Action.LOGOUT, "Đã đăng xuất");
    }

    /**
     * Client gửi kèm theo payload:
     * "userType" : "BIDDER" hoặc "SELLER"
     * "username" : String
     * "password" : String
     * <p>
     * AuthService sẽ kiểm tra trùng username và tạo đúng loại user.
     */
    private SocketMessage handleRegister(SocketMessage msg) {
        String userType = msg.getString("userType");
        String username = msg.getString("username");
        String email = msg.getString("Email");
        String password = msg.getString("password");
        String fullName = msg.getString("fullname");

        if (userType == null || userType.isBlank()) {
            return SocketMessage.error(Action.REGISTER, "Thiếu userType");
        }

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return SocketMessage.error(Action.REGISTER, "Thiếu username hoặc password");
        }

        int result;

        if ("SELLER".equalsIgnoreCase(userType)) {
            result = authService.registerSeller(username, email, password, fullName);
        } else if ("BIDDER".equalsIgnoreCase(userType)) {
            result = authService.registerBidder(username, email, password, fullName);
        } else {
            return SocketMessage.error(Action.REGISTER, "userType không hợp lệ, chỉ nhận BIDDER hoặc SELLER");
        }

        return switch (result) {
            case 0 -> SocketMessage.ok(Action.REGISTER, "Đăng ký thành công");
            case 1 -> SocketMessage.error(Action.REGISTER, "Username đã tồn tại, vui lòng chọn tên khác");
            case 3 -> SocketMessage.error(Action.REGISTER, "Email đã tồn tại,vui lòng nhập email khác");
            default -> SocketMessage.error(Action.REGISTER, "Đăng ký thất bại, lỗi kết nối cơ sở dữ liệu");
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  AUCTION
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handleGetAllAuctions() {
        List<Auction> list = auctionService.getAllAuctions();
        return SocketMessage.ok(Action.GET_ALL_AUCTIONS, "OK")
                .put("auctions", list);
    }

    private SocketMessage handleGetMySellerAuctions() {
        if (!isLoggedIn()) return requireLogin(Action.GET_MY_SELLER_AUCTIONS);

        List<Auction> list = auctionService.getAuctionsBySeller(currentUser.getId());
        return SocketMessage.ok(Action.GET_MY_SELLER_AUCTIONS, "OK")
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

        Item item = (Item) msg.get("item");
        LocalDateTime start = (LocalDateTime) msg.get("startTime");
        LocalDateTime end = (LocalDateTime) msg.get("endTime");
        boolean antiSniping = msg.getBoolean("antiSnipingEnabled");
        int snipeWindow = msg.getInt("snipeWindowSeconds");
        int snipeExtend = msg.getInt("snipeExtendSeconds");

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
            server.broadcastToWatchersExcept(id,
                    SocketMessage.ok(Action.BROADCAST_AUCTION_END, "Phiên đấu giá đã kết thúc")
                            .put("auction", finished),
                    this);
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

    private SocketMessage handleGetMyBids(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.GET_MY_BIDS);
        String bidderId = currentUser.getId();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();
        List<Auction> auctions = auctionDAO.getAuctionsByBidder(bidderId);
        Map<String, Double> myBestBids = bidDAO.getMyBestBids(bidderId);
        return SocketMessage.ok(Action.GET_MY_BIDS, "OK")
                .put("auctions", auctions)
                .put("myBestBids", myBestBids);
    }

    private SocketMessage handlePlaceBid(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.PLACE_BID);

        String auctionId = msg.getString("auctionId");
        double amount = msg.getDouble("amount");

        if (auctionId == null || auctionId.isBlank()) {
            return SocketMessage.error(Action.PLACE_BID, "Thiếu auctionId");
        }

        if (amount <= 0) {
            return SocketMessage.error(Action.PLACE_BID, "Số tiền bid phải lớn hơn 0");
        }

        String bidderId = currentUser.getId();
        String bidderName = currentUser.getUsername();

        try {
            String bidId = bidService.placeManualBid(auctionId, bidderId, bidderName, amount);

            if (bidId == null) {
                return SocketMessage.error(Action.PLACE_BID, "Đặt giá thất bại");
            }

            Auction updated = auctionService.getAuctionById(auctionId);

            server.broadcastToWatchersExcept(auctionId,
                    SocketMessage.ok(Action.BROADCAST_BID_UPDATE, "Có bid mới!")
                            .put("auction", updated)
                            .put("bidderName", bidderName)
                            .put("amount", amount),
                    this);

            return SocketMessage.ok(Action.PLACE_BID, "Đặt giá thành công")
                    .put("bidId", bidId)
                    .put("auction", updated)
                    .put("user", userService.getUserById(bidderId));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return SocketMessage.error(Action.PLACE_BID, e.getMessage());
        }
    }

    private SocketMessage handleRegisterAutoBid(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.REGISTER_AUTO_BID);

        String auctionId = msg.getString("auctionId");
        double maxBid = msg.getDouble("maxBid");
        double increment = msg.getDouble("increment");

        if (auctionId == null || auctionId.isBlank()) {
            return SocketMessage.error(Action.REGISTER_AUTO_BID, "Thiếu auctionId");
        }

        String bidderId = currentUser.getId();
        String bidderName = currentUser.getUsername();

        try {
            boolean ok = bidService.registerAutoBid(auctionId, bidderId, bidderName, maxBid, increment);

            return ok
                    ? SocketMessage.ok(Action.REGISTER_AUTO_BID, "Đăng ký auto bid thành công").put("user", userService.getUserById(bidderId))
                    : SocketMessage.error(Action.REGISTER_AUTO_BID, "Đăng ký thất bại");

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.REGISTER_AUTO_BID, e.getMessage());
        }
    }

    private SocketMessage handleCancelAutoBid(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.CANCEL_AUTO_BID);

        String auctionId = msg.getString("auctionId");

        if (auctionId == null || auctionId.isBlank()) {
            return SocketMessage.error(Action.CANCEL_AUTO_BID, "Thiếu auctionId");
        }

        String bidderId = currentUser.getId();

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

        Item item = (Item) msg.get("item");

        try {
            String sellerId = currentUser.getId();

            boolean ok = itemService.createItem(item, sellerId);

            return ok
                    ? SocketMessage.ok(Action.CREATE_ITEM, "Tạo item thành công").put("itemId", item.getId())
                    : SocketMessage.error(Action.CREATE_ITEM, "Tạo item thất bại");

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.CREATE_ITEM, e.getMessage());
        }
    }

    private SocketMessage handleUpdateItem(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.UPDATE_ITEM);

        Item item = (Item) msg.get("item");

        try {
            boolean ok = itemService.updateItem(item);

            return ok
                    ? SocketMessage.ok(Action.UPDATE_ITEM, "Cập nhật thành công")
                    : SocketMessage.error(Action.UPDATE_ITEM, "Cập nhật thất bại");

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.UPDATE_ITEM, e.getMessage());
        }
    }

    private SocketMessage handleDeleteItem(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.DELETE_ITEM);

        String itemId = msg.getString("itemId");

        try {
            boolean ok = itemService.deleteItem(itemId);

            return ok
                    ? SocketMessage.ok(Action.DELETE_ITEM, "Xóa item thành công")
                    : SocketMessage.error(Action.DELETE_ITEM, "Xóa thất bại");

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.DELETE_ITEM, e.getMessage());
        }
    }

    private SocketMessage handleGetItemsBySeller(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.GET_ITEMS_BY_SELLER);

        try {
            String sellerId = currentUser.getId();

            List<Item> items = itemService.getItemsBySeller(sellerId);

            return SocketMessage.ok(Action.GET_ITEMS_BY_SELLER, "OK")
                    .put("items", items);

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.GET_ITEMS_BY_SELLER, e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  USER
    // ══════════════════════════════════════════════════════════════

    private SocketMessage handleGetAllUsers() {
        if (!isLoggedIn()) return requireLogin(Action.GET_ALL_USERS);

        List<User> users = userService.getAllUsers();

        return SocketMessage.ok(Action.GET_ALL_USERS, "OK")
                .put("users", users);
    }

    private SocketMessage handleUpdateUser(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.UPDATE_USER);

        User user = (User) msg.get("user");

        try {
            boolean ok = userService.updateUser(user);

            return ok
                    ? SocketMessage.ok(Action.UPDATE_USER, "Cập nhật thành công")
                    : SocketMessage.error(Action.UPDATE_USER, "Cập nhật thất bại");

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.UPDATE_USER, e.getMessage());
        }
    }

    private SocketMessage handleDeleteUser(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.DELETE_USER);

        String userId = msg.getString("userId");

        try {
            boolean ok = userService.deleteUser(userId);

            return ok
                    ? SocketMessage.ok(Action.DELETE_USER, "Xóa user thành công")
                    : SocketMessage.error(Action.DELETE_USER, "Xóa thất bại");

        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.DELETE_USER, e.getMessage());
        }
    }

    private SocketMessage handleDepositUser(SocketMessage msg) {
        if (!isLoggedIn()) return requireLogin(Action.DEPOSIT_USER);

        String userId = msg.getString("userId");
        double amount = msg.getDouble("amount");

        if (userId == null || userId.isBlank()) {
            return SocketMessage.error(Action.DEPOSIT_USER, "Thieu userId");
        }

        if (!currentUser.getId().equals(userId)) {
            return SocketMessage.error(Action.DEPOSIT_USER, "Khong the nap tien cho tai khoan khac");
        }

        try {
            boolean ok = userService.deposit(userId, amount);
            return ok
                    ? SocketMessage.ok(Action.DEPOSIT_USER, "Nap tien thanh cong")
                    .put("user", userService.getUserById(userId))
                    : SocketMessage.error(Action.DEPOSIT_USER, "Nap tien that bai");
        } catch (IllegalArgumentException e) {
            return SocketMessage.error(Action.DEPOSIT_USER, e.getMessage());
        }
    }
    // ══════════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════════

    /**
     * Gửi response về client, thread-safe
     */
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
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {
        }
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        System.out.println("[Server] Client ngắt kết nối: " + socket.getRemoteSocketAddress());
    }
}
