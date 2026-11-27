package com.preen.preen_backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 작업(예: 결제 시간이 지난 예약 만료)을 실행하도록 설정합니다.
@Configuration
@EnableScheduling
public class SchedulingConfig {}
