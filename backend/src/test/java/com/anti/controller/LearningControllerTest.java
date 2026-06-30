package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.CaseBrowseLog;
import com.anti.entity.Challenge;
import com.anti.entity.ForumPost;
import com.anti.entity.FraudCase;
import com.anti.entity.News;
import com.anti.entity.NewsBrowseLog;
import com.anti.entity.ScenarioProgress;
import com.anti.entity.UserChallengeRecord;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.ForumPostMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.ScenarioProgressMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningControllerTest {

    private CaseBrowseLogMapper caseBrowseLogMapper;
    private FraudCaseMapper fraudCaseMapper;
    private NewsBrowseLogMapper newsBrowseLogMapper;
    private NewsMapper newsMapper;
    private UserChallengeRecordMapper challengeRecordMapper;
    private ChallengeMapper challengeMapper;
    private ScenarioProgressMapper scenarioProgressMapper;
    private ForumPostMapper forumPostMapper;
    private LearningController controller;

    @BeforeEach
    void setUp() {
        caseBrowseLogMapper = mock(CaseBrowseLogMapper.class);
        fraudCaseMapper = mock(FraudCaseMapper.class);
        newsBrowseLogMapper = mock(NewsBrowseLogMapper.class);
        newsMapper = mock(NewsMapper.class);
        challengeRecordMapper = mock(UserChallengeRecordMapper.class);
        challengeMapper = mock(ChallengeMapper.class);
        scenarioProgressMapper = mock(ScenarioProgressMapper.class);
        forumPostMapper = mock(ForumPostMapper.class);
        controller = new LearningController(
                caseBrowseLogMapper,
                fraudCaseMapper,
                newsBrowseLogMapper,
                newsMapper,
                challengeRecordMapper,
                challengeMapper,
                scenarioProgressMapper,
                forumPostMapper
        );
    }

    @Test
    void recordsAggregateAllLearningSourcesWithTitlesAndSafePagination() {
        when(caseBrowseLogMapper.selectList(any())).thenReturn(List.of(caseLog()));
        when(fraudCaseMapper.selectBatchIds(anyCollection())).thenReturn(List.of(caseItem()));
        when(newsBrowseLogMapper.selectList(any())).thenReturn(List.of(newsLog()));
        when(newsMapper.selectBatchIds(anyCollection())).thenReturn(List.of(newsItem()));
        when(challengeRecordMapper.selectList(any())).thenReturn(List.of(challengeRecord()));
        when(scenarioProgressMapper.selectList(any())).thenReturn(List.of(scenarioProgress()));
        when(challengeMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(quizChallenge(), scenarioChallenge()));
        when(forumPostMapper.selectList(any())).thenReturn(List.of(forumPost()));

        Result<Map<String, Object>> result = controller.getLearningRecords(0, 100, loginUser());

        Map<String, Object> data = result.getData();
        assertThat(data.get("current")).isEqualTo(1);
        assertThat(data.get("size")).isEqualTo(30);
        assertThat(data.get("total")).isEqualTo(5);

        List<?> records = (List<?>) data.get("records");
        assertThat(records).extracting("type")
                .containsExactly("forum", "scenario", "challenge", "news", "case");
        assertThat(records).extracting("title")
                .containsExactly("如何识别钓鱼链接", "快递诈骗情景", "初识诈骗", "最新反诈预警", "刷单返利诈骗");
        assertThat(records.get(1)).hasFieldOrPropertyWithValue("content", "完成了情景模拟：快递诈骗情景，得分：80，安全结局");
    }

    private LoginUser loginUser() {
        return new LoginUser(1L, "student", "student");
    }

    private CaseBrowseLog caseLog() {
        CaseBrowseLog log = new CaseBrowseLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setCaseId(10L);
        log.setBrowseTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        log.setStayDuration(45);
        return log;
    }

    private FraudCase caseItem() {
        FraudCase item = new FraudCase();
        item.setId(10L);
        item.setTitle("刷单返利诈骗");
        return item;
    }

    private NewsBrowseLog newsLog() {
        NewsBrowseLog log = new NewsBrowseLog();
        log.setId(2L);
        log.setUserId(1L);
        log.setNewsId(20L);
        log.setBrowseTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        log.setStayDuration(60);
        return log;
    }

    private News newsItem() {
        News item = new News();
        item.setId(20L);
        item.setTitle("最新反诈预警");
        return item;
    }

    private UserChallengeRecord challengeRecord() {
        UserChallengeRecord record = new UserChallengeRecord();
        record.setId(3L);
        record.setUserId(1L);
        record.setChallengeId(30L);
        record.setScore(90);
        record.setPassed(1);
        record.setEndTime(LocalDateTime.of(2026, 6, 3, 10, 0));
        return record;
    }

    private Challenge quizChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(30L);
        challenge.setTitle("初识诈骗");
        challenge.setType("quiz");
        return challenge;
    }

    private ScenarioProgress scenarioProgress() {
        ScenarioProgress progress = new ScenarioProgress();
        progress.setId(4L);
        progress.setUserId(1L);
        progress.setChallengeId(40L);
        progress.setStatus("completed");
        progress.setFinalScore(80);
        progress.setUpdateTime(LocalDateTime.of(2026, 6, 4, 10, 0));
        return progress;
    }

    private Challenge scenarioChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(40L);
        challenge.setTitle("快递诈骗情景");
        challenge.setType("scenario");
        return challenge;
    }

    private ForumPost forumPost() {
        ForumPost post = new ForumPost();
        post.setId(5L);
        post.setUserId(1L);
        post.setTitle("如何识别钓鱼链接");
        post.setStatus(1);
        post.setCreateTime(LocalDateTime.of(2026, 6, 5, 10, 0));
        return post;
    }
}
