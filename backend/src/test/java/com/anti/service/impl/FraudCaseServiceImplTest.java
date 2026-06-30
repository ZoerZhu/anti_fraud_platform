package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.CaseBrowseLog;
import com.anti.entity.FraudCase;
import com.anti.entity.dto.CreateCaseRequest;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.CaseLikeMapper;
import com.anti.mapper.CaseTagMapper;
import com.anti.mapper.CaseTagRelationMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.service.AchievementService;
import com.anti.service.CacheRefreshService;
import com.anti.service.LeaderboardService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudCaseServiceImplTest {

    @Mock
    private FraudCaseMapper fraudCaseMapper;
    @Mock
    private CaseTagMapper caseTagMapper;
    @Mock
    private CaseTagRelationMapper caseTagRelationMapper;
    @Mock
    private CaseLikeMapper caseLikeMapper;
    @Mock
    private CaseBrowseLogMapper caseBrowseLogMapper;
    @Mock
    private AchievementService achievementService;
    @Mock
    private CacheRefreshService cacheRefreshService;
    @Mock
    private ScoreService scoreService;
    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private ProfileService profileService;

    private FraudCaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FraudCaseServiceImpl(
                caseTagMapper,
                caseTagRelationMapper,
                caseLikeMapper,
                caseBrowseLogMapper,
                achievementService,
                cacheRefreshService,
                scoreService,
                leaderboardService,
                profileService
        );
        ReflectionTestUtils.setField(service, "baseMapper", fraudCaseMapper);
    }

    @Test
    void getCaseDetailRejectsDisabledCase() {
        FraudCase disabled = publishedCase();
        disabled.setStatus(0);
        when(fraudCaseMapper.selectById(10L)).thenReturn(disabled);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getCaseDetail(10L, 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("已下线");
        verify(caseLikeMapper, never()).existsByCaseIdAndUserId(any(), any());
    }

    @Test
    void publishCaseRejectsIncompleteDraft() {
        FraudCase draft = publishedCase();
        draft.setStatus(0);
        draft.setContent(" ");
        when(fraudCaseMapper.selectById(10L)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.publishCase(10L));

        assertThat(exception.getMessage()).contains("案例内容不能为空");
        verify(fraudCaseMapper, never()).updateById(any());
        verify(cacheRefreshService, never()).handlePublishEvent(any(), any());
    }

    @Test
    void setFeaturedRejectsInvalidSwitchValueBeforePersistence() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.setFeatured(10L, 2));

        assertThat(exception.getMessage()).contains("精选状态只能为0或1");
        verify(fraudCaseMapper, never()).selectById(any());
        verify(fraudCaseMapper, never()).updateById(any());
    }

    @Test
    void likeCaseRejectsDisabledCase() {
        FraudCase disabled = publishedCase();
        disabled.setStatus(0);
        when(fraudCaseMapper.selectById(10L)).thenReturn(disabled);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.likeCase(10L, 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        verify(caseLikeMapper, never()).insert(any());
    }

    @Test
    void browseCaseClampsStayDurationAndRefreshesCache() {
        when(fraudCaseMapper.selectById(10L)).thenReturn(publishedCase());
        when(caseBrowseLogMapper.countByUserIdAndCaseId(1L, 10L)).thenReturn(0);
        when(caseBrowseLogMapper.insert(any())).thenReturn(1);

        service.browseCase(10L, 1L, 999999);

        ArgumentCaptor<CaseBrowseLog> captor = ArgumentCaptor.forClass(CaseBrowseLog.class);
        verify(caseBrowseLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getStayDuration()).isEqualTo(7200);
        verify(fraudCaseMapper).incrementViewCount(10L);
        verify(cacheRefreshService).handleBrowseEvent(1L, "case", 10L);
        verify(scoreService).addScore(1L, 2, "浏览案例");
    }

    @Test
    void browseCaseSkipsKnowledgeUpdateWhenProfileMissing() {
        when(fraudCaseMapper.selectById(10L)).thenReturn(publishedCase());
        when(caseBrowseLogMapper.countByUserIdAndCaseId(1L, 10L)).thenReturn(0);
        when(caseBrowseLogMapper.insert(any())).thenReturn(1);
        when(profileService.getProfileByUserId(1L)).thenReturn(null);

        service.browseCase(10L, 1L, 90);

        verify(caseBrowseLogMapper).insert(any(CaseBrowseLog.class));
        verify(scoreService).addScore(1L, 2, "浏览案例");
        verify(profileService, never()).updateKnowledgeLevel(any(), any());
    }

    @Test
    void createCaseAsDraftDoesNotSetPublishTime() {
        when(fraudCaseMapper.insert(any())).thenAnswer(invocation -> {
            FraudCase saved = invocation.getArgument(0);
            saved.setId(10L);
            return 1;
        });
        when(caseTagRelationMapper.selectTagIdsByCaseId(10L)).thenReturn(List.of());

        CreateCaseRequest request = new CreateCaseRequest();
        request.setTitle("草稿案例");
        request.setCaseType("刷单返利");
        request.setContent("案例内容");
        request.setStatus(0);

        service.createCase(request, 1L);

        ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isZero();
        assertThat(captor.getValue().getPublishTime()).isNull();
    }

    private FraudCase publishedCase() {
        FraudCase fraudCase = new FraudCase();
        fraudCase.setId(10L);
        fraudCase.setTitle("防诈案例");
        fraudCase.setCaseType("刷单返利");
        fraudCase.setContent("案例内容");
        fraudCase.setDifficultyLevel(2);
        fraudCase.setRiskScore(BigDecimal.valueOf(6.5));
        fraudCase.setViewCount(20);
        fraudCase.setLikeCount(3);
        fraudCase.setLikeRate(BigDecimal.ZERO);
        fraudCase.setWilsonScore(BigDecimal.ZERO);
        fraudCase.setStatus(1);
        fraudCase.setIsFeatured(0);
        fraudCase.setPublishTime(LocalDateTime.now());
        return fraudCase;
    }
}
