package com.preen.preen_backend.accommodation.service;


import com.preen.preen_backend.accommodation.dto.HostAccommodationSummary;
import com.preen.preen_backend.host.service.HostService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


/** 호스트의 숙소·객실·환불정책 관리와 조회. 모든 요청은 본인 소유인지 먼저 검사한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class HostAccommodationService {


    private final HostService hostService;
    private final AccommodationQueryService queryService;

    // ---------- 조회 ----------

    /** 내 숙소 목록. DRAFT·LISTED·HIDDEN 모두. */
    @Transactional(readOnly = true)
    public List<HostAccommodationSummary> getMine(Long userId) {
        return queryService.getHostSummaries(hostService.getBusinessOf(userId).businessId());
    }



    }


