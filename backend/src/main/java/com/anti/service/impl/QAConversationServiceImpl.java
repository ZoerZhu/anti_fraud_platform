package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.QAConversation;
import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;
import com.anti.mapper.QAConversationMapper;
import com.anti.service.QAConversationService;
import com.anti.util.AntiFraudPromptTemplate;
import com.anti.util.DeepSeekClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 问答会话服务实现类
 */
@Slf4j
@Service
public class QAConversationServiceImpl extends ServiceImpl<QAConversationMapper, QAConversation> implements QAConversationService {

    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int MAX_SESSION_ID_LENGTH = 50;
    private static final int MAX_HISTORY_PAIRS = 5;
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^session_(\\d+)_(\\d+)$");

    private final QAConversationMapper qaConversationMapper;
    private final DeepSeekClient deepSeekClient;

    public QAConversationServiceImpl(QAConversationMapper qaConversationMapper, DeepSeekClient deepSeekClient) {
        this.qaConversationMapper = qaConversationMapper;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    @Transactional
    public ChatVO askQuestion(String question, String sessionId, Long userId) {
        validateUserId(userId);
        String normalizedQuestion = normalizeQuestion(question);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, true);

        // 如果没有会话ID，创建新会话
        if (normalizedSessionId == null) {
            normalizedSessionId = createSession(userId);
        }

        // 获取历史对话
        List<QAConversation> history = qaConversationMapper.findByUserIdAndSessionId(userId, normalizedSessionId);
        List<QAConversation> pairedHistory = history.stream()
                .filter(h -> hasText(h.getQuestion()) && hasText(h.getAnswer()))
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, pairedHistory.size() - MAX_HISTORY_PAIRS);
        List<String[]> historyMessages = pairedHistory.subList(fromIndex, pairedHistory.size()).stream()
                .flatMap(h -> {
                    List<String[]> messages = new ArrayList<>();
                    messages.add(new String[]{"user", h.getQuestion().trim()});
                    messages.add(new String[]{"assistant", h.getAnswer().trim()});
                    return messages.stream();
                })
                .collect(Collectors.toList());

        // 构建prompt
        String systemPrompt = AntiFraudPromptTemplate.getSystemPrompt();

        // 调用DeepSeek API
        DeepSeekClient.DeepSeekResponse response = callDeepSeekSafely(systemPrompt, normalizedQuestion, historyMessages);

        // 构建结果
        ChatVO chatVO = new ChatVO();
        chatVO.setSessionId(normalizedSessionId);
        chatVO.setQuestion(normalizedQuestion);

        if (response.isSuccess() && hasText(response.getContent())) {
            chatVO.setAnswer(response.getContent().trim());
            chatVO.setTokensUsed(response.getTotalTokens());
            chatVO.setFallback(false);
        } else {
            chatVO.setAnswer(buildFallbackAnswer(response.getErrorMessage()));
            chatVO.setTokensUsed(0);
            chatVO.setFallback(true);
        }
        chatVO.setCreateTime(LocalDateTime.now());

        // 保存问答记录
        QAConversation conversation = new QAConversation();
        conversation.setUserId(userId);
        conversation.setSessionId(normalizedSessionId);
        conversation.setQuestion(normalizedQuestion);
        conversation.setAnswer(chatVO.getAnswer());
        conversation.setModel(deepSeekClient.getModel());
        conversation.setTokensUsed(chatVO.getTokensUsed());
        conversation.setFeedback(null);
        conversation.setCreateTime(LocalDateTime.now());
        qaConversationMapper.insert(conversation);

