package com.preen.preen_backend.host.service;


import com.preen.preen_backend.common.BaseException;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.host.domain.BusinessRegistrationNo;
import com.preen.preen_backend.host.domain.HostBusiness;
import com.preen.preen_backend.host.dto.HostDtos;
import com.preen.preen_backend.host.exception.HostErrorCode;
import com.preen.preen_backend.host.repository.HostBusinessRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.preen.preen_backend.host.dto.HostDtos.*;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class HostService {
    private final HostBusinessRepository businessRepository;


    /**
     * 사업자 등록. 번호 형식 검증을 통과하면 바로 APPROVED로 저장하고 회원 역할을 HOST로 바꾼다.
     * 기존 액세스 토큰에는 예전 역할이 들어 있으므로 새 토큰을 응답에 담는다.
     *
     */
    @Transactional
    public RegisterResponse register(Long userId, RegisterRequest request){
        String registrationNo = BusinessRegistrationNo.nomalize(request.businessRegistrationNo())
                .orElseThrow(()-> new BusinessException(HostErrorCode.INVALID_REGISTRATION_NO));
        if(businessRepository.existsByHostUserId(userId)){
            throw new BusinessException(HostErrorCode.BUSINESS_ALREADY_REGISTERED);
        }
        if(businessRepository.existsByBusinessRegistrationNo(registrationNo)){
            throw new BusinessException(HostErrorCode.DUPLICATE_REGISTRATION_NO);
        }

        HostBusiness business;
        try{
            business = businessRepository.saveAndFlush(HostBusiness.approved(
                    userId, request.businessName().trim(), registrationNo, LocalDateTime.now()));
        }catch (DataIntegrityViolationException e){
            throw new BusinessException(HostErrorCode.DUPLICATE_REGISTRATION_NO);
        }
        ///////////////////////////
        //권한 변경 로직 필수. 일단은 게스트로 둔다.
        String tempToken = "tempToken";
        long tempTokenTTL = 100000L;
        /////////////////////////////

        return new RegisterResponse(
                BusinessResponse.from(business), tempToken, tempTokenTTL);
    }




    @Transactional
    public BusinessInfo getBusinessOf(Long userId){
        return businessRepository.findFirstByHostUserIdOrderByIdAsc(userId)
                .map(b -> new BusinessInfo(b.getId(), b.getHostUserId()))
                .orElseThrow(()-> new BusinessException(HostErrorCode.BUSINESS_NOT_FOUND));


    }



}
