package spring.service.impl;

import spring.service.HelloService;

public class HelloServiceImpl implements HelloService {

    public String name;

    public void sayHello() {
        System.out.println("say hello");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
