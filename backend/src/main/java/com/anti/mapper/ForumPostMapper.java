package com.anti.mapper;

import com.anti.entity.ForumPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 论坛帖子Mapper接口
 */
@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {

    /**
     * 根据类型分页查询帖子(按时间排序)
     */
    IPage<ForumPost> selectByTypeOrderByTime(Page<ForumPost> page, @Param("postType") String postType);

    /**
     * 根据类型分页查询帖子(按点赞数排序)
     */
    IPage<ForumPost> selectByTypeOrderByLike(Page<ForumPost> page, @Param("postType") String postType);

    /**
     * 根据类型分页查询帖子(按评论数排序)
     */
    IPage<ForumPost> selectByTypeOrderByComment(Page<ForumPost> page, @Param("postType") String postType);

    /**
     * 关键词搜索帖子
     */
    IPage<ForumPost> searchByKeyword(Page<ForumPost> page,
                                     @Param("keyword") String keyword,
                                     @Param("postType") String postType);

    /**
     * 增加浏览量
     */
    @Update("UPDATE forum_post SET view_count = view_count + 1 WHERE id = #{postId}")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 增加评论数
     */
    @Update("UPDATE forum_post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    int incrementCommentCount(@Param("postId") Long postId);

    /**
     * 减少评论数
     */
    @Update("UPDATE forum_post SET comment_count = comment_count - 1 WHERE id = #{postId} AND comment_count > 0")
    int decrementCommentCount(@Param("postId") Long postId);
}
