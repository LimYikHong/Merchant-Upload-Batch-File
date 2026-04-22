package rta.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter that secures /api/internal/** endpoints with API key validation. The
 * main system (RTA_BANK) must send the correct X-API-Key header.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);

    @Value("${rta.internal.api-key}")
    private String expectedApiKey;

    @Value("${rta.internal.allowed-ips}")
    private String allowedIpsConfig;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to /api/internal/** endpoints
        return !request.getRequestURI().startsWith("/api/internal");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");
        String remoteAddr = request.getRemoteAddr();

        // Validate API key
        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            log.warn("Internal API rejected: invalid API key from IP {}", remoteAddr);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
            return;
        }

        // Validate source IP
        Set<String> allowedIps = Arrays.stream(allowedIpsConfig.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        if (!allowedIps.contains(remoteAddr)) {
            log.warn("Internal API rejected: IP {} not in allowed list", remoteAddr);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"IP not allowed\"}");
            return;
        }

        log.info("Internal API authorized: {} {} from IP {}", request.getMethod(), request.getRequestURI(), remoteAddr);
        filterChain.doFilter(request, response);
    }
}
