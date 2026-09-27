package com.movie.booking_service.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisSeatHoldService {
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
              if redis.call('exists', key) == 1 then return 0 end
            end
            for i, key in ipairs(KEYS) do
              redis.call('psetex', key, ARGV[2], ARGV[1])
            end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            local released = 0
            for i, key in ipairs(KEYS) do
              if redis.call('get', key) == ARGV[1] then
                redis.call('del', key)
                released = released + 1
              end
            end
            return released
            """, Long.class);
    private static final DefaultRedisScript<Long> ENSURE = new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
              local value = redis.call('get', key)
              if value and value ~= ARGV[1] then return 0 end
            end
            for i, key in ipairs(KEYS) do
              redis.call('psetex', key, ARGV[2], ARGV[1])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisSeatHoldService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean acquire(Long showId, List<Long> seatIds, String token, Duration ttl) {
        Long result = redis.execute(ACQUIRE, keys(showId, seatIds), token, String.valueOf(ttl.toMillis()));
        return result != null && result == 1;
    }

    public void release(Long showId, List<Long> seatIds, String token) {
        redis.execute(RELEASE, keys(showId, seatIds), token);
    }

    public boolean ensure(Long showId, List<Long> seatIds, String token, Duration ttl) {
        Long result = redis.execute(ENSURE, keys(showId, seatIds), token, String.valueOf(ttl.toMillis()));
        return result != null && result == 1;
    }

    public List<Boolean> held(Long showId, List<Long> seatIds) {
        List<String> values = redis.opsForValue().multiGet(keys(showId, seatIds));
        List<Boolean> held = new ArrayList<>(seatIds.size());
        for (int index = 0; index < seatIds.size(); index++) {
            held.add(values != null && index < values.size() && values.get(index) != null);
        }
        return held;
    }

    private List<String> keys(Long showId, List<Long> seatIds) {
        return seatIds.stream().sorted().map(seatId -> "showhub:seat:" + showId + ":" + seatId).toList();
    }
}
