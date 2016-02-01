package com.hitechbunny;
import java.util.HashMap;
import java.util.Map;
public class Table {
    public static final Map<String, Rollout> table = new HashMap<>(1000000);
    public static void add(String key, double rlScore) {
        Rollout r = new Rollout();
        r.rlScore = rlScore;
        table.put(key, r);
    }
    static {

    }
}
