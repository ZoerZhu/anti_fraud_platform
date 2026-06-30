package com.anti.security;

import com.anti.entity.User;
import com.anti.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesEnabledUserToken() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        UserMapper userMapper = mock(UserMapper.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils, redisTemplate, userMapper);

        when(redisTemplate.hasKey("token:blacklist:valid-token")).thenReturn(false);
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("student001");
        when(jwtUtils.getRoleFromToken("valid-token")).thenReturn("student");
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(user(1));

        filter.doFilter(requestWithToken("valid-token"), new MockHttpServletResponse(), noopChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(LoginUser.class);
        assertThat(((LoginUser) authentication.getPrincipal()).getUserId()).isEqualTo(1L);
    }

    @Test
    void skipsAuthenticationForDisabledUserToken() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        UserMapper userMapper = mock(UserMapper.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils, redisTemplate, userMapper);

        when(redisTemplate.hasKey("token:blacklist:valid-token")).thenReturn(false);
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("student001");
        when(jwtUtils.getRoleFromToken("valid-token")).thenReturn("student");
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(user(0));

        filter.doFilter(requestWithToken("valid-token"), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private FilterChain noopChain() {
        return (request, response) -> {
        };
    }

    private User user(Integer status) {
        User user = new User();
        user.setId(1L);
        user.setUsername("student001");
        user.setRole("student");
        user.setStatus(status);
        return user;
    }
}
