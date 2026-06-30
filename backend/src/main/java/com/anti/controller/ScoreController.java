package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.ScoreChangeRequest;
import com.anti.entity.vo.ScoreVO;
import com.anti.security.LoginUser;
import com.anti.service.AchievementService;
import com.anti.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
@Tag(name = "积分管理")
public class ScoreController {

    private final ScoreService scoreService;
    private final AchievementService achievementService;

    @GetMapping("/info")
    @Operation(summary = "获取积分信息")
    public Result<ScoreVO> getScoreInfo(@AuthenticationPrincipal LoginUser loginUser) {
        var score = scoreService.getScoreByUserId(loginUser.getUserId());
        var vo = new ScoreVO();
        vo.setId(score.getId());
        vo.setUserId(score.getUserId());
        vo.setTotalScore(score.getTotalScore());
        vo.setCurrentLevel(score.getCurrentLevel());
        vo.setWeeklyScore(score.getWeeklyScore());
        vo.setUpdateTime(score.getUpdateTime());
        vo.setUnlockedAchievements(achievementService.getUnlockedCount(loginUser.getUserId()));
        vo.setTotalAchievements(achievementService.getTotalAchievementCount());
        return Result.success(vo);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "增加积分(管理员)")
    public Result<Void> addScore(@Valid @RequestBody ScoreChangeRequest request) {
        scoreService.addScore(request.getUserId(), request.getScore(), request.getReason());
        return Result.success();
    }

    @PostMapping("/deduct")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "扣减积分(管理员)")
    public Result<Void> deductScore(@Valid @RequestBody ScoreChangeRequest request) {
        scoreService.deductScore(request.getUserId(), request.getScore(), request.getReason());
        return Result.success();
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "获取指定等级需要的积分")
    public Result<Integer> getRequiredScore(@PathVariable Integer level) {
        if (level == null || level < 1) {
            return Result.error(400, "等级必须大于0");
        }
        int requiredScore = (level - 1) * 100;
        return Result.success(requiredScore);
    }
}
