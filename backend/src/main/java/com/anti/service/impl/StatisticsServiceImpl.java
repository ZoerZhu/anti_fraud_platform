package com.anti.service.impl;

import com.anti.entity.DailyStatistics;
import com.anti.entity.DepartmentStatistics;
import com.anti.entity.vo.*;
import com.anti.mapper.DailyStatisticsMapper;
import com.anti.mapper.DepartmentStatisticsMapper;
import com.anti.mapper.StatisticsQueryMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private DailyStatisticsMapper dailyStatisticsMapper;

    @Autowired
    private DepartmentStatisticsMapper departmentStatisticsMapper;

    @Autowired
    private StatisticsQueryMapper statisticsQueryMapper;

    @Autowired
    private FraudCaseMapper fraudCaseMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private UserChallengeRecordMapper userChallengeRecordMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public DashboardVO getDashboardData() {
        DashboardVO dashboard = new DashboardVO();

        LocalDate today = LocalDate.now();

        try {
            DailyStatistics todayStat = dailyStatisticsMapper.selectToday();
            if (todayStat == null) {
                todayStat = calculateTodayStatistics(today);
            }

            dashboard.setTodayViews(todayStat.getTotalPageViews() != null ? todayStat.getTotalPageViews() : 0);
            dashboard.setTodayNewUsers(todayStat.getNewUsers() != null ? todayStat.getNewUsers() : 0);
            dashboard.setTodayActiveUsers(todayStat.getDailyActiveUsers() != null ? todayStat.getDailyActiveUsers() : 0);
            int challengeToday = todayStat.getChallengeCompletions() != null ? todayStat.getChallengeCompletions() : 0;
            try {
                String dayStart = today.format(DATE_FORMATTER) + " 00:00:00";
                String dayEnd = today.plusDays(1).format(DATE_FORMATTER) + " 00:00:00";
                Long live = userChallengeRecordMapper.countPassedOnDay(dayStart, dayEnd);
                if (live != null) {
                    challengeToday = live.intValue();
                }
            } catch (Exception ignored) {
                // 保留快照表中的值
            }
            dashboard.setTotalChallengeCompletions(challengeToday);
            dashboard.setAvgTestScore(todayStat.getAvgTestScore() != null ? todayStat.getAvgTestScore() : BigDecimal.ZERO);
        } catch (Exception e) {
            dashboard.setTodayViews(0);
            dashboard.setTodayNewUsers(0);
            dashboard.setTodayActiveUsers(0);
            dashboard.setTotalChallengeCompletions(0);
            dashboard.setAvgTestScore(BigDecimal.ZERO);
        }

        try {
            dashboard.setTotalCases(fraudCaseMapper.selectCount(null).intValue());
        } catch (Exception e) {
            dashboard.setTotalCases(0);
        }

        try {
            Long totalStudents = statisticsQueryMapper.selectTotalStudents();
            dashboard.setTotalUsers(totalStudents != null ? totalStudents.intValue() : 0);
        } catch (Exception e) {
            dashboard.setTotalUsers(0);
        }

        dashboard.setVisitTrend(getVisitTrend(7));
        dashboard.setFraudTypeDist(getFraudTypeDistribution());
        dashboard.setDepartmentScores(getDepartmentScores());
        dashboard.setTopCases(getTopCases(10));
        dashboard.setHourlyActivity(getHourlyActivity(today.format(DATE_FORMATTER)));

        return dashboard;
    }

    @Override
    public VisitTrendVO getVisitTrend(int days) {
        VisitTrendVO trend = new VisitTrendVO();
        int safeDays = Math.max(1, Math.min(days, 90));
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1);

        List<DailyStatistics> stats = new ArrayList<>();
        try {
            stats = dailyStatisticsMapper.selectVisitTrend(startDate);
        } catch (Exception e) {
            stats = new ArrayList<>();
        }

        if (stats == null || stats.isEmpty()) {
            trend.setDates(new ArrayList<>());
            trend.setPageViews(new ArrayList<>());
            trend.setActiveUsers(new ArrayList<>());
            trend.setNewUsers(new ArrayList<>());
            return trend;
        }

        Map<LocalDate, DailyStatistics> statMap = stats.stream()
                .filter(Objects::nonNull)
                .filter(stat -> stat.getStatDate() != null)
                .collect(Collectors.toMap(DailyStatistics::getStatDate, stat -> stat, (first, second) -> first));

        List<String> dates = new ArrayList<>();
        List<Integer> pageViews = new ArrayList<>();
        List<Integer> activeUsers = new ArrayList<>();
        List<Integer> newUsers = new ArrayList<>();

        for (int i = 0; i < safeDays; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyStatistics stat = statMap.get(date);
            dates.add(date.format(DATE_FORMATTER));
            pageViews.add(stat != null && stat.getTotalPageViews() != null ? stat.getTotalPageViews() : 0);
            activeUsers.add(stat != null && stat.getDailyActiveUsers() != null ? stat.getDailyActiveUsers() : 0);
            newUsers.add(stat != null && stat.getNewUsers() != null ? stat.getNewUsers() : 0);
        }

        trend.setDates(dates);
        trend.setPageViews(pageViews);
        trend.setActiveUsers(activeUsers);
        trend.setNewUsers(newUsers);

        return trend;
    }

    @Override
    public FraudTypeDistVO getFraudTypeDistribution() {
        FraudTypeDistVO dist = new FraudTypeDistVO();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<Map<String, Object>> typeViews;
        try {
            typeViews = statisticsQueryMapper.selectCaseTypeViews(
                    weekAgo.format(DATE_FORMATTER) + " 00:00:00",
                    today.plusDays(1).format(DATE_FORMATTER) + " 00:00:00"
            );
        } catch (Exception e) {
            typeViews = new ArrayList<>();
        }

        if (typeViews == null || typeViews.isEmpty()) {
            dist.setTypes(new ArrayList<>());
            dist.setCounts(new ArrayList<>());
            return dist;
        }

        List<String> types = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (Map<String, Object> item : typeViews) {
            if (item == null) continue;
            String type = item.get("case_type") != null ? item.get("case_type").toString() : "其他";
            Object countObj = item.get("view_count");
            Long count = countObj != null ? ((Number) countObj).longValue() : 0L;
            types.add(type);
            counts.add(count.intValue());
        }

        dist.setTypes(types);
        dist.setCounts(counts);

        return dist;
    }

    @Override
    public List<Map<String, Object>> getTopFraudTypes(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> topTypes;
        try {
            topTypes = statisticsQueryMapper.selectTopFraudTypes(safeLimit);
        } catch (Exception e) {
            topTypes = new ArrayList<>();
        }

        return (topTypes == null ? new ArrayList<Map<String, Object>>() : topTypes).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentScoreVO getDepartmentScores() {
        DepartmentScoreVO deptScore = new DepartmentScoreVO();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<Map<String, Object>> deptScores;
        try {
            deptScores = statisticsQueryMapper.selectDepartmentScores(
                    weekAgo.format(DATE_FORMATTER),
                    today.plusDays(1).format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            deptScores = new ArrayList<>();
        }

        if (deptScores == null || deptScores.isEmpty()) {
            deptScore.setDepartments(new ArrayList<>());
            deptScore.setAvgScores(new ArrayList<>());
            deptScore.setUserCounts(new ArrayList<>());
            deptScore.setCompletionRates(new ArrayList<>());
            return deptScore;
        }

        List<String> departments = new ArrayList<>();
        List<BigDecimal> avgScores = new ArrayList<>();
        List<Integer> userCounts = new ArrayList<>();
        List<BigDecimal> completionRates = new ArrayList<>();

        for (Map<String, Object> item : deptScores) {
            if (item == null) continue;
            String grade = item.get("grade") != null ? item.get("grade").toString() : "";
            String major = item.get("major") != null ? item.get("major").toString() : "";
            departments.add(formatDepartmentLabel(grade, major));
            Object avgScoreObj = item.get("avg_score");
            avgScores.add(avgScoreObj != null ?
                    new BigDecimal(avgScoreObj.toString()).setScale(1, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO);
            Object userCountObj = item.get("user_count");
            userCounts.add(userCountObj != null ?
                    ((Number) userCountObj).intValue() : 0);
            Object completionRateObj = item.get("completion_rate");
            completionRates.add(completionRateObj != null ?
                    new BigDecimal(completionRateObj.toString()).setScale(1, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO);
        }

        deptScore.setDepartments(departments);
        deptScore.setAvgScores(avgScores);
        deptScore.setUserCounts(userCounts);
        deptScore.setCompletionRates(completionRates);

        return deptScore;
    }

    @Override
    public List<Map<String, Object>> getCompletionRate() {
        try {
            return statisticsQueryMapper.selectCompletionRate();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<TopCaseVO> getTopCases(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> topMaps;
        try {
            topMaps = statisticsQueryMapper.selectTopCases(safeLimit);
        } catch (Exception e) {
            topMaps = new ArrayList<>();
        }

        return (topMaps == null ? new ArrayList<Map<String, Object>>() : topMaps).stream()
                .filter(Objects::nonNull)
                .map(m -> {
            TopCaseVO vo = new TopCaseVO();
            Object idObj = m.get("id");
            vo.setId(idObj != null ? ((Number) idObj).longValue() : 0L);
            vo.setTitle((String) m.get("title"));
            vo.setCaseType((String) m.get("case_type"));
            Object viewCountObj = m.get("view_count");
            vo.setViewCount(viewCountObj != null ? ((Number) viewCountObj).intValue() : 0);
            Object likeCountObj = m.get("like_count");
            vo.setLikeCount(likeCountObj != null ? ((Number) likeCountObj).intValue() : 0);
            Object hotScoreObj = m.get("hot_score");
            vo.setHotScore(hotScoreObj != null ? ((Number) hotScoreObj).intValue() : 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HourlyActivityVO> getHourlyActivity(String statDate) {
        List<Map<String, Object>> hourlyData;
        try {
            hourlyData = statisticsQueryMapper.selectHourlyActivity(statDate);
        } catch (Exception e) {
            hourlyData = new ArrayList<>();
        }

        return (hourlyData == null ? new ArrayList<Map<String, Object>>() : hourlyData).stream()
                .filter(Objects::nonNull)
                .map(m -> {
            HourlyActivityVO vo = new HourlyActivityVO();
            Object hourObj = m.get("hour");
            vo.setHour(hourObj != null ? ((Number) hourObj).intValue() : 0);
            Object countObj = m.get("count");
            vo.setCount(countObj != null ? ((Number) countObj).intValue() : 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void triggerStatisticsUpdate() {
        LocalDate today = LocalDate.now();

        DailyStatistics todayStat = calculateTodayStatistics(today);

        DailyStatistics existing = dailyStatisticsMapper.selectByStatDate(today);
        if (existing != null) {
            todayStat.setId(existing.getId());
            dailyStatisticsMapper.updateById(todayStat);
        } else {
            dailyStatisticsMapper.insert(todayStat);
        }

        updateDepartmentStatistics(today);
    }

    private DailyStatistics calculateTodayStatistics(LocalDate date) {
        DailyStatistics stat = new DailyStatistics();
        stat.setStatDate(date);

        String dateStr = date.format(DATE_FORMATTER);
        String nextDateStr = date.plusDays(1).format(DATE_FORMATTER);

        Integer totalViews = 0;
        try {
            Integer result = statisticsQueryMapper.selectTotalPageViews(dateStr);
            totalViews = result != null ? result : 0;
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setTotalPageViews(totalViews);

        int newUsers = 0;
        try {
            List<Map<String, Object>> newUsersList = statisticsQueryMapper.selectNewUsersDaily(dateStr, nextDateStr);
            if (newUsersList != null && !newUsersList.isEmpty()) {
                newUsers = newUsersList.stream()
                        .filter(Objects::nonNull)
                        .mapToInt(m -> {
                            Object val = m.get("new_users");
                            return val != null ? ((Number) val).intValue() : 0;
                        }).sum();
            }
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setNewUsers(newUsers);

        int activeUsers = 0;
        try {
            List<Map<String, Object>> activeUsersList = statisticsQueryMapper.selectDailyActiveUsers(dateStr, nextDateStr);
            if (activeUsersList != null && !activeUsersList.isEmpty()) {
                activeUsers = activeUsersList.stream()
                        .filter(Objects::nonNull)
                        .mapToInt(m -> {
                            Object val = m.get("daily_active");
                            return val != null ? ((Number) val).intValue() : 0;
                        }).sum();
            }
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setDailyActiveUsers(activeUsers);

        int challengeCompletions = 0;
        try {
            String dayStart = dateStr + " 00:00:00";
            String dayEnd = date.plusDays(1).format(DATE_FORMATTER) + " 00:00:00";
            Long cc = userChallengeRecordMapper.countPassedOnDay(dayStart, dayEnd);
            challengeCompletions = cc != null ? cc.intValue() : 0;
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setChallengeCompletions(challengeCompletions);

        BigDecimal avgScore = BigDecimal.ZERO;
        try {
            avgScore = statisticsQueryMapper.selectAverageChallengeScore(dateStr, nextDateStr);
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setAvgTestScore(avgScore != null ? avgScore.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        Integer newPosts = 0;
        try {
            newPosts = statisticsQueryMapper.selectNewPosts(dateStr);
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setNewPosts(newPosts != null ? newPosts : 0);

        Integer newComments = 0;
        try {
            newComments = statisticsQueryMapper.selectNewComments(dateStr);
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setNewComments(newComments != null ? newComments : 0);

        return stat;
    }

    private void updateDepartmentStatistics(LocalDate date) {
        List<Map<String, Object>> deptScores;
        try {
            deptScores = statisticsQueryMapper.selectDepartmentScores(
                    date.format(DATE_FORMATTER),
                    date.plusDays(1).format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            return;
        }

        if (deptScores == null || deptScores.isEmpty()) {
            return;
        }

        List<DepartmentStatistics> existing = new ArrayList<>();
        try {
            existing = departmentStatisticsMapper.selectByStatDate(date);
        } catch (Exception e) {
            existing = new ArrayList<>();
        }

        for (Map<String, Object> dept : deptScores) {
            if (dept == null) continue;
            DepartmentStatistics ds = new DepartmentStatistics();
            ds.setStatDate(date);
            ds.setGrade((String) dept.get("grade"));
            ds.setMajor((String) dept.get("major"));
            Object userCountObj = dept.get("user_count");
            ds.setUserCount(userCountObj != null ? ((Number) userCountObj).intValue() : 0);
            Object avgScoreObj = dept.get("avg_score");
            BigDecimal avgTestScore = avgScoreObj != null ?
                    new BigDecimal(avgScoreObj.toString()).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;
            ds.setAvgTestScore(avgTestScore);
            ds.setAvgKnowledgeLevel(avgTestScore);
            Object completionRateObj = dept.get("completion_rate");
            ds.setCompletionRate(completionRateObj != null ?
                    new BigDecimal(completionRateObj.toString()).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO);

            if (existing != null) {
                Optional<DepartmentStatistics> match = existing.stream()
                        .filter(Objects::nonNull)
                        .filter(e -> Objects.equals(e.getGrade(), ds.getGrade()) &&
                                Objects.equals(e.getMajor(), ds.getMajor()))
                        .findFirst();

                if (match.isPresent()) {
                    ds.setId(match.get().getId());
                    departmentStatisticsMapper.updateById(ds);
                } else {
                    departmentStatisticsMapper.insert(ds);
                }
            } else {
                departmentStatisticsMapper.insert(ds);
            }
        }
    }

    private String formatDepartmentLabel(String grade, String major) {
        String safeGrade = grade == null || grade.isBlank() ? "未分组" : grade;
        String safeMajor = major == null || major.isBlank() ? "" : major;
        return safeMajor.isEmpty() ? safeGrade : safeGrade + "-" + safeMajor;
    }
}
