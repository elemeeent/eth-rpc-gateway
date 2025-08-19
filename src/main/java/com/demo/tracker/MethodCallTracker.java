package com.demo.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class MethodCallTracker {

    private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();

    public void inc(String method) {
        if (method != null) counters.computeIfAbsent(method, k -> new LongAdder())
                .increment();
    }

    public Map<String, Long> snapshot() {
        return counters.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
    }
}