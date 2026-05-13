package com.auction.client;

import com.auction.network.client.AuctionClient;

public final class ClientContext {
    private static final ClientContext INSTANCE = new ClientContext();

    private AuctionClient client = new AuctionClient("localhost", 9090);

    private ClientContext() {
    }

    public static ClientContext getInstance() {
        return INSTANCE;
    }

    public AuctionClient getClient() {
        return client;
    }

    public void setClient(AuctionClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        this.client = client;
    }
}
