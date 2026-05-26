package com.alevo.usermodule.security;


import com.alevo.usermodule.entity.Device;
import com.alevo.usermodule.entity.UserAccount;
import com.alevo.usermodule.repository.DeviceRepository;
import com.alevo.usermodule.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                Claims claims = tokenProvider.parseToken(token);
                String userId = claims.get("userId", String.class);
                String deviceId = claims.get("deviceId", String.class);

                Optional<UserAccount> userOpt = userRepository.findById(userId);
                Optional<Device> deviceOpt = deviceRepository.findById(deviceId);

                if (userOpt.isPresent() && deviceOpt.isPresent()) {
                    UserAccount user = userOpt.get();
                    Device device = deviceOpt.get();

                    if (!user.isActive()) {
                        sendError(response, HttpStatus.FORBIDDEN, "Account is deactivated");
                        return;
                    }

                    if (!device.isActive()) {
                        sendError(response, HttpStatus.UNAUTHORIZED, "Device has been unlinked");
                        return;
                    }

                    // Update device activity
                    device.touch();
                    deviceRepository.save(device);

                    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(user, device);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user: {} via device: {}", user.getPhoneNumber(), device.getDeviceName());
                }
            }
        } catch (Exception ex) {
            log.error("Authentication filter error: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", message, "status", status.value())));
    }
}
