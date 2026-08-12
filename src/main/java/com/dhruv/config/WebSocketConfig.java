package com.dhruv.config;

import com.dhruv.websocket.CoStudyWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CoStudyWebSocketHandler coStudyWebSocketHandler;

    /**
     * Exact origins permitted to open the co-study socket.
     *
     * <p>This was {@code "*"}, which meant any website a signed-in user visited could open
     * a socket to the room. The browser's same-origin policy does not apply to WebSockets,
     * so the server-side origin check is the only control here. A wildcard is rejected at
     * startup under the prod profile; see {@link StartupConfigValidator}.
     */
    @Value("${app.websocket.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public WebSocketConfig(CoStudyWebSocketHandler coStudyWebSocketHandler) {
        this.coStudyWebSocketHandler = coStudyWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addHandler(coStudyWebSocketHandler, "/ws/costudy")
                .setAllowedOrigins(origins);
    }
}
