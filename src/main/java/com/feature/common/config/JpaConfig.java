package com.feature.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 엔티티의 생성·수정 시각을 자동 기록하도록 설정합니다.
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
