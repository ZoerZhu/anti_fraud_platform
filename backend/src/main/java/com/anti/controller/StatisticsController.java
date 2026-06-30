package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.common.Result;
import com.anti.entity.vo.*;
import com.anti.mapper.DailyStatisticsMapper;
import com.anti.mapper.DepartmentStatisticsMapper;
import com.anti.service.StatisticsService;
import com.anti.entity.DailyStatistics;
import com.anti.entity.DepartmentStatistics;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private DailyStatisticsMapper dailyStatisticsMapper;

    @Autowired
    private DepartmentStatisticsMapper departmentStatisticsMapper;

    /**
     * 获取管理后台看板数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardVO> getDashboard() {
        return Result.success(statisticsService.getDashboardData());
    }

    /**
     * 获取访问量趋势
     */
    @GetMapping("/visit/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VisitTrendVO> getVisitTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.getVisitTrend(days));
    }

    /**
     * 获取诈骗类型分布
     */
    @GetMapping("/fraud/types")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<FraudTypeDistVO> getFraudTypeDistribution() {
        return Result.success(statisticsService.getFraudTypeDistribution());
    }

    /**
     * 获取高频诈骗类型TOP N
     */
    @GetMapping("/fraud/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getTopFraudTypes(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(statisticsService.getTopFraudTypes(limit));
    }

    /**
     * 获取各院系测试得分统计
     */
    @GetMapping("/department/scores")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DepartmentScoreVO> getDepartmentScores() {
        return Result.success(statisticsService.getDepartmentScores());
    }

    /**
     * 获取学生学习完成率统计
     */
    @GetMapping("/completion/rate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getCompletionRate() {
        return Result.success(statisticsService.getCompletionRate());
    }

    /**
     * 获取TOP案例排行榜
     */
    @GetMapping("/cases/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<TopCaseVO>> getTopCases(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsService.getTopCases(limit));
    }

    /**
     * 获取用户活跃度热力图数据
     */
    @GetMapping("/activity/hourly")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<HourlyActivityVO>> getHourlyActivity(
            @RequestParam(required = false) String statDate) {
        if (statDate == null || statDate.isEmpty()) {
            statDate = java.time.LocalDate.now().toString();
        }
        return Result.success(statisticsService.getHourlyActivity(statDate));
    }

    /**
     * 手动触发统计数据更新
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refreshStatistics() {
        statisticsService.triggerStatisticsUpdate();
        return Result.success();
    }

    /**
     * 导出每日统计数据(Excel)
     */
    @GetMapping("/export/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportDailyStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        DateRange range = resolveDateRange(startDate, endDate, LocalDate.now().minusMonths(1), LocalDate.now());
        List<DailyStatistics> list = dailyStatisticsMapper.selectDateRange(range.startDate(), range.endDate());

        List<StatisticsExportVO> exportList = safeList(list).stream().map(stat -> {
            StatisticsExportVO vo = new StatisticsExportVO();
            vo.setStatDate(stat.getStatDate());
            vo.setDailyActiveUsers(defaultInt(stat.getDailyActiveUsers()));
            vo.setNewUsers(defaultInt(stat.getNewUsers()));
            vo.setTotalPageViews(defaultInt(stat.getTotalPageViews()));
            vo.setChallengeCompletions(defaultInt(stat.getChallengeCompletions()));
            vo.setAvgTestScore(defaultDecimal(stat.getAvgTestScore()));
            vo.setNewPosts(defaultInt(stat.getNewPosts()));
            vo.setNewComments(defaultInt(stat.getNewComments()));
            vo.setCreateTime(stat.getCreateTime() != null ? stat.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            return vo;
        }).collect(java.util.stream.Collectors.toList());

        String filename = URLEncoder.encode("每日统计数据_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StatisticsExportVO.class).sheet("每日统计").doWrite(exportList);
    }

    /**
     * 导出院系统计数据(Excel)
     */
    @GetMapping("/export/department")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportDepartmentStatistics(
            @RequestParam(required = false) String statDate,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        List<DepartmentStatistics> list;
        if (hasText(statDate) && !hasText(startDate) && !hasText(endDate)) {
            LocalDate date = parseDate(statDate, "statDate");
            list = departmentStatisticsMapper.selectByStatDate(date);
        } else {
            DateRange range = resolveDateRange(startDate, endDate, LocalDate.now().minusMonths(1), LocalDate.now());
            list = departmentStatisticsMapper.selectDateRange(range.startDate(), range.endDate());
        }

        List<DepartmentExportVO> exportList = safeList(list).stream().map(stat -> {
            DepartmentExportVO vo = new DepartmentExportVO();
            vo.setStatDate(stat.getStatDate() != null ? stat.getStatDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
            vo.setGrade(defaultText(stat.getGrade()));
            vo.setMajor(defaultText(stat.getMajor()));
            vo.setUserCount(defaultInt(stat.getUserCount()));
            vo.setAvgKnowledgeLevel(defaultDecimal(stat.getAvgKnowledgeLevel()));
            vo.setAvgTestScore(defaultDecimal(stat.getAvgTestScore()));
            vo.setCompletionRate(defaultDecimal(stat.getCompletionRate()));
            return vo;
        }).collect(java.util.stream.Collectors.toList());

        String filename = URLEncoder.encode("院系统计数据_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), DepartmentExportVO.class).sheet("院系统计").doWrite(exportList);
    }

    private DateRange resolveDateRange(String startDate,
                                       String endDate,
                                       LocalDate defaultStartDate,
                                       LocalDate defaultEndDate) {
        LocalDate start = hasText(startDate) ? parseDate(startDate, "startDate") : defaultStartDate;
        LocalDate end = hasText(endDate) ? parseDate(endDate, "endDate") : defaultEndDate;
        if (start.isAfter(end)) {
            throw new BusinessException(400, "开始日期不能晚于结束日期");
        }
        if (start.plusYears(1).isBefore(end)) {
            throw new BusinessException(400, "导出日期范围不能超过一年");
        }
        return new DateRange(start, end);
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new BusinessException(400, fieldName + "格式必须为yyyy-MM-dd");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
