package com.hitechbunny;

import java.util.HashMap;
import java.util.Map;

public class Table {
    public static final Map<String, Rollout> table = new HashMap<>(1000000);

    private static void add(String key, double rlScore) {
        Rollout r = new Rollout();
        r.rlScore = rlScore;
        table.put(key, r);
    }

    static {
//        add("000000000000000000000000000000000000001000",0.9999);
//        add("000000000000000000000000000000000000000100",0.0);
//        add("000000000000000000000000000000000000000010",0.0);
//        add("000000000000000000000000000000000000000001",0.0);
//        add("000000000000000000000000000000020000001000",0.9999);
//        add("000000000000000000000000000000000000001200",0.0);
//        add("000000000000000000000000000000000000001020",0.0);
//        add("000000000000000000000000000000000000001002",0.0);
    }
}
