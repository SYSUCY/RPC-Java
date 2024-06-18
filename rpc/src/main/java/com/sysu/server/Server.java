package com.sysu.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class Server {
    public void start(String hostname, Integer port){
        try (ServerSocket serverSocket = new ServerSocket()){
            //将服务端socket与服务器的IP地址和端口号绑定
            serverSocket.bind(new InetSocketAddress(hostname, port));
            System.out.println("服务端正在监听 " + hostname + ":" + port);

            ExecutorService executorService = new ThreadPoolExecutor(
                    10,  // corePoolSize: 核心线程数
                    20,  // maximumPoolSize: 最大线程数
                    60L, // keepAliveTime: 空闲线程存活时间
                    TimeUnit.SECONDS, // unit: 时间单位（秒）
                    new LinkedBlockingQueue<>() // workQueue: 工作队列
            );

            //监控线程池状态
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
                System.out.println(
                        "Pool Size: " + tpe.getPoolSize() +
                        ", Active Threads: " + tpe.getActiveCount() +
                        ", Completed Tasks: " + tpe.getCompletedTaskCount() +
                        ", Total Tasks: " + tpe.getTaskCount()
                );
            }, 0, 10, TimeUnit.SECONDS);

            // 并发处理每个客户端请求
            while(true){
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ServerHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
