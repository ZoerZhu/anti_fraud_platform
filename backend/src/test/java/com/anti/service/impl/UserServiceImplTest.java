package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.User;
import com.anti.entity.dto.LoginRequest;
import com.anti.entity.dto.RegisterRequest;
import com.anti.entity.dto.UpdateProfileRequest;
import com.anti.entity.dto.UpdateUserRequest;
import com.anti.entity.vo.UserVO;
import com.anti.mapper.UserMapper;
import com.anti.security.JwtUtils;
import com.anti.service.AchievementService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private AchievementService achievementService;
    @Mock
    private ProfileService profileService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void registerAlwaysCreatesStudentAccount() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student001");
        request.setPassword("123456");
        request.setNickname("Student");
        request.setStudentNo("20260001");
        request.setGrade("大一");
        request.setMajor("软件工程");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getRole()).isEqualTo("student");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getStudentNo()).isEqualTo("20260001");
    }

    @Test
    void loginRejectsDisabledUserBeforePasswordCheck() {
        LoginRequest request = new LoginRequest();
        request.setUsername("blocked");
        request.setPassword("123456");

        User user = new User();
        user.setId(9L);
        user.setUsername("blocked");
        user.setPassword("encoded");
        user.setStatus(0);

        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.login(request));

        assertThat(exception.getMessage()).contains("账号已被禁用");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtils, never()).generateToken(any(), any(), any());
    }

    @Test
    void registerTrimsUsernameAndStudentNoBeforeCheckingDuplicates() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  student001  ");
        request.setPassword("123456");
        request.setStudentNo("  20260001  ");
        request.setGrade("  大一  ");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("student001");
        assertThat(saved.getStudentNo()).isEqualTo("20260001");
        assertThat(saved.getGrade()).isEqualTo("大一");
    }

    @Test
    void getUserListClampsPagingAndIgnoresInvalidFilters() {
        Page<User> requestedPage = new Page<>(0, 500);
        Page<User> mapperPage = new Page<>(1, 50, 1);
        User user = new User();
        user.setId(1L);
        user.setUsername("student001");
        user.setRole("student");
        user.setStatus(1);
        mapperPage.setRecords(List.of(user));
        when(userMapper.selectUserPage(any(), eq("keyword"), eq(null), eq(null))).thenReturn(mapperPage);

        IPage<UserVO> result = service.getUserList(requestedPage, "  keyword  ", "teacher", 3);

        ArgumentCaptor<Page<User>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(userMapper).selectUserPage(pageCaptor.capture(), eq("keyword"), eq(null), eq(null));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(50);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void updateUserTrimsClearsNullableFieldsAndSyncsProfile() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student001");
        user.setNickname("旧昵称");
        user.setPhone("13800138000");
        user.setEmail("old@example.com");
        user.setGrade("大一");
        user.setMajor("软件工程");
        when(userMapper.selectById(1L)).thenReturn(user);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setId(1L);
        request.setNickname("  新昵称  ");
        request.setPhone("");
        request.setEmail("  new@example.com  ");
        request.setGrade("  大二  ");
        request.setMajor("");

        service.updateUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User updated = userCaptor.getValue();
        assertThat(updated.getNickname()).isEqualTo("新昵称");
        assertThat(updated.getPhone()).isNull();
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getGrade()).isEqualTo("大二");
        assertThat(updated.getMajor()).isNull();

        ArgumentCaptor<UpdateProfileRequest> profileCaptor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(profileService).updateProfile(eq(1L), profileCaptor.capture());
        assertThat(profileCaptor.getValue().getGrade()).isEqualTo("大二");
        assertThat(profileCaptor.getValue().getMajor()).isNull();
    }
}
