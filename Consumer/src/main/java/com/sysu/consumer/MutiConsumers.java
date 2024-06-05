package com.sysu.consumer;

public class MutiConsumers {
    public static void main(String[] args) {
        MyThread consumers[] = new MyThread[100];
        for(MyThread consumer : consumers){
            consumer = new MyThread();
            consumer.start();
        }
    }
}
