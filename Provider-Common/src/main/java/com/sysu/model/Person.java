package com.sysu.model;

import com.sun.xml.internal.bind.v2.runtime.RuntimeUtil;

import java.io.Serializable;

public class Person implements Serializable
{
    private String name;
    private int age;

    public Person(){

    }

    public Person(String name, int age){
        this.name = name;
        this.age = age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }

    public String toString(){
        return name + ":" + age;
    }
}
