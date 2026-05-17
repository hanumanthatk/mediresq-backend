package com.smartemergency.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration using STOMP protocol.
 * Enables real-time bidirectional communication for emergency updates.
 * Compatible with Render deployment and all major browsers.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory message broker
        registry.enableSimpleBroker(
            "/topic",   // Broadcast messages (all subscribers)
            "/queue"    // User-specific messages
        );
        // Prefix for messages from client to server
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                // Heartbeat to keep connection alive on Render
                .setHeartbeatTime(25000)
                // Disable session cookie for cross-origin deployments
                .setSessionCookieNeeded(false)
                // Increase disconnect delay for slow connections
                .setDisconnectDelay(30000)
                // Client library URL for SockJS fallback
                .setClientLibraryUrl(
                    "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"
                );
    }
}