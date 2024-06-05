package com.sysu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcResponse implements Serializable {
    private Object data;    //返回数据

    private Class<?> dataType;  //返回数据类型

    private String message; //返回消息

    private Exception exception;    //返回异常信息
}
