package com.dhruv.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CoStudyWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeCount = new AtomicInteger(142);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        int current = activeCount.incrementAndGet();
        broadcastState(current);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("PING".equalsIgnoreCase(message.getPayload())) {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("PONG:" + activeCount.get()));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        int current = activeCount.decrementAndGet();
        broadcastState(current);
    }

    private void broadcastState(int count) {
        String payload = "{\"type\":\"PRESENCE_UPDATE\",\"activeCount\":" + count + ",\"secondsRemaining\":2400,\"phase\":\"FOCUS_50MIN\"}";
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                synchronized (session) {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(payload));
                        }
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    public int getActiveCount() {
        return activeCount.get();
    }
}
