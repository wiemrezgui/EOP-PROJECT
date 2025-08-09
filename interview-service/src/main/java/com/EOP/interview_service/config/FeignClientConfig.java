package com.EOP.interview_service.config;

import com.EOP.common_lib.common.security.JwtTokenProvider;
import feign.RequestInterceptor;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignClientConfig {

    private final JwtTokenProvider tokenProvider;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();

            if (authentication != null) {
                // Get token from HTTP headers directly
                HttpServletRequest request = ((ServletRequestAttributes)
                        RequestContextHolder.currentRequestAttributes()).getRequest();

                String token = request.getHeader("Authorization");
                if (token != null) {
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }
}