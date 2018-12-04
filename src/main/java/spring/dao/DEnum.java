package spring.dao;

import org.apache.ibatis.session.SqlSession;
import spring.entity.LevelEnum;

public class DEnum extends DBase {

    public void addEnum(LevelEnum levelEnum) {
        SqlSession sqlSession = openSession();
        try {
            IEnumDao dao = sqlSession.getMapper(IEnumDao.class);
            dao.addLevel(levelEnum);
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }
}
