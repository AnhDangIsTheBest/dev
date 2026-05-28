package com.auction.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static volatile HikariDataSource ds;

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getProperty(
                "auction.db.url",
                "jdbc:mysql://auction-db-auction-system.a.aivencloud.com:23104/defaultdb?sslMode=REQUIRED"));
        config.setUsername(System.getProperty("auction.db.username", "avnadmin"));
        config.setPassword(System.getProperty("auction.db.password", "AVNS_H8nWIUDmI3M0krkJZMz"));
        config.setMaximumPoolSize(Integer.getInteger("auction.db.maximumPoolSize", 10));
        config.setMinimumIdle(Integer.getInteger("auction.db.minimumIdle", 2));
        config.setConnectionTimeout(Long.getLong("auction.db.connectionTimeout", 5000L));
        config.setIdleTimeout(Long.getLong("auction.db.idleTimeout", 300000L));
        config.setInitializationFailTimeout(Long.getLong("auction.db.initializationFailTimeout", 10000L));
        return new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (ds == null) {
            synchronized (DBConnection.class) {
                if (ds == null) {
                    ds = createDataSource();
                }
            }
        }
        return ds.getConnection();
    }

    public static synchronized void configure(String jdbcUrl, String username, String password) {
        HikariDataSource old = ds;
        System.setProperty("auction.db.url", jdbcUrl);
        System.setProperty("auction.db.username", username);
        System.setProperty("auction.db.password", password);
        System.setProperty("auction.db.initializationFailTimeout", "-1");
        ds = createDataSource();
        if (old != null) {
            old.close();
        }
    }
}
