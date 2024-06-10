package com.sysu.register;

import com.sysu.model.ServiceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteRegister implements IRegister{
    private final Map<String, List<ServiceInfo>> serviceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> heartbeatMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RemoteRegister() {
        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void register(String serviceName, String serviceAddress, String serviceImplClassName) {
        ServiceInfo serviceInfo = new ServiceInfo(serviceAddress, serviceImplClassName);
        serviceMap.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(serviceInfo);
        heartbeat(serviceName, serviceAddress);
    }

    @Override
    public synchronized List<ServiceInfo> getServiceInfos(String serviceName) {
        return new ArrayList<>(serviceMap.getOrDefault(serviceName, new ArrayList<>()));
    }

    @Override
    public synchronized void heartbeat(String serviceName, String serviceAddress) {
        String key = serviceName + "@" + serviceAddress;
        heartbeatMap.put(key, System.currentTimeMillis());
    }

    public void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();
        long timeout = 30000; // 30秒没有心跳则认为服务下线

        heartbeatMap.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > timeout) {
                String key = entry.getKey();
                String[] parts = key.split("@");
                String serviceName = parts[0];
                String serviceAddress = parts[1];
                List<ServiceInfo> serviceInfos = serviceMap.get(serviceName);
                if (serviceInfos != null) {
                    serviceInfos.removeIf(serviceInfo -> serviceInfo.getServiceAddress().equals(serviceAddress));
                    if (serviceInfos.isEmpty()) {
                        serviceMap.remove(serviceName);
                    }
                }
                return true;
            }
            return false;
        });
    }
}
