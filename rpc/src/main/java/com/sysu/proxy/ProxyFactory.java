package com.sysu.proxy;

import com.sysu.loadbalance.LoadBalance;
import com.sysu.model.RpcRequest;
import com.sysu.model.RpcResponse;
import com.sysu.model.ServiceInfo;
import com.sysu.serializer.JdkSerializer;
import com.sysu.serializer.JsonSerializer;
import com.sysu.serializer.Serializer;
import com.sysu.register.RegisterConsumerHandler;

import javax.xml.ws.Service;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

public class ProxyFactory {
    //根据接口返回实现类
    public static <T> T getProxy(Class interfaceClass){
        //第三个参数就是代理逻辑，就是rpc简化服务端调用的逻辑
        Object proxyInstance = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] args) throws Throwable {
                //服务发现，从注册中心获取提供服务的服务器地址
                RegisterConsumerHandler registerConsumerHandler = new RegisterConsumerHandler();
                List<ServiceInfo> serviceInfos = registerConsumerHandler.discoverServiceInfo(interfaceClass.getName());
                if(serviceInfos.isEmpty()){
                    throw new RuntimeException("没有服务器提供该服务");
                }

                //负载均衡，随机选择一个服务器
                ServiceInfo serviceInfo = LoadBalance.random(serviceInfos);
                String[] addrParts = serviceInfo.getServiceAddress().split(":");
                String host = addrParts[0];
                int port = Integer.parseInt(addrParts[1]);

                //选择序列化的方式
                //实现1：java内置的序列化
                //final Serializer serializer = new JdkSerializer();
                //实现2：Json序列化
                final Serializer serializer = new JsonSerializer();
                //服务调用，其实就是网络传输的过程
                Socket socket = null;
                try{
                    //尝试与负载均衡算法选择的服务器建立连接
                    socket = new Socket(host, port);
                    //设置io读取/写出超时时间
                    socket.setSoTimeout(2000);

                    //从socket中获取输入和输出流
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    //向服务器发送请求对象
                    try{
                        //构造请求对象
                        RpcRequest rpcRequest = RpcRequest.builder()
                                .serviceImplName(serviceInfo.getServiceImplClassName())
                                .methodName(method.getName())
                                .parameterTypes(method.getParameterTypes())
                                .parameters(args)
                                .build();
                        byte[] rpcRequestBytes = serializer.serialize(rpcRequest);
                        //在发送字节流之前，先发送一个长度字段，保证服务端能够接收完整的字节流
                        out.writeInt(rpcRequestBytes.length);
                        out.write(rpcRequestBytes);
                        out.flush();
                    } catch (SocketTimeoutException e) {
                        System.err.println("发送请求到服务端，写数据时出现的超时：" + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("发送请求到服务端，写数据时出现的异常：" + e.getMessage());
                    }

                    //接收服务器返回的响应对象
                    try {
                        //读取响应对象的字节流
                        int len = in.readInt();
                        byte[] responseBytes = new byte[len];
                        in.readFully(responseBytes);
                        //响应对象的字节流反序列化为响应对象
                        RpcResponse rpcResponse = serializer.deserialize(responseBytes, RpcResponse.class);
                        //响应对象带有异常信息，向外抛出
                        if(rpcResponse.getException() != null){
                            throw rpcResponse.getException();
                        }
                        return rpcResponse.getData();
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
        });

        return (T)proxyInstance;
    }
}
