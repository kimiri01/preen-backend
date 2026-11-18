package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.common.util.EmailUtil;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

// 가입 인증 증명, 회원 정보와 약관 동의 내용을 전달합니다.
public record SignupRequest(

        // 이메일은 필수이며, 이메일 형식에 맞아야 하고 최대 254자까지 입력 가능
        @NotBlank @Email @Size(max = 254) String email,

        // 이메일 인증 성공 후 받은 가입 토큰. 필수이며 최대 100자
        @NotBlank @Size(max = 100) String signupToken,

        // 이름은 공백만 입력할 수 없으며 최대 100자
        @NotBlank @Size(max = 100) String name,

        // 생년월일은 필수이며 오늘보다 이전 날짜여야 함
        @NotNull @Past LocalDate birthDate,

        // 비밀번호는 필수이며 8~50자. 문자 조합 규칙은 SignupService에서 검사
        @NotBlank @Size(min = 8, max = 50) String password,

        // 비밀번호 확인은 필수이며 최대 50자. 비밀번호와 일치하는지는 SignupService에서 검사
        @NotBlank @Size(max = 50) String passwordConfirmation,

        // 서비스 이용약관은 필수 동의이므로 true만 허용
        @NotNull @AssertTrue Boolean termsAgreed,

        // 개인정보 처리 약관은 필수 동의이므로 true만 허용
        @NotNull @AssertTrue Boolean privacyAgreed,

        // 마케팅 수신 동의는 선택. true 또는 false를 명시적으로 보내야 함
        @NotNull Boolean marketingAgreed,

        // 화면에 표시된 약관의 버전. 필수이며 최대 50자
        @NotBlank @Size(max = 50) String termsVersion) {

    // record의 컴팩트 생성자. 요청 객체를 만들 때 입력값을 정리함
    public SignupRequest {
        if (email != null) email = EmailUtil.normalize(email);  // 이메일이 null이 아니면 프로젝트의 이메일 정규화 규칙을 적용
        if (name != null) name = name.strip();  // 이름이 null이 아니면 앞뒤 공백을 제거

    }

    // 객체를 로그에 출력할 때 이메일, 비밀번호, 가입 토큰 등이 노출되지 않도록 함
    @Override public String toString() { return "SignupRequest[redacted]"; }
}
