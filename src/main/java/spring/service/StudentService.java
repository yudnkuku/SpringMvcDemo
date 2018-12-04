package spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("student")
public class StudentService {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TeacherService teacherService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void addStudent() {
        String sql = "insert into student (name) values ('st0')";
        jdbcTemplate.execute(sql);
        teacherService.addTeacher();
//        throw new RuntimeException();
    }
}
