package com.sysu.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 序列化待扩展
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcRequest implements Serializable {
    private String serviceName;   //调用方法属于的接口

    private String methodName;  //调用的方法

    private Class[] parameterTypes; //参数类型

    private Object[] parameters;    //参数值
}
