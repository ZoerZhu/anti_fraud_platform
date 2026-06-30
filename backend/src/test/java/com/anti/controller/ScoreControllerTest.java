package com.anti.controller;

import com.anti.common.Result;
import com.anti.service.AchievementService;
import com.anti.service.ScoreService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScoreControllerTest {

    @Test
    void requiredScoreUsesOneHundredPointsPerLevel() {
        ScoreController controller = new ScoreController(mock(ScoreService.class), mock(AchievementService.class));

        Result<Integer> levelOne = controller.getRequiredScore(1);
        Result<Integer> levelThree = controller.getRequiredScore(3);

        assertThat(levelOne.getData()).isZero();
        assertThat(levelThree.getData()).isEqualTo(200);
    }

    @Test
    void requiredScoreRejectsInvalidLevel() {
        ScoreController controller = new ScoreController(mock(ScoreService.class), mock(AchievementService.class));

        Result<Integer> result = controller.getRequiredScore(0);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("等级必须大于0");
    }
}
