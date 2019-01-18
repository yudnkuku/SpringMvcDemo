package spring.dao;

import org.apache.ibatis.annotations.Param;
import spring.entity.Article;
import spring.entity.Author;
import spring.entity.Blog;

import java.util.List;

public interface IBlogDao {

    Blog selectBlogById(int id);

    Author selectAuthorById(int id);

    List<Article> selectArticleByBlogId(int blogId);

    Blog selectBlogByAll(@Param("blog_id") int id, String name, @Param("author_id") int author_id);

    List<Blog> selectBlogsByIds(List<Integer> idList);

    int updateBlogById(@Param("blog_id") int blog_id, @Param("name") String name, @Param("author_id") int author_id);

}
