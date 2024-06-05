package com.sysu.server;

import com.sysu.model.RpcResponse;
import com.sysu.model.RpcRequest;
import com.sysu.serializer.JdkSerializer;
import com.sysu.serializer.Serializer;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ClientHandler {
    final private int timeout = 2000;   //超时时间

    public RpcResponse send(String host, Integer port, RpcRequest rpcRequest){
        //指定序列化器，接口指向实现类的方式提高扩展性
        final Serializer serializer = new JdkSerializer();
        Socket socket = null;

        try{
            // 尝试与对应的服务端建立连接
            socket = new Socket(host, port);
            // 给socket操作设置超时时间
            socket.setSoTimeout(timeout);

            // 从socket中获取输入和输出流
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            try{
                // 序列化请求
                byte[] rpcRequestBytes = serializer.serialize(rpcRequest);

                // 发送带有长度的请求，解决半包粘包问题
                out.writeInt(rpcRequestBytes.length);
                out.write(rpcRequestBytes);
                out.flush();
            } catch (SocketTimeoutException e) {
                System.err.println("发送请求到服务端，写数据时出现的超时：" + e.getMessage());
            } catch (IOException e) {
                System.err.println("发送请求到服务端，写数据时出现的异常：" + e.getMessage());
            }

            try {
                // 读取响应对象的字节流
                int len = in.readInt();
                byte[] responseBytes = new byte[len];
                in.readFully(responseBytes);

                RpcResponse rpcResponse = serializer.deserialize(responseBytes, RpcResponse.class);
                return rpcResponse;
            }catch (SocketTimeoutException e) {
                System.err.println("从服务端接收响应时，读数据导致的超时: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("从服务端接收响应时，读数据导致的异常: " + e.getMessage());
            }
        } catch (UnknownHostException e) {
            System.err.println("主机未知: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.err.println("与服务端建立连接时产生的超时或等待服务端处理时，等待处理导致的超时: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("与服务端建立连接时产生的异常或等待服务端处理时，等待处理导致的异常：" + e.getMessage());
        } finally {
            // 关闭socket去释放资源
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("关闭socket失败: " + e.getMessage());
                }
            }
        }
        return null;
    }
}
