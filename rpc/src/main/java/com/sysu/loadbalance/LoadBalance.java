package com.sysu.loadbalance;

import com.sysu.model.ServiceInfo;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalance {
    private static AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public enum Strategy {
        RANDOM,
        ROUND_ROBIN,
    }

    public static ServiceInfo choose(Strategy strategy, List<ServiceInfo> serviceInfos) {
        switch (strategy) {
            case RANDOM:
                return random(serviceInfos);
            case ROUND_ROBIN:
                return roundRobin(serviceInfos);
            default:
                throw new IllegalArgumentException("未知策略: " + strategy);
        }
    }

    private static ServiceInfo random(List<ServiceInfo> serviceInfos) {
        Random random = new Random();
        int i = random.nextInt(serviceInfos.size());
        return serviceInfos.get(i);
    }

    private static ServiceInfo roundRobin(List<ServiceInfo> serviceInfos) {
        int index = roundRobinIndex.getAndIncrement() % serviceInfos.size();
        return serviceInfos.get(index);
    }
}

