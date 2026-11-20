package com.preen.preen_backend.accommodation.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * [kny] 로그인한 회원의 ID를 꺼냅니다. (호스트 API 공통)
 *
 * 로그인(AUTH-004)·토큰 필터는 아직 develop에 없습니다. 그래서 지금은 /api/host/** 요청이
 * SecurityConfig에서 401로 막히고, 이 클래스까지 오지 않습니다.
 * 로그인 기능이 붙으면 토큰 필터가 만든 Authentication에서 회원 ID를 다음 순서로 찾습니다.
 *   1) principal이 Long(회원 ID)인 경우
 *   2) getName()이 숫자(회원 ID)인 경우 — JWT의 subject에 회원 ID를 넣는 일반적인 방식
 * 로그인 담당자가 다른 방식으로 회원 ID를 담으면 이 클래스만 고치면 됩니다.
 * 요청 본문이나 헤더로 보낸 회원 ID는 절대 믿지 않습니다.
 */
@Component
public class HostUserResolver {

	public Long requireUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			throw unauthorized();
		}
		if (authentication.getPrincipal() instanceof Long userId && userId > 0) {
			return userId;
		}
		try {
			long userId = Long.parseLong(authentication.getName());
			if (userId > 0) {
				return userId;
			}
		} catch (NumberFormatException ignored) {
			// 회원 ID 형식이 아니면 아래에서 401로 처리합니다.
		}
		throw unauthorized();
	}

	private static ResponseStatusException unauthorized() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
	}
}
