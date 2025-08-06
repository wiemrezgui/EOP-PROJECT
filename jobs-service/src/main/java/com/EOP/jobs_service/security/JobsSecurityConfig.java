package com.EOP.jobs_service.security;

import com.EOP.common_lib.common.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class JobsSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // Allow all Swagger and API docs requests
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Public endpoints
                        .requestMatchers("/api/jobs/candidates/apply").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/test/**").permitAll()

                        // All other API endpoints require authentication
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required\"}"
                            );
                        })
                )

                .addFilterBefore(new SimpleJwtFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    private static class SimpleJwtFilter extends OncePerRequestFilter {
        private final JwtTokenProvider jwtTokenProvider;

        public SimpleJwtFilter(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            // Skip JWT processing for public paths
            if (shouldNotFilter(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = extractToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                setAuthentication(token);
            }

            filterChain.doFilter(request, response);
        }

        private String extractToken(HttpServletRequest request) {
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return null;
        }

        private void setAuthentication(String token) {
            String username = jwtTokenProvider.extractUsername(token);
            String role = jwtTokenProvider.extractRole(token);

            if (username != null && role != null) {
                Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();

            // Skip JWT filter for Swagger UI and related resources
            return path.startsWith("/swagger-ui") ||
                    path.startsWith("/v3/api-docs") ||
                    path.equals("/swagger-ui.html") ||
                    path.startsWith("/swagger-resources") ||
                    path.startsWith("/webjars") ||
                    path.startsWith("/actuator/health") ||
                    path.equals("/api/jobs/candidates/apply") ||
                    path.equals("/favicon.ico");
        }
    }
}