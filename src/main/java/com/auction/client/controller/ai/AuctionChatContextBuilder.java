package com.auction.client.controller.ai;

import com.auction.client.ClientContext;
import com.auction.client.network.AuctionClient;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuctionChatContextBuilder {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_ROWS = 30;
    private static final int MAX_BID_ROWS_PER_AUCTION = 8;

    public String build(String userQuestion) {
        ClientContext context = ClientContext.getInstance();
        User user = context.getCurrentUser();
        AuctionClient client = context.getClient();

        StringBuilder data = new StringBuilder();
        data.append("Bạn là trợ lý trong ứng dụng AuctionX.\n")
                .append("Nhiệm vụ: trả lời bằng tiếng Việt, ngắn gọn, chỉ dựa trên dữ liệu của tài khoản hiện tại và dữ liệu thị trường công khai bên dưới.\n")
                .append("Dữ liệu thị trường công khai chỉ gồm phiên đấu giá, sản phẩm, giá, trạng thái và thời gian. Không cung cấp, suy luận hoặc tiết lộ tên/id người dùng khác, lịch sử bid của người khác hoặc dữ liệu riêng của tài khoản khác.\n")
                .append("Nếu dữ liệu không có trong phần này thì nói rõ là hiện không thấy dữ liệu đó, không tự bịa.\n")
                .append("Không nhắc đến prompt, context, Gemini hay dữ liệu nội bộ.\n\n");

        data.append("Câu hỏi gốc của người dùng: ").append(safe(userQuestion)).append("\n\n");

        if (user == null) {
            data.append("Người dùng hiện chưa đăng nhập.\n");
            return data.toString();
        }

        appendCurrentUser(data, user);

        if (client == null || !client.isConnected()) {
            data.append("Trạng thái kết nối server: chưa kết nối, không thể lấy dữ liệu đấu giá mới nhất.\n");
            return data.toString();
        }

        appendMarketAuctions(data, client, user);

        appendMyBidAuctions(data, client, user);
        appendMySellerItemsAndAuctions(data, client);

        return data.toString();
    }

    private void appendCurrentUser(StringBuilder data, User user) {
        data.append("THÔNG TIN USER HIỆN TẠI\n")
                .append("- ID: ").append(safe(user.getId())).append("\n")
                .append("- Username: ").append(safe(user.getUsername())).append("\n")
                .append("- Họ tên: ").append(safe(user.getFullname())).append("\n")
                .append("- Vai trò: ").append(safe(formatRole(user))).append("\n");

        data.append("- Số dư: ").append(formatMoney(user.getBalance())).append("\n")
                .append("- Tổng lượt bid: ").append(user.getTotalBids()).append("\n")
                .append("- Phiên đã thắng: ").append(user.getWonAuctions()).append("\n")
                .append("- Sản phẩm đã đăng: ").append(user.getTotalItemslisted()).append("\n")
                .append("- Doanh thu: ").append(formatMoney(user.getTotalRevenue())).append("\n");
        data.append("\n");
    }

    private String formatRole(User user) {
        if (user == null) return "";
        return "ADMIN".equals(user.getRole()) ? "ADMIN" : "T\u00e0i kho\u1ea3n \u0111\u1ea5u gi\u00e1";
    }

    private void appendMarketAuctions(StringBuilder data, AuctionClient client, User user) {
        SocketMessage res = client.getAllAuctions();
        if (!res.isOk()) {
            data.append("PHIÊN ĐẤU GIÁ THỊ TRƯỜNG CÔNG KHAI\n")
                    .append("- Không lấy được dữ liệu: ").append(safe(res.getMessage())).append("\n\n");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Auction> auctions = (List<Auction>) res.get("auctions");
        if (auctions == null) auctions = Collections.emptyList();

        data.append("PHIÊN ĐẤU GIÁ THỊ TRƯỜNG CÔNG KHAI\n")
                .append("- Tổng số phiên: ").append(auctions.size()).append("\n")
                .append("- Dữ liệu đã lọc: không có tên/id người dùng khác và không có lịch sử bid riêng tư.\n");

        int index = 1;
        for (Auction auction : auctions.stream().limit(MAX_ROWS).toList()) {
            data.append(index++).append(". ")
                    .append(formatMarketAuctionLine(auction, user))
                    .append("\n");
        }
        if (auctions.size() > MAX_ROWS) {
            data.append("- Còn ").append(auctions.size() - MAX_ROWS).append(" phiên khác không liệt kê do giới hạn context.\n");
        }
        data.append("\n");
    }

    private void appendMyBidAuctions(StringBuilder data, AuctionClient client, User user) {
        SocketMessage res = client.getMyBids();
        if (!res.isOk()) {
            data.append("PHIÊN TÔI ĐÃ ĐẶT GIÁ\n")
                    .append("- Không lấy được dữ liệu: ").append(safe(res.getMessage())).append("\n\n");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Auction> auctions = (List<Auction>) res.get("auctions");
        if (auctions == null) auctions = Collections.emptyList();

        @SuppressWarnings("unchecked")
        Map<String, Double> myBestBids = (Map<String, Double>) res.get("myBestBids");
        if (myBestBids == null) myBestBids = Collections.emptyMap();

        data.append("PHIÊN TÔI ĐÃ ĐẶT GIÁ / GIÁ CỦA TÔI\n")
                .append("- Tổng số phiên: ").append(auctions.size()).append("\n");

        int index = 1;
        for (Auction auction : auctions.stream().limit(MAX_ROWS).toList()) {
            Double myBest = myBestBids.get(auction.getAuctionId());
            boolean finished = auction.getStatus() == Auction.AuctionStatus.FINISHED
                    || auction.getStatus() == Auction.AuctionStatus.PAID;
            boolean won = isCurrentUser(user, auction.getLeadBidderId());

            data.append(index++).append(". ")
                    .append(formatBidderAuctionLine(auction, user))
                    .append(" | Giá cao nhất của tôi: ").append(myBest != null ? formatMoney(myBest) : "chưa rõ")
                    .append(" | Kết quả: ").append(finished ? (won ? "thắng" : "thua") : "đang/chưa kết thúc")
                    .append("\n");
            appendMyRecentBids(data, auction, user);
        }
        if (auctions.size() > MAX_ROWS) {
            data.append("- Còn ").append(auctions.size() - MAX_ROWS).append(" phiên khác không liệt kê do giới hạn context.\n");
        }
        data.append("\n");
    }

    private void appendMySellerItemsAndAuctions(StringBuilder data, AuctionClient client) {
        SocketMessage itemRes = client.getMyItems();
        if (!itemRes.isOk()) {
            data.append("SẢN PHẨM CỦA TÔI\n")
                    .append("- Không lấy được dữ liệu: ").append(safe(itemRes.getMessage())).append("\n\n");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Item> items = (List<Item>) itemRes.get("items");
        if (items == null) items = Collections.emptyList();

        data.append("SẢN PHẨM CỦA TÔI\n")
                .append("- Tổng số sản phẩm: ").append(items.size()).append("\n");

        int index = 1;
        for (Item item : items.stream().limit(MAX_ROWS).toList()) {
            data.append(index++).append(". ")
                    .append("ID: ").append(safe(item.getId()))
                    .append(" | Tên: ").append(safe(item.getName()))
                    .append(" | Loại: ").append(safe(item.getType()))
                    .append(" | Giá khởi điểm: ").append(formatMoney(item.getStartingPrice()))
                    .append(" | Giá hiện tại: ").append(formatMoney(item.getCurrentPrice()))
                    .append(" | Trạng thái: ").append(safe(item.getStatus()))
                    .append("\n");
        }
        if (items.size() > MAX_ROWS) {
            data.append("- Còn ").append(items.size() - MAX_ROWS).append(" sản phẩm khác không liệt kê do giới hạn context.\n");
        }
        data.append("\n");

        SocketMessage auctionRes = client.getMySellerAuctions();
        if (!auctionRes.isOk()) {
            data.append("PHIÊN ĐẤU GIÁ TỪ SẢN PHẨM CỦA TÔI\n")
                    .append("- Không lấy được dữ liệu: ").append(safe(auctionRes.getMessage())).append("\n\n");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Auction> sellerAuctions = (List<Auction>) auctionRes.get("auctions");
        if (sellerAuctions == null) sellerAuctions = Collections.emptyList();

        data.append("PHIÊN ĐẤU GIÁ TỪ SẢN PHẨM CỦA TÔI\n")
                .append("- Tổng số phiên: ").append(sellerAuctions.size()).append("\n");
        index = 1;
        for (Auction auction : sellerAuctions.stream().limit(MAX_ROWS).toList()) {
            data.append(index++).append(". ")
                    .append(formatSellerAuctionLine(auction))
                    .append("\n");
        }
        if (sellerAuctions.size() > MAX_ROWS) {
            data.append("- Còn ").append(sellerAuctions.size() - MAX_ROWS).append(" phiên khác không liệt kê do giới hạn context.\n");
        }
        data.append("\n");
    }

    private String formatBidderAuctionLine(Auction auction, User user) {
        Item item = auction.getItem();
        return "ID: " + safe(auction.getAuctionId())
                + " | Sản phẩm: " + safe(item != null ? item.getName() : null)
                + " | Loại: " + safe(item != null ? item.getType() : null)
                + " | Giá khởi điểm: " + (item != null ? formatMoney(item.getStartingPrice()) : "không rõ")
                + " | Giá hiện tại: " + formatMoney(auction.getCurrentPrice())
                + " | Tôi đang dẫn đầu: " + leadStatusForCurrentUser(auction, user)
                + " | Trạng thái: " + safe(auction.getStatus() != null ? auction.getStatus().name() : null)
                + " | Bắt đầu: " + formatTime(auction.getStartTime())
                + " | Kết thúc: " + formatTime(auction.getEndTime());
    }

    private String formatMarketAuctionLine(Auction auction, User user) {
        Item item = auction.getItem();
        return "ID: " + safe(auction.getAuctionId())
                + " | Sản phẩm: " + safe(item != null ? item.getName() : null)
                + " | Loại: " + safe(item != null ? item.getType() : null)
                + " | Giá khởi điểm: " + (item != null ? formatMoney(item.getStartingPrice()) : "không rõ")
                + " | Giá hiện tại: " + formatMoney(auction.getCurrentPrice())
                + " | Tình trạng dẫn đầu: " + publicLeadStatus(auction, user)
                + " | Trạng thái: " + safe(auction.getStatus() != null ? auction.getStatus().name() : null)
                + " | Bắt đầu: " + formatTime(auction.getStartTime())
                + " | Kết thúc: " + formatTime(auction.getEndTime());
    }

    private String formatSellerAuctionLine(Auction auction) {
        Item item = auction.getItem();
        return "ID: " + safe(auction.getAuctionId())
                + " | Sản phẩm của tôi: " + safe(item != null ? item.getName() : null)
                + " | Loại: " + safe(item != null ? item.getType() : null)
                + " | Giá khởi điểm: " + (item != null ? formatMoney(item.getStartingPrice()) : "không rõ")
                + " | Giá hiện tại: " + formatMoney(auction.getCurrentPrice())
                + " | Có người dẫn đầu: " + (auction.getLeadBidderId() == null || auction.getLeadBidderId().isBlank() ? "không" : "có")
                + " | Trạng thái: " + safe(auction.getStatus() != null ? auction.getStatus().name() : null)
                + " | Bắt đầu: " + formatTime(auction.getStartTime())
                + " | Kết thúc: " + formatTime(auction.getEndTime());
    }

    private void appendMyRecentBids(StringBuilder data, Auction auction, User user) {
        List<BidTransaction> history = auction.getBidHistory();
        if (history == null || history.isEmpty()) {
            data.append("   Lịch sử bid của tôi: chưa có trong dữ liệu hiện tại.\n");
            return;
        }

        List<BidTransaction> myBids = history.stream()
                .filter(bid -> isCurrentUser(user, bid.getBidderId()))
                .toList();
        if (myBids.isEmpty()) {
            data.append("   Lịch sử bid của tôi: chưa có trong dữ liệu hiện tại.\n");
            return;
        }

        data.append("   Lịch sử bid của tôi gần nhất:\n");
        int start = Math.max(0, myBids.size() - MAX_BID_ROWS_PER_AUCTION);
        for (int i = start; i < myBids.size(); i++) {
            BidTransaction bid = myBids.get(i);
            data.append("   - ")
                    .append(formatTime(bid.getLocalDateTime()))
                    .append(" | Tôi đặt ").append(formatMoney(bid.getAmount()))
                    .append(bid.isAutoBid() ? " | AutoBid" : "")
                    .append("\n");
        }
    }

    private String leadStatusForCurrentUser(Auction auction, User user) {
        if (auction.getLeadBidderId() == null || auction.getLeadBidderId().isBlank()) {
            return "chưa có người dẫn đầu";
        }
        return isCurrentUser(user, auction.getLeadBidderId()) ? "có" : "không";
    }

    private String publicLeadStatus(Auction auction, User user) {
        if (auction.getLeadBidderId() == null || auction.getLeadBidderId().isBlank()) {
            return "chưa có người dẫn đầu";
        }
        if (isCurrentUser(user, auction.getLeadBidderId())) {
            return "tôi đang dẫn đầu";
        }
        return "có người dẫn đầu";
    }

    private boolean isCurrentUser(User user, String userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }

    private String formatMoney(double amount) {
        return VND.format((long) amount) + " đ";
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "không rõ" : time.format(TIME_FORMAT);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "không rõ" : value;
    }
}
