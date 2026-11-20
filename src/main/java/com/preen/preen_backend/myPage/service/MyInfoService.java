
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyInfoService {

    private final UserRepository userRepository;
    private final UserPersonalInfoRepository personalInfoRepository;
    private final EmailCryptoService emailCryptoService;

    // 회원정보 조회
    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        validateActiveUser(userId);

        UserPersonalInfo info = findPersonalInfo(userId);

        return toResponse(info);
    }

    // 회원정보 수정
    @Transactional
    public MyInfoResponse updateMyInfo(
            Long userId,
            MyInfoUpdateRequest request
    ) {
        validateActiveUser(userId);

        UserPersonalInfo info = findPersonalInfo(userId);

        info.updateProfile(
                request.name(),
                request.birthDate()
        );

        return toResponse(info);
    }

    // 활성 회원인지 확인
    private void validateActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        MyInfoErrorCode.USER_NOT_FOUND
                ));

        if (!"ACTIVE".equals(user.getStatus())
                || user.getDeletedAt() != null) {
            throw new BusinessException(
                    MyInfoErrorCode.USER_NOT_ACTIVE
            );
        }
    }

    // 회원 개인정보 조회
    private UserPersonalInfo findPersonalInfo(Long userId) {
        return personalInfoRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        MyInfoErrorCode.PERSONAL_INFO_NOT_FOUND
                ));
    }

    // 응답 DTO 변환
    private MyInfoResponse toResponse(UserPersonalInfo info) {
        String decryptedEmail =
                emailCryptoService.decrypt(info.getEmail());

        return new MyInfoResponse(
                info.getName(),
                decryptedEmail,
                info.getBirthDate()
        );
    }
}
