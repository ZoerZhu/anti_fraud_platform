package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.common.Result;
import com.anti.entity.dto.ChatRequest;
import com.anti.entity.dto.FeedbackRequest;
import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;
import com.anti.security.LoginUser;
import com.anti.service.QAConversationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能客服控制器
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final QAConversationService qaConversationService;

    public ChatController(QAConversationService qaConversationService) {
        this.qaConversationService = qaConversationService;
    }

    /**
     * 发送问题并获取AI回答
     */
    @PostMapping("/ask")
    public Result<ChatVO> ask(@Valid @RequestBody ChatRequest request,
                              @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        ChatVO result = qaConversationService.askQuestion(request.getQuestion(), request.getSessionId(), userId);
        return Result.success(result);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatVO>> getHistory(@PathVariable String sessionId,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        List<ChatVO> history = qaConversationService.getConversationHistory(sessionId, userId);
        return Result.success(history);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<List<SessionVO>> getSessions(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        List<SessionVO> sessions = qaConversationService.getSessionList(userId);
        return Result.success(sessions);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        qaConversationService.submitFeedback(request.getSessionId(), request.getFeedback(), userId);
        return Result.success();
    }

    /**
     * 获取Token统计
     */
    @GetMapping("/stats")
    public Result<TokenStatsVO> getStats(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        TokenStatsVO stats = qaConversationService.getTokenStats(userId);
        return Result.success(stats);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        qaConversationService.deleteSession(sessionId, userId);
        return Result.success();
    }

    /**
     * 创建新会话
     */
    @PostMapping("/new-session")
    public Result<String> createSession(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        String sessionId = qaConversationService.createSession(userId);
        return Result.success(sessionId);
    }

    private Long requireLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        return loginUser.getUserId();
    }
}
