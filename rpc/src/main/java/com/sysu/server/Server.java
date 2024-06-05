package com.sysu.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public void start(String hostname, Integer port){
        try (ServerSocket serverSocket = new ServerSocket()){

            //将服务端socket与服务器的IP地址和端口号绑定
            serverSocket.bind(new InetSocketAddress(hostname, port));
            System.out.println("服务端正在监听 " + hostname + ":" + port);

            // 并发处理每个客户端请求
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("新的服务已经建立");
                new Thread(new ServerHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
