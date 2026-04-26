package com.campus.languageexchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallSignalingHandler extends TextWebSocketHandler {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public CallSignalingHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put("userId", userId);
        activeSessions.put(userId, session);
        sendToSession(session, Map.of("type", "connected", "userId", userId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromUserId = sessionUserId(session);
        if (fromUserId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), MAP_TYPE);
        Long targetUserId = parseLong(payload.get("targetUserId"));
        String type = String.valueOf(payload.getOrDefault("type", ""));

        if (targetUserId == null || type.isBlank()) {
            sendError(session, "信令消息缺少必要字段");
            return;
        }

        WebSocketSession targetSession = activeSessions.get(targetUserId);
        if (targetSession == null || !targetSession.isOpen()) {
            sendError(session, "对方当前不在线，无法建立通话");
            return;
        }

        Map<String, Object> forwardPayload = new LinkedHashMap<>(payload);
        forwardPayload.put("fromUserId", fromUserId);
        sendToSession(targetSession, forwardPayload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUserId(session);
        if (userId != null) {
            activeSessions.remove(userId, session);
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendToSession(session, Map.of("type", "error", "message", message));
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (!session.isOpen()) return;
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private Long extractUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return null;
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "userId".equals(parts[0])) {
                return parseLong(parts[1]);
            }
        }
        return null;
    }

    private Long sessionUserId(WebSocketSession session) {
        Object value = Optional.ofNullable(session.getAttributes().get("userId")).orElse(null);
        return parseLong(value);
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
