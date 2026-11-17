
package com.preen.backend.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailVerificationController {

    private final EmailVerificationService service;

    public EmailVerificationController(
            EmailVerificationService service
    ) {
        this.service = service;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendCode(
            @Valid @RequestBody SendEmailRequest request
    ) {
        service.sendVerificationCode(request.email());

        return ResponseEntity.ok(
                "인증번호가 이메일로 발송되었습니다."
        );
    }

    public record SendEmailRequest(
            @NotBlank
            @Email
            String email
    ) {
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        boolean verified = service.verifyCode(
                request.email(),
                request.code()
        );

        if (!verified) {
            return ResponseEntity.badRequest().body(
                    "인증번호가 올바르지 않거나 만료되었습니다."
            );
        }

        return ResponseEntity.ok(
                "이메일 인증이 완료되었습니다."
        );
    }

    public record VerifyEmailRequest(
            @NotBlank
            @Email
            String email,

            @NotBlank
            @jakarta.validation.constraints.Pattern(
                    regexp = "\\d{6}"
            )
            String code
    ) {
    }

}
