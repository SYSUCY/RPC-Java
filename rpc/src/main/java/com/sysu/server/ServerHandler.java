package com.sysu.server;

import com.sysu.model.RpcRequest;
import com.sysu.model.RpcResponse;
import com.sysu.serializer.JdkSerializer;
import com.sysu.serializer.JsonSerializer;
import com.sysu.serializer.Serializer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerHandler implements Runnable{
    private Socket socket;  //需要处理的客户端socket
    private final int TIMEOUT = 2000;
    final Serializer serializer = new JdkSerializer();

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            socket.setSoTimeout(TIMEOUT);

            RpcRequest rpcRequest = null;
            try {
                int len = in.readInt();
                byte[] rpcRequestBytes = new byte[len];
                in.readFully(rpcRequestBytes);
                rpcRequest = serializer.deserialize(rpcRequestBytes, RpcRequest.class);
            } catch (SocketTimeoutException e) {
                System.err.println("读取客户端请求数据时，读数据导致的超时: " + e.getMessage());
                sendErrorResponse(out, e);
                return;
            } catch (IOException e) {
                System.err.println("读取客户端请求数据时，读数据导致的异常: " + e.getMessage());
                sendErrorResponse(out, e);
                return;
            }

            RpcResponse rpcResponse;
            try {
                Class<?> serviceImpl = Class.forName(rpcRequest.getServiceImplName());
                Method method = serviceImpl.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(serviceImpl.newInstance(), rpcRequest.getParameters());
                rpcResponse = RpcResponse.builder()
                        .data(result)
                        .dataType(result != null ? result.getClass() : null)
                        .message("Success")
                        .build();
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                rpcResponse = RpcResponse.builder()
                        .message(e.getMessage())
                        .exception(e)
                        .build();
                System.err.println("服务或方法未找到: " + e.getMessage());
            } catch (Exception e) {
                rpcResponse = RpcResponse.builder()
                        .message(e.getMessage())
                        .exception(e)
                        .build();
                System.err.println("内部服务器错误：" + e.getMessage());
            }

            try {
                byte[] rpcResponseBytes = serializer.serialize(rpcResponse);
                out.writeInt(rpcResponseBytes.length);
                out.write(rpcResponseBytes);
                out.flush();
            } catch (SocketTimeoutException e) {
                System.err.println("发送响应数据时，写数据导致的超时: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("发送响应数据时，写数据导致的异常: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("I/O错误: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("关闭socket时发生错误: " + e.getMessage());
            }
        }
    }

    private void sendErrorResponse(DataOutputStream out, Exception e) {
        RpcResponse errorResponse = RpcResponse.builder()
                .message(e.getMessage())
                .exception(e)
                .build();
        try {
            byte[] rpcResponseBytes = serializer.serialize(errorResponse);
            out.writeInt(rpcResponseBytes.length);
            out.write(rpcResponseBytes);
            out.flush();
        } catch (IOException ioException) {
            System.err.println("发送错误响应数据时，写数据导致的异常: " + ioException.getMessage());
        }
    }
}
