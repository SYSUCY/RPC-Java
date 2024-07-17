package com.sysu.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysu.model.ServiceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 注册中心给客户端提供的服务
 */
public class RegisterConsumerHandler {
    private String registryAddress;

    public RegisterConsumerHandler(){
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

    public List<ServiceInfo> discoverServiceInfo(String serviceName) {
        String url = "http://" + registryAddress + "/discoverServiceInfo";
        url += "?serviceName=" + serviceName;
        List<ServiceInfo> serviceInfos = new ArrayList<>();
        try {
            URL obj = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // 解析 JSON 响应
                ObjectMapper mapper = new ObjectMapper();
                serviceInfos = mapper.readValue(response.toString(), mapper.getTypeFactory().constructCollectionType(List.class, ServiceInfo.class));
            } else {
                System.out.println("GET request not worked");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serviceInfos;
    }
}
