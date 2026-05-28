USE
defaultdb;

-- USERS
CREATE TABLE users
(
    id                 VARCHAR(36) PRIMARY KEY,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    email              VARCHAR(100) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    fullname           VARCHAR(100) NOT NULL,
    role               ENUM('ADMIN','BIDDER','SELLER') NOT NULL,
    admin_level        VARCHAR(50)    DEFAULT NULL,
    balance            DECIMAL(15, 2) DEFAULT NULL,
    total_bids         INT            DEFAULT 0,
    won_auctions       INT            DEFAULT 0,
    total_items_listed INT            DEFAULT 0,
    total_revenue      DECIMAL(15, 2) DEFAULT 0,
    created_at         TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- ITEMS
CREATE TABLE items
(
    id             VARCHAR(36) PRIMARY KEY,
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    type           ENUM('ELECTRONICS','VEHICLE','ART','OTHER') NOT NULL,
    starting_price DECIMAL(15, 2) NOT NULL,
    current_price  DECIMAL(15, 2) NOT NULL,
    status         VARCHAR(50)    NOT NULL DEFAULT 'AVAILABLE',
    image_data     LONGBLOB       DEFAULT NULL,
    seller_id      VARCHAR(36)    NOT NULL,
    CONSTRAINT chk_starting_price CHECK (starting_price > 0),
    FOREIGN KEY (seller_id) REFERENCES users (id)
);

CREATE INDEX idx_items_seller_id ON items (seller_id);
CREATE INDEX idx_items_type ON items (type);
CREATE INDEX idx_items_status ON items (status);

-- ITEM_ELECTRONICS
CREATE TABLE item_electronics
(
    item_id  VARCHAR(36) PRIMARY KEY,
    brand    VARCHAR(100),
    model    VARCHAR(100),
    warranty INT,
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

-- ITEM_VEHICLES
CREATE TABLE item_vehicles
(
    item_id       VARCHAR(36) PRIMARY KEY,
    brand         VARCHAR(100),
    vehicle_model VARCHAR(100),
    year          INT,
    mileage       INT,
    vehicle_type  VARCHAR(100),
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

-- ITEM_ARTS
CREATE TABLE item_arts
(
    item_id      VARCHAR(36) PRIMARY KEY,
    artist       VARCHAR(100),
    year_created INT,
    material     VARCHAR(100),
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

-- ITEM_OTHERS
CREATE TABLE item_others
(
    item_id  VARCHAR(36) PRIMARY KEY,
    category VARCHAR(100),
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

-- AUCTIONS
CREATE TABLE auctions
(
    id                   VARCHAR(36) PRIMARY KEY,
    item_id              VARCHAR(36)    NOT NULL,
    current_price        DECIMAL(15, 2) NOT NULL,
    lead_bidder_id       VARCHAR(36)  DEFAULT NULL,
    lead_bidder_name     VARCHAR(100) DEFAULT NULL,
    start_time           DATETIME       NOT NULL,
    end_time             DATETIME       NOT NULL,
    status               ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELED') DEFAULT 'OPEN',
    anti_sniping_enabled TINYINT(1) DEFAULT 0,
    snipe_window_seconds INT          DEFAULT 30,
    snipe_extend_seconds INT          DEFAULT 60,
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auction_time CHECK (end_time > start_time),
    CONSTRAINT chk_current_price CHECK (current_price >= 0),
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    FOREIGN KEY (lead_bidder_id) REFERENCES users (id)
);

CREATE INDEX idx_auctions_status ON auctions (status);
CREATE INDEX idx_auctions_item_id ON auctions (item_id);
CREATE INDEX idx_auctions_lead_bidder ON auctions (lead_bidder_id);

-- BID_TRANSACTIONS
CREATE TABLE bid_transactions
(
    id          VARCHAR(36) PRIMARY KEY,
    auction_id  VARCHAR(36)    NOT NULL,
    bidder_id   VARCHAR(36)    NOT NULL,
    bidder_name VARCHAR(100)   NOT NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    bid_time    DATETIME       NOT NULL,
    is_auto_bid TINYINT(1) DEFAULT 0,
    CONSTRAINT chk_bid_amount CHECK (amount > 0),
    FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users (id)
);

CREATE INDEX idx_bids_auction_id ON bid_transactions (auction_id);
CREATE INDEX idx_bids_bidder_id ON bid_transactions (bidder_id);
CREATE INDEX idx_bids_bid_time ON bid_transactions (bid_time);

-- AUTO_BID_CONFIGS
CREATE TABLE auto_bid_configs
(
    auction_id    VARCHAR(36)    NOT NULL,
    bidder_id     VARCHAR(36)    NOT NULL,
    bidder_name   VARCHAR(100)   NOT NULL,
    max_bid       DECIMAL(15, 2) NOT NULL,
    increment     DECIMAL(15, 2) NOT NULL,
    registered_at BIGINT         NOT NULL,
    PRIMARY KEY (auction_id, bidder_id),
    CONSTRAINT chk_max_bid CHECK (max_bid > 0),
    CONSTRAINT chk_increment CHECK (increment > 0),
    FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users (id)
);

CREATE INDEX idx_autobid_auction_id ON auto_bid_configs (auction_id);
