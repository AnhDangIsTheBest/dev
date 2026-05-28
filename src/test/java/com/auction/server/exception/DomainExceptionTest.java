package com.auction.server.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainExceptionTest {
    @Test
    void domainExceptionsExposeTheirMessages() {
        assertEquals("closed", new AuctionClosedException("closed").getMessage());
        assertEquals("missing", new AuctionNotFoundException("missing").getMessage());
        assertEquals("auth", new AuthenticationException("auth").getMessage());
        assertEquals("bid", new InvalidBidException("bid").getMessage());
    }
}
