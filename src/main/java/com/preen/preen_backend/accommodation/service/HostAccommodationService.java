package com.preen.preen_backend.accommodation.service;


import com.preen.preen_backend.accommodation.domain.*;
import com.preen.preen_backend.accommodation.dto.RefundDtos.*;
import com.preen.preen_backend.accommodation.exception.AccommodationErrorCode;
import com.preen.preen_backend.accommodation.repository.*;
import com.preen.preen_backend.common.BaseException;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.host.service.HostService;
import org.springframework.security.core.parameters.P;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.preen.preen_backend.accommodation.dto.HostAccommodationDtos.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/** 호스트의 숙소·객실·환불정책 관리와 조회. 모든 요청은 본인 소유인지 먼저 검사한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class HostAccommodationService {


    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository imageRepository;
    private final AccommodationAmenityRepository accommodationAmenityRepository;
    private final AmenityRepository amenityRepository;
    private final RegionRepository regionRepository;
    private final AccommodationCategoryRepository categoryRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final RefundPolicyRuleRepository refundPolicyRuleRepository;
    private final HostService hostService;
    private final AccommodationQueryService queryService;


    // ---------- 숙소 ----------

    /**
     * 숙소 등록. DRAFT로 만들어지고, 객실과 환불정책을 등록한 뒤 LISTED로 바꿔 공개한다.
     */
    public HostAccommodationDetail create(Long userId, AccommodationCreateRequest request) {
        Long businessId = hostService.getBusinessOf(userId).businessId();
        validateRegion(request.regionId());
        validateCategory(request.categoryId());

        Accommodation accommodation = accommodationRepository.save(Accommodation.create(
                businessId, request.regionId(), request.categoryId(), request.name().trim(), request.description(),
                request.address().trim(), request.latitude(), request.longitude(),
                request.checkInTime(), request.checkOutTime(), LocalDateTime.now()));
        replaceImages(accommodation.getId(), request.imageUrls());
        replaceAmenities(accommodation.getId(), request.amenityIds());
        request.rooms().forEach(r -> createRoom(accommodation.getId(), r));

        return queryService.toHostDetail(accommodation, false);
    }

    public HostAccommodationDetail update(Long userId, Long accommodationId, AccommodationUpdateRequest request) {
        Accommodation accommodation = loadOwned(accommodationId, userId);
        if (request.regionId() != null) {
            validateRegion(request.regionId());
        }
        if (request.categoryId() != null) {
            validateCategory(request.categoryId());
        }
        accommodation.update(request.regionId(), request.categoryId(), trim(request.name()),
                request.description(), trim(request.address()), request.latitude(), request.longitude(),
                request.checkInTime(), request.checkOutTime());
        if(request.imageUrls()!= null){
            replaceImages(accommodationId, request.imageUrls());
        }
        if(request.amenityIds()!= null){
            replaceAmenities(accommodationId, request.amenityIds());
        }
        return queryService.toHostDetail(accommodation, false);
    }


    // ---------- 조회 ----------

    /**
     * 내 숙소 목록. DRAFT·LISTED·HIDDEN 모두.
     */
    @Transactional(readOnly = true)
    public List<HostAccommodationSummary> getMine(Long userId) {
        return queryService.getHostSummaries(hostService.getBusinessOf(userId).businessId());
    }


    /**
     * 내 숙소 상세. 공개 상태와 관계없이 조회하고, 판매 중지 객실도 포함한다.
     */
    @Transactional(readOnly = true)
    public HostAccommodationDetail getMyDetail(Long userId, Long accommodationId) {
        return queryService.toHostDetail(loadOwned(accommodationId, userId), false);
    }



    // ------------객실---------------

    //INACTIVE상태로 객실을 생성한다. 호스트 페이지에서 직접 객실을 생성할 때 사용한다.
    public RoomResponse addRoom(Long userId, Long accommodationId, RoomCreateRequest request) {
        loadOwned(accommodationId, userId);
        validateCapacity(request.capacity(), request.maxCapacity());
        Room room = roomRepository.save(Room.createInactive(accommodationId, request.name().trim(),
                request.capacity(), request.maxCapacity(), request.extraPersonFee(), request.nightlyPrice()));
        List<String> imageUrls = replaceRoomImages(room.getId(), request.imageUrls());
        return queryService.toRoomResponse(room, imageUrls);
    }

    //ACTIVE상태로 객실을 생성한다. 숙소 등록 시 함께 객실을 등록할 때 사용한다.
    private Room createRoom(Long accommodationId, RoomCreateRequest request) {
        validateCapacity(request.capacity(), request.maxCapacity());
        Room room = roomRepository.save(Room.create(accommodationId, request.name().trim(),
                request.capacity(), request.maxCapacity(), request.extraPersonFee(), request.nightlyPrice()));
        replaceRoomImages(room.getId(), request.imageUrls());
        return room;
    }

    public RoomResponse updateRoom(Long userId, Long accommodationId, Long roomId, RoomUpdateRequest request) {
        Accommodation accommodation = loadOwned(accommodationId, userId);
        Room room = roomRepository.findById(roomId)
                .filter(r -> r.getAccommodationId().equals(accommodationId))
                .orElseThrow(() -> new BusinessException(AccommodationErrorCode.ROOM_NOT_FOUND));


        room.update(trim(request.name()), request.capacity(), request.maxCapacity(),
                request.extraPersonFee(), request.nightlyPrice(), null);
        validateCapacity(room.getCapacity(), room.getMaxCapacity());

        List<String> imageUrls = request.imageUrls() != null
                ? replaceRoomImages(roomId, request.imageUrls())
                : roomImageRepository.findByRoomIdInOrderBySortOrderAsc(List.of(roomId)).stream()
                .map(RoomImage::getImageUrl).toList();
        return queryService.toRoomResponse(room, imageUrls);
    }



    //----------환불 정책------------------
    /**
     * 환불정책 설정. 기존 정책은 고치지 않고 비활성으로 돌린 뒤 새 버전을 만든다.
     * 기존 예약이 과거 버전을 참조하기 때문이다.
     */
