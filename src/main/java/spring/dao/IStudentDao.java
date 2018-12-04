package spring.dao;

import org.apache.ibatis.annotations.Param;
import spring.entity.Student;

import java.util.List;

public interface IStudentDao {

    /**
     * 根据name查询student
     * @param name
     * @return
     */
    Student getStudentInfoByName(String name);

    /**
     * 根据name & className 查询student
     * @param name
     * @param className
     * @return
     */
    Student getStudentInfoByNameAndClass(@Param("student") String name, @Param("class") String className);

    /**
     * 根据name模糊查询students info
     * @param name
     * @return
     */
    List<Student> getStudentsInfo(String name);

    /**
     * 插入student
     * @param student
     */
    void addStudent(Student student);

    /**
     * 删除student
     * @param id
     */
    void deleteStudent(int id);

}
