package com.sysu.register;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 注册中心给服务端提供的服务
 */
public class RegisterProviderHandler {
    private final List<String[]> serviceNamesAndServiceImplNames;
    private final String serviceAddress;
    private final String registryAddress;

    public RegisterProviderHandler(List<String[]> serviceNamesAndServiceImplNames, String serviceAddress){
        this.serviceNamesAndServiceImplNames = serviceNamesAndServiceImplNames;
        this.serviceAddress = serviceAddress;
        registryAddress = "localhost:8081";
    }

    public void start() throws Exception {
        for(String[] service : serviceNamesAndServiceImplNames){
            registerService(service[0],  service[1]);
        }

        // 定期发送心跳信号
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for(String[] service : serviceNamesAndServiceImplNames){
                    sendHeartbeat(service[0]);
                }
            }
        }, 0, 10000);   //每10秒发送一次心跳

        // 模拟服务启动
        System.out.println("Services " + serviceNamesAndServiceImplNames + " started at " + serviceAddress);
    }

    private void registerService(String serviceName, String serviceImplName) throws Exception {
        URL url = new URL("http://" + registryAddress + "/register");
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        String requestBody = "serviceName="+serviceName;
        requestBody += "&serviceAddress="+ serviceAddress;
        requestBody += "&serviceImplName=" + serviceImplName;
        try (OutputStream os = httpURLConnection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = httpURLConnection.getResponseCode();
        if(responseCode == 200){
            System.out.println("Service registered successfully");
        }else{
            throw new RuntimeException("Failed to register service");
        }
    }

    private void sendHeartbeat(String serviceName) {
        try {
            URL url = new URL("http://" + registryAddress + "/heartbeat");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            String requestBody = "serviceName=" + serviceName + "&serviceAddress=" + serviceAddress;
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Failed to send heartbeat for service " + serviceName + ": " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
