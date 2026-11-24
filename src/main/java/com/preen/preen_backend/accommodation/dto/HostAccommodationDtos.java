package com.preen.preen_backend.accommodation.dto;


import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Room;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

//host 페이지에서 사용하는 accommodation 요청, 응답 모음
public final class HostAccommodationDtos {

    private static final String BLANK_ONLY = ".*\\S.*";


    private HostAccommodationDtos(){
    }


    // ---------- 요청 ----------

    public record AccommodationCreateRequest(
            @NotNull(message = "지역을 선택해 주세요.") Long regionId,
            @NotNull(message = "카테고리를 선택해 주세요.") Long categoryId,
            @NotBlank(message = "숙소 이름을 입력해 주세요.") @Size(max = 100) String name,
            @Size(max = 5000) String description,
            @NotBlank(message = "주소를 입력해 주세요.") @Size(max = 255) String address,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @NotNull(message = "체크인 시각을 입력해 주세요.") LocalTime checkInTime,
            @NotNull(message = "체크아웃 시각을 입력해 주세요.") LocalTime checkOutTime,
            @Size(max = 20, message = "이미지는 20장까지 등록할 수 있습니다.")
            List<@NotBlank @Size(max = 500) String> imageUrls,
            List<@NotNull Long> amenityIds,
            @NotEmpty(message = "객실을 1개 이상 등록해 주세요.")
            List<@Valid RoomCreateRequest> rooms
    ) {
    }

    public record RoomCreateRequest(
            @NotBlank(message = "객실 이름을 입력해 주세요.") @Size(max = 100) String name,
            @Min(1) int capacity,
            @Min(1) int maxCapacity,
            @NotNull @DecimalMin("0") BigDecimal extraPersonFee,
            @NotNull @DecimalMin("0") BigDecimal nightlyPrice,
            @Size(max = 20) List<@NotBlank @Size(max = 500) String> imageUrls
    ) {}


    /**
     * 호스트의 내 숙소 목록 한 줄. 공개 상태와 관계없이 모두 나온다.
     *
     * @param readyToList 판매 중인 객실과 환불정책이 있어 공개할 수 있는지
     */
    public record HostAccommodationSummary (Long id, String name, String address, Long regionId,
                                            String regionName, Long categoryId, String categoryName,
                                            String thumbnailUrl, Accommodation.ListingStatus listingStatus,
                                            int roomCount, int activeRoomCount, boolean hasRefundPolicy, boolean readyToList)

    { }


    //--------------응답


    public record AmenityResponse(Long id, String name) {
    }

    public record RoomResponse(Long id, String name, int capacity, int maxCapacity, BigDecimal extraPersonFee,
                               BigDecimal nightlyPrice, Room.RoomStatus status, List<String> imageUrls) {
    }


    //일반 detailResponse와 다른 점은 체크인, 체크아웃 날짜가 포함되어있다.
    // 호스트의 숙소 등록용이다.
    //객실이 포함되어 있는 이유는 객실이 1개 이상 포함되어야 관리자 승인 요청이 가능하다.
    public record HostAccommodationDetail(
            Long id,
            String name,
            String description,
            String address,
            Long regionId,
            String regionName,
            Long categoryId,
            String categoryName,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            Accommodation.ListingStatus listingStatus,
            List<String> imageUrls,
            List<AmenityResponse> amenities,
            List<RoomResponse> rooms,
            RefundPolicyResponse refundPolicy
    ) {
    }

}
