package com.campus.languageexchange;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CallSignalingHandler callSignalingHandler;

    public WebSocketConfig(CallSignalingHandler callSignalingHandler) {
        this.callSignalingHandler = callSignalingHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callSignalingHandler, "/ws/calls")
            .setAllowedOriginPatterns("*");
    }
}
