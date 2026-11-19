package com.preen.preen_backend.host.service;


import com.preen.preen_backend.common.BaseException;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.host.dto.HostDtos;
import com.preen.preen_backend.host.exception.HostErrorCode;
import com.preen.preen_backend.host.repository.HostBusinessRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class HostService {
    private final HostBusinessRepository businessRepository;

    @Transactional
    public HostDtos.BusinessInfo getBusinessOf(Long userId){
        return businessRepository.findFirstByHostUserIdOrderByIdAsc(userId)
                .map(b -> new HostDtos.BusinessInfo(b.getId(), b.getHostUserId()))
                .orElseThrow(()-> new BusinessException(HostErrorCode.BUSINESS_NOT_FOUND));


    }



}
