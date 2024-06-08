package com.sysu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterConsumerHandler {
    private final String registryAddress;

    public RegisterConsumerHandler(){
        registryAddress = "localhost:8081";
    }

    public List<String> discoverServiceAddress(String serviceName){
        String url = "http://" + registryAddress + "/discover";
        url += "?serviceName=" + serviceName;
        List<String> addresses = new ArrayList<>();
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
                // 将逗号分隔的字符串转换回List<String>
                String responseString = response.toString();
                addresses = Arrays.asList(responseString.split(","));
            } else {
                System.out.println("GET request not worked");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addresses;
    }

    public String discoverServiceImplClassName(String serviceName, String serviceAddress) {
        String url = "http://" + registryAddress + "/getServiceImplClassName";
        url += "?serviceName=" + serviceName;
        url += "&serviceAddress=" + serviceAddress;
        String serviceImplClassName = null;
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
                serviceImplClassName = response.toString();
            } else {
                System.out.println("GET request not worked");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serviceImplClassName;
    }
}
