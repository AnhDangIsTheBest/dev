package com.auction.shared.network.protocol;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketMessageTest {
    @Test
    void requestOkAndErrorFactoriesInitializeExpectedState() {
        SocketMessage request = SocketMessage.request(SocketMessage.Action.LOGIN);
        SocketMessage ok = SocketMessage.ok(SocketMessage.Action.SUCCESS, "done");
        SocketMessage error = SocketMessage.error(SocketMessage.Action.ERROR, "bad");

        assertEquals(SocketMessage.Action.LOGIN, request.getAction());
        assertTrue(request.getPayload().isEmpty());

        assertEquals(SocketMessage.Action.SUCCESS, ok.getAction());
        assertEquals(SocketMessage.Status.OK, ok.getStatus());
        assertEquals("done", ok.getMessage());
        assertTrue(ok.isOk());
        assertFalse(ok.isFail());

        assertEquals(SocketMessage.Action.ERROR, error.getAction());
        assertEquals(SocketMessage.Status.FAIL, error.getStatus());
        assertEquals("bad", error.getMessage());
        assertFalse(error.isOk());
        assertTrue(error.isFail());
    }

    @Test
    void payloadHelpersStoreAndCoerceCommonTypes() {
        SocketMessage message = new SocketMessage()
                .put("name", "An")
                .put("number", 12)
                .put("intString", "12")
                .put("decimal", "12.5")
                .put("flag", "true")
                .put("bool", true);

        assertSame(message, message.put("extra", null));
        assertEquals("An", message.get("name"));
        assertEquals("An", message.getString("name"));
        assertNull(message.getString("extra"));
        assertEquals(12.0, message.getDouble("number"));
        assertEquals(12.5, message.getDouble("decimal"));
        assertEquals(12, message.getInt("number"));
        assertEquals(12, message.getInt("intString"));
        assertTrue(message.getBoolean("flag"));
        assertTrue(message.getBoolean("bool"));
        assertEquals(0.0, message.getDouble("missing"));
        assertEquals(0, message.getInt("missing"));
        assertFalse(message.getBoolean("missing"));
    }

    @Test
    void settersReplaceMetadataAndPayload() {
        SocketMessage message = new SocketMessage(SocketMessage.Action.GET_ALL_AUCTIONS);
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", "A1");

        message.setAction(SocketMessage.Action.GET_AUCTION);
        message.setStatus(SocketMessage.Status.OK);
        message.setMessage("loaded");
        message.setPayload(payload);

        assertEquals(SocketMessage.Action.GET_AUCTION, message.getAction());
        assertEquals(SocketMessage.Status.OK, message.getStatus());
        assertEquals("loaded", message.getMessage());
        assertSame(payload, message.getPayload());
        assertTrue(message.toString().contains("GET_AUCTION"));
    }
}
