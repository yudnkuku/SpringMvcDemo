package spring.dao;


import org.apache.ibatis.session.SqlSession;
import spring.entity.Student;

public class DStudent extends DBase {

    public Student getStudentInfoByName(String name) {
        Student student = null;
        SqlSession sqlSession = openSession();
        try {
            IStudentDao studentDao = sqlSession.getMapper(IStudentDao.class);
            student = studentDao.getStudentInfoByName(name);
        } finally {
            sqlSession.close();
        }
        return student;
    }

    public Student getStudentInfoByNameAndClass(String name, String className) {
        Student student = null;
        SqlSession sqlSession = openSession();
        try {
            IStudentDao studentDao = sqlSession.getMapper(IStudentDao.class);
            student = studentDao.getStudentInfoByNameAndClass(name, className);
        } finally {
            sqlSession.close();
        }
        return student;
    }

    public void addStudent(Student student) {
        SqlSession sqlSession = openSession();
        try {
            IStudentDao studentDao = sqlSession.getMapper(IStudentDao.class);
            studentDao.addStudent(student);
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }
}
