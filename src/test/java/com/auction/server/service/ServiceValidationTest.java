package com.auction.server.service;

import com.auction.server.exception.AuthenticationException;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceValidationTest {
    @Test
    void authServiceLogsInAndValidatesBlankCredentials() throws AuthenticationException {
        FakeUserDAO userDAO = new FakeUserDAO();
        User user = new Bidder("U1", "ann", "ann@example.test", "Ann", "secret", 0.0, 0, 0);
        userDAO.users.put(user.getUsername(), user);
        AuthService service = new AuthService(userDAO);

        assertSame(user, service.login("ann", "secret"));
        assertNull(service.login("ann", "bad"));
        assertThrows(AuthenticationException.class, () -> service.login(" ", "secret"));
        assertThrows(AuthenticationException.class, () -> service.login("ann", ""));
    }

    @Test
    void authServiceRegisterBidderReturnsExpectedCodes() {
        FakeUserDAO userDAO = new FakeUserDAO();
        AuthService service = new AuthService(userDAO);

        assertEquals(0, service.registerBidder("new", "new@example.test", "pw", "New User"));
        assertEquals(1, service.registerBidder("new", "other@example.test", "pw", "Duplicate Username"));
        assertEquals(3, service.registerSeller("seller", "new@example.test", "pw", "Duplicate Email"));

        userDAO.insertResult = false;
        assertEquals(2, service.registerBidder("failed", "failed@example.test", "pw", "Failed Insert"));

        userDAO.usernameFailure = new RuntimeException("db down");
        assertEquals(2, service.registerBidder("error", "error@example.test", "pw", "DB Error"));
    }

    @Test
    void itemServiceCreatesItemAndIncrementsSellerStats() {
        FakeItemDAO itemDAO = new FakeItemDAO();
        FakeUserDAO userDAO = new FakeUserDAO();
        ItemService service = new ItemService(itemDAO, userDAO);
        Item item = TestData.item("ITEM-1", null);

        assertTrue(service.createItem(item, "SELLER"));
        assertSame(item, service.getItemById("ITEM-1"));
        assertEquals(1, service.getAllItems().size());
        assertEquals(1, service.getItemsBySeller("SELLER").size());
        assertEquals(1, userDAO.totalItemIncrements.get("SELLER"));
    }

    @Test
    void itemServiceRejectsInvalidItemsAndIds() {
        FakeItemDAO itemDAO = new FakeItemDAO();
        FakeUserDAO userDAO = new FakeUserDAO();
        ItemService service = new ItemService(itemDAO, userDAO);

        assertThrows(IllegalArgumentException.class, () -> service.createItem(null, "SELLER"));
        assertThrows(IllegalArgumentException.class, () -> service.createItem(TestData.item("ITEM-2", null), " "));
        assertThrows(IllegalArgumentException.class, () -> service.getItemById(""));
        assertThrows(IllegalArgumentException.class, () -> service.getItemsBySeller(null));
        assertThrows(IllegalArgumentException.class, () -> service.deleteItem(" "));

        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(new Electronics("I1", "", "Desc", 10.0, "NEW", 10.0,
                        "Dell", 12, "XPS"), "SELLER"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(new Electronics("I2", "Laptop", "Desc", -1.0, "NEW", 0.0,
                        "Dell", 12, "XPS"), "SELLER"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(new Electronics("I3", "Laptop", "Desc", 1.0, "NEW", -1.0,
                        "Dell", 12, "XPS"), "SELLER"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(new Electronics("I4", "Laptop", "Desc", 1.0, "", 1.0,
                        "Dell", 12, "XPS"), "SELLER"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(new Electronics("", "Laptop", "Desc", 1.0, "NEW", 1.0,
                        "Dell", 12, "XPS")));
    }

    @Test
    void itemServiceUpdatesAndDeletesItems() {
        FakeItemDAO itemDAO = new FakeItemDAO();
        FakeUserDAO userDAO = new FakeUserDAO();
        ItemService service = new ItemService(itemDAO, userDAO);
        Item item = TestData.item("ITEM-3", "SELLER");
        itemDAO.items.put(item.getId(), item);

        item.setName("Updated Laptop");

        assertTrue(service.updateItem(item));
        assertEquals("Updated Laptop", service.getItemById("ITEM-3").getName());
        assertTrue(service.deleteItem("ITEM-3"));
        assertNull(service.getItemById("ITEM-3"));
    }

    @Test
    void userServiceReadsUpdatesDeletesAndDeposits() {
        FakeUserDAO userDAO = new FakeUserDAO();
        User user = new Bidder("U1", "ann", "ann@example.test", "Ann", "secret", 10.0, 0, 0);
        userDAO.users.put(user.getUsername(), user);
        UserService service = new UserService(userDAO);

        assertEquals(1, service.getAllUsers().size());
        assertSame(user, service.getUserById("U1"));
        user.setFullName("Ann Updated");
        assertTrue(service.updateUser(user));
        assertTrue(service.deposit("U1", 5.0));
        assertEquals(15.0, ((Bidder) userDAO.findById("U1")).getBalance());
        assertTrue(service.deleteUser("U1"));
    }

    @Test
    void userServiceRejectsInvalidInput() {
        UserService service = new UserService(new FakeUserDAO());

        assertThrows(IllegalArgumentException.class, () -> service.getUserById(null));
        assertThrows(IllegalArgumentException.class, () -> service.deleteUser(" "));
        assertThrows(IllegalArgumentException.class, () -> service.deposit("U1", 0.0));
        assertThrows(IllegalArgumentException.class, () -> service.updateUser(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateUser(new Bidder("", "ann", "ann@example.test", "Ann", "secret", 0.0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateUser(new Bidder("U1", "", "ann@example.test", "Ann", "secret", 0.0, 0, 0)));
    }
}
