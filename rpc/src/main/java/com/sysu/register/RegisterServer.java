package com.sysu.register;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class RegisterServer {
    private final IRegister serviceRegistry = new RemoteRegister();

    public void start(String host, int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);

        // 服务注册处理器
        server.createContext("/register", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                if("POST".equals(httpExchange.getRequestMethod())){
                    try {
                        InputStream inputStream = httpExchange.getRequestBody();
                        byte[] bytes = new byte[(int) inputStream.available()];
                        inputStream.read(bytes);
                        String query = new String(bytes, StandardCharsets.UTF_8);
                        String[] parts = query.split("&");
                        String serviceName = parts[0].split("=")[1];
                        String serviceAddress = parts[1].split("=")[1];
                        String serviceImplClassName = parts[2].split("=")[1];
                        serviceRegistry.register(serviceName, serviceAddress, serviceImplClassName);
                        String response = "Service registered: " + serviceName + " at " + serviceAddress;
                        httpExchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream os = httpExchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } catch (IOException e) {
                        // 处理异常，例如记录日志、返回错误响应等
                        e.printStackTrace();
                    }

                } else {
                    httpExchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // 服务发现处理器
        server.createContext("/discover", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                if("GET".equals(httpExchange.getRequestMethod())){
                    String query = httpExchange.getRequestURI().getQuery();
                    String serviceName = query.split("=")[1];
                    List<String> address = serviceRegistry.getServiceAddr(serviceName);
                    String response = address.stream().collect(Collectors.joining(","));
                    httpExchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }else{
                    httpExchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // 服务实现类名称处理器
        server.createContext("/getServiceImplClassName", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                if ("GET".equals(httpExchange.getRequestMethod())) {
                    String query = httpExchange.getRequestURI().getQuery();
                    String serviceName = query.split("&")[0].split("=")[1];
                    String serviceAddress = query.split("&")[1].split("=")[1];
                    String serviceImplClassName = serviceRegistry.getServiceImplClassName(serviceName, serviceAddress);
                    httpExchange.sendResponseHeaders(200, serviceImplClassName.getBytes().length);
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(serviceImplClassName.getBytes());
                    os.close();
                } else {
                    httpExchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // 服务心跳处理器
        server.createContext("/heartbeat", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {

                if("POST".equals(httpExchange.getRequestMethod())) {
                    try {
                        InputStream inputStream = httpExchange.getRequestBody();
                        byte[] bytes = new byte[(int) inputStream.available()];
                        inputStream.read(bytes);
                        String query = new String(bytes, StandardCharsets.UTF_8);
                        String[] parts = query.split("&");
                        String serviceName = parts[0].split("=")[1];
                        String serviceAddress = parts[1].split("=")[1];
                        // 更新心跳
                        serviceRegistry.heartbeat(serviceName, serviceAddress);
                        String response = "Heartbeat received for: " + serviceName + " at " + serviceAddress;

                        httpExchange.sendResponseHeaders(200, response.getBytes().length);

                        OutputStream os = httpExchange.getResponseBody();

                        os.write(response.getBytes());

                        os.close();
                    }catch (IOException e) {
                        // 处理异常，例如记录日志、返回错误响应等
                        e.printStackTrace();
                    }
                } else {
                    httpExchange.sendResponseHeaders(405, -1);
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Registry server started on address " + host + ":" + port);

        // 定期检测服务超时
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ((RemoteRegister) serviceRegistry).checkHeartbeats();
            }
        }, 0, 5000); // 每5秒检查一次
    }

    public static void main(String[] args) throws IOException {
        new RegisterServer().start("localhost", 8081);
    }
}
