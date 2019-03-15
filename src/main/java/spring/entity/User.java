package spring.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;


import java.util.Date;

@JsonRootName("user")
public class User {

    public String username;

    @JsonIgnore
    public String password;

    @JsonProperty(value = "e-mail")
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

    public Date birthday;


}
