package com.sysu.consumer;

import com.sysu.proxy.ProxyFactory;
import com.sysu.service.HelloService;

public class MyThread extends Thread{
    @Override
    public void run(){
        //动态获取服务接口的实现类
        HelloService helloService = ProxyFactory.getProxy(HelloService.class);
        //调用接口的方法一
        String result1 = helloService.sayHello("chenyun");
        //输出返回结果
        System.out.println(result1);
    }
}
