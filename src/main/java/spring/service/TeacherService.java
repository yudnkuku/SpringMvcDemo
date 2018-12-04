package spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("teacher")
public class TeacherService {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.NEVER)
    public void addTeacher() {
        String sql = "insert into teacher (name) values ('t0')";
        jdbcTemplate.execute(sql);
        throw new RuntimeException();
    }
}
