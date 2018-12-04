package spring.dao;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;

public class DBase {

    private static SqlSessionFactory sqlSessionFactory = null;

    public DBase() {
        if (sqlSessionFactory != null) {
            return;
        }
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
    }

    protected SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }
}
