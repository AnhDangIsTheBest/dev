package com.auction.client.network;

import com.auction.shared.network.protocol.SocketMessage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionClientTest {
    @Test
    void requestBuildersReturnConnectionErrorWhenDisconnected() {
        AuctionClient client = new AuctionClient("127.0.0.1", 1);

        assertFalse(client.login("ann", "secret").isOk());
        assertFalse(client.logout().isOk());
        assertFalse(client.register("BIDDER", "ann", "a@example.test", "secret", "Ann").isOk());
        assertFalse(client.getAllAuctions().isOk());
        assertFalse(client.getMySellerAuctions().isOk());
        assertFalse(client.getMyBids().isOk());
        assertFalse(client.getAuction("A1").isOk());
        assertFalse(client.createAuction("item", null, null, false, 0, 0).isOk());
        assertFalse(client.startAuction("A1").isOk());
        assertFalse(client.finishAuction("A1").isOk());
        assertFalse(client.cancelAuction("A1").isOk());
        assertFalse(client.deleteAuction("A1").isOk());
        assertFalse(client.placeBid("A1", 10.0).isOk());
        assertFalse(client.registerAutoBid("A1", 100.0, 10.0).isOk());
        assertFalse(client.cancelAutoBid("A1").isOk());
        assertFalse(client.createItem("item").isOk());
        assertFalse(client.updateItem("item").isOk());
        assertFalse(client.deleteItem("I1").isOk());
        assertFalse(client.getMyItems().isOk());
        assertFalse(client.getAllUsers().isOk());
        assertFalse(client.updateUser("user").isOk());
        assertFalse(client.deleteUser("U1").isOk());
        assertFalse(client.depositUser("U1", 10.0).isOk());

        client.setBroadcastListener(msg -> {});
        client.setGlobalListener(msg -> {});
        assertEquals("127.0.0.1", client.getHost());
        assertEquals(1, client.getPort());
        assertFalse(client.isConnected());
        client.disconnect();
    }

    @Test
    void connectFailsCleanlyAndSendReadsSuccessfulResponse() throws Exception {
        AuctionClient unreachable = new AuctionClient("256.256.256.256", 1);
        assertFalse(unreachable.connect());

        AuctionClient client = new AuctionClient("test", 123);
        AtomicReference<SocketMessage> observed = new AtomicReference<>();
        client.setGlobalListener(observed::set);
        injectObjectStreams(client, SocketMessage.ok(SocketMessage.Action.LOGIN, "ok"));

        SocketMessage response = client.login("ann", "secret");

        assertTrue(response.isOk());
        assertEquals(SocketMessage.Action.LOGIN, observed.get().getAction());
    }

    private static void injectObjectStreams(AuctionClient client, SocketMessage response) throws Exception {
        ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(inputBytes)) {
            objectOutput.writeObject(response);
        }
        set(client, "in", new ObjectInputStream(new ByteArrayInputStream(inputBytes.toByteArray())));
        set(client, "out", new ObjectOutputStream(new ByteArrayOutputStream()));
        set(client, "connected", true);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = AuctionClient.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
