package com.sysu.proxy;

import com.sysu.model.RpcResponse;
import com.sysu.model.RpcRequest;
import com.sysu.server.ClientHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {
    //根据接口返回实现类
    public static <T> T getProxy(Class interfaceClass){
        //第三个参数就是代理逻辑，就是rpc简化服务端调用的逻辑
        Object proxyInstance = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] args) throws Throwable {
                RpcRequest rpcRequest = new RpcRequest(interfaceClass.getName(), method.getName(),
                        method.getParameterTypes(), args);

                //to do：从注册中心中获取提供该服务的服务器IP地址和端口号
                //System.out.println(interfaceClass.getName());
                //List<URL> urls = MapRemoteRegister.get(interfaceClass.getName());

                //to do：从多个服务段中选择一个（负载均衡）
                //URL url = LoadBalance.random(urls);

                //服务调用，里面才是真正处理网络传输的地方
                ClientHandler clientHandler = new ClientHandler();
                RpcResponse rpcResponse = clientHandler.send("localhost", 8080, rpcRequest);

                // 请求中带有异常信息，向外抛出
                if(rpcResponse.getException() != null){
                    throw rpcResponse.getException();
                }

                return rpcResponse.getData();
            }
        });

        return (T)proxyInstance;
    }
}
