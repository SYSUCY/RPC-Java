package com.sysu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ServiceInfo {
    private String serviceAddress;
    private  String serviceImplClassName;
}

