package com.auction.server.network;

import com.auction.shared.network.protocol.SocketMessage;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionServerTest {
    @Test
    void broadcastsRouteToConnectedWatchersAndClientCountTracksRemoval() throws Exception {
        AuctionServer server = new AuctionServer(0);
        RecordingHandler watcher = new RecordingHandler("A1", server);
        RecordingHandler otherWatcher = new RecordingHandler("A2", server);
        RecordingHandler secondWatcher = new RecordingHandler("A1", server);
        clients(server).add(watcher);
        clients(server).add(otherWatcher);
        clients(server).add(secondWatcher);

        SocketMessage message = SocketMessage.ok(SocketMessage.Action.BROADCAST_BID_UPDATE, "update");
        server.broadcastToWatchers("A1", message);
        assertEquals(1, watcher.responses);
        assertEquals(0, otherWatcher.responses);
        assertEquals(1, secondWatcher.responses);

        server.broadcastToWatchersExcept("A1", message, watcher);
        assertEquals(1, watcher.responses);
        assertEquals(2, secondWatcher.responses);

        server.broadcastToAll(message);
        assertEquals(2, watcher.responses);
        assertEquals(1, otherWatcher.responses);
        assertEquals(3, secondWatcher.responses);
        assertEquals(3, server.getClientCount());

        server.removeClient(otherWatcher);
        assertEquals(2, server.getClientCount());
        server.stop();
    }

    @SuppressWarnings("unchecked")
    private static List<ClientHandler> clients(AuctionServer server) throws Exception {
        Field field = AuctionServer.class.getDeclaredField("connectedClients");
        field.setAccessible(true);
        return (List<ClientHandler>) field.get(server);
    }

    private static final class RecordingHandler extends ClientHandler {
        private final String watching;
        int responses;

        private RecordingHandler(String watching, AuctionServer server) {
            super(new Socket(), server);
            this.watching = watching;
        }

        @Override
        public String getAuctionIdWatching() {
            return watching;
        }

        @Override
        public synchronized void sendResponse(SocketMessage msg) {
            responses++;
        }
    }
}
