package com.feature.auth.dto;

import com.feature.common.util.EmailUtil;
import jakarta.validation.constraints.*;

// 이메일, 인증 요청 ID, 사용자가 입력한 인증번호를 받는 DTO
public record VerifyEmailRequest(
        // 이메일은 필수이며, 이메일 형식에 맞아야 하고 최대 254자
        @NotBlank @Email @Size(max = 254) String email,

        // 인증번호 발송 요청이 성공했을 때 서버가 돌려준 인증 요청 ID
        @NotNull Long verificationId,

        // 인증번호는 숫자 6자리이며, 0으로 시작할 수 있어 문자열로 받음
        @NotBlank @Pattern(regexp = "[0-9]{6}") String code
) {
    // record의 컴팩트 생성자. 객체를 만들 때 이메일을 정규화함
    public VerifyEmailRequest {
        if (email != null) email = EmailUtil.normalize(email);
    }

    // 객체를 로그에 출력할 때 이메일과 인증번호가 노출되지 않도록 함
    @Override
    public String toString() {
        return "VerifyEmailRequest[redacted]";
    }
}