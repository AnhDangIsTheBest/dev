USE defaultdb;
CREATE TABLE users(
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VaRCHAR(255),
    role ENUM('BIDDER','SELLER','ADMIN'),
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE items(
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    decription TEXT,
    starting_price DOUBLE,
    seller_id INT,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);
CREATE TABLE auctions(
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT,
    start_time DATETIME,
    end_time DATETIME,
    current_price DOUBLE,
    status ENUM('OPEN','RUNNING','FINISHED', 'CANCELED'),
    FOREIGN KEY (item_id) REFERENCES items(id)
);
CREATE TABLE bids(
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT,
    bidder_id INT,
    amount DOUBLE,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);