package com.anti.controller;

import com.anti.common.Result;
import com.anti.common.BusinessException;
import com.anti.entity.dto.ScenarioDecisionRequest;
import com.anti.entity.vo.ScenarioProgressVO;
import com.anti.security.LoginUser;
import com.anti.service.ScenarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 情景模拟控制器(FSM状态机)
 */
@RestController
@RequestMapping("/api/scenario")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * 开始情景模拟
     */
    @PostMapping("/start/{challengeId}")
    public Result<ScenarioProgressVO> startScenario(@PathVariable Long challengeId,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        return Result.success(scenarioService.startScenario(challengeId, userId));
    }

    /**
     * 获取当前进度
     */
    @GetMapping("/progress/{challengeId}")
    public Result<ScenarioProgressVO> getProgress(@PathVariable Long challengeId,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        return Result.success(scenarioService.getProgress(challengeId, userId));
    }

    /**
     * 做出决策
     */
    @PostMapping("/decision")
    public Result<ScenarioProgressVO> makeDecision(@Valid @RequestBody ScenarioDecisionRequest request,
                                                   @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        return Result.success(scenarioService.makeDecision(request, userId));
    }

    /**
     * 重置情景模拟
     */
    @PostMapping("/reset/{challengeId}")
    public Result<Void> resetScenario(@PathVariable Long challengeId,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        scenarioService.resetScenario(challengeId, userId);
        return Result.success();
    }

    /**
     * 获取结局
     */
    @GetMapping("/ending/{challengeId}")
    public Result<ScenarioProgressVO> getEnding(@PathVariable Long challengeId,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        return Result.success(scenarioService.getEnding(challengeId, userId));
    }

    private Long requireLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        return loginUser.getUserId();
    }
}
