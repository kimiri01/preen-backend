package com.preen.preen_backend.auth.repository;

import com.preen.preen_backend.common.security.CurrentSessionLookup;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

// 회원마다 현재 로그인 정보를 Redis에 저장한다.
@Repository
@RequiredArgsConstructor
public class RedisLoginSessionRepository implements CurrentSessionLookup {

    // 회원 번호가 42라면 auth:session:42라는 이름으로 저장한다.
    private static final String KEY_PREFIX = "auth:session:";

    private static final Duration SESSION_LIFETIME = Duration.ofDays(14);

    // Redis에 로그인 정보를 저장하고, 언제 자동으로 지울지도 함께 정한다.
    private static final RedisScript<Long> SAVE_SESSION = RedisScript.of("""
            redis.call('HSET', KEYS[1],
                'sessionId', ARGV[1],
                'refreshTokenHash', ARGV[2],
                'issuedAt', ARGV[3],
                'expiresAt', ARGV[4])
            return redis.call('PEXPIREAT', KEYS[1], ARGV[4])
            """, Long.class);

    // 현재 세션과 기존 토큰 해시가 모두 맞을 때 새 해시만 저장한다.
    private static final RedisScript<Long> ROTATE_REFRESH_TOKEN = RedisScript.of("""
            if redis.call('HGET', KEYS[1], 'sessionId') ~= ARGV[1]
                or redis.call('HGET', KEYS[1], 'refreshTokenHash') ~= ARGV[2] then
                return 0
            end
            local expiresAt = redis.call('PEXPIRETIME', KEYS[1])
            if expiresAt <= 0 then
                return 0
            end
            redis.call('HSET', KEYS[1], 'refreshTokenHash', ARGV[3])
            return expiresAt
            """, Long.class);

    // 요청한 세션이 현재 세션일 때만 Redis에서 지운다.
    private static final RedisScript<Long> DELETE_SESSION = RedisScript.of("""
            if redis.call('HGET', KEYS[1], 'sessionId') ~= ARGV[1] then
                return 0
            end
            return redis.call('DEL', KEYS[1])
            """, Long.class);

    // Redis에 세션 저장, 조회, 교체, 삭제 명령을 보낸다.
    private final StringRedisTemplate redis;

    // 같은 회원 번호로 다시 저장하면 이전 로그인 정보를 덮어쓴다.
    public Instant save(long userId, LoginSession session) {
        // 최초 로그인 시각에서 14일이 지나면 세션을 자동으로 지운다.
        Instant expiresAt = session.issuedAt().plus(SESSION_LIFETIME);

        // 저장할 이름을 먼저 보내고, 그 뒤에 저장할 값 네 개를 보낸다.
        // 로그인 시각과 만료 시각은 밀리초 단위 숫자를 문자열로 바꿔 보낸다.
        Long result = redis.execute(
                SAVE_SESSION,
                List.of(KEY_PREFIX + userId),
                session.sessionId(),
                session.refreshTokenHash(),
                Long.toString(session.issuedAt().toEpochMilli()),
                Long.toString(expiresAt.toEpochMilli()));
        if (!Long.valueOf(1L).equals(result)) {
            throw new DataRetrievalFailureException("Redis에 로그인 세션을 저장하지 못했습니다.");
        }
        return expiresAt;
    }

    // 해시만 바꾸므로 처음 정한 만료 시각은 그대로 유지된다.
    public Optional<Instant> rotateRefreshToken(
            long userId, String sessionId, String currentHash, String newHash) {
        Long result = redis.execute(
                ROTATE_REFRESH_TOKEN,
                List.of(KEY_PREFIX + userId),
                sessionId,
                currentHash,
                newHash);
        return result != null && result > 0 ? Optional.of(Instant.ofEpochMilli(result)) : Optional.empty();
    }

    // Redis에서 읽은 세션 ID가 요청의 세션 ID와 같을 때만 현재 세션으로 인정한다.
    // Redis 연결 오류는 인증을 처리하는 쪽으로 전달한다.
    @Override
    public boolean isCurrentSession(long userId, String sessionId) {
        Object currentSessionId = redis.opsForHash().get(KEY_PREFIX + userId, "sessionId");
        return currentSessionId instanceof String currentId && currentId.equals(sessionId);
    }

    // 예전 세션에서 로그아웃을 요청해도 새 세션은 유지한다.
    public boolean deleteIfCurrent(long userId, String sessionId) {
        Long result = redis.execute(DELETE_SESSION, List.of(KEY_PREFIX + userId), sessionId);
        return Long.valueOf(1L).equals(result);
    }

    // 비밀번호를 재설정한 회원의 현재 로그인 세션을 지운다.
    public void deleteByUserId(long userId) {
        redis.delete(KEY_PREFIX + userId);
    }

    // 로그인에서 받은 정보를 묶는다. Refresh Token은 해시만 담는다.
    public record LoginSession(
            String sessionId,
            String refreshTokenHash,
            Instant issuedAt) {
    }
}




























