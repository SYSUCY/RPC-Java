package com.sysu.register;

import com.sysu.model.ServiceInfo;

import java.util.List;

public interface IRegister {
    void register(String serviceName, String serviceAddress, String serviceImplClassName);

    List<ServiceInfo> getServiceInfos(String serviceName);

    void heartbeat(String serviceName, String serviceAddress);
}
