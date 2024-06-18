package com.sysu.server;

import com.sysu.model.RpcRequest;
import com.sysu.model.RpcResponse;
import com.sysu.serializer.JsonSerializer;
import com.sysu.serializer.Serializer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * 处理客户端请求的具体逻辑
 */
public class ServerHandler implements Runnable{
    private Socket socket;  //需要处理的客户端socket
    private final int TIMEOUT = 2000;
    //选择序列化的方式
    //实现1：java内置的序列化
    //final Serializer serializer = new JdkSerializer();
    //实现2：Json序列化
    final Serializer serializer = new JsonSerializer();

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try{
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            //设置io读取/写出超时时间
            socket.setSoTimeout(TIMEOUT);

            //接收客户端发送的请求对象
            RpcRequest rpcRequest = null;
            try{
                //先接受请求对象的字节流的长度，确保能够接收到完整的请求对象字节流
                int len = in.readInt();
                byte[] rpcRequestBytes = new byte[len];
                in.readFully(rpcRequestBytes);
                //将请求对象字节流反序列化成请求对象
                rpcRequest = serializer.deserialize(rpcRequestBytes, RpcRequest.class);
            }catch (SocketTimeoutException e) {
                System.err.println("读取客户端请求数据时，读数据导致的超时: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("读取客户端请求数据时，读数据导致的异常: " + e.getMessage());
            }

            //处理请求并根据处理结果构造响应对象
            RpcResponse rpcResponse;
            try{
                Class<?> serviceImpl = Class.forName(rpcRequest.getServiceImplName());
                Method method = serviceImpl.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(serviceImpl.newInstance(), rpcRequest.getParameters());
                //构造响应对象
                rpcResponse = RpcResponse.builder()
                        .data(result)
                        .dataType(result != null ? result.getClass() : null)
                        .message("Success")
                        .build();
            } catch (ClassNotFoundException | NoSuchMethodException e){
                rpcResponse = RpcResponse.builder()
                        .message(e.getMessage())
                        .exception(e)
                        .build();
                System.err.println("服务或方法为找到: " + e.getMessage());
            } catch (Exception e){
                rpcResponse = RpcResponse.builder()
                        .message(e.getMessage())
                        .exception(e)
                        .build();
                System.err.println("内部服务器错误：" + e.getMessage());
            }

            //向客户端发送响应对象
            try {
                //序列化响应对象
                byte[] rpcResponseBytes = serializer.serialize(rpcResponse);
                //将序列化后的响应对象发送给客户端
                out.writeInt(rpcResponseBytes.length);
                out.write(rpcResponseBytes);
                out.flush();
            }catch (SocketTimeoutException e) {
                System.err.println("发送响应数据时，写数据导致的超时: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("发送响应数据时，写数据导致的异常: " + e.getMessage());
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
