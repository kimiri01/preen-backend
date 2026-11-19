
package com.preen.preen_backend.myPage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MyInfoUpdateRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 100, message = "이름은 100자 이하로 입력해 주세요.")
        String name,

        @NotNull(message = "생년월일을 입력해 주세요.")
        @Past(message = "생년월일은 오늘 이전 날짜여야 합니다.")
        LocalDate birthDate
) {
}
