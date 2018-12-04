package spring.entity;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class User {

    @NotEmpty
    public String username;

    @NotEmpty
    public String password;

    @Email
    public String email;

    public User() {
    }

    public User(String username, String password, String email, Date birthday) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.birthday = birthday;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    @DateTimeFormat(pattern = "yy-MM-dd")
    public Date birthday;


}
