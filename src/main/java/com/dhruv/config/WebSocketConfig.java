package com.dhruv.config;

import com.dhruv.websocket.CoStudyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CoStudyWebSocketHandler coStudyWebSocketHandler;

    public WebSocketConfig(CoStudyWebSocketHandler coStudyWebSocketHandler) {
        this.coStudyWebSocketHandler = coStudyWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(coStudyWebSocketHandler, "/ws/costudy")
                .setAllowedOriginPatterns("*");
    }
}
