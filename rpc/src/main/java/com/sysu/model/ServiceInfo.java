package com.sysu.model;

public class ServiceInfo {
    private final String serviceAddress;
    private final String serviceImplClassName;

    public ServiceInfo(String serviceAddress, String serviceImplClassName) {
        this.serviceAddress = serviceAddress;
        this.serviceImplClassName = serviceImplClassName;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public String getServiceImplClassName() {
        return serviceImplClassName;
    }
}

