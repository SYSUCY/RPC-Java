package com.sysu.proxy;

import com.sysu.loadbalance.LoadBalance;
import com.sysu.model.RpcRequest;
import com.sysu.model.RpcResponse;
import com.sysu.model.ServiceInfo;
import com.sysu.register.RegisterConsumerHandler;
import com.sysu.serializer.JdkSerializer;
import com.sysu.serializer.JsonSerializer;
import com.sysu.serializer.Serializer;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProxyFactory {
    private static final int MAX_RETRY_TIMES = 3;

    public static <T> T getProxy(Class interfaceClass){
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] args) throws Throwable {
                RegisterConsumerHandler registerConsumerHandler = new RegisterConsumerHandler();
                List<ServiceInfo> serviceInfos = registerConsumerHandler.discoverServiceInfo(interfaceClass.getName());
                if(serviceInfos.isEmpty()){
                    throw new RuntimeException("没有服务器提供该服务");
                }

                Set<ServiceInfo> triedServices = new HashSet<>();
                final Serializer serializer = new JdkSerializer();

                for (int attempt = 0; attempt < MAX_RETRY_TIMES; attempt++) {
                    ServiceInfo serviceInfo = LoadBalance.choose(LoadBalance.Strategy.RANDOM, serviceInfos);
                    if (triedServices.contains(serviceInfo)) {
                        continue;
                    }
                    triedServices.add(serviceInfo);

                    String[] addrParts = serviceInfo.getServiceAddress().split(":");
                    String host = addrParts[0];
                    int port = Integer.parseInt(addrParts[1]);

                    try (Socket socket = new Socket(host, port)) {
                        socket.setSoTimeout(2000);
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                        RpcRequest rpcRequest = RpcRequest.builder()
                                .serviceImplName(serviceInfo.getServiceImplClassName())
                                .methodName(method.getName())
                                .parameterTypes(method.getParameterTypes())
                                .parameters(args)
                                .build();
                        byte[] rpcRequestBytes = serializer.serialize(rpcRequest);
                        out.writeInt(rpcRequestBytes.length);
                        out.write(rpcRequestBytes);
                        out.flush();

                        int len = in.readInt();
                        byte[] responseBytes = new byte[len];
                        in.readFully(responseBytes);
                        RpcResponse rpcResponse = serializer.deserialize(responseBytes, RpcResponse.class);
                        if(rpcResponse.getException() != null){
                            throw rpcResponse.getException();
                        }
                        return rpcResponse.getData();
                    } catch (UnknownHostException e) {
                        System.err.println("主机未知: " + e.getMessage());
                    } catch (SocketTimeoutException e) {
                        System.err.println("超时: " + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("I/O错误: " + e.getMessage());
                    }
                }
                throw new RuntimeException("所有服务器均无法连接");
            }
        });
    }
}
