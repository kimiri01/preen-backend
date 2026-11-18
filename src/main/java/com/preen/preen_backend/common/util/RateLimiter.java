package com.preen.preen_backend.common.util;

import lombok.RequiredArgsConstructor;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.exception.CommonErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

// 서버 메모리에서 요청 식별자별 호출 횟수를 제한합니다.
@Component
@RequiredArgsConstructor
public class RateLimiter {
    private record Window(Instant expiresAt, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;


    // 식별자별 60초 호출 제한을 검사하고 초과 요청을 거부합니다.
    public synchronized void check(String key, int limit) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        Window window = windows.getOrDefault(key, new Window(now.plusSeconds(60), 0));
        if (window.count() >= limit || windows.size() >= 10000) {
            throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS);
        }
        windows.put(key, new Window(window.expiresAt(), window.count() + 1));
    }
}
