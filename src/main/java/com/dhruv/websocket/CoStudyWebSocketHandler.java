package com.dhruv.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CoStudyWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger activeCount = new AtomicInteger(142);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Decorate session to handle concurrent sends safely without Tomcat state conflicts
        WebSocketSession decoratedSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 1024 * 1024);
        sessions.put(session.getId(), decoratedSession);
        int current = activeCount.incrementAndGet();
        broadcastState(current);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("PING".equalsIgnoreCase(message.getPayload())) {
            WebSocketSession decoratedSession = sessions.get(session.getId());
            if (decoratedSession != null && decoratedSession.isOpen()) {
                try {
                    decoratedSession.sendMessage(new TextMessage("PONG:" + activeCount.get()));
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        int current = activeCount.decrementAndGet();
        broadcastState(current);
    }

    private void broadcastState(int count) {
        String payload = "{\"type\":\"PRESENCE_UPDATE\",\"activeCount\":" + count + ",\"secondsRemaining\":2400,\"phase\":\"FOCUS_50MIN\"}";
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception ignored) {}
            }
        }
    }

    public int getActiveCount() {
        return activeCount.get();
    }
}

