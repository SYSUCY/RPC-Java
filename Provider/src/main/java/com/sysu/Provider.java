package com.sysu;

import com.sysu.register.LocalRegister;
import com.sysu.server.Server;
import com.sysu.service.GoodByeService;
import com.sysu.service.HelloService;
import com.sysu.serviceImpl.GoodByeServiceImpl;
import com.sysu.serviceImpl.HelloServiceImpl;

public class Provider {
    public static void main(String[] args) {
        //to do 将本地注册中心修改成真正的注册中心
        //本地注册，注册的内容是接口：实现类
        LocalRegister.register(HelloService.class.getName(), HelloServiceImpl.class);
        LocalRegister.register(GoodByeService.class.getName(), GoodByeServiceImpl.class);

        Server server = new Server();
        server.start("localhost", 8080);
    }
}
