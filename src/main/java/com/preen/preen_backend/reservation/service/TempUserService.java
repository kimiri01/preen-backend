package com.preen.preen_backend.reservation.service;



import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class TempUserService {

    // 무조건 성인(true)으로 통과시킵니다
    public boolean isAdult(Long userId) {
        return true;
    }

    // 가짜 회원 정보 반환
    public UserMeResponse getMe(Long userId) {
        return new UserMeResponse(
                userId,
                "홍길동",
                "test@staybook.com",
                "010-1234-5678",
                LocalDate.of(1995, 1, 1),
                true
        );
    }


    public record UserMeResponse(
            Long id,
            String name,
            String email,
            String phone,
            LocalDate birthDate,
            //UserRole role,
            boolean signupCompleted
    ) {
    }
}