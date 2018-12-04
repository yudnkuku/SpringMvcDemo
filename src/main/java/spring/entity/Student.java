package spring.entity;

import java.util.List;

public class Student {

    private int id;

    private String name;

    private Class sClass;

    private List<Teacher> teachers;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getsClass() {
        return sClass;
    }

    public void setsClass(Class sClass) {
        this.sClass = sClass;
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
    }

    @Override
    public String toString() {
        if (sClass == null || teachers == null || teachers.isEmpty()) {
            return "Student{" + "id=" + id + ", name=" + name + "}";
        }
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sClass=" + sClass +
                ", teachers=" + teachers +
                '}';
    }
}
