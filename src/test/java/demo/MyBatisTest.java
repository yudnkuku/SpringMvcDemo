package demo;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import spring.SqlSessionUtil;
import spring.dao.*;
import spring.entity.Blog;
import spring.entity.LevelEnum;
import spring.entity.Student;

import java.util.ArrayList;
import java.util.List;

public class MyBatisTest {

    @Test
    public void test() {
        String name = "yuding";
        String className = "8";
        DStudent dao = new DStudent();
        System.out.println("根据姓名查询student  >>>>>" + dao.getStudentInfoByName(name));
        System.out.println("根据姓名和班级名称查询student  >>>>" + dao.getStudentInfoByNameAndClass(name, className));
    }

    @Test
    public void testEnumTypeHandler() {
        DEnum dao = new DEnum();
        System.out.println(LevelEnum.valueOf("LOW").ordinal());
        dao.addEnum(LevelEnum.valueOf("LOW"));
    }

    @Test
    public void testAddStudent() {
        Student student = new Student();
        student.setName("jack");
        student.setId(10);
        DStudent dStudent = new DStudent();
        dStudent.addStudent(student);
        System.out.println("添加的学生id:" + student.getId());
    }

    @Test
    public void testBlog() {
        DBlog dBlog = new DBlog();
        Blog blog = dBlog.selectBlog(1);
        System.out.println(blog.toString());
    }

    @Test
    public void testSelectAll() {
        DBlog dBlog = new DBlog();
        Blog blog = dBlog.selectBlogByAll(2, "", 2);
        System.out.println(blog);
    }

    @Test
    public void testSelectBlogsByIds() {
        List ids = new ArrayList();
        ids.add(1);
        ids.add(2);
        List<Blog> blogs = new ArrayList();
        DBlog dBlog = new DBlog();
        blogs = dBlog.selectBlogsByIds(ids);
        for (Blog blog : blogs) {
            System.out.println(blog);
        }
    }

    @Test
    public void testCache() {
        SqlSession sqlSession = SqlSessionUtil.getSession();
        try {
            IStudentDao dao = sqlSession.getMapper(IStudentDao.class);
            String name = "yuding";
            dao.getStudentInfoByName(name);
            sqlSession.commit();
            //new sqlSession
            sqlSession = SqlSessionUtil.getSession();
            dao = sqlSession.getMapper(IStudentDao.class);
            Student s = new Student();
            s.setName("deacon1");
            dao.addStudent(s);
            sqlSession.commit();
            //new sqlSession
            sqlSession = SqlSessionUtil.getSession();
            dao = sqlSession.getMapper(IStudentDao.class);
            dao.getStudentInfoByName(name);

        } finally {
            if(sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    @Test
    public void testUpdate() {
        DBlog dBlog = new DBlog();
        int result = dBlog.updateBlogById(1, "c sharp", 3);
    }
}
