package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.CaseBrowseLog;
import com.anti.entity.Challenge;
import com.anti.entity.FraudCase;
import com.anti.entity.ForumPost;
import com.anti.entity.News;
import com.anti.entity.NewsBrowseLog;
import com.anti.entity.ScenarioProgress;
import com.anti.entity.UserChallengeRecord;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.mapper.ForumPostMapper;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.ScenarioProgressMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.anti.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "学习记录")
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private static final int RECENT_LIMIT = 100;
    private static final int MAX_PAGE_SIZE = 30;

    private final CaseBrowseLogMapper caseBrowseLogMapper;
    private final FraudCaseMapper fraudCaseMapper;
    private final NewsBrowseLogMapper newsBrowseLogMapper;
    private final NewsMapper newsMapper;
    private final UserChallengeRecordMapper challengeRecordMapper;
    private final ChallengeMapper challengeMapper;
    private final ScenarioProgressMapper scenarioProgressMapper;
    private final ForumPostMapper forumPostMapper;

    @GetMapping("/records")
    @Operation(summary = "获取学习记录")
    public Result<Map<String, Object>> getLearningRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal LoginUser loginUser) {

        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long userId = loginUser.getUserId();
        List<LearningRecord> allRecords = new ArrayList<>();

        List<CaseBrowseLog> caseLogs = caseBrowseLogMapper.selectList(
                new LambdaQueryWrapper<CaseBrowseLog>()
                        .eq(CaseBrowseLog::getUserId, userId)
                        .orderByDesc(CaseBrowseLog::getBrowseTime)
                        .last("LIMIT " + RECENT_LIMIT)
        );
        Map<Long, String> caseTitles = loadCaseTitles(caseLogs);
        for (CaseBrowseLog log : caseLogs) {
            String title = titleOrFallback(caseTitles, log.getCaseId(), "案例");
            allRecords.add(new LearningRecord(
                    "case-" + log.getId(),
                    log.getId(),
                    "case",
                    log.getCaseId(),
                    title,
                    "浏览了案例：" + title,
                    log.getBrowseTime(),
                    Map.of("stayDuration", defaultInt(log.getStayDuration()))
            ));
        }

        List<NewsBrowseLog> newsLogs = newsBrowseLogMapper.selectList(
                new LambdaQueryWrapper<NewsBrowseLog>()
                        .eq(NewsBrowseLog::getUserId, userId)
                        .orderByDesc(NewsBrowseLog::getBrowseTime)
                        .last("LIMIT " + RECENT_LIMIT)
        );
        Map<Long, String> newsTitles = loadNewsTitles(newsLogs);
        for (NewsBrowseLog log : newsLogs) {
            String title = titleOrFallback(newsTitles, log.getNewsId(), "资讯");
            allRecords.add(new LearningRecord(
                    "news-" + log.getId(),
                    log.getId(),
                    "news",
                    log.getNewsId(),
                    title,
                    "阅读了资讯：" + title,
                    log.getBrowseTime(),
                    Map.of("stayDuration", defaultInt(log.getStayDuration()))
            ));
        }

        List<UserChallengeRecord> challengeRecords = challengeRecordMapper.selectList(
                new LambdaQueryWrapper<UserChallengeRecord>()
                        .eq(UserChallengeRecord::getUserId, userId)
                        .orderByDesc(UserChallengeRecord::getEndTime)
                        .last("LIMIT " + RECENT_LIMIT)
        );
        Map<Long, Challenge> challenges = loadChallenges(challengeRecords.stream()
                .map(UserChallengeRecord::getChallengeId)
                .toList());
        for (UserChallengeRecord record : challengeRecords) {
            Challenge challenge = challenges.get(record.getChallengeId());
            String title = challenge != null && challenge.getTitle() != null
                    ? challenge.getTitle()
                    : "关卡 " + record.getChallengeId();
            String type = challenge != null && "scenario".equals(challenge.getType()) ? "scenario" : "challenge";
            String content = ("scenario".equals(type) ? "完成了情景模拟：" : "完成了闯关：") + title
                    + (record.getScore() != null ? "，得分：" + record.getScore() : "")
                    + (Integer.valueOf(1).equals(record.getPassed()) ? "，已通关" : "，未通关");
            allRecords.add(new LearningRecord(
                    type + "-record-" + record.getId(),
                    record.getId(),
                    type,
                    record.getChallengeId(),
                    title,
                    content,
                    record.getEndTime(),
                    Map.of(
                            "score", defaultInt(record.getScore()),
                            "passed", Integer.valueOf(1).equals(record.getPassed())
                    )
            ));
        }

        List<ScenarioProgress> scenarioProgresses = scenarioProgressMapper.selectList(
                new LambdaQueryWrapper<ScenarioProgress>()
                        .eq(ScenarioProgress::getUserId, userId)
                        .in(ScenarioProgress::getStatus, "completed", "failed")
                        .orderByDesc(ScenarioProgress::getUpdateTime)
                        .last("LIMIT " + RECENT_LIMIT)
        );
        Map<Long, Challenge> scenarioChallenges = loadChallenges(scenarioProgresses.stream()
                .map(ScenarioProgress::getChallengeId)
                .toList());
        for (ScenarioProgress progress : scenarioProgresses) {
            Challenge challenge = scenarioChallenges.get(progress.getChallengeId());
            String title = challenge != null && challenge.getTitle() != null
                    ? challenge.getTitle()
                    : "情景模拟 " + progress.getChallengeId();
            boolean completed = "completed".equals(progress.getStatus());
            allRecords.add(new LearningRecord(
                    "scenario-progress-" + progress.getId(),
                    progress.getId(),
                    "scenario",
                    progress.getChallengeId(),
                    title,
                    "完成了情景模拟：" + title
                            + (progress.getFinalScore() != null ? "，得分：" + progress.getFinalScore() : "")
                            + (completed ? "，安全结局" : "，风险结局"),
                    progress.getUpdateTime(),
                    Map.of(
                            "score", defaultInt(progress.getFinalScore()),
                            "status", Objects.toString(progress.getStatus(), "")
                    )
            ));
        }

        List<ForumPost> posts = forumPostMapper.selectList(
                new LambdaQueryWrapper<ForumPost>()
                        .eq(ForumPost::getUserId, userId)
                        .orderByDesc(ForumPost::getCreateTime)
                        .last("LIMIT " + RECENT_LIMIT)
        );
        for (ForumPost post : posts) {
            allRecords.add(new LearningRecord(
                    "forum-" + post.getId(),
                    post.getId(),
                    "forum",
                    post.getId(),
                    post.getTitle(),
                    "发布了帖子：" + post.getTitle(),
                    post.getCreateTime(),
                    Map.of("status", defaultInt(post.getStatus()))
            ));
        }

        allRecords.sort(Comparator.comparing(LearningRecord::time,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int total = allRecords.size();
        int start = (safePage - 1) * safeSize;
        int end = Math.min(start + safeSize, total);
        List<LearningRecord> pageRecords = start < total
                ? allRecords.subList(start, end)
                : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("size", safeSize);
        result.put("current", safePage);
        result.put("pages", (total + safeSize - 1) / safeSize);

        return Result.success(result);
    }

    private Map<Long, String> loadCaseTitles(List<CaseBrowseLog> logs) {
        List<Long> ids = distinctIds(logs, CaseBrowseLog::getCaseId);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return fraudCaseMapper.selectBatchIds(ids).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(FraudCase::getId, FraudCase::getTitle, (a, b) -> a));
    }

    private Map<Long, String> loadNewsTitles(List<NewsBrowseLog> logs) {
        List<Long> ids = distinctIds(logs, NewsBrowseLog::getNewsId);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return newsMapper.selectBatchIds(ids).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(News::getId, News::getTitle, (a, b) -> a));
    }

    private Map<Long, Challenge> loadChallenges(List<Long> ids) {
        List<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return challengeMapper.selectBatchIds(distinctIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Challenge::getId, Function.identity(), (a, b) -> a));
    }

    private static <T> List<Long> distinctIds(List<T> items, Function<T, Long> extractor) {
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static String titleOrFallback(Map<Long, String> titles, Long id, String label) {
        String title = titles.get(id);
        if (title != null && !title.isBlank()) {
            return title;
        }
        return label + " " + id;
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record LearningRecord(
            String recordKey,
            Long id,
            String type,
            Long targetId,
            String title,
            String content,
            LocalDateTime time,
            Map<String, Object> meta
    ) {
    }
}
