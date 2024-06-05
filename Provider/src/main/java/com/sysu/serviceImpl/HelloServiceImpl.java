package com.sysu.serviceImpl;

import com.sysu.service.HelloService;


public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }

    @Override
    public String sayHi(String name) {
        return "Hi, " + name;
    }
}
