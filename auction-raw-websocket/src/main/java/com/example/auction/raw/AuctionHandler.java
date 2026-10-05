package com.example.auction.raw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Raw WebSocket: we own the protocol (JSON envelope {type, ...}), the session registry,
 * the broadcasting and the thread-safety of sends.
 */
@Component
public class AuctionHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AuctionHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final AuctionService service;
    private final ObjectMapper mapper;

    public AuctionHandler(AuctionService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) {
        // Decorator makes concurrent sends safe and bounds slow-consumer buffering.
        var session = new ConcurrentWebSocketSessionDecorator(raw, 5_000, 64 * 1024);
        sessions.put(raw.getId(), session);
        send(session, Map.of("type", "STATE", "data", service.snapshot()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession raw, TextMessage message) throws Exception {
        WebSocketSession session = sessions.get(raw.getId());
        try {
            JsonNode n = mapper.readTree(message.getPayload());
            if (!"BID".equals(n.path("type").asText())) {
                throw new IllegalArgumentException("Unknown message type");
            }
            var state = service.placeBid(n.path("user").asText(), new BigDecimal(n.path("amount").asText()));
            broadcast(Map.of("type", "STATE", "data", state));
        } catch (IllegalArgumentException e) { // includes NumberFormatException
            send(session, Map.of("type", "ERROR", "message", e.getMessage() == null ? "Invalid bid" : e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession raw, CloseStatus status) {
        sessions.remove(raw.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession raw, Throwable ex) {
        log.warn("Transport error on {}: {}", raw.getId(), ex.getMessage());
        sessions.remove(raw.getId());
    }

    private void broadcast(Object payload) {
        sessions.values().forEach(s -> send(s, payload));
    }

    private void send(WebSocketSession s, Object payload) {
        try {
            if (s != null && s.isOpen()) s.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("Send failed for {}: {}", s.getId(), e.getMessage());
        }
    }
}
