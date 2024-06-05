package com.sysu.serviceImpl;

import com.sysu.service.GoodByeService;

public class GoodByeServiceImpl implements GoodByeService {
    @Override
    public String sayGoodBye(String name) {
        return "Bye~, "  +name;
    }
}
