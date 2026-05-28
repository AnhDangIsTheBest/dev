package com.auction.server.network;

import com.auction.server.exception.AuthenticationException;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.server.service.BidService;
import com.auction.server.service.ItemService;
import com.auction.server.service.UserService;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;
import com.auction.shared.network.protocol.SocketMessage;
import com.auction.shared.network.protocol.SocketMessage.Action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientHandlerTest {
    private FakeServer server;
    private FakeAuctionService auctionService;
    private FakeBidService bidService;
    private FakeAuthService authService;
    private FakeUserService userService;
    private FakeItemService itemService;
    private ClientHandler handler;

    @BeforeEach
    void setUp() {
        server = new FakeServer();
        auctionService = new FakeAuctionService();
        bidService = new FakeBidService();
        authService = new FakeAuthService();
        userService = new FakeUserService();
        itemService = new FakeItemService();
        handler = new ClientHandler(new Socket(), server, auctionService, bidService, authService, userService, itemService);
    }

    @Test
    void authActionsCoverSuccessAndValidationPaths() throws Exception {
        SocketMessage missingLogin = handle(SocketMessage.request(Action.LOGIN));
        assertFalse(missingLogin.isOk());

        SocketMessage login = handle(SocketMessage.request(Action.LOGIN)
                .put("username", "ann")
                .put("password", "secret"));
        assertTrue(login.isOk());
        assertSame(authService.user, handler.getCurrentUser());

        authService.failLogin = true;
        SocketMessage badLogin = handle(SocketMessage.request(Action.LOGIN)
                .put("username", "ann")
                .put("password", "bad"));
        assertFalse(badLogin.isOk());
        authService.failLogin = false;

        assertFalse(handle(SocketMessage.request(Action.REGISTER)).isOk());
        assertFalse(handle(SocketMessage.request(Action.REGISTER).put("userType", "ADMIN")
                .put("username", "x").put("password", "secret")).isOk());
        assertTrue(handle(register("BIDDER", "new-user")).isOk());
        authService.registerResult = 1;
        assertFalse(handle(register("BIDDER", "dup-user")).isOk());
        authService.registerResult = 3;
        assertFalse(handle(register("SELLER", "dup-email")).isOk());
        authService.registerResult = 2;
        assertFalse(handle(register("SELLER", "db-error")).isOk());

        SocketMessage logout = handle(SocketMessage.request(Action.LOGOUT));
        assertTrue(logout.isOk());
        assertEquals(null, handler.getCurrentUser());
    }

    @Test
    void auctionBidItemAndUserActionsDispatchToServices() throws Exception {
        login();

        assertTrue(handle(SocketMessage.request(Action.GET_ALL_AUCTIONS)).isOk());
        assertTrue(handle(SocketMessage.request(Action.GET_MY_SELLER_AUCTIONS)).isOk());
        assertFalse(handle(SocketMessage.request(Action.GET_AUCTION)).isOk());
        assertTrue(handle(SocketMessage.request(Action.GET_AUCTION).put("auctionId", "A1")).isOk());
        assertEquals("A1", handler.getAuctionIdWatching());

        Item item = auctionService.auction.getItem();
        assertFalse(handle(SocketMessage.request(Action.CREATE_AUCTION)).isOk());
        assertTrue(handle(SocketMessage.request(Action.CREATE_AUCTION)
                .put("item", item)
                .put("startTime", LocalDateTime.now())
                .put("endTime", LocalDateTime.now().plusHours(1))
                .put("antiSnipingEnabled", true)
                .put("snipeWindowSeconds", 30)
                .put("snipeExtendSeconds", 60)).isOk());
        assertTrue(handle(SocketMessage.request(Action.START_AUCTION).put("auctionId", "A1")).isOk());
        assertTrue(handle(SocketMessage.request(Action.FINISH_AUCTION).put("auctionId", "A1")).isOk());
        assertEquals(1, server.broadcastExceptCalls);
        assertTrue(handle(SocketMessage.request(Action.CANCEL_AUCTION).put("auctionId", "A1")).isOk());
        assertTrue(handle(SocketMessage.request(Action.DELETE_AUCTION).put("auctionId", "A1")).isOk());

        assertFalse(handle(SocketMessage.request(Action.PLACE_BID)).isOk());
        assertTrue(handle(SocketMessage.request(Action.PLACE_BID).put("auctionId", "A1").put("amount", 150.0)).isOk());
        assertEquals(2, server.broadcastExceptCalls);
        bidService.bidId = null;
        assertFalse(handle(SocketMessage.request(Action.PLACE_BID).put("auctionId", "A1").put("amount", 160.0)).isOk());
        bidService.bidId = "BID-1";
        bidService.throwBid = true;
        assertFalse(handle(SocketMessage.request(Action.PLACE_BID).put("auctionId", "A1").put("amount", 170.0)).isOk());
        bidService.throwBid = false;

        assertFalse(handle(SocketMessage.request(Action.REGISTER_AUTO_BID).put("maxBid", 200.0)).isOk());
        assertTrue(handle(SocketMessage.request(Action.REGISTER_AUTO_BID)
                .put("auctionId", "A1").put("maxBid", 200.0).put("increment", 10.0)).isOk());
        assertTrue(handle(SocketMessage.request(Action.CANCEL_AUTO_BID).put("auctionId", "A1")).isOk());

        assertTrue(handle(SocketMessage.request(Action.CREATE_ITEM).put("item", item)).isOk());
        assertTrue(handle(SocketMessage.request(Action.UPDATE_ITEM).put("item", item)).isOk());
        assertTrue(handle(SocketMessage.request(Action.DELETE_ITEM).put("itemId", "ITEM-1")).isOk());
        assertTrue(handle(SocketMessage.request(Action.GET_ITEMS_BY_SELLER)).isOk());

        assertTrue(handle(SocketMessage.request(Action.GET_ALL_USERS)).isOk());
        assertTrue(handle(SocketMessage.request(Action.UPDATE_USER).put("user", authService.user)).isOk());
        assertTrue(handle(SocketMessage.request(Action.DELETE_USER).put("userId", "BIDDER")).isOk());
        assertFalse(handle(SocketMessage.request(Action.DEPOSIT_USER).put("userId", "OTHER").put("amount", 10.0)).isOk());
        assertTrue(handle(SocketMessage.request(Action.DEPOSIT_USER).put("userId", "BIDDER").put("amount", 10.0)).isOk());
        assertFalse(handle(SocketMessage.request(Action.SUCCESS)).isOk());
    }

    @Test
    void protectedActionsRequireLogin() throws Exception {
        assertFalse(handle(SocketMessage.request(Action.GET_MY_SELLER_AUCTIONS)).isOk());
        assertFalse(handle(SocketMessage.request(Action.CREATE_AUCTION)).isOk());
        assertFalse(handle(SocketMessage.request(Action.START_AUCTION)).isOk());
        assertFalse(handle(SocketMessage.request(Action.FINISH_AUCTION)).isOk());
        assertFalse(handle(SocketMessage.request(Action.CANCEL_AUCTION)).isOk());
        assertFalse(handle(SocketMessage.request(Action.DELETE_AUCTION)).isOk());
        assertFalse(handle(SocketMessage.request(Action.PLACE_BID)).isOk());
        assertFalse(handle(SocketMessage.request(Action.REGISTER_AUTO_BID)).isOk());
        assertFalse(handle(SocketMessage.request(Action.CANCEL_AUTO_BID)).isOk());
        assertFalse(handle(SocketMessage.request(Action.GET_MY_BIDS)).isOk());
        assertFalse(handle(SocketMessage.request(Action.CREATE_ITEM)).isOk());
        assertFalse(handle(SocketMessage.request(Action.UPDATE_ITEM)).isOk());
        assertFalse(handle(SocketMessage.request(Action.DELETE_ITEM)).isOk());
        assertFalse(handle(SocketMessage.request(Action.GET_ITEMS_BY_SELLER)).isOk());
        assertFalse(handle(SocketMessage.request(Action.GET_ALL_USERS)).isOk());
        assertFalse(handle(SocketMessage.request(Action.UPDATE_USER)).isOk());
        assertFalse(handle(SocketMessage.request(Action.DELETE_USER)).isOk());
        assertFalse(handle(SocketMessage.request(Action.DEPOSIT_USER)).isOk());
    }

    private void login() throws Exception {
        handle(SocketMessage.request(Action.LOGIN).put("username", "ann").put("password", "secret"));
    }

    private SocketMessage register(String userType, String username) {
        return SocketMessage.request(Action.REGISTER)
                .put("userType", userType)
                .put("username", username)
                .put("Email", username + "@example.test")
                .put("password", "secret")
                .put("fullname", "User " + username);
    }

    private SocketMessage handle(SocketMessage request) throws Exception {
        Method method = ClientHandler.class.getDeclaredMethod("handle", SocketMessage.class);
        method.setAccessible(true);
        return (SocketMessage) method.invoke(handler, request);
    }

    private static Auction auction() {
        Electronics item = new Electronics("ITEM-1", "Laptop", "Desc", 100.0, "NEW", 100.0,
                "Dell", 24, "G15");
        item.setSellerId("SELLER");
        Auction auction = new Auction("A1", item, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1), "Ann", "BIDDER", false, 30, 60);
        auction.setStatus(AuctionStatus.RUNNING);
        return auction;
    }

    private static final class FakeServer extends AuctionServer {
        int broadcastExceptCalls;

        private FakeServer() {
            super(0);
        }

        @Override
        public void broadcastToWatchersExcept(String auctionId, SocketMessage msg, ClientHandler excludedClient) {
            broadcastExceptCalls++;
        }
    }

    private static final class FakeAuthService extends AuthService {
        final User user = new Bidder("BIDDER", "ann", "ann@example.test", "Ann", "secret", 100.0, 0, 0);
        boolean failLogin;
        int registerResult;

        @Override
        public User login(String username, String password) throws AuthenticationException {
            return failLogin ? null : user;
        }

        @Override
        public int registerBidder(String username, String email, String password, String fullName) {
            return registerResult;
        }

        @Override
        public int registerSeller(String username, String email, String password, String fullName) {
            return registerResult;
        }
    }

    private static final class FakeAuctionService extends AuctionService {
        final Auction auction = auction();

        @Override public List<Auction> getAllAuctions() { return List.of(auction); }
        @Override public List<Auction> getAuctionsBySeller(String sellerId) { return List.of(auction); }
        @Override public Auction getAuctionById(String auctionId) { return "A1".equals(auctionId) ? auction : null; }
        @Override public String createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime,
                boolean antiSnipingEnabled, int snipeWindowSeconds, int snipeExtendSeconds) { return "A2"; }
        @Override public boolean startAuction(String auctionId) { return true; }
        @Override public boolean finishAuction(String auctionId) { return true; }
        @Override public boolean cancelAuction(String auctionId) { return true; }
        @Override public boolean deleteAuction(String auctionId) { return true; }
    }

    private static final class FakeBidService extends BidService {
        String bidId = "BID-1";
        boolean throwBid;

        @Override
        public String placeManualBid(String auctionId, String bidderId, String bidderName, double amount) {
            if (throwBid) throw new IllegalArgumentException("bad bid");
            return bidId;
        }

        @Override public boolean registerAutoBid(String auctionId, String bidderId, String bidderName, double maxBid, double increment) { return true; }
        @Override public boolean cancelAutoBid(String auctionId, String bidderId) { return true; }
    }

    private static final class FakeUserService extends UserService {
        final User user = new Bidder("BIDDER", "ann", "ann@example.test", "Ann", "secret", 110.0, 1, 0);

        @Override public List<User> getAllUsers() { return List.of(user); }
        @Override public User getUserById(String userId) { return user; }
        @Override public boolean updateUser(User user) { return true; }
        @Override public boolean deleteUser(String userId) { return true; }
        @Override public boolean deposit(String id, double amount) { return true; }
    }

    private static final class FakeItemService extends ItemService {
        final Item item = auction().getItem();

        @Override public boolean createItem(Item item, String sellerId) { return true; }
        @Override public boolean updateItem(Item item) { return true; }
        @Override public boolean deleteItem(String itemId) { return true; }
        @Override public List<Item> getItemsBySeller(String sellerId) { return List.of(item); }
    }
}
