package com.example.auction.raw;

import java.math.BigDecimal;

public record AuctionState(String item, BigDecimal currentBid, String leader, int bidCount) {}
