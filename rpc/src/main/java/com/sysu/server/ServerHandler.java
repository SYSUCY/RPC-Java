package com.sysu.server;

import com.sysu.model.RpcResponse;
import com.sysu.model.RpcRequest;
import com.sysu.register.LocalRegister;
import com.sysu.serializer.JdkSerializer;
import com.sysu.serializer.Serializer;
import lombok.AllArgsConstructor;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * 处理客户端请求的具体逻辑
 */
@AllArgsConstructor
public class ServerHandler implements Runnable{
    private Socket socket;  //需要处理的客户端socket
    final int timeout = 5000;
    long lastReadTime; //记录最后一次读取数据的时间

    public ServerHandler(Socket socket) {
        this.socket = socket;
        this.lastReadTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        final Serializer serializer = new JdkSerializer();

        try{
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            //设置超时时间
            socket.setSoTimeout(timeout);
            // 不断处理客户端socket发送来的请求
            while (true){
                RpcRequest rpcRequest = null;
                try{
                    // 根据长度字段获取请求的数据流（防止粘包）
                    if (in.available() > 0) {
                        lastReadTime = System.currentTimeMillis();  //更新最后一次读取数据的时间
                        int len = in.readInt();
                        if (len <= 0) {
                            System.err.println("无效的请求长度: " + len);
                            break;
                        }

                        byte[] rpcRequestBytes = new byte[len];
                        in.readFully(rpcRequestBytes);

                        // 反序列化请求
                        rpcRequest = serializer.deserialize(rpcRequestBytes, RpcRequest.class);
                    } else {
                        //检查是否超时
                        if(System.currentTimeMillis() - lastReadTime > timeout){
                            System.err.println("超过2秒没有接收到客户端请求，连接超时关闭");
                            break;
                        }
                        try {
                            Thread.sleep(1000); // 等待1秒钟
                            continue;
                        } catch (InterruptedException e) {
                            System.err.println("线程等待被中断: " + e.getMessage());
                        }
                    }
                }catch (SocketTimeoutException e) {
                    System.err.println("读取客户端请求数据时，读数据导致的超时: " + e.getMessage());
                    break; // 退出循环，结束当前线程
                } catch (IOException e) {
                    System.err.println("读取客户端请求数据时，读数据导致的异常: " + e.getMessage());
                    break; // 退出循环，结束当前线程
                }

                // 处理请求并将处理结果封装到响应对象中
                RpcResponse rpcResponse;
                try{
                    //用反射机制去调用对应的方法
                    //从本地注册中心中获取调用服务的实现类
                    Class<?> serviceImpl = LocalRegister.getImplClass(rpcRequest.getServiceName());
                    Method method = serviceImpl.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                    Object result = method.invoke(serviceImpl.newInstance(), rpcRequest.getParameters());

                    rpcResponse = RpcResponse.builder()
                            .data(result)
                            .dataType(result != null ? result.getClass() : null)
                            .message("Success")
                            .build();
                }catch (Exception e){
                    rpcResponse = RpcResponse.builder()
                            .message(e.getMessage())
                            .exception(e)
                            .build();
                    System.err.println("调用映射服务的方法时，处理数据导致的异常: " + e.getMessage());
                }

                //序列化响应对象
                byte[] rpcResponseBytes = serializer.serialize(rpcResponse);
                try {
                    //将序列化后的响应对象发送给客户端
                    out.writeInt(rpcResponseBytes.length);
                    out.write(rpcResponseBytes);
                    out.flush();
                }catch (SocketTimeoutException e) {
                    System.err.println("发送响应数据时，写数据导致的超时: " + e.getMessage());
                    break;
                } catch (IOException e) {
                    System.err.println("发送响应数据时，写数据导致的异常: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            // 捕获IO流操作异常
            System.err.println("I/O错误: " + e.getMessage());
        } finally {
            try{
                socket.close();
            }catch (IOException e){
                System.err.println("关闭socket时发生错误: " + e.getMessage());
            }
        }
    }
}
