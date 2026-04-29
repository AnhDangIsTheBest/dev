package com.auction.test;

import com.auction.dao.UserDAO;
import com.auction.model.User.Admin;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.model.User.User;

import java.util.List;

public class TestUserDAO {
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        String suffix = String.valueOf(System.currentTimeMillis());

        String bidderId = "BID" + suffix.substring(suffix.length() - 8);
        String sellerId = "SEL" + suffix.substring(suffix.length() - 8);
        String adminId = "ADM" + suffix.substring(suffix.length() - 8);

        String bidderUsername = "dang_bidder_" + suffix;
        String sellerUsername = "dang_seller_" + suffix;
        String adminUsername = "dang_admin_" + suffix;

        System.out.println("===== TEST USER DAO =====");

        User bidder = new Bidder(
                bidderId,
                bidderUsername,
                bidderUsername + "@test.com",
                "Dang Bidder",
                "123456",
                1_000_000,
                0,
                0
        );

        User seller = new Seller(
                sellerId,
                sellerUsername,
                sellerUsername + "@test.com",
                "123456",
                "Dang Seller",
                0,
                0
        );

        User admin = new Admin(
                adminId,
                adminUsername,
                adminUsername + "@test.com",
                "123456",
                "Dang Admin",
                "SUPER"
        );

        System.out.println("\n--- 1. INSERT ---");
        System.out.println("Insert bidder: " + dao.insert(bidder));
        System.out.println("Insert seller: " + dao.insert(seller));
        System.out.println("Insert admin : " + dao.insert(admin));

        System.out.println("\n--- 2. FIND BY ID ---");
        User found = dao.findById(bidderId);
        System.out.println(found != null ? found.display() + " | ID = " + found.getId() : "Not found");

        System.out.println("\n--- 3. LOGIN ---");
        User loginUser = dao.login(bidderUsername, "123456");
        System.out.println(loginUser != null ? "Login OK: " + loginUser.display() : "Login failed");

        System.out.println("\n--- 4. GET ALL ---");
        List<User> users = dao.getAll();
        System.out.println("Total users: " + users.size());
        for (User u : users) {
            System.out.println("  " + u.display() + " | ID = " + u.getId());
        }

        System.out.println("\n--- 5. UPDATE USER ---");
        User updatedBidder = new Bidder(
                bidderId,
                bidderUsername + "_updated",
                bidderUsername + "_updated@test.com",
                "Dang Bidder Updated",
                "999999",
                2_000_000,
                5,
                1
        );
        System.out.println("Update: " + dao.update(updatedBidder));

        System.out.println("\n--- 6. LOGIN AFTER UPDATE ---");
        User updatedLogin = dao.login(bidderUsername + "_updated", "999999");
        System.out.println(updatedLogin != null ? "Login OK: " + updatedLogin.display() : "Login failed");

        System.out.println("\n--- 7. DELETE TEST USERS ---");
        System.out.println("Delete bidder: " + dao.delete(bidderId));
        System.out.println("Delete seller: " + dao.delete(sellerId));
        System.out.println("Delete admin : " + dao.delete(adminId));

        System.out.println("\n===== DONE =====");
    }
}
