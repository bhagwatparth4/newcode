package com.cafe.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Only these exact topics exist. Anything else (including wildcards like /topic/**) is refused.
    private static final java.util.Set<String> ADMIN_TOPICS = java.util.Set.of(
            "/topic/admin/orders", "/topic/admin/order-updates", "/topic/admin/requests");
    private static final java.util.Set<String> KITCHEN_TOPICS = java.util.Set.of(
            "/topic/kitchen/orders", "/topic/kitchen/order-updates");
    private static final java.util.regex.Pattern CUSTOMER_TOPIC =
            java.util.regex.Pattern.compile("^/topic/customer/orders/[A-Za-z0-9-]{20,100}$");

    public WebSocketConfig(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(frontendUrl);
    }

    /*
     * The /ws handshake is public (customers need it), so protection
     * happens at the STOMP level:
     *  - CONNECT: if an "Authorization: Bearer <jwt>" header is sent,
     *    validate it and attach the user to this WebSocket session.
     *  - SUBSCRIBE: /topic/admin/** needs ADMIN,
     *    /topic/kitchen/** needs KITCHEN or ADMIN.
     *    Customer topics stay open; their token in the path is the secret.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor = MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");

                    if (header != null && header.startsWith("Bearer ")) {
                        // Throws if token is invalid/expired -> connection rejected
                        accessor.setUser(
                                jwtAuthenticationConverter.convert(
                                        jwtDecoder.decode(header.substring(7))
                                )
                        );
                    }
                }

                // Browsers may only listen. Only the server publishes events.
                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new AccessDeniedException("Sending messages is not allowed");
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();

                    if (destination == null) {
                        throw new AccessDeniedException("Missing destination");
                    }
                    if (ADMIN_TOPICS.contains(destination)) {
                        if (!hasRole(accessor, "ROLE_ADMIN")) {
                            throw new AccessDeniedException("Admin access required");
                        }
                    } else if (KITCHEN_TOPICS.contains(destination)) {
                        if (!hasRole(accessor, "ROLE_KITCHEN") && !hasRole(accessor, "ROLE_ADMIN")) {
                            throw new AccessDeniedException("Kitchen access required");
                        }
                    } else if (!CUSTOMER_TOPIC.matcher(destination).matches()) {
                        // Blocks wildcards (/topic/**) and unknown topics
                        throw new AccessDeniedException("Unknown topic");
                    }
                }

                return message;
            }
        });
    }

    private boolean hasRole(StompHeaderAccessor accessor, String role) {
        return accessor.getUser() instanceof Authentication authentication
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
