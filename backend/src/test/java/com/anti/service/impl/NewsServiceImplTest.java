package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.News;
import com.anti.entity.NewsBrowseLog;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.NewsLikeMapper;
import com.anti.mapper.NewsMapper;
import com.anti.service.AchievementService;
import com.anti.service.CacheRefreshService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsMapper newsMapper;
    @Mock
    private NewsLikeMapper newsLikeMapper;
    @Mock
    private NewsBrowseLogMapper browseLogMapper;
    @Mock
    private AchievementService achievementService;
    @Mock
    private CacheRefreshService cacheRefreshService;

    @InjectMocks
    private NewsServiceImpl service;

    @Test
    void getAdminNewsPageAllowsDraftStatusFilter() {
        Page<News> pageResult = new Page<>(1, 10, 0);
        when(newsMapper.selectNewsPage(any(Page.class), isNull(), eq("warning"), eq("关键词"), eq(0)))
                .thenReturn(pageResult);

        service.getAdminNewsPage(1, 10, null, " warning ", " 关键词 ", 0);

        verify(newsMapper).selectNewsPage(any(Page.class), isNull(), eq("warning"), eq("关键词"), eq(0));
    }

    @Test
    void getRequiredNewsUsesDedicatedMandatoryQuery() {
        News required = publishedNews();
        required.setIsMandatory(1);
        when(newsMapper.selectRequiredNews(3)).thenReturn(List.of(required));

        List<News> result = service.getRequiredNews(3, null);

        assertThat(result).containsExactly(required);
        verify(newsMapper).selectRequiredNews(3);
    }

    @Test
    void publishNewsRejectsIncompleteDraft() {
        News draft = publishedNews();
        draft.setStatus(0);
        draft.setCategoryId(null);
        when(newsMapper.selectById(10L)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.publishNews(10L));

        assertThat(exception.getMessage()).contains("分类不能为空");
        verify(newsMapper, never()).updateById(any());
        verify(cacheRefreshService, never()).handlePublishEvent(any(), any());
    }

    @Test
    void topNewsRejectsInvalidSwitchValue() {
        when(newsMapper.selectById(10L)).thenReturn(publishedNews());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.topNews(10L, 2));

        assertThat(exception.getMessage()).contains("置顶状态只能为0或1");
        verify(newsMapper, never()).updateById(any());
    }

    @Test
    void getNewsDetailRejectsDraft() {
        News draft = publishedNews();
        draft.setStatus(0);
        when(newsMapper.selectNewsDetailById(10L)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getNewsDetail(10L, 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).contains("资讯不存在");
    }

    @Test
    void likeNewsRejectsDraft() {
        News draft = publishedNews();
        draft.setStatus(0);
        when(newsMapper.selectById(10L)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.likeNews(10L, 1L));

        assertThat(exception.getCode()).isEqualTo(404);
        verify(newsLikeMapper, never()).insert(any());
    }

    @Test
    void addBrowseRecordClampsNegativeStayDurationAndRefreshesCache() {
        when(newsMapper.selectById(10L)).thenReturn(publishedNews());

        service.addBrowseRecord(10L, 1L, -5);

        ArgumentCaptor<NewsBrowseLog> captor = ArgumentCaptor.forClass(NewsBrowseLog.class);
        verify(browseLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getStayDuration()).isZero();
        verify(cacheRefreshService).handleBrowseEvent(1L, "news", 10L);
        verify(achievementService).refreshContinuousLearningStreak(1L);
    }

    @Test
    void addBrowseRecordClampsHugeStayDuration() {
        when(newsMapper.selectById(10L)).thenReturn(publishedNews());

        service.addBrowseRecord(10L, 1L, 999999);

        ArgumentCaptor<NewsBrowseLog> captor = ArgumentCaptor.forClass(NewsBrowseLog.class);
        verify(browseLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getStayDuration()).isEqualTo(7200);
    }

    private News publishedNews() {
        News news = new News();
        news.setId(10L);
        news.setTitle("防诈提醒");
        news.setContent("内容");
        news.setCategoryId(1L);
        news.setNewsType("warning");
        news.setStatus(1);
        news.setIsTop(0);
        news.setIsMandatory(0);
        return news;
    }
}
