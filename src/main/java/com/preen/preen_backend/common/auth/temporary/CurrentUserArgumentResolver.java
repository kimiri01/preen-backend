package com.preen.preen_backend.common.auth.temporary;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUser 파라미터에 AuthUser를 넣어줍니다.
 * TODO: 인증 붙이면 SecurityContext/토큰에서 실제 userId를 꺼내도록 교체하세요.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Long TEMP_USER_ID = 1L;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return new AuthUser(TEMP_USER_ID);
    }
}
