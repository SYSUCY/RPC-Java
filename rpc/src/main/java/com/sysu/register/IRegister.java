package com.sysu.register;

import java.util.List;

public interface IRegister {
    void register(String serviceName, String serviceAddress, String serviceImplClassName);

    List<String> getServiceAddr(String serviceName);

    String getServiceImplClassName(String serviceName, String serviceAddress);

    void heartbeat(String serviceName, String serviceAddress);
}
