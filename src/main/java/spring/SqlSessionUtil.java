package spring;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class SqlSessionUtil {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        InputStream is = null;
        try {
            is = Resources.getResourceAsStream("mybatis-config.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
    }

    public static SqlSession getSession() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        return sqlSession;
    }

}
