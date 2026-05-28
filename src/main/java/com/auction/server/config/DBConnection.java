package com.auction.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final HikariDataSource ds;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://auction-db-auction-system.a.aivencloud.com:23104/defaultdb?sslMode=REQUIRED");
        config.setUsername("avnadmin");
        config.setPassword("AVNS_H8nWIUDmI3M0krkJZMz");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        // warm up: kết nối sẵn khi app khởi động, không phải khi mở dashboard
        config.setInitializationFailTimeout(10000);
        ds = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection(); // lấy từ pool, gần như tức thì
    }
}