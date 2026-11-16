package com.feature;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 서버에서 공통으로 사용할 시간 기준을 설정합니다.
@Configuration
public class AppConfig {
    // 인증 만료 등 시간 계산에 사용할 UTC 기준 시계를 제공합니다.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
