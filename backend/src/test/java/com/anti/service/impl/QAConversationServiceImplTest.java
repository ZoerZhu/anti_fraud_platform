package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.QAConversation;
import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;
import com.anti.mapper.QAConversationMapper;
import com.anti.util.DeepSeekClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QAConversationServiceImplTest {

    @Mock
    private QAConversationMapper qaConversationMapper;
    @Mock
    private DeepSeekClient deepSeekClient;

    @InjectMocks
    private QAConversationServiceImpl service;

    @Test
    @SuppressWarnings("unchecked")
    void askQuestionSendsOnlyCompleteRecentPairsAndSavesResponse() {
        String sessionId = "session_1_1000";
        List<QAConversation> history = List.of(
                conversation("q1", "a1"),
                conversation("q2", "a2"),
                conversation("q3", "a3"),
                conversation("q4", "a4"),
                conversation("q5", "a5"),
                conversation("q6", "a6"),
                conversation("orphan", null)
        );
        when(qaConversationMapper.findByUserIdAndSessionId(1L, sessionId)).thenReturn(history);
        when(deepSeekClient.chat(anyString(), eq("什么是刷单"), any())).thenReturn(successResponse("这是一段安全回答", 42));
        when(deepSeekClient.getModel()).thenReturn("deepseek-chat");

        ChatVO result = service.askQuestion("  什么是刷单  ", sessionId, 1L);

        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getQuestion()).isEqualTo("什么是刷单");
        assertThat(result.getAnswer()).isEqualTo("这是一段安全回答");
        assertThat(result.getTokensUsed()).isEqualTo(42);
        assertThat(result.getFallback()).isFalse();

        ArgumentCaptor<List<String[]>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(deepSeekClient).chat(anyString(), eq("什么是刷单"), historyCaptor.capture());
        assertThat(historyCaptor.getValue())
                .extracting(message -> message[0] + ":" + message[1])
                .containsExactly(
                        "user:q2", "assistant:a2",
                        "user:q3", "assistant:a3",
                        "user:q4", "assistant:a4",
                        "user:q5", "assistant:a5",
                        "user:q6", "assistant:a6"
                );

        ArgumentCaptor<QAConversation> conversationCaptor = ArgumentCaptor.forClass(QAConversation.class);
        verify(qaConversationMapper).insert(conversationCaptor.capture());
        QAConversation saved = conversationCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
        assertThat(saved.getQuestion()).isEqualTo("什么是刷单");
        assertThat(saved.getAnswer()).isEqualTo("这是一段安全回答");
        assertThat(saved.getModel()).isEqualTo("deepseek-chat");
        assertThat(saved.getTokensUsed()).isEqualTo(42);
    }

    @Test
    void askQuestionRejectsOtherUsersSessionBeforeCallingDeepSeek() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.askQuestion("测试问题", "session_2_1000", 1L));

        assertThat(exception.getCode()).isEqualTo(403);
        assertThat(exception.getMessage()).contains("无权限访问该会话");
        verifyNoInteractions(qaConversationMapper, deepSeekClient);
    }

    @Test
    void askQuestionFallsBackWhenDeepSeekThrowsWithoutLeakingError() {
        String sessionId = "session_1_1000";
        when(qaConversationMapper.findByUserIdAndSessionId(1L, sessionId)).thenReturn(List.of());
        when(deepSeekClient.chat(anyString(), eq("测试问题"), any())).thenThrow(new RuntimeException("secret-api-error"));
        when(deepSeekClient.getModel()).thenReturn("deepseek-chat");

        ChatVO result = service.askQuestion("测试问题", sessionId, 1L);

        assertThat(result.getAnswer()).contains("AI服务暂时不可用");
        assertThat(result.getAnswer()).doesNotContain("secret-api-error");
        assertThat(result.getTokensUsed()).isZero();
        assertThat(result.getFallback()).isTrue();
        verify(qaConversationMapper).insert(any(QAConversation.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void askQuestionTreatsNullHistoryAsEmpty() {
        String sessionId = "session_1_1000";
        when(qaConversationMapper.findByUserIdAndSessionId(1L, sessionId)).thenReturn(null);
        when(deepSeekClient.chat(anyString(), eq("测试问题"), any())).thenReturn(successResponse("安全回答", 8));
        when(deepSeekClient.getModel()).thenReturn("deepseek-chat");

        ChatVO result = service.askQuestion("测试问题", sessionId, 1L);

        assertThat(result.getAnswer()).isEqualTo("安全回答");
        ArgumentCaptor<List<String[]>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(deepSeekClient).chat(anyString(), eq("测试问题"), historyCaptor.capture());
        assertThat(historyCaptor.getValue()).isEmpty();
    }

    @Test
    void askQuestionUsesClearFallbackWhenApiKeyMissing() {
        String sessionId = "session_1_1000";
        when(qaConversationMapper.findByUserIdAndSessionId(1L, sessionId)).thenReturn(List.of());
        when(deepSeekClient.chat(anyString(), eq("测试问题"), any())).thenReturn(failedResponse("AI_API_KEY_MISSING"));
        when(deepSeekClient.getModel()).thenReturn("deepseek-chat");

        ChatVO result = service.askQuestion("测试问题", sessionId, 1L);

        assertThat(result.getAnswer()).contains("AI服务尚未配置");
        assertThat(result.getFallback()).isTrue();
        verify(qaConversationMapper).insert(any(QAConversation.class));
    }

    @Test
    void getSessionListSkipsInvalidOrForeignSessionIds() {
        when(qaConversationMapper.findSessionIdsByUserId(1L))
                .thenReturn(Arrays.asList("session_1_1000", "session_2_1000", "bad", "", null));
        QAConversation conversation = conversation("如何识别诈骗", "请先核验对方身份");
        conversation.setCreateTime(LocalDateTime.of(2026, 6, 30, 9, 0));
        conversation.setTokensUsed(12);
        when(qaConversationMapper.findByUserIdAndSessionId(1L, "session_1_1000")).thenReturn(List.of(conversation));

        List<SessionVO> sessions = service.getSessionList(1L);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getSessionId()).isEqualTo("session_1_1000");
        assertThat(sessions.get(0).getFirstQuestion()).isEqualTo("如何识别诈骗");
        assertThat(sessions.get(0).getTotalTokens()).isEqualTo(12);
        verify(qaConversationMapper, never()).findByUserIdAndSessionId(1L, "session_2_1000");
        verify(qaConversationMapper, never()).findByUserIdAndSessionId(1L, "bad");
    }

    @Test
    void getTokenStatsDefaultsNullMapperValuesToZero() {
        when(qaConversationMapper.sumTokensByUserId(1L)).thenReturn(null);
        when(qaConversationMapper.countByUserId(1L)).thenReturn(null);
        QAConversation satisfied = conversation("q1", "a1");
        satisfied.setFeedback(1);
        QAConversation dissatisfied = conversation("q2", "a2");
        dissatisfied.setFeedback(-1);
        when(qaConversationMapper.selectList(any())).thenReturn(Arrays.asList(satisfied, dissatisfied, null));

        TokenStatsVO stats = service.getTokenStats(1L);

        assertThat(stats.getTotalTokens()).isZero();
        assertThat(stats.getTotalQuestions()).isZero();
        assertThat(stats.getSatisfiedCount()).isEqualTo(1);
        assertThat(stats.getDissatisfiedCount()).isEqualTo(1);
    }

    @Test
    void getConversationHistoryRejectsMissingSession() {
        when(qaConversationMapper.findByUserIdAndSessionId(1L, "session_1_1000")).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getConversationHistory("session_1_1000", 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("会话不存在");
    }

    @Test
    void getConversationHistoryRejectsNullMapperResultAsMissingSession() {
        when(qaConversationMapper.findByUserIdAndSessionId(1L, "session_1_1000")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getConversationHistory("session_1_1000", 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("会话不存在");
    }

    @Test
    void submitFeedbackRejectsMissingSession() {
        when(qaConversationMapper.selectList(any())).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitFeedback("session_1_1000", 1, 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("会话不存在");
        verify(qaConversationMapper, never()).updateById(any());
    }

    @Test
    void submitFeedbackRejectsNullMapperResultAsMissingSession() {
        when(qaConversationMapper.selectList(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitFeedback("session_1_1000", 1, 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("会话不存在");
        verify(qaConversationMapper, never()).updateById(any());
    }

    @Test
    void deleteSessionRejectsMissingSession() {
        when(qaConversationMapper.delete(any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deleteSession("session_1_1000", 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("会话不存在");
    }

    private QAConversation conversation(String question, String answer) {
        QAConversation conversation = new QAConversation();
        conversation.setUserId(1L);
        conversation.setSessionId("session_1_1000");
        conversation.setQuestion(question);
        conversation.setAnswer(answer);
        return conversation;
    }

    private DeepSeekClient.DeepSeekResponse successResponse(String content, int totalTokens) {
        DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
        response.setSuccess(true);
        response.setContent(content);
        response.setTotalTokens(totalTokens);
        return response;
    }

    private DeepSeekClient.DeepSeekResponse failedResponse(String errorMessage) {
        DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