//    public RefundPolicyResponse putRefundPolicy(Long userId, Long accommodationId, RefundPolicyRequest request){
//
//    }

    // ---------- 내부 ----------


    /**
     * 호스트 API는 리소스가 본인 소유인지 반드시 검사한다.
     */
    private Accommodation loadOwned(Long accommodationId, Long userId) {
        return assertOwner(accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new BusinessException(AccommodationErrorCode.ACCOMMODATION_NOT_FOUND)), userId);
    }


    /** 공개 상태나 판매 객실 수를 바꿀 때 쓴다. 숙소 행을 잠근다(FOR UPDATE). */
    private Accommodation loadOwnedForUpdate(Long accommodationId, Long userId) {
        return assertOwner(accommodationRepository.findByIdForUpdate(accommodationId)
                .orElseThrow(() -> new BusinessException(AccommodationErrorCode.ACCOMMODATION_NOT_FOUND)), userId);
    }


    private Accommodation assertOwner(Accommodation accommodation, Long userId) {
        if (!hostService.getBusinessOf(userId).businessId().equals(accommodation.getHostBusinessId())) {
            throw new BusinessException(AccommodationErrorCode.NOT_OWNER);
        }
        return accommodation;
    }

    private void validateRegion(Long regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw new BusinessException(AccommodationErrorCode.REGION_NOT_FOUND);
        }
    }

    private void validateCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(AccommodationErrorCode.CATEGORY_NOT_FOUND);
        }
    }


    private void validateCapacity(int capacity, int maxCapacity) {
        if (maxCapacity < capacity) {
            throw new BusinessException(AccommodationErrorCode.INVALID_CAPACITY);
        }
    }

    private void replaceImages(Long accommodationId, List<String> imageUrls) {
        imageRepository.deleteAllByAccommodationId(accommodationId);
        if (imageUrls == null) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            imageRepository.save(AccommodationImage.of(accommodationId, imageUrls.get(i), i));
        }
    }

    private List<String> replaceRoomImages(Long roomId, List<String> imageUrls) {
        roomImageRepository.deleteAllByRoomId(roomId);
        if (imageUrls == null) {
            return List.of();
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            roomImageRepository.save(RoomImage.of(roomId, imageUrls.get(i), i));
        }
        return imageUrls;
    }

    private void replaceAmenities(Long accommodationId, List<Long> amenityIds) {
        accommodationAmenityRepository.deleteAllByAccommodationId(accommodationId);
        if (amenityIds == null) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>(amenityIds);
        if (amenityRepository.findAllById(distinct).size() != distinct.size()) {
            throw new BusinessException(AccommodationErrorCode.AMENITY_NOT_FOUND);
        }
        distinct.forEach(id -> accommodationAmenityRepository.save(AccommodationAmenity.of(accommodationId, id)));
    }

    //update시 숙소 이름은 null이 들어올 수 있으니 trim메서드를 재정의해 사용합니다.
    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

}


