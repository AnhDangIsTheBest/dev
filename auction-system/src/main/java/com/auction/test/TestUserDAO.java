package com.auction.test;

import com.auction.dao.UserDAO;
import com.auction.model.User.User;
import com.auction.model.User.Bidder;

import java.util.List;

public class TestUserDAO {
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();

        System.out.println("===== TEST USERDAO =====");

        // 1. Insert
        User newUser = new Bidder("0", "dangtest", "", "", "123456", 0.0, 0, 0);
        boolean inserted = dao.insert(newUser);
        System.out.println("Insert: " + inserted);

        // 2. Get all
        System.out.println("\n===== GET ALL USERS =====");
        List<User> users = dao.getAll();
        for (User u : users) {
            System.out.println(u.display() + " | ID = " + u.getId());
        }

        // 3. Login
        System.out.println("\n===== LOGIN TEST =====");
        User loginUser = dao.login("dangtest", "123456");
        if (loginUser != null) {
            System.out.println("Login thanh cong: " + loginUser.display() + " | ID = " + loginUser.getId());
        } else {
            System.out.println("Login that bai");
        }

        // 4. Tìm id của user vừa tạo để update/delete
        int foundId = -1;
        for (User u : users) {
            if (u.getUsername().equals("dangtest")) {
                foundId = Integer.parseInt(u.getId());
                break;
            }
        }

        // Nếu chưa thấy trong list cũ thì load lại
        if (foundId == -1) {
            users = dao.getAll();
            for (User u : users) {
                if (u.getUsername().equals("dangtest")) {
                    foundId = Integer.parseInt(u.getId());
                    break;
                }
            }
        }

        // 5. Update
        System.out.println("\n===== UPDATE TEST =====");
        if (foundId != -1) {
            boolean updated = dao.update(foundId, "dangupdated", "999999", "BIDDER");
            System.out.println("Update: " + updated);
        } else {
            System.out.println("Khong tim thay ID cua user dangtest de update");
        }

        // 6. Login lại sau update
        System.out.println("\n===== LOGIN AFTER UPDATE =====");
        User updatedLogin = dao.login("dangupdated", "999999");
        if (updatedLogin != null) {
            System.out.println("Login sau update thanh cong: " + updatedLogin.display() + " | ID = " + updatedLogin.getId());
        } else {
            System.out.println("Login sau update that bai");
        }

        // 7. Delete
        System.out.println("\n===== DELETE TEST =====");
        if (updatedLogin != null) {
            int deleteId = Integer.parseInt(updatedLogin.getId());
            boolean deleted = dao.delete(deleteId);
            System.out.println("Delete: " + deleted);
        } else {
            System.out.println("Khong co user de delete");
        }

        // 8. Check lại sau delete
        System.out.println("\n===== CHECK AFTER DELETE =====");
        User deletedCheck = dao.login("dangupdated", "999999");
        if (deletedCheck == null) {
            System.out.println("Delete thanh cong, user khong con trong DB");
        } else {
            System.out.println("Delete that bai, user van con: " + deletedCheck.display());
        }

        System.out.println("\n===== DONE =====");
    }
}