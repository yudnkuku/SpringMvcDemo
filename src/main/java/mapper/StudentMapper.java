package mapper;

import java.util.List;
import mybatis.entity.Student;

public interface StudentMapper {
    int deleteByPrimaryKey(Integer studentId);

    int insert(Student record);

    Student selectByPrimaryKey(Integer studentId);

    List<Student> selectAll();

    int updateByPrimaryKey(Student record);
}