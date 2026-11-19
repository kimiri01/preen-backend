package com.preen.preen_backend.accommodation.dto;


import com.preen.preen_backend.accommodation.domain.Accommodation;

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
