package com.xuecheng.auth;

import java.util.ArrayList;
import java.util.List;

public class TestOOM {
    static class OOMObject {
    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<>();
        System.out.println(1);
        //无限的创建对象放在堆中
        while (true) {
            list.add(new OOMObject());
        }
    }
}
