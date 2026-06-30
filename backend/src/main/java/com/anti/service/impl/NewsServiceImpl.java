package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.News;
import com.anti.entity.NewsBrowseLog;
import com.anti.entity.NewsLike;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.NewsLikeMapper;
import com.anti.mapper.NewsMapper;
import com.anti.service.AchievementService;
import com.anti.service.CacheRefreshService;
import com.anti.service.NewsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper newsMapper;
    private final NewsLikeMapper newsLikeMapper;
    private final NewsBrowseLogMapper browseLogMapper;
    private final AchievementService achievementService;
    private final @Lazy CacheRefreshService cacheRefreshService;

    private static final int MAX_STAY_DURATION_SECONDS = 7200;

    @Override
    public IPage<News> getNewsPage(Integer pageNum, Integer pageSize, Long categoryId, String newsType, String keyword, Long userId) {
        Page<News> page = new Page<>(safePageNum(pageNum), safePageSize(pageSize));
        IPage<News> result = newsMapper.selectNewsPage(
                page,
                categoryId,
                normalizeType(newsType),
                normalizeKeyword(keyword),
                1
        );
        fillLikeState(result.getRecords(), userId);
        return result;
    }

    @Override
    public IPage<News> getAdminNewsPage(Integer pageNum, Integer pageSize, Long categoryId, String newsType, String keyword, Integer status) {
        Integer normalizedStatus = null;
        if (status != null) {
            validateSwitchValue(status, "资讯状态");
            normalizedStatus = status;
        }
        Page<News> page = new Page<>(safePageNum(pageNum), safePageSize(pageSize));
        return newsMapper.selectNewsPage(
                page,
                categoryId,
                normalizeType(newsType),
                normalizeKeyword(keyword),
                normalizedStatus
        );
    }

    @Override
    public List<News> getRequiredNews(Integer limit, Long userId) {
        int safeLimit = limit == null ? 3 : Math.max(1, Math.min(limit, 10));
        List<News> records = newsMapper.selectRequiredNews(safeLimit);
        fillLikeState(records, userId);
        return records;
    }

    @Override
    public News getNewsDetail(Long id, Long userId) {
        News news = newsMapper.selectNewsDetailById(id);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new BusinessException(404, "资讯不存在");
        }
        
        if (userId != null) {
            int liked = newsLikeMapper.countByNewsIdAndUserId(id, userId);
            news.setIsLiked(liked > 0);
        }
        
        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News createNews(News news, Long authorId) {
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (news.getContent() == null || news.getContent().trim().isEmpty()) {
            throw new BusinessException(400, "内容不能为空");
        }
        validateNewsType(news.getNewsType());
        validateSwitchValue(news.getIsTop(), "置顶状态");
        validateSwitchValue(news.getIsMandatory(), "必读状态");
        news.setTitle(news.getTitle().trim());
        news.setContent(news.getContent().trim());
        news.setSummary(StringUtils.hasText(news.getSummary()) ? news.getSummary().trim() : null);
        news.setCoverImage(StringUtils.hasText(news.getCoverImage()) ? news.getCoverImage().trim() : null);
        news.setNewsType(StringUtils.hasText(news.getNewsType()) ? news.getNewsType().trim() : "news");
        news.setAuthorId(authorId);
        news.setViewCount(0);
        news.setStatus(0);
        news.setIsTop(news.getIsTop() != null ? news.getIsTop() : 0);
        news.setIsMandatory(news.getIsMandatory() != null ? news.getIsMandatory() : 0);
        newsMapper.insert(news);
        log.info("创建资讯: {}", news.getTitle());
        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News updateNews(News news) {
        News existing = newsMapper.selectById(news.getId());
        if (existing == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        if (news.getTitle() != null) {
            if (!StringUtils.hasText(news.getTitle())) {
                throw new BusinessException(400, "标题不能为空");
            }
            existing.setTitle(news.getTitle().trim());
        }
        if (news.getContent() != null) {
            if (!StringUtils.hasText(news.getContent())) {
                throw new BusinessException(400, "内容不能为空");
            }
            existing.setContent(news.getContent().trim());
        }
        if (news.getSummary() != null) {
            existing.setSummary(StringUtils.hasText(news.getSummary()) ? news.getSummary().trim() : null);
        }
        if (news.getCoverImage() != null) {
            existing.setCoverImage(StringUtils.hasText(news.getCoverImage()) ? news.getCoverImage().trim() : null);
        }
        if (news.getCategoryId() != null) {
            existing.setCategoryId(news.getCategoryId());
        }
        if (news.getNewsType() != null) {
            validateNewsType(news.getNewsType());
            existing.setNewsType(news.getNewsType().trim());
        }
        if (news.getIsTop() != null) {
            validateSwitchValue(news.getIsTop(), "置顶状态");
            existing.setIsTop(news.getIsTop());
        }
        if (news.getIsMandatory() != null) {
            validateSwitchValue(news.getIsMandatory(), "必读状态");
            existing.setIsMandatory(news.getIsMandatory());
        }
        newsMapper.updateById(existing);
        log.info("更新资讯: {}", existing.getTitle());
        cacheRefreshService.handleUpdateEvent("news", existing.getId());
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        newsMapper.deleteById(id);
        log.info("删除资讯: {}", news.getTitle());
        
        // 异步刷新相关缓存
        cacheRefreshService.handleDeleteEvent("news", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        validatePublishable(news);
        news.setStatus(1);
        if (news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        newsMapper.updateById(news);
        log.info("发布资讯: {}", news.getTitle());
        
        // 异步刷新相关缓存
        cacheRefreshService.handlePublishEvent("news", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topNews(Long id, Integer isTop) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        validateSwitchValue(isTop, "置顶状态");
        news.setIsTop(isTop);
        newsMapper.updateById(news);
        log.info("设置资讯 {} 置顶状态: {}", news.getTitle(), isTop == 1 ? "是" : "否");
        cacheRefreshService.handleUpdateEvent("news", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMandatory(Long id, Integer isMandatory) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        validateSwitchValue(isMandatory, "必读状态");
        news.setIsMandatory(isMandatory);
        newsMapper.updateById(news);
        log.info("设置资讯 {} 必读状态: {}", news.getTitle(), isMandatory == 1 ? "是" : "否");
        cacheRefreshService.handleUpdateEvent("news", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new BusinessException(404, "资讯不存在");
        }
        newsMapper.incrementViewCount(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeNews(Long newsId, Long userId) {
        News news = newsMapper.selectById(newsId);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new BusinessException(404, "资讯不存在");
        }
        int existing = newsLikeMapper.countByNewsIdAndUserId(newsId, userId);
        if (existing > 0) {
            throw new BusinessException(400, "已点赞过该资讯");
        }
        NewsLike like = new NewsLike();
        like.setNewsId(newsId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        newsLikeMapper.insert(like);
        log.info("用户 {} 点赞资讯 {}", userId, newsId);
        
        // 异步刷新相关缓存
        cacheRefreshService.handleLikeEvent(userId, "news", newsId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikeNews(Long newsId, Long userId) {
        LambdaQueryWrapper<NewsLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NewsLike::getNewsId, newsId).eq(NewsLike::getUserId, userId);
        int deleted = newsLikeMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("用户 {} 取消点赞资讯 {}", userId, newsId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBrowseRecord(Long newsId, Long userId, Integer stayDuration) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        News news = newsMapper.selectById(newsId);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new BusinessException(404, "资讯不存在");
        }
        NewsBrowseLog browseLog = new NewsBrowseLog();
        browseLog.setNewsId(newsId);
        browseLog.setUserId(userId);
        browseLog.setBrowseTime(LocalDateTime.now());
        browseLog.setStayDuration(clampStayDuration(stayDuration));
        int rows = browseLogMapper.insert(browseLog);
        log.debug("资讯浏览记录插入结果 rows={}, logId={}", rows, browseLog.getId());
        cacheRefreshService.handleBrowseEvent(userId, "news", newsId);
        try {
            achievementService.refreshContinuousLearningStreak(userId);
        } catch (Exception e) {
            log.warn("资讯浏览后连续学习成就校验失败 userId={} msg={}", userId, e.getMessage());
        }
    }

    @Override
    public IPage<News> getUserBrowseHistory(Long userId, Integer pageNum, Integer pageSize) {
        Page<NewsBrowseLog> page = new Page<>(pageNum, pageSize);
        IPage<NewsBrowseLog> browseLogs = browseLogMapper.selectUserBrowseHistory(page, userId);
        
        Page<News> result = new Page<>(pageNum, pageSize);
        result.setTotal(browseLogs.getTotal());
        
        for (NewsBrowseLog browseLog : browseLogs.getRecords()) {
            News news = newsMapper.selectById(browseLog.getNewsId());
            if (news != null) {
                result.getRecords().add(news);
            }
        }
        
        return result;
    }

    private void fillLikeState(List<News> records, Long userId) {
        if (records == null || records.isEmpty()) {
            return;
        }
        if (userId != null) {
            List<Long> ids = records.stream().map(News::getId).collect(Collectors.toList());
            List<Long> likedIds = newsLikeMapper.selectLikedNewsIds(userId, ids);
            Set<Long> likedSet = new HashSet<>(likedIds);
            for (News news : records) {
                news.setIsLiked(likedSet.contains(news.getId()));
            }
        }
    }

    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        return Math.max(1, Math.min(pageSize, 100));
    }

    private int clampStayDuration(Integer stayDuration) {
        if (stayDuration == null) {
            return 0;
        }
        return Math.max(0, Math.min(stayDuration, MAX_STAY_DURATION_SECONDS));
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeType(String newsType) {
        if (!StringUtils.hasText(newsType)) {
            return null;
        }
        String normalizedType = newsType.trim();
        validateNewsType(normalizedType);
        return normalizedType;
    }

    private void validateNewsType(String newsType) {
        if (!StringUtils.hasText(newsType)) {
            return;
        }
        String normalizedType = newsType.trim();
        if (!Set.of("news", "warning", "policy").contains(normalizedType)) {
            throw new BusinessException(400, "资讯类型只能为news、warning或policy");
        }
    }

    private void validateSwitchValue(Integer value, String fieldName) {
        if (value != null && value != 0 && value != 1) {
            throw new BusinessException(400, fieldName + "只能为0或1");
        }
    }

    private void validatePublishable(News news) {
        if (!StringUtils.hasText(news.getTitle())) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (!StringUtils.hasText(news.getContent())) {
            throw new BusinessException(400, "内容不能为空");
        }
        if (news.getCategoryId() == null) {
            throw new BusinessException(400, "分类不能为空");
        }
        validateNewsType(news.getNewsType());
        validateSwitchValue(news.getIsTop(), "置顶状态");
        validateSwitchValue(news.getIsMandatory(), "必读状态");
    }
}
