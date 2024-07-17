package com.sysu.service;

import com.sysu.model.Person;

public interface HelloService {
    String sayHello(String name);
    String sayHi(String name);
    String sayHelloToPerson(Person person);
}
