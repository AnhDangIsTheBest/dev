package com.auction.client;

import com.auction.shared.model.User.User;
import com.auction.client.network.AuctionClient;

/**
 * Singleton giữ trạng thái client: kết nối socket + user đang đăng nhập.
 * Tất cả controller dùng ClientContext.getInstance() để lấy client/user.
 */
public class ClientContext {

    private static volatile ClientContext instance;

    private AuctionClient client;
    private User currentUser;

    private ClientContext() {}

    public static ClientContext getInstance() {
        if (instance == null) {
            synchronized (ClientContext.class) {
                if (instance == null) instance = new ClientContext();
            }
        }
        return instance;
    }

    // ── AuctionClient ────────────────────────────────────────────

    public AuctionClient getClient() {
        return client;
    }

    /**
     * Khởi tạo và kết nối client. Gọi 1 lần khi app start.
     */
    public boolean connect(String host, int port) {
        client = new AuctionClient(host, port);
        return client.connect();
    }

    public void disconnect() {
        if (client != null) client.disconnect();
    }

    // ── User hiện tại ─────────────────────────────────────────────

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        if (client != null) client.logout();
        currentUser = null;
    }
}