        return chatVO;
    }

    @Override
    public List<ChatVO> getConversationHistory(String sessionId, Long userId) {
        validateUserId(userId);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, false);
        List<QAConversation> conversations = qaConversationMapper.findByUserIdAndSessionId(userId, normalizedSessionId);
        if (conversations.isEmpty()) {
            throw new BusinessException(404, "会话不存在");
        }

        return conversations.stream().map(c -> {
            ChatVO vo = new ChatVO();
            vo.setSessionId(c.getSessionId());
            vo.setQuestion(c.getQuestion());
            vo.setAnswer(c.getAnswer());
            vo.setTokensUsed(c.getTokensUsed());
            vo.setFallback(false);
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SessionVO> getSessionList(Long userId) {
        validateUserId(userId);
        List<String> sessionIds = qaConversationMapper.findSessionIdsByUserId(userId);
        List<SessionVO> sessions = new ArrayList<>();

        for (String sid : safeList(sessionIds)) {
            if (!isOwnedSessionId(sid, userId)) {
                continue;
            }
            List<QAConversation> conversations = qaConversationMapper.findByUserIdAndSessionId(userId, sid);

            if (conversations != null && !conversations.isEmpty()) {
                SessionVO sessionVO = new SessionVO();
                sessionVO.setSessionId(sid);

                // 第一条问题
                String firstQuestion = conversations.get(0).getQuestion();
                sessionVO.setFirstQuestion(abbreviate(firstQuestion, 50));

                // 最后一条回答
                QAConversation last = conversations.get(conversations.size() - 1);
                String lastAnswer = last.getAnswer();
                sessionVO.setLastAnswer(abbreviate(lastAnswer, 50));

                sessionVO.setMessageCount(conversations.size());
                sessionVO.setCreateTime(conversations.get(0).getCreateTime());
                sessionVO.setUpdateTime(last.getCreateTime());

                // 统计token
                int totalTokens = conversations.stream()
                        .filter(c -> c.getTokensUsed() != null)
                        .mapToInt(QAConversation::getTokensUsed)
                        .sum();
                sessionVO.setTotalTokens(totalTokens);

                sessions.add(sessionVO);
            }
        }

        return sessions;
    }

    @Override
    @Transactional
    public void submitFeedback(String sessionId, Integer feedback, Long userId) {
        validateUserId(userId);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, false);
        if (feedback == null || (feedback != 1 && feedback != -1)) {
            throw new BusinessException(400, "反馈值只能为-1或1");
        }

        // 更新该会话最后一条记录的反馈
        List<QAConversation> conversations = qaConversationMapper.selectList(
                new LambdaQueryWrapper<QAConversation>()
                        .eq(QAConversation::getUserId, userId)
                        .eq(QAConversation::getSessionId, normalizedSessionId)
                        .orderByDesc(QAConversation::getCreateTime)
                        .last("LIMIT 1")
        );

        if (conversations.isEmpty()) {
            throw new BusinessException(404, "会话不存在");
        }
        QAConversation lastConversation = conversations.get(0);
        lastConversation.setFeedback(feedback);
        qaConversationMapper.updateById(lastConversation);
    }

    @Override
    public TokenStatsVO getTokenStats(Long userId) {
        validateUserId(userId);
        TokenStatsVO stats = new TokenStatsVO();
        stats.setTotalTokens(defaultInt(qaConversationMapper.sumTokensByUserId(userId)));
        stats.setTotalQuestions(defaultInt(qaConversationMapper.countByUserId(userId)));

        // 统计反馈
        List<QAConversation> allConversations = qaConversationMapper.selectList(
                new LambdaQueryWrapper<QAConversation>()
                        .eq(QAConversation::getUserId, userId)
                        .isNotNull(QAConversation::getFeedback)
        );

        stats.setSatisfiedCount((int) safeList(allConversations).stream()
                .filter(Objects::nonNull)
                .filter(c -> Integer.valueOf(1).equals(c.getFeedback()))
                .count());
        stats.setDissatisfiedCount((int) safeList(allConversations).stream()
                .filter(Objects::nonNull)
                .filter(c -> Integer.valueOf(-1).equals(c.getFeedback()))
                .count());

        return stats;
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        validateUserId(userId);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, false);
        int deleted = qaConversationMapper.delete(new LambdaQueryWrapper<QAConversation>()
                .eq(QAConversation::getUserId, userId)
                .eq(QAConversation::getSessionId, normalizedSessionId)
        );
        if (deleted == 0) {
            throw new BusinessException(404, "会话不存在");
        }
    }

    @Override
    public String createSession(Long userId) {
        validateUserId(userId);
        return "session_" + userId + "_" + System.currentTimeMillis();
    }

    private DeepSeekClient.DeepSeekResponse callDeepSeekSafely(String systemPrompt,
                                                               String normalizedQuestion,
                                                               List<String[]> historyMessages) {
        try {
            return deepSeekClient.chat(systemPrompt, normalizedQuestion, historyMessages);
        } catch (Exception e) {
            log.warn("调用AI服务失败，已降级为本地提示: {}", e.getClass().getSimpleName());
            DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
            response.setSuccess(false);
            response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
            return response;
        }
    }

    private String normalizeQuestion(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isEmpty()) {
            throw new BusinessException(400, "问题内容不能为空");
        }
        if (normalizedQuestion.length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(400, "问题内容不能超过2000个字符");
        }
        return normalizedQuestion;
    }

    private String normalizeSessionId(String sessionId, Long userId, boolean allowBlank) {
        if (sessionId == null || sessionId.isBlank()) {
            if (allowBlank) {
                return null;
            }
            throw new BusinessException(400, "会话ID不能为空");
        }

        String normalizedSessionId = sessionId.trim();
        if (normalizedSessionId.length() > MAX_SESSION_ID_LENGTH) {
            throw new BusinessException(400, "会话ID不能超过50个字符");
        }

        Matcher matcher = SESSION_ID_PATTERN.matcher(normalizedSessionId);
        if (!matcher.matches()) {
            throw new BusinessException(400, "会话ID格式不正确");
        }

        Long ownerId;
        try {
            ownerId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "会话ID格式不正确");
        }
        if (!ownerId.equals(userId)) {
            throw new BusinessException(403, "无权限访问该会话");
        }

        return normalizedSessionId;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isOwnedSessionId(String sessionId, Long userId) {
        if (!hasText(sessionId)) {
            return false;
        }
        Matcher matcher = SESSION_ID_PATTERN.matcher(sessionId.trim());
        if (!matcher.matches()) {
            return false;
        }
        try {
            return Long.parseLong(matcher.group(1)) == userId;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    private String buildFallbackAnswer(String errorMessage) {
        if ("AI_API_KEY_MISSING".equals(errorMessage)) {
            return "AI服务尚未配置，请联系管理员配置后再使用。";
        }
        return "AI服务暂时不可用，请稍后再试。你也可以先查看资讯、案例和闯关内容获取反诈建议。";
    }
}
