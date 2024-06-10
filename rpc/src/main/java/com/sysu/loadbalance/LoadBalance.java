package com.sysu.loadbalance;

import com.sysu.model.ServiceInfo;

import java.util.List;
import java.util.Random;

public class LoadBalance {
    public static ServiceInfo random(List<ServiceInfo> urls){
        Random random = new Random();
        int i = random.nextInt(urls.size());
        return urls.get(i);
    }
}
