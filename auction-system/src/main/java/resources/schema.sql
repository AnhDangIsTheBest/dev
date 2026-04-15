USE defaultdb;

CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role ENUM('BIDDER','SELLER','ADMIN') NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE items (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description TEXT,
                       type ENUM('ELECTRONICS','VEHICLE','ART','OTHER') NOT NULL,
                       starting_price DOUBLE NOT NULL,
                       status ENUM('AVAILABLE','IN_AUCTION','SOLD') DEFAULT 'AVAILABLE',
                       seller_id INT NOT NULL,

    -- Electronics
                       brand VARCHAR(100),
                       model VARCHAR(100),
                       warranty INT,

    -- Vehicle
                       vehicle_model VARCHAR(100),
                       year INT,
                       mileage INT,
                       vehicle_type VARCHAR(100),

    -- Art
                       artist VARCHAR(100),
                       year_created INT,
                       material VARCHAR(100),

    -- Other
                       category VARCHAR(100),

                       FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE auctions (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id INT NOT NULL,
                          start_time DATETIME NOT NULL,
                          end_time DATETIME NOT NULL,
                          current_price DOUBLE NOT NULL,
                          status ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELLED') DEFAULT 'OPEN',

                          FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE bids (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      auction_id INT NOT NULL,
                      bidder_id INT NOT NULL,
                      amount DOUBLE NOT NULL,
                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                      FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                      FOREIGN KEY (bidder_id) REFERENCES users(id)
);