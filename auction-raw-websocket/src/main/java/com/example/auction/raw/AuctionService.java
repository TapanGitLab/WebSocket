package com.example.auction.raw;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Single in-memory auction. All state changes go through one lock, so bids are serialized. */
@Service
public class AuctionService {

    private final String item = "Vintage Mechanical Keyboard";
    private BigDecimal currentBid = new BigDecimal("100.00");
    private String leader = "-";
    private int bidCount = 0;

    public synchronized AuctionState placeBid(String user, BigDecimal amount) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (amount == null || amount.compareTo(currentBid) <= 0) {
            throw new IllegalArgumentException("Bid must be higher than " + currentBid);
        }
        currentBid = amount;
        leader = user;
        bidCount++;
        return snapshot();
    }

    public synchronized AuctionState snapshot() {
        return new AuctionState(item, currentBid, leader, bidCount);
    }
}
