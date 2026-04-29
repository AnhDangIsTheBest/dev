package com.auction.service;

import com.auction.dao.UserDAO;
import com.auction.exception.AuthenticationException;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.model.User.User;

public class AuthService {
    private final UserDAO userDao;
    public AuthService(){
        this.userDao = new UserDAO();
    }

    // Đăng nhập
    public User login(String username, String password) throws AuthenticationException{
        if (username == null ||username.isBlank() || password == null || password.isBlank()){
            throw new AuthenticationException("Tài khoản hoặc mật khẩu trống!");
        }
        User user = userDao.login(username,password);
        return user;
    }

    // Kiểm tra xem tên đăng nhập tồn tại chưa
    private  boolean isUsernameExists(String username){
        return userDao.getAll().stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }

    // Xử lí đăng kí cho Bidder
    public boolean registerBidder(String username, String password){
        if (isUsernameExists(username)) return false; // Nếu username tồn tại

        User newUser = new Bidder(null,username,"","",password,0.0,0,0);
        return userDao.insert(newUser);

    }

    // Xử lí  đăng kí cho người bán (Seller)
    public boolean registerSeller(String username, String password){
        if (isUsernameExists(username)) return false;

        User newUser = new Seller(null,username,"",password,"",0,0);
        return userDao.insert(newUser);
    }
}
