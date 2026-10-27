package com.cafe.backend.config;

import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // PUBLIC AUTHENTICATION
                        // =========================

                        .requestMatchers("/api/admin/auth/**").permitAll()

                        // Spring forwards unhandled errors here; must be public
                        .requestMatchers("/error").permitAll()


                        // =========================
                        // CUSTOMER PUBLIC APIs
                        // =========================

                        .requestMatchers("/api/menu/**").permitAll()

                        // Menu/category photos saved in local storage mode (read-only)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/api/qr/**").permitAll()

                        // Customer order tracking using notification token
                        .requestMatchers("/api/orders/customer/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        // Customer payment flow
                        .requestMatchers("/api/payments/razorpay/**").permitAll()

                        // Customer coupon validation
                        .requestMatchers("/api/customer/coupons/**").permitAll()

                        // Customer notifications / push subscription
                        .requestMatchers("/api/customer/notifications/**").permitAll()
                        .requestMatchers("/api/customer/push/**").permitAll()



                        // WebSocket handshake
                        .requestMatchers("/ws/**").permitAll()


                        // =========================
                        // ADMIN ONLY
                        // =========================

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Marketing consent/opt-out changes are managed by Admin
                        // (Kitchen must never touch customer marketing data)
                        .requestMatchers("/api/customer/marketing/**")
                        .hasRole("ADMIN")


                        // =========================
                        // KITCHEN ONLY
                        // =========================

                        .requestMatchers("/api/kitchen/**")
                        .hasRole("KITCHEN")


                        // =========================
                        // EVERYTHING ELSE
                        // =========================

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println(
                                    "401 SECURITY: "
                                            + request.getMethod()
                                            + " "
                                            + request.getRequestURI()
                                            + " | "
                                            + authException.getMessage()
                            );

                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":\"Unauthorized\",\"message\":\"Please log in again.\"}"
                            );
                        })
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }


    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }


    @Bean
    public JwtDecoder jwtDecoder() {

        SecretKey secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"
        );

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}