package com.sysu.consumer;

import com.sysu.model.Person;
import com.sysu.proxy.ProxyFactory;
import com.sysu.service.GoodByeService;
import com.sysu.service.HelloService;

public class Consumer1 {
    public static void main(String[] args) {
        //动态获取HelloService接口的实现类
        HelloService helloService = ProxyFactory.getProxy(HelloService.class);
        //调用HelloService接口的方法1
        String result1 = helloService.sayHello("chenyun");
        //输出返回结果
        System.out.println(result1);

        //调用HelloService接口的方法2
        String result2 = helloService.sayHi("chenyun");
        System.out.println(result2);

        //调用HelloService接口的方法3
        String result3 = helloService.sayHelloToPerson(new Person("chenyun", 20));
        System.out.println(result3);

        //调用GoodByeService接口的方法
        GoodByeService goodByeService = ProxyFactory.getProxy(GoodByeService.class);
        String result4 = goodByeService.sayGoodBye("chenyun");
        System.out.println(result4);
    }
}
