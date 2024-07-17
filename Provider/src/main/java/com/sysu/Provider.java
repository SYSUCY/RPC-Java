package com.sysu;

import com.sysu.register.RegisterProviderHandler;
import com.sysu.server.Server;
import com.sysu.service.GoodByeService;
import com.sysu.service.HelloService;
import com.sysu.serviceImpl.GoodByeServiceImpl;
import com.sysu.serviceImpl.HelloServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Provider {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        // 加载配置文件
        try (InputStream input = Provider.class.getClassLoader().getResourceAsStream("address.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return; // 如果加载配置文件失败，直接返回
        }
        // 从配置文件中读取主机名和端口号
        String host = properties.getProperty("provider-host");
        int port = Integer.parseInt(properties.getProperty("provider-port"));
        String serviceAddress = host + ":" + port;

        // 注册服务
        List<String[]> serviceList = new ArrayList<>();
        serviceList.add(new String[]{HelloService.class.getName(), HelloServiceImpl.class.getName()});
        serviceList.add(new String[]{GoodByeService.class.getName(), GoodByeServiceImpl.class.getName()});
        RegisterProviderHandler registerProviderHandler = new RegisterProviderHandler(serviceList, serviceAddress);
        registerProviderHandler.start();

        // 启动服务
        Server server = new Server();
        server.start(host, port);
    }
}
