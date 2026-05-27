package com.auction.server.exception;

public class InvalidBidException extends Exception {
    public InvalidBidException(String message) {
        super(message);
    }
}
