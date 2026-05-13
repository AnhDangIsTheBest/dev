package com.auction.service;

import com.auction.dao.UserDAO;
import com.auction.model.User.User;

import java.util.List;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public List<User> getAllUsers() {
        return userDAO.getAll();
    }

    public User getUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được trống");
        }

        return userDAO.findById(userId);
    }

    public boolean updateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User không được null");
        }

        if (user.getId() == null || user.getId().isBlank()) {
            throw new IllegalArgumentException("userId không được trống");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username không được trống");
        }

        return userDAO.update(user);
    }

    public boolean deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được trống");
        }

        return userDAO.delete(userId);
    }
}