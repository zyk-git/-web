package com.example.giftbook.util;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Set<String> tokenSet = ConcurrentHashMap.newKeySet();

    public void add(String token) {
        tokenSet.add(token);
    }

    public boolean valid(String token) {
        return token != null && tokenSet.contains(token);
    }
}
