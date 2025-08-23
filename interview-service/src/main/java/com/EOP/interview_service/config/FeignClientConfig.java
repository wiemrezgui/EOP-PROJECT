package com.EOP.interview_service.config;

import com.EOP.common_lib.common.security.JwtTokenProvider;
import feign.RequestInterceptor;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignClientConfig {

    private final JwtTokenProvider tokenProvider;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            log.debug("Feign interceptor called for URL: {}", requestTemplate.url());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authentication in context: {}", authentication);

            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();

            String token = request.getHeader("Authorization");
            log.debug("Authorization header from request: {}", token);

            if (token != null) {
                requestTemplate.header("Authorization", token);
                log.debug("Authorization header added to Feign request");
            } else {
                log.warn("No Authorization header found for Feign request to: {}", requestTemplate.url());
            }
        };
    }
}