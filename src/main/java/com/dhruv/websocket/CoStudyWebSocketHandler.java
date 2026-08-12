package com.dhruv.websocket;

import com.dhruv.dto.CoStudyRoomStateDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Presence tracking for the silent co-study room.
 *
 * <p>Changes from the original: the occupant count is derived from the live session map
 * rather than an {@link java.util.concurrent.atomic.AtomicInteger} pre-seeded to 142 —
 * which reported a fabricated audience and drifted permanently downward, eventually
 * negative, because a close that never had a matching open still decremented it.
 * Broadcast payloads are serialised by Jackson instead of string concatenation.
 */
@Component
public class CoStudyWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CoStudyWebSocketHandler.class);

    private static final String ROOM_ID = "NEET-REPEATERS-ROOM-1";
    private static final String PHASE = "FOCUS_50MIN";
    private static final int FOCUS_SECONDS_REMAINING = 2400;

    private static final int SEND_TIMEOUT_MS = 5_000;
    private static final int SEND_BUFFER_BYTES = 512 * 1024;

    /** Session id to a decorated session that is safe to write to from many threads. */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public CoStudyWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIMEOUT_MS, SEND_BUFFER_BYTES));
        broadcastPresence();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (!"PING".equalsIgnoreCase(message.getPayload())) {
            return; // the protocol has exactly one client-to-server message
        }
        WebSocketSession target = sessions.get(session.getId());
        if (target == null || !target.isOpen()) {
            return;
        }
        try {
            target.sendMessage(new TextMessage("PONG:" + sessions.size()));
        } catch (IOException e) {
            log.debug("Dropping PONG for closed session {}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        broadcastPresence();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WebSocket transport error on session {}: {}", session.getId(), exception.toString());
        sessions.remove(session.getId());
    }

    /** Snapshot of the room, shared by the REST endpoint and the broadcast payload. */
    public CoStudyRoomStateDto currentRoomState() {
        return new CoStudyRoomStateDto(ROOM_ID, sessions.size(), FOCUS_SECONDS_REMAINING, true, PHASE);
    }

    public int getActiveCount() {
        return sessions.size();
    }

    private void broadcastPresence() {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new PresenceUpdate(
                    sessions.size(), FOCUS_SECONDS_REMAINING, PHASE));
        } catch (JsonProcessingException e) {
            log.error("Could not serialise presence update", e);
            return;
        }

        TextMessage message = new TextMessage(payload);
        sessions.forEach((id, session) -> {
            if (!session.isOpen()) {
                sessions.remove(id);
                return;
            }
            try {
                session.sendMessage(message);
            } catch (Exception e) {
                // A send failure means the peer is gone; drop it rather than retrying.
                log.debug("Removing unwritable session {}", id);
                sessions.remove(id);
            }
        });
    }

    /**
     * Wire format for a presence broadcast. The discriminator is a component so Jackson
     * emits exactly one {@code type} property; the client switches on it.
     */
    private record PresenceUpdate(String type, int activeCount, int secondsRemaining, String phase) {

        PresenceUpdate(int activeCount, int secondsRemaining, String phase) {
            this("PRESENCE_UPDATE", activeCount, secondsRemaining, phase);
        }
    }
}
