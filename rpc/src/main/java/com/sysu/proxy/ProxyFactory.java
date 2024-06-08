package com.sysu.proxy;

import com.sysu.loadbalance.LoadBalance;
import com.sysu.model.RpcRequest;
import com.sysu.model.RpcResponse;
import com.sysu.serializer.JdkSerializer;
import com.sysu.serializer.Serializer;
import com.sysu.server.RegisterConsumerHandler;

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
                RpcRequest rpcRequest = new RpcRequest(interfaceClass.getName(), method.getName(),
                        method.getParameterTypes(), args);

                //add 从注册中心中获取提供该服务的服务器IP地址和端口号
                RegisterConsumerHandler registerConsumerHandler = new RegisterConsumerHandler();
                List<String> addrs = registerConsumerHandler.discoverServiceAddress(interfaceClass.getName());
                if(addrs.isEmpty()){
                    throw new RuntimeException("没有服务器提供该服务");
                }
                else{
                    System.out.println("所有的服务发现: " + addrs);
                }

                //add 从多个服务段中选择一个（负载均衡）
                String addr = LoadBalance.random(addrs);
                System.out.println(addr);
                String[] addrParts = addr.split(":");
                String host = addrParts[0];
                int port = Integer.parseInt(addrParts[1]);

                //服务调用，里面才是真正处理网络传输的地方
                //指定序列化器，接口指向实现类的方式提高扩展性
                final Serializer serializer = new JdkSerializer();
                Socket socket = null;

                try{
                    // 尝试与对应的服务端建立连接
                    socket = new Socket(host, port);
                    // 给socket操作设置超时时间
                    socket.setSoTimeout(2000);

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
                        // 请求中带有异常信息，向外抛出
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
