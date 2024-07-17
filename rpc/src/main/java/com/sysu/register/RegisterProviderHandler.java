package com.sysu.register;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 注册中心给服务端提供的服务
 */
public class RegisterProviderHandler {
    private final List<String[]> serviceNamesAndServiceImplNames;
    private final String serviceAddress;
    private String registryAddress;

    public RegisterProviderHandler(List<String[]> serviceNamesAndServiceImplNames, String serviceAddress){
        this.serviceNamesAndServiceImplNames = serviceNamesAndServiceImplNames;
        this.serviceAddress = serviceAddress;
        Properties properties = new Properties();
        // 加载配置文件
        try (InputStream input = RegisterConsumerHandler.class.getClassLoader().getResourceAsStream("register.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return; // 如果加载配置文件失败，直接返回
        }
        // 从配置文件中读取主机名和端口号
        String host = properties.getProperty("host");
        int port = Integer.parseInt(properties.getProperty("port"));
        registryAddress = host + ":" + port;
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
            System.out.println(serviceName + "注册成功");
        }else{
            throw new RuntimeException(serviceName + "注册成功");
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
                System.err.println("发送心跳失败 " + serviceName + ": " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
