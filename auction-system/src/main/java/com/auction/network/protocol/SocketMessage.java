package com.auction.network.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Object duy nhất được gửi qua Socket giữa Client và Server.
 * Mọi request/response đều dùng class này.
 */
public class SocketMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    // ── Loại hành động ──────────────────────────────────────────
    public enum Action {
        // Auth
        LOGIN, LOGOUT, REGISTER,

        // Auction
        GET_ALL_AUCTIONS, GET_AUCTION, CREATE_AUCTION,
        START_AUCTION, FINISH_AUCTION, CANCEL_AUCTION, DELETE_AUCTION,

        // Bid
        PLACE_BID, REGISTER_AUTO_BID, CANCEL_AUTO_BID, GET_MY_BIDS,

        // Item
        CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_ITEMS_BY_SELLER,

        // User
        GET_ALL_USERS, UPDATE_USER, DELETE_USER,

        // Server → Client push (realtime)
        BROADCAST_BID_UPDATE,   // Có bid mới
        BROADCAST_AUCTION_END,  // Phiên kết thúc

        // Kết quả
        SUCCESS, ERROR
    }

    // ── Trạng thái response ──────────────────────────────────────
    public enum Status {
        OK, FAIL
    }

    private Action action;
    private Status status;
    private String message;                 // Thông báo lỗi / thành công
    private Map<String, Object> payload;    // Dữ liệu gửi kèm (flexible)

    // ── Constructor ──────────────────────────────────────────────
    public SocketMessage() {
        this.payload = new HashMap<>();
    }

    public SocketMessage(Action action) {
        this.action = action;
        this.payload = new HashMap<>();
    }

    // ── Builder-style helpers ────────────────────────────────────

    /** Tạo request từ client gửi lên server */
    public static SocketMessage request(Action action) {
        return new SocketMessage(action);
    }

    /** Tạo response thành công từ server */
    public static SocketMessage ok(Action action, String message) {
        SocketMessage msg = new SocketMessage(action);
        msg.status = Status.OK;
        msg.message = message;
        return msg;
    }

    /** Tạo response lỗi từ server */
    public static SocketMessage error(Action action, String errorMessage) {
        SocketMessage msg = new SocketMessage(action);
        msg.status = Status.FAIL;
        msg.message = errorMessage;
        return msg;
    }

    /** Thêm dữ liệu vào payload, trả về chính nó để chain */
    public SocketMessage put(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }

    /** Lấy dữ liệu từ payload */
    public Object get(String key) {
        return payload.get(key);
    }

    public String getString(String key) {
        Object val = payload.get(key);
        return val != null ? val.toString() : null;
    }

    public double getDouble(String key) {
        Object val = payload.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) return Double.parseDouble((String) val);
        return 0.0;
    }

    public int getInt(String key) {
        Object val = payload.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return 0;
    }

    public boolean getBoolean(String key) {
        Object val = payload.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return false;
    }

    // ── Getters / Setters ────────────────────────────────────────
    public Action getAction()           { return action; }
    public void setAction(Action a)     { this.action = a; }
    public Status getStatus()           { return status; }
    public void setStatus(Status s)     { this.status = s; }
    public String getMessage()          { return message; }
    public void setMessage(String m)    { this.message = m; }
    public Map<String, Object> getPayload()          { return payload; }
    public void setPayload(Map<String, Object> map)  { this.payload = map; }

    public boolean isOk()   { return status == Status.OK; }
    public boolean isFail() { return status == Status.FAIL; }

    @Override
    public String toString() {
        return String.format("SocketMessage{action=%s, status=%s, message='%s', payload=%s}",
                action, status, message, payload);
    }
}