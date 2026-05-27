package com.auction.server.service;

import com.auction.server.dao.UserDAO;
import com.auction.server.exception.AuthenticationException;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;

public class AuthService {
    private final UserDAO userDao;
    public AuthService(){
        this(new UserDAO());
    }

    AuthService(UserDAO userDao) {
        this.userDao = userDao;
    }

    // Đăng nhập
    public User login(String username, String password) throws AuthenticationException{
        if (username == null ||username.isBlank() || password == null || password.isBlank()){
            throw new AuthenticationException("Tài khoản hoặc mật khẩu trống!");
        }
        User user = userDao.login(username,password);
        return user;
    }
    private boolean isUsernameExists(String username) {
        return userDao.existsByUsername(username);
    }
    private boolean isEmailExists(String email) {
        return userDao.existsByEmail(email);
    }
    public int registerBidder(String username, String email, String password, String fullName) {
        try {
            if (isUsernameExists(username)) return 1;
            if (isEmailExists(email)) return 3;

            User newUser = new Bidder(null, username, email, fullName, password, 0.0, 0, 0);
            return userDao.insert(newUser) ? 0 : 2;
        } catch (RuntimeException e) {
            System.err.println("[AuthService] registerBidder lỗi DB: " + e.getMessage());
            return 2;
        }
    }

    // Legacy API: seller registration now creates the same auction account role (BIDDER).
    public int registerSeller(String username,String email, String password,String fullName) {
        return registerBidder(username, email, password, fullName);
    }
}
