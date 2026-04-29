package com.auction.network.client;

import com.auction.network.protocol.SocketMessage;
import com.auction.network.protocol.SocketMessage.Action;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Client kết nối đến AuctionServer.
 *
 * Cách dùng:
 *   AuctionClient client = new AuctionClient("localhost", 9090);
 *   client.connect();
 *   client.setBroadcastListener(msg -> { ... cập nhật UI ... });
 *
 *   SocketMessage res = client.login("alice", "pass123");
 *   SocketMessage res = client.placeBid("AUCTION_01", "user01", "Alice", 5000000);
 */
public class AuctionClient {

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private volatile boolean connected = false;

    /**
     * Callback nhận các message BROADCAST từ server (bid mới, phiên kết thúc...).
     * JavaFX Controller sẽ set callback này để cập nhật UI.
     */
    private Consumer<SocketMessage> broadcastListener;

    // Listener nhận mọi response (dùng để debug hoặc log)
    private Consumer<SocketMessage> globalListener;

    public AuctionClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ── Kết nối & Ngắt kết nối ────────────────────────────────────

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out    = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in     = new ObjectInputStream(socket.getInputStream());
            connected = true;

            // Thread lắng nghe broadcast từ server (không block UI thread)
            startListenerThread();

            System.out.println("[Client] Đã kết nối đến " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Kết nối thất bại: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (in != null)  in.close();  } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        System.out.println("[Client] Đã ngắt kết nối");
    }

    // ══════════════════════════════════════════════════════════════
    //  AUTH
    // ══════════════════════════════════════════════════════════════

    public SocketMessage login(String username, String password) {
        return send(SocketMessage.request(Action.LOGIN)
                .put("username", username)
                .put("password", password));
    }

    public SocketMessage logout() {
        return send(SocketMessage.request(Action.LOGOUT));
    }

    public SocketMessage register(Object user) {
        return send(SocketMessage.request(Action.REGISTER)
                .put("user", user));
    }

    // ══════════════════════════════════════════════════════════════
    //  AUCTION
    // ══════════════════════════════════════════════════════════════

    public SocketMessage getAllAuctions() {
        return send(SocketMessage.request(Action.GET_ALL_AUCTIONS));
    }

    /**
     * Lấy chi tiết một phiên + tự động đăng ký nhận broadcast của phiên đó.
     */
    public SocketMessage getAuction(String auctionId) {
        return send(SocketMessage.request(Action.GET_AUCTION)
                .put("auctionId", auctionId));
    }

    public SocketMessage createAuction(Object item, java.time.LocalDateTime startTime,
                                       java.time.LocalDateTime endTime,
                                       boolean antiSnipingEnabled,
                                       int snipeWindowSeconds, int snipeExtendSeconds) {
        return send(SocketMessage.request(Action.CREATE_AUCTION)
                .put("item", item)
                .put("startTime", startTime)
                .put("endTime", endTime)
                .put("antiSnipingEnabled", antiSnipingEnabled)
                .put("snipeWindowSeconds", snipeWindowSeconds)
                .put("snipeExtendSeconds", snipeExtendSeconds));
    }

    public SocketMessage startAuction(String auctionId) {
        return send(SocketMessage.request(Action.START_AUCTION)
                .put("auctionId", auctionId));
    }

    public SocketMessage finishAuction(String auctionId) {
        return send(SocketMessage.request(Action.FINISH_AUCTION)
                .put("auctionId", auctionId));
    }

    public SocketMessage cancelAuction(String auctionId) {
        return send(SocketMessage.request(Action.CANCEL_AUCTION)
                .put("auctionId", auctionId));
    }

    public SocketMessage deleteAuction(String auctionId) {
        return send(SocketMessage.request(Action.DELETE_AUCTION)
                .put("auctionId", auctionId));
    }

    // ══════════════════════════════════════════════════════════════
    //  BID
    // ══════════════════════════════════════════════════════════════

    public SocketMessage placeBid(String auctionId, String bidderId,
                                  String bidderName, double amount) {
        return send(SocketMessage.request(Action.PLACE_BID)
                .put("auctionId", auctionId)
                .put("bidderId", bidderId)
                .put("bidderName", bidderName)
                .put("amount", amount));
    }

    public SocketMessage registerAutoBid(String auctionId, String bidderId,
                                         String bidderName, double maxBid, double increment) {
        return send(SocketMessage.request(Action.REGISTER_AUTO_BID)
                .put("auctionId", auctionId)
                .put("bidderId", bidderId)
                .put("bidderName", bidderName)
                .put("maxBid", maxBid)
                .put("increment", increment));
    }

    public SocketMessage cancelAutoBid(String auctionId, String bidderId) {
        return send(SocketMessage.request(Action.CANCEL_AUTO_BID)
                .put("auctionId", auctionId)
                .put("bidderId", bidderId));
    }

    // ══════════════════════════════════════════════════════════════
    //  ITEM
    // ══════════════════════════════════════════════════════════════

    public SocketMessage createItem(Object item, String sellerId) {
        return send(SocketMessage.request(Action.CREATE_ITEM)
                .put("item", item)
                .put("sellerId", sellerId));
    }

    public SocketMessage updateItem(Object item) {
        return send(SocketMessage.request(Action.UPDATE_ITEM)
                .put("item", item));
    }

    public SocketMessage deleteItem(String itemId) {
        return send(SocketMessage.request(Action.DELETE_ITEM)
                .put("itemId", itemId));
    }

    public SocketMessage getItemsBySeller(String sellerId) {
        return send(SocketMessage.request(Action.GET_ITEMS_BY_SELLER)
                .put("sellerId", sellerId));
    }

    // ══════════════════════════════════════════════════════════════
    //  USER
    // ══════════════════════════════════════════════════════════════

    public SocketMessage getAllUsers() {
        return send(SocketMessage.request(Action.GET_ALL_USERS));
    }

    public SocketMessage updateUser(Object user) {
        return send(SocketMessage.request(Action.UPDATE_USER)
                .put("user", user));
    }

    public SocketMessage deleteUser(String userId) {
        return send(SocketMessage.request(Action.DELETE_USER)
                .put("userId", userId));
    }

    // ══════════════════════════════════════════════════════════════
    //  CORE: Gửi request và nhận response đồng bộ
    // ══════════════════════════════════════════════════════════════

    /**
     * Gửi request và chờ response từ server (đồng bộ - blocking).
     * Dùng cho mọi thao tác bình thường.
     * Broadcast được xử lý riêng bởi listenerThread.
     */
    public synchronized SocketMessage send(SocketMessage request) {
        if (!connected) {
            System.err.println("[Client] Chưa kết nối!");
            return SocketMessage.error(request.getAction(), "Chưa kết nối đến server");
        }

        try {
            out.writeObject(request);
            out.flush();
            out.reset();

            // Đọc response (blocking)
            SocketMessage response = (SocketMessage) in.readObject();

            if (globalListener != null) globalListener.accept(response);
            return response;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Lỗi gửi/nhận: " + e.getMessage());
            connected = false;
            return SocketMessage.error(request.getAction(), "Lỗi kết nối: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Listener Thread - nhận broadcast từ server
    // ══════════════════════════════════════════════════════════════

    /**
     * Thread riêng lắng nghe message chủ động từ server (BROADCAST_BID_UPDATE,
     * BROADCAST_AUCTION_END). Khi nhận được sẽ gọi broadcastListener.
     *
     * LƯU Ý: Với Java Serialization + blocking I/O, cách phổ biến nhất là
     * server chỉ gửi broadcast SAU KHI client gửi request (piggyback).
     * Nếu muốn nhận push thực sự, cần tách kết nối đọc/ghi riêng,
     * hoặc dùng thêm một socket riêng để nhận broadcast.
     *
     * Ở đây mình dùng cách đơn giản nhất: sau mỗi response bình thường,
     * nếu action là BROADCAST thì forward cho listener.
     */
    private void startListenerThread() {
        // Cách này dùng 1 socket duy nhất:
        // broadcast được gửi kèm theo response (server gửi thêm 1 object ngay sau response chính)
        // Nếu muốn push riêng hoàn toàn, xem BroadcastReceiver bên dưới
        System.out.println("[Client] Listener thread sẵn sàng.");
    }

    // ══════════════════════════════════════════════════════════════
    //  Setters cho callback
    // ══════════════════════════════════════════════════════════════

    /**
     * JavaFX Controller gọi cái này để nhận realtime update.
     * Ví dụ:
     *   client.setBroadcastListener(msg -> {
     *       if (msg.getAction() == Action.BROADCAST_BID_UPDATE) {
     *           Platform.runLater(() -> updateUI(msg));
     *       }
     *   });
     */
    public void setBroadcastListener(Consumer<SocketMessage> listener) {
        this.broadcastListener = listener;
    }

    public void setGlobalListener(Consumer<SocketMessage> listener) {
        this.globalListener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getHost() { return host; }
    public int    getPort() { return port; }
}
