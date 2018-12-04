package spring.ws;

import javax.xml.ws.Endpoint;

public class WebServicePublish {
    public static void main(String[] args) {
        String address = "http://10.99.20.56:8889/WebService";
        Endpoint.publish(address,new WebServiceImpl());
        System.out.println("WebService发布成功");
    }
}
