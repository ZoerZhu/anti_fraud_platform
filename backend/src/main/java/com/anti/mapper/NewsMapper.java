package com.anti.mapper;

import com.anti.entity.News;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 资讯Mapper接口
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {

    @Select("SELECT n.*, nc.name as category_name, u.nickname as author_name, " +
           "(SELECT COUNT(*) FROM news_like WHERE news_id = n.id) as like_count " +
           "FROM news n " +
           "LEFT JOIN news_category nc ON n.category_id = nc.id " +
           "LEFT JOIN sys_user u ON n.author_id = u.id " +
           "WHERE n.id = #{id}")
    News selectNewsDetailById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT n.*, nc.name as category_name, u.nickname as author_name, " +
            "(SELECT COUNT(*) FROM news_like WHERE news_id = n.id) as like_count " +
            "FROM news n " +
            "LEFT JOIN news_category nc ON n.category_id = nc.id " +
            "LEFT JOIN sys_user u ON n.author_id = u.id " +
            "WHERE 1 = 1 " +
            "<if test='status != null'>AND n.status = #{status} </if>" +
            "<if test='categoryId != null'>AND n.category_id = #{categoryId} </if>" +
            "<if test='newsType != null and newsType != \"\"'>AND n.news_type = #{newsType} </if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%') OR n.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY n.is_top DESC, COALESCE(n.publish_time, n.update_time, n.create_time) DESC" +
            "</script>")
    IPage<News> selectNewsPage(Page<News> page,
                               @Param("categoryId") Long categoryId,
                               @Param("newsType") String newsType,
                               @Param("keyword") String keyword,
                               @Param("status") Integer status);

    @Select("SELECT n.*, nc.name as category_name, u.nickname as author_name, " +
            "(SELECT COUNT(*) FROM news_like WHERE news_id = n.id) as like_count " +
            "FROM news n " +
            "LEFT JOIN news_category nc ON n.category_id = nc.id " +
            "LEFT JOIN sys_user u ON n.author_id = u.id " +
            "WHERE n.status = 1 AND n.is_mandatory = 1 " +
            "ORDER BY n.is_top DESC, COALESCE(n.publish_time, n.update_time, n.create_time) DESC " +
            "LIMIT #{limit}")
    List<News> selectRequiredNews(@Param("limit") int limit);

    @Update("UPDATE news SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);
}
