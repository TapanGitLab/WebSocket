package com.example.auction.raw;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class AuctionServiceTest {

    @Test
    void rejectsLowBid() {
        var s = new AuctionService();
        assertThrows(IllegalArgumentException.class, () -> s.placeBid("a", new BigDecimal("50")));
    }

    @Test
    void concurrentBidsKeepHighestAndConsistentCount() throws Exception {
        var s = new AuctionService();
        var pool = Executors.newFixedThreadPool(16);
        var latch = new CountDownLatch(1);
        for (int i = 1; i <= 200; i++) {
            final int amt = 100 + i;
            pool.submit(() -> {
                try { latch.await(); s.placeBid("u" + amt, new BigDecimal(amt)); }
                catch (Exception ignored) {}
            });
        }
        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(new BigDecimal("300"), s.snapshot().currentBid());
    }
}
