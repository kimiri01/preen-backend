package com.feature.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 허용할 프론트 주소 등 보안 설정값을 읽어옵니다.
@ConfigurationProperties("app.security")
public record SecurityProperties(List<String> allowedOrigins) {}
