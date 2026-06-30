package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.User;
import com.anti.entity.dto.*;
import com.anti.entity.vo.LoginVO;
import com.anti.entity.vo.UserVO;
import com.anti.mapper.UserMapper;
import com.anti.security.JwtUtils;
import com.anti.service.AchievementService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements com.anti.service.UserService {

    private final UserMapper userMapper;
    private final ScoreService scoreService;
    private final AchievementService achievementService;
    private final ProfileService profileService;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final int MAX_USER_PAGE_SIZE = 50;

    @Override
    public LoginVO login(LoginRequest request) {
        String username = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : "";
        String password = request.getPassword();

        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getId());

        try {
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        } catch (Exception e) {
            log.warn("更新最后登录时间失败 userId={}: {}", user.getId(), e.getMessage());
        }
        try {
            achievementService.checkAndUnlockAchievements(user.getId(), "login_count", 1);
            achievementService.refreshContinuousLearningStreak(user.getId());
        } catch (Exception e) {
            log.warn("登录后成就校验失败 userId={}", user.getId(), e);
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRole(user.getRole());
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());

        log.info("用户登录成功: {}", username);
        return loginVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        String username = normalizeRequired(request.getUsername());
        String studentNo = normalizeRequired(request.getStudentNo());

        User existUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User existStudent = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo)
        );
        if (existStudent != null) {
            throw new BusinessException("学号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(normalizeOptional(request.getNickname(), username));
        user.setPhone(normalizeOptional(request.getPhone(), null));
        user.setEmail(normalizeOptional(request.getEmail(), null));
        user.setStudentNo(studentNo);
        user.setGrade(normalizeRequired(request.getGrade()));
        user.setMajor(normalizeOptional(request.getMajor(), null));
        user.setRole("student");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        scoreService.initScore(user.getId());
        profileService.initProfile(user.getId(), user.getGrade(), user.getMajor());

        log.info("用户注册成功: {}", username);
        return user;
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UpdateUserRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(normalizeOptional(request.getPhone(), null));
        }
        if (request.getEmail() != null) {
            user.setEmail(normalizeOptional(request.getEmail(), null));
        }
        if (request.getAvatar() != null) {
            user.setAvatar(normalizeOptional(request.getAvatar(), null));
        }
        if (request.getGrade() != null) {
            user.setGrade(normalizeOptional(request.getGrade(), null));
        }
        if (request.getMajor() != null) {
            user.setMajor(normalizeOptional(request.getMajor(), null));
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        if (request.getGrade() != null || request.getMajor() != null) {
            UpdateProfileRequest profileRequest = new UpdateProfileRequest();
            profileRequest.setGrade(user.getGrade());
            profileRequest.setMajor(user.getMajor());
            try {
                profileService.updateProfile(user.getId(), profileRequest);
            } catch (Exception e) {
                log.warn("同步用户画像年级专业失败 userId={}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("用户信息更新: userId={}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户密码修改: userId={}", userId);
    }

    @Override
    public IPage<UserVO> getUserList(Page<User> page, String keyword, String role, Integer status) {
        page.setCurrent(Math.max(1, page.getCurrent()));
        page.setSize(Math.min(Math.max(1, page.getSize()), MAX_USER_PAGE_SIZE));
        String safeKeyword = normalizeOptional(keyword, null);
        if (safeKeyword != null && safeKeyword.length() > 50) {
            safeKeyword = safeKeyword.substring(0, 50);
        }
        String safeRole = ("admin".equals(role) || "student".equals(role)) ? role : null;
        Integer safeStatus = (status != null && (status == 0 || status == 1)) ? status : null;

        IPage<User> userPage = userMapper.selectUserPage(page, safeKeyword, safeRole, safeStatus);
        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());

        voPage.setRecords(userPage.getRecords().stream().map(this::convertToUserVO).toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("启用用户: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(0);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("禁用用户: userId={}", userId);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserVO(user);
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && jwtUtils.validateToken(token)) {
            Long expiration = jwtUtils.getExpirationFromToken(token);
            if (expiration > 0) {
                try {
                    redisTemplate.opsForValue().set(
                        TOKEN_BLACKLIST_PREFIX + token,
                        "blacklisted",
                        expiration,
                        TimeUnit.MILLISECONDS
                    );
                } catch (Exception e) {
                    log.warn("Token加入黑名单失败(Redis不可用): {}", e.getMessage());
                }
            }
        }
        log.info("用户退出登录");
    }

    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setStudentNo(user.getStudentNo());
        return vo;
    }

    private static String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeOptional(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }
}
