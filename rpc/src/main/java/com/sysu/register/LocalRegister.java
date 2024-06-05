package com.sysu.register;

import java.util.HashMap;
import java.util.Map;

public class LocalRegister {
    private static Map<String, Class> map = new HashMap<>();

    public static void register(String interfaceName, Class implClass){
        map.put(interfaceName, implClass);
    }

    public static Class getImplClass(String interfaceName){
        return map.get(interfaceName);
    }
}
