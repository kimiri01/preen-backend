package com.preen.preen_backend.myPage.dto;

import java.time.LocalDate;

public record MyInfoResponse(
        String name,
        String email,
        LocalDate birthDate
) {
}