package com.twitter.ums.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.ums.config.AppProperties;
import com.twitter.ums.contracts.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiCorsFilter extends OncePerRequestFilter {

    public static final String MESSAGE = "Origin не из списка или метод не разрешён";

    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public ApiCorsFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        String method = request.getMethod();
        List<String> allowedMethods = properties.getCors().getAllowedMethods();
        List<String> allowedOrigins = properties.getCors().getAllowedOrigins();

        if (!allowedMethods.contains(method)) {
            writeForbidden(response);
            return;
        }
        if (origin != null && !origin.isBlank() && !allowedOrigins.contains(origin)) {
            writeForbidden(response);
            return;
        }
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", String.join(",", allowedMethods));
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Session-Id");
            response.setHeader("Vary", "Origin");
        }
        if ("OPTIONS".equals(method)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiError(403, MESSAGE));
    }
}
