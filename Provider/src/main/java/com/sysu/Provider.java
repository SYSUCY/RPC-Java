package com.sysu;

import com.sysu.server.RegisterProviderHandler;
import com.sysu.server.Server;
import com.sysu.service.HelloService;
import com.sysu.serviceImpl.HelloServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class Provider {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8080;
        String serviceAddress = host + ":" + port;

        // 注册服务
        List<String[]> serviceList = new ArrayList<>();
        serviceList.add(new String[]{HelloService.class.getName(), "com.sysu.serviceImpl.HelloServiceImpl"});
        RegisterProviderHandler registerProviderHandler = new RegisterProviderHandler(serviceList, serviceAddress);
        registerProviderHandler.start();

        // 启动服务
        Server server = new Server();
        server.start("localhost", 8080);
    }
}
