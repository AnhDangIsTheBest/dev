package com.auction.server.network;

import com.auction.shared.network.protocol.SocketMessage;
import com.auction.server.service.AuctionService;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server chính của hệ thống đấu giá.
 *
 * Chạy: new AuctionServer(9090).start();
 *
 * Tính năng:
 * - Chấp nhận nhiều client đồng thời (thread pool)
 * - Broadcast realtime khi có bid mới hoặc phiên kết thúc
 * - Auto-finish auction scheduler (kiểm tra mỗi 30 giây)
 */
public class AuctionServer {

    private static final int DEFAULT_PORT    = 9090;
    private static final int THREAD_POOL_SIZE = 50;   // Tối đa 50 client cùng lúc

    private final int port;
    private final ExecutorService threadPool;
    private final List<ClientHandler> connectedClients; // Thread-safe list
    private final AuctionService auctionService;

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public AuctionServer(int port) {
        this.port             = port;
        this.threadPool       = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.connectedClients = new CopyOnWriteArrayList<>();
        this.auctionService   = new AuctionService();
    }

    // ── Khởi động Server ──────────────────────────────────────────
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("╔══════════════════════════════════════╗");
            System.out.println("║     AUCTION SERVER STARTED           ║");
            System.out.printf ("║     Listening on port: %-4d          ║%n", port);
            System.out.println("╚══════════════════════════════════════╝");

            // Scheduler tự động kết thúc phiên hết giờ
            startAuctionScheduler();

            // Vòng lặp chấp nhận client
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    connectedClients.add(handler);
                    threadPool.submit(handler);
                    System.out.printf("[Server] Client mới: %s | Tổng: %d%n",
                            clientSocket.getRemoteSocketAddress(), connectedClients.size());
                } catch (IOException e) {
                    if (running) System.err.println("[Server] Lỗi accept: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[Server] Không thể khởi động: " + e.getMessage());
        } finally {
            stop();
        }
    }

    // ── Dừng Server ───────────────────────────────────────────────
    public void stop() {
        running = false;
        threadPool.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khi dừng: " + e.getMessage());
        }
        System.out.println("[Server] Đã dừng.");
    }

    // ── Broadcast ─────────────────────────────────────────────────

    /**
     * Gửi message đến TẤT CẢ client đang xem một phiên đấu giá cụ thể.
     * Dùng khi có bid mới hoặc phiên kết thúc.
     */
    public void broadcastToWatchers(String auctionId, SocketMessage msg) {
        int count = 0;
        for (ClientHandler client : connectedClients) {
            if (auctionId.equals(client.getAuctionIdWatching())) {
                client.sendResponse(msg);
                count++;
            }
        }
        System.out.printf("[Server] Broadcast '%s' → %d client đang xem phiên %s%n",
                msg.getAction(), count, auctionId);
    }

    /**
     * Gửi message đến TẤT CẢ client đang kết nối.
     */
    public void broadcastToAll(SocketMessage msg) {
        for (ClientHandler client : connectedClients) {
            client.sendResponse(msg);
        }
        System.out.printf("[Server] Broadcast toàn bộ '%s' → %d clients%n",
                msg.getAction(), connectedClients.size());
    }

    // ── Client management ─────────────────────────────────────────
    public void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.printf("[Server] Client rời đi | Còn lại: %d%n", connectedClients.size());
    }

    public int getClientCount() {
        return connectedClients.size();
    }

    // ── Auto-finish Scheduler ────────────────────────────────────
    /**
     * Chạy mỗi 30 giây, kiểm tra phiên nào hết giờ thì tự động FINISH
     * và broadcast cho các client đang xem.
     */
    private void startAuctionScheduler() {
        Thread scheduler = new Thread(() -> {
            System.out.println("[Scheduler] Bắt đầu kiểm tra phiên đấu giá...");
            while (running) {
                try {
                    Thread.sleep(30_000); // 30 giây
                    checkAndFinishExpiredAuctions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "AuctionScheduler");
        scheduler.setDaemon(true); // Dừng theo server
        scheduler.start();
    }

    private void checkAndFinishExpiredAuctions() {
        try {
            List<Auction> running = auctionService.getAuctionsByStatus(AuctionStatus.RUNNING);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            for (Auction auction : running) {
                if (now.isAfter(auction.getEndTime())) {
                    boolean finished = auctionService.finishAuction(auction.getAuctionId());
                    if (finished) {
                        System.out.printf("[Scheduler] Tự động kết thúc phiên: %s%n",
                                auction.getAuctionId());

                        Auction updated = auctionService.getAuctionById(auction.getAuctionId());
                        broadcastToWatchers(auction.getAuctionId(),
                                SocketMessage.ok(SocketMessage.Action.BROADCAST_AUCTION_END,
                                        "Phiên đấu giá đã kết thúc!")
                                        .put("auction", updated));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Lỗi: " + e.getMessage());
        }
    }

    // ── Entry point ───────────────────────────────────────────────
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new AuctionServer(port).start();
    }
}
