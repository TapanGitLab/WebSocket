package com.example.auction.raw;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AuctionHandler handler;

    public WebSocketConfig(AuctionHandler handler) { this.handler = handler; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Same-origin only by default. Add setAllowedOrigins(...) deliberately if needed.
        registry.addHandler(handler, "/ws/auction");
    }
}
