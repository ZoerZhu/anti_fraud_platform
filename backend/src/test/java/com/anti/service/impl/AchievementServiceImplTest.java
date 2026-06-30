package com.anti.service.impl;

import com.anti.entity.Achievement;
import com.anti.mapper.AchievementMapper;
import com.anti.mapper.LearningActivityMapper;
import com.anti.mapper.UserAchievementMapper;
import com.anti.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementServiceImplTest {

    @Mock
    private AchievementMapper achievementMapper;

    @Mock
    private UserAchievementMapper userAchievementMapper;

    @Mock
    private LearningActivityMapper learningActivityMapper;

    @Mock
    private ScoreService scoreService;

    @InjectMocks
    private AchievementServiceImpl service;

    @Test
    void unlockAchievementRewardsScoreOnlyAfterFirstInsert() {
        Achievement achievement = achievement(1L, "first_login", 20);
        when(achievementMapper.selectByCode("first_login")).thenReturn(achievement);
        when(userAchievementMapper.insertIfAbsent(7L, 1L)).thenReturn(1);

        boolean unlocked = service.unlockAchievement(7L, "first_login");

        assertThat(unlocked).isTrue();
        verify(scoreService).addScore(7L, 20, "解锁成就: 首次登录");
    }

    @Test
    void unlockAchievementSkipsRewardWhenUserAlreadyHasAchievement() {
        Achievement achievement = achievement(1L, "first_login", 20);
        when(achievementMapper.selectByCode("first_login")).thenReturn(achievement);
        when(userAchievementMapper.insertIfAbsent(7L, 1L)).thenReturn(0);

        boolean unlocked = service.unlockAchievement(7L, "first_login");

        assertThat(unlocked).isFalse();
        verify(scoreService, never()).addScore(7L, 20, "解锁成就: 首次登录");
    }

    @Test
    void consecutiveLearningDaysAllowsYesterdayAsCurrentStreakStart() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);

        int streak = AchievementServiceImpl.computeConsecutiveLearningDays(
                List.of(today.minusDays(1).toString(), today.minusDays(2).toString()),
                zone
        );

        assertThat(streak).isEqualTo(2);
    }

    private Achievement achievement(Long id, String code, Integer reward) {
        Achievement achievement = new Achievement();
        achievement.setId(id);
        achievement.setCode(code);
        achievement.setName("首次登录");
        achievement.setScoreReward(reward);
        achievement.setConditionType("login");
        achievement.setConditionValue(1);
        return achievement;
    }
}
