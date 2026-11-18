package com.preen.preen_backend.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 허용할 프론트 주소(app.cors.allowed-origins)를 읽어옵니다.
@ConfigurationProperties("app.cors")
public record SecurityProperties(List<String> allowedOrigins) {}
