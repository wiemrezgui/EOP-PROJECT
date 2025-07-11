package com.EOP.auth_service.jwt;

import com.EOP.auth_service.exceptions.InvalidDataException;
import com.EOP.auth_service.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json");

        if (authException instanceof UserNotFoundException) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.getWriter().write("{ \"error\": \"User not found\" }");
        }
        else if (authException instanceof InvalidDataException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{ \"error\": \"Invalid credentials\" }");
        }
        else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{ \"error\": \"Authentication failed\" }");
        }
    }
}
