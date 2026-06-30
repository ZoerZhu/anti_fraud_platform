package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.UserScore;
import com.anti.mapper.UserScoreMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceImplTest {

    @Mock
    private UserScoreMapper scoreMapper;

    @InjectMocks
    private ScoreServiceImpl service;

    @Test
    void addScoreInitializesMissingScoreRecordAndUpdatesLevel() {
        UserScore beforeUpdate = score(1L, 0, 1);
        UserScore afterUpdate = score(1L, 120, 1);
        when(scoreMapper.selectByUserId(1L)).thenReturn(null, null, beforeUpdate, afterUpdate);

        service.addScore(1L, 120, "后台调分");

        ArgumentCaptor<UserScore> inserted = ArgumentCaptor.forClass(UserScore.class);
        verify(scoreMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getUserId()).isEqualTo(1L);
        verify(scoreMapper).updateScore(1L, 120);

        ArgumentCaptor<UserScore> updated = ArgumentCaptor.forClass(UserScore.class);
        verify(scoreMapper).updateById(updated.capture());
        assertThat(updated.getValue().getCurrentLevel()).isEqualTo(2);
    }

    @Test
    void deductScoreRejectsInsufficientBalance() {
        when(scoreMapper.selectByUserId(1L)).thenReturn(score(1L, 50, 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deductScore(1L, 100, "后台扣分"));

        assertThat(exception.getMessage()).contains("积分不足");
        verify(scoreMapper, never()).updateScore(1L, -100);
    }

    @Test
    void calculateLevelUsesEveryOneHundredPointsAsLevelThreshold() {
        assertThat(service.calculateLevel(null)).isEqualTo(1);
        assertThat(service.calculateLevel(0)).isEqualTo(1);
        assertThat(service.calculateLevel(99)).isEqualTo(1);
        assertThat(service.calculateLevel(100)).isEqualTo(2);
        assertThat(service.calculateLevel(250)).isEqualTo(3);
    }

    private UserScore score(Long userId, int totalScore, int currentLevel) {
        UserScore score = new UserScore();
        score.setUserId(userId);
        score.setTotalScore(totalScore);
        score.setCurrentLevel(currentLevel);
        return score;
    }
}
