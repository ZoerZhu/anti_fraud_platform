package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.Comment;
import com.anti.entity.CommentLike;
import com.anti.entity.ForumPost;
import com.anti.entity.dto.CreateCommentRequest;
import com.anti.mapper.CommentLikeMapper;
import com.anti.mapper.CommentMapper;
import com.anti.mapper.ForumPostMapper;
import com.anti.mapper.UserMapper;
import com.anti.service.LeaderboardService;
import com.anti.service.ScoreService;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentLikeMapper commentLikeMapper;
    @Mock
    private ForumPostMapper forumPostMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private CommentServiceImpl service;

    @Test
    void createCommentRejectsMissingPost() {
        when(forumPostMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createComment(request(10L, 0L), 1L));

        assertThat(exception.getMessage()).contains("帖子不存在或不可评论");
        verify(commentMapper, never()).insert(any());
        verify(forumPostMapper, never()).incrementCommentCount(any());
    }

    @Test
    void createCommentRejectsParentCommentFromAnotherPost() {
        when(forumPostMapper.selectById(10L)).thenReturn(post(10L));
        Comment parent = new Comment();
        parent.setId(99L);
        parent.setPostId(11L);
        parent.setStatus(1);
        when(commentMapper.selectById(99L)).thenReturn(parent);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createComment(request(10L, 99L), 1L));

        assertThat(exception.getMessage()).contains("父评论不存在");
        verify(commentMapper, never()).insert(any());
        verify(forumPostMapper, never()).incrementCommentCount(any());
    }

    @Test
    void createCommentRejectsNegativeParentId() {
        when(forumPostMapper.selectById(10L)).thenReturn(post(10L));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createComment(request(10L, -1L), 1L));

        assertThat(exception.getMessage()).contains("父评论ID不能小于0");
        verify(commentMapper, never()).insert(any());
        verify(forumPostMapper, never()).incrementCommentCount(any());
    }

    @Test
    void createCommentIncrementsPostCommentCountAfterValidParent() {
        when(forumPostMapper.selectById(10L)).thenReturn(post(10L));
        Comment parent = new Comment();
        parent.setId(99L);
        parent.setPostId(10L);
        parent.setStatus(1);
        when(commentMapper.selectById(99L)).thenReturn(parent);

        service.createComment(request(10L, 99L), 1L);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getPostId()).isEqualTo(10L);
        assertThat(captor.getValue().getParentId()).isEqualTo(99L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        verify(forumPostMapper).incrementCommentCount(10L);
    }

    @Test
    void createCommentTrimsContentBeforeSaving() {
        when(forumPostMapper.selectById(10L)).thenReturn(post(10L));
        CreateCommentRequest request = request(10L, 0L);
        request.setContent("  注意核对网址  ");

        service.createComment(request, 1L);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("注意核对网址");
    }

    @Test
    void deleteCommentDeletesChildTreeAndLikes() {
        Comment root = comment(100L, 10L, 1L, 0L);
        Comment child = comment(101L, 10L, 2L, 100L);
        Comment grandchild = comment(102L, 10L, 3L, 101L);
        Comment sibling = comment(103L, 10L, 4L, 0L);
        when(commentMapper.selectById(100L)).thenReturn(root);
        when(commentMapper.selectList(any())).thenReturn(List.of(root, child, grandchild, sibling));

        service.deleteComment(100L, 1L);

        verify(commentLikeMapper).delete(any());
        verify(commentMapper).deleteBatchIds(argThat(ids ->
                ids != null
                        && ids.size() == 3
                        && ids.containsAll(List.of(100L, 101L, 102L))
                        && !ids.contains(103L)));
        verify(forumPostMapper, times(3)).decrementCommentCount(10L);
    }

    @Test
    void likeCommentRejectsDisabledCommentBeforeWritingLike() {
        Comment disabled = comment(100L, 10L, 1L, 0L);
        disabled.setStatus(0);
        when(commentMapper.selectById(100L)).thenReturn(disabled);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.likeComment(100L, 2L));

        assertThat(exception.getMessage()).contains("评论不存在");
        verify(commentLikeMapper, never()).insert(any(CommentLike.class));
        verify(commentMapper, never()).incrementLikeCount(any());
    }

    @Test
    void unlikeCommentDoesNotDecrementWhenLikeRecordDoesNotExist() {
        when(commentMapper.selectById(100L)).thenReturn(comment(100L, 10L, 1L, 0L));
        when(commentLikeMapper.delete(any())).thenReturn(0);

        service.unlikeComment(100L, 2L);

        verify(commentMapper, never()).decrementLikeCount(any());
    }

    private CreateCommentRequest request(Long postId, Long parentId) {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setPostId(postId);
        request.setParentId(parentId);
        request.setContent("这是一条评论");
        return request;
    }

    private ForumPost post(Long postId) {
        ForumPost post = new ForumPost();
        post.setId(postId);
        post.setStatus(1);
        return post;
    }

    private Comment comment(Long id, Long postId, Long userId, Long parentId) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setStatus(1);
        return comment;
    }
}
