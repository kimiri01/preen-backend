
package com.preen.preen_backend.myPage.service;

import com.preen.preen_backend.auth.entity.User;
import com.preen.preen_backend.auth.entity.UserPersonalInfo;
import com.preen.preen_backend.auth.repository.UserPersonalInfoRepository;
import com.preen.preen_backend.auth.repository.UserRepository;
import com.preen.preen_backend.auth.service.EmailCryptoService;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.myPage.MyInfoErrorCode;
import com.preen.preen_backend.myPage.dto.MyInfoResponse;
import com.preen.preen_backend.myPage.dto.MyInfoUpdateRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyInfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPersonalInfoRepository personalInfoRepository;

    @Mock
    private EmailCryptoService emailCryptoService;

    @InjectMocks
    private MyInfoService myInfoService;

    private UserPersonalInfo personalInfo;

    @BeforeEach
    void setUp() {
        personalInfo = UserPersonalInfo.create(
                1L,
                "홍길동",
                "encrypted-email",
                "email-hash",
                LocalDate.of(2000, 1, 1)
        );
    }

    // 1. 정상 회원정보 조회
    @Test
    void 정상_회원은_내_정보를_조회할_수_있다() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.guest()));

        when(personalInfoRepository.findById(1L))
                .thenReturn(Optional.of(personalInfo));

        when(emailCryptoService.decrypt("encrypted-email"))
                .thenReturn("test@example.com");

        MyInfoResponse response = myInfoService.getMyInfo(1L);

        assertEquals("홍길동", response.name());
        assertEquals("test@example.com", response.email());
        assertEquals(
                LocalDate.of(2000, 1, 1),
                response.birthDate()
        );
    }

    // 2. 정상 회원정보 수정
    @Test
    void 정상_회원은_이름과_생년월일을_수정할_수_있다() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.guest()));

        when(personalInfoRepository.findById(1L))
                .thenReturn(Optional.of(personalInfo));

        when(emailCryptoService.decrypt("encrypted-email"))
                .thenReturn("test@example.com");

        MyInfoUpdateRequest request = new MyInfoUpdateRequest(
                "김철수",
                LocalDate.of(1999, 5, 10)
        );

        MyInfoResponse response =
                myInfoService.updateMyInfo(1L, request);

        assertEquals("김철수", response.name());
        assertEquals(
                LocalDate.of(1999, 5, 10),
                response.birthDate()
        );
        assertEquals("test@example.com", response.email());

        assertEquals("김철수", personalInfo.getName());
        assertEquals(
                LocalDate.of(1999, 5, 10),
                personalInfo.getBirthDate()
        );
    }

    // 3. 존재하지 않는 회원 조회 차단
    @Test
    void 존재하지_않는_회원은_조회할_수_없다() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> myInfoService.getMyInfo(999L)
        );

        assertEquals(
                MyInfoErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );

        verifyNoInteractions(personalInfoRepository);
    }

    // 4. 개인정보가 없는 회원 조회 차단
    @Test
    void 개인정보가_없으면_조회할_수_없다() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.guest()));

        when(personalInfoRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> myInfoService.getMyInfo(1L)
        );

        assertEquals(
                MyInfoErrorCode.PERSONAL_INFO_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    // 5. 비활성 회원 조회 차단
    @Test
    void 비활성_회원은_내_정보를_조회할_수_없다() {

        User inactiveUser = mock(User.class);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(inactiveUser));

        when(inactiveUser.getStatus())
                .thenReturn("INACTIVE");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> myInfoService.getMyInfo(1L)
        );

        assertEquals(
                MyInfoErrorCode.USER_NOT_ACTIVE,
                exception.getErrorCode()
        );

        verifyNoInteractions(personalInfoRepository);
    }

    // 6. 탈퇴 회원 조회 차단
    @Test
    void 탈퇴_회원은_내_정보를_조회할_수_없다() {

        User deletedUser = mock(User.class);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(deletedUser));

        when(deletedUser.getStatus())
                .thenReturn("ACTIVE");

        when(deletedUser.getDeletedAt())
                .thenReturn(Instant.now());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> myInfoService.getMyInfo(1L)
        );

        assertEquals(
                MyInfoErrorCode.USER_NOT_ACTIVE,
                exception.getErrorCode()
        );

        verifyNoInteractions(personalInfoRepository);
    }
}
