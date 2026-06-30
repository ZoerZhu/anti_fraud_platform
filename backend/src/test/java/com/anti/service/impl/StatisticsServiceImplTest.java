package com.anti.service.impl;

import com.anti.entity.vo.DepartmentScoreVO;
import com.anti.entity.vo.VisitTrendVO;
import com.anti.mapper.DailyStatisticsMapper;
import com.anti.mapper.DepartmentStatisticsMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.StatisticsQueryMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private DailyStatisticsMapper dailyStatisticsMapper;
    @Mock
    private DepartmentStatisticsMapper departmentStatisticsMapper;
    @Mock
    private StatisticsQueryMapper statisticsQueryMapper;
    @Mock
    private FraudCaseMapper fraudCaseMapper;
    @Mock
    private NewsMapper newsMapper;
    @Mock
    private UserChallengeRecordMapper userChallengeRecordMapper;

    @InjectMocks
    private StatisticsServiceImpl service;

    @Test
    void getVisitTrendReturnsEmptySeriesWhenNoSnapshotDataExists() {
        when(dailyStatisticsMapper.selectVisitTrend(any())).thenReturn(List.of());

        VisitTrendVO trend = service.getVisitTrend(7);

        assertThat(trend.getDates()).isEmpty();
        assertThat(trend.getPageViews()).isEmpty();
        assertThat(trend.getActiveUsers()).isEmpty();
        assertThat(trend.getNewUsers()).isEmpty();
    }

    @Test
    void getDepartmentScoresMapsRealQueryRows() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("grade", "2026级");
        row.put("major", "软件工程");
        row.put("avg_score", new BigDecimal("86.26"));
        row.put("user_count", 42L);
        row.put("completion_rate", new BigDecimal("57.55"));
        when(statisticsQueryMapper.selectDepartmentScores(any(), any())).thenReturn(List.of(row));

        DepartmentScoreVO scores = service.getDepartmentScores();

        assertThat(scores.getDepartments()).containsExactly("2026级-软件工程");
        assertThat(scores.getAvgScores()).containsExactly(new BigDecimal("86.3"));
        assertThat(scores.getUserCounts()).containsExactly(42);
        assertThat(scores.getCompletionRates()).containsExactly(new BigDecimal("57.6"));
    }

    @Test
    void triggerStatisticsUpdateLoadsExistingDepartmentRowsOnce() {
        Map<String, Object> software = departmentRow("2026级", "软件工程", 18);
        Map<String, Object> network = departmentRow("2026级", "网络工程", 12);
        when(statisticsQueryMapper.selectDepartmentScores(any(), any())).thenReturn(List.of(software, network));
        when(departmentStatisticsMapper.selectByStatDate(any())).thenReturn(List.of());

        service.triggerStatisticsUpdate();

        verify(departmentStatisticsMapper, times(1)).selectByStatDate(any());
        verify(departmentStatisticsMapper, times(2)).insert(any());
    }

    private Map<String, Object> departmentRow(String grade, String major, int userCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("grade", grade);
        row.put("major", major);
        row.put("avg_score", new BigDecimal("80.00"));
        row.put("user_count", userCount);
        row.put("completion_rate", new BigDecimal("50.00"));
        return row;
    }
}
