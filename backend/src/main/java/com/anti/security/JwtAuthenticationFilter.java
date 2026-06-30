package com.anti.security;

import com.anti.entity.User;
import com.anti.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserMapper userMapper;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = extractJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            // Redis黑名单检查失败不应阻塞认证流程
            boolean blacklisted = false;
            try {
                blacklisted = isTokenBlacklisted(jwt);
            } catch (Exception e) {
                log.warn("Redis黑名单检查异常，跳过黑名单验证: {}", e.getMessage());
            }

            if (blacklisted) {
                log.debug("Token已被加入黑名单");
                SecurityContextHolder.clearContext();
            } else {
                try {
                    if (jwtUtils.validateToken(jwt)) {
                        String username = jwtUtils.getUsernameFromToken(jwt);
                        String role = jwtUtils.getRoleFromToken(jwt);
                        Long userId = jwtUtils.getUserIdFromToken(jwt);

                        User user = userMapper.selectById(userId);
                        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
                            log.debug("Token用户不存在或已禁用: userId={}", userId);
                            SecurityContextHolder.clearContext();
                            filterChain.doFilter(request, response);
                            return;
                        }

                        LoginUser loginUser = new LoginUser(userId, username, role);
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                loginUser,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    log.error("JWT认证失败: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
