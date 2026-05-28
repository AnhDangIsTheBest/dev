package com.auction.client.controller.ai;

import com.auction.client.ClientContext;
import com.auction.client.network.AuctionClient;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.network.protocol.SocketMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionChatContextBuilderTest {
    @AfterEach
    void tearDown() throws Exception {
        ClientContext.getInstance().setCurrentUser(null);
        setClient(null);
    }

    @Test
    void buildExplainsWhenUserIsNotLoggedIn() throws Exception {
        setClient(null);

        String context = new AuctionChatContextBuilder().build("co gi moi?");

        assertTrue(context.contains("co gi moi?"));
        assertTrue(context.toLowerCase().contains("ng"));
    }

    @Test
    void buildIncludesMarketBidsSellerItemsAndHandlesServiceFailures() throws Exception {
        Bidder user = new Bidder("BIDDER", "ann", "ann@example.test", "Ann", "secret", 1000.0, 2, 1);
        ClientContext.getInstance().setCurrentUser(user);
        FakeAuctionClient client = new FakeAuctionClient();
        setClient(client);

        String context = new AuctionChatContextBuilder().build("toi dang dan dau khong?");

        assertTrue(context.contains("A1"));
        assertTrue(context.contains("Laptop"));
        assertTrue(context.contains("ITEM-1"));
        assertTrue(context.contains("toi dang dan dau khong?"));

        client.failMarket = true;
        client.failMyBids = true;
        client.failItems = true;
        String failureContext = new AuctionChatContextBuilder().build("fail?");
        assertTrue(failureContext.contains("market down") || failureContext.contains("Kh"));
    }

    private static void setClient(AuctionClient client) throws Exception {
        Field field = ClientContext.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(ClientContext.getInstance(), client);
    }

    private static Auction auction(String id, String sellerId, String leadBidderId) {
        Electronics item = new Electronics("ITEM-1", "Laptop", "Gaming laptop", 100.0, "NEW", 100.0,
                "Dell", 24, "G15");
        item.setSellerId(sellerId);
        Auction auction = new Auction(id, item,
                LocalDateTime.of(2026, 5, 28, 9, 0),
                LocalDateTime.of(2026, 5, 28, 10, 0),
                leadBidderId == null ? null : "Ann",
                leadBidderId,
                true,
                30,
                60);
        auction.applyBid(new BidTransaction("B1", id, "BIDDER", "Ann", 150.0,
                LocalDateTime.of(2026, 5, 28, 9, 15), false));
        return auction;
    }

    private static final class FakeAuctionClient extends AuctionClient {
        boolean failMarket;
        boolean failMyBids;
        boolean failItems;

        private FakeAuctionClient() {
            super("test", 1);
        }

        @Override public boolean isConnected() { return true; }

        @Override
        public SocketMessage getAllAuctions() {
            if (failMarket) return SocketMessage.error(SocketMessage.Action.GET_ALL_AUCTIONS, "market down");
            return SocketMessage.ok(SocketMessage.Action.GET_ALL_AUCTIONS, "OK")
                    .put("auctions", List.of(auction("A1", "SELLER", "BIDDER")));
        }

        @Override
        public SocketMessage getMyBids() {
            if (failMyBids) return SocketMessage.error(SocketMessage.Action.GET_MY_BIDS, "bids down");
            return SocketMessage.ok(SocketMessage.Action.GET_MY_BIDS, "OK")
                    .put("auctions", List.of(auction("A1", "SELLER", "BIDDER")))
                    .put("myBestBids", Map.of("A1", 150.0));
        }

        @Override
        public SocketMessage getMyItems() {
            if (failItems) return SocketMessage.error(SocketMessage.Action.GET_ITEMS_BY_SELLER, "items down");
            Item item = auction("A2", "BIDDER", null).getItem();
            return SocketMessage.ok(SocketMessage.Action.GET_ITEMS_BY_SELLER, "OK")
                    .put("items", List.of(item));
        }

        @Override
        public SocketMessage getMySellerAuctions() {
            return SocketMessage.ok(SocketMessage.Action.GET_MY_SELLER_AUCTIONS, "OK")
                    .put("auctions", List.of(auction("A2", "BIDDER", null)));
        }
    }
}
