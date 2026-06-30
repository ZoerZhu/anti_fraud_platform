package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.entity.DepartmentStatistics;
import com.anti.mapper.DailyStatisticsMapper;
import com.anti.mapper.DepartmentStatisticsMapper;
import com.anti.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsControllerTest {

    private StatisticsController controller;
    private DailyStatisticsMapper dailyStatisticsMapper;
    private DepartmentStatisticsMapper departmentStatisticsMapper;

    @BeforeEach
    void setUp() {
        controller = new StatisticsController();
        dailyStatisticsMapper = mock(DailyStatisticsMapper.class);
        departmentStatisticsMapper = mock(DepartmentStatisticsMapper.class);
        ReflectionTestUtils.setField(controller, "statisticsService", mock(StatisticsService.class));
        ReflectionTestUtils.setField(controller, "dailyStatisticsMapper", dailyStatisticsMapper);
        ReflectionTestUtils.setField(controller, "departmentStatisticsMapper", departmentStatisticsMapper);
    }

    @Test
    void exportDailyStatisticsUsesClosedDateRange() throws Exception {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(dailyStatisticsMapper.selectDateRange(start, end)).thenReturn(List.of());

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDailyStatistics("2026-06-01", "2026-06-30", response);

        verify(dailyStatisticsMapper).selectDateRange(start, end);
        assertThat(response.getHeader("Content-disposition")).contains("xlsx");
    }

    @Test
    void exportDepartmentStatisticsUsesClosedDateRange() throws Exception {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(departmentStatisticsMapper.selectDateRange(start, end)).thenReturn(List.of());

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDepartmentStatistics(null, "2026-06-01", "2026-06-30", response);

        verify(departmentStatisticsMapper).selectDateRange(start, end);
        assertThat(response.getHeader("Content-disposition")).contains("xlsx");
    }

    @Test
    void exportRejectsInvalidDateRange() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.exportDailyStatistics("2026-07-01", "2026-06-30", response));

        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).contains("开始日期不能晚于结束日期");
    }

    @Test
    void exportDailyStatisticsHandlesNullMapperResult() throws Exception {
        when(dailyStatisticsMapper.selectDateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDailyStatistics("2026-06-01", "2026-06-30", response);

        assertThat(response.getHeader("Content-disposition")).contains("xlsx");
        assertThat(response.getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void exportDepartmentStatisticsHandlesNullSnapshotFields() throws Exception {
        DepartmentStatistics snapshot = new DepartmentStatistics();
        when(departmentStatisticsMapper.selectByStatDate(LocalDate.of(2026, 6, 29))).thenReturn(List.of(snapshot));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDepartmentStatistics("2026-06-29", null, null, response);

        verify(departmentStatisticsMapper).selectByStatDate(LocalDate.of(2026, 6, 29));
        assertThat(response.getHeader("Content-disposition")).contains("xlsx");
        assertThat(response.getContentAsByteArray()).isNotEmpty();
    }
}
