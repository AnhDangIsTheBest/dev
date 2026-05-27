package com.auction.server.exception;
// Phiên kết thúc mà vẫn cố đặt giá
public class AuctionClosedException extends Exception{
    public AuctionClosedException(String msg){
        super(msg);
    }
}
