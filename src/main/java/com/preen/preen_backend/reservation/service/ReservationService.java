package com.preen.preen_backend.reservation.service;


import com.preen.preen_backend.accommodation.dto.RefundPolicyResponse;
import com.preen.preen_backend.accommodation.dto.RoomInfo;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.response.PageResponse;
import com.preen.preen_backend.inventory.service.InventoryService;
import com.preen.preen_backend.reservation.domain.ReservationPriceItem;
import com.preen.preen_backend.reservation.domain.ReservationPriceItem.ItemType;
import com.preen.preen_backend.reservation.domain.ReservationStatusHistory;
import com.preen.preen_backend.reservation.dto.ReservationDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.core.JacksonException; // JsonProcessingException 대신 사용
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.preen.preen_backend.common.BaseException;
import com.preen.preen_backend.accommodation.service.AccommodationQueryService;
import com.preen.preen_backend.reservation.domain.Reservation;
import com.preen.preen_backend.reservation.domain.Reservation.Status;
import com.preen.preen_backend.reservation.exception.ReservationErrorCode;
import com.preen.preen_backend.reservation.repository.ReservationPriceItemRepository;
import com.preen.preen_backend.reservation.repository.ReservationRepository;
import com.preen.preen_backend.reservation.repository.ReservationStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.preen.preen_backend.inventory.dto.InventoryDtos.NightPrice;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.preen.preen_backend.reservation.service.TempUserService.UserMeResponse;




    @Service
    @RequiredArgsConstructor
    public class ReservationService {

        private static final int MAX_PAGE_SIZE = 50;
        private static final String NO_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        private static final int NO_RANDOM_LENGTH = 12;
        private static final SecureRandom RANDOM = new SecureRandom();

        private final ReservationRepository reservationRepository;
        private final ReservationPriceItemRepository priceItemRepository;
        private final ReservationStatusHistoryRepository historyRepository;
        private final TempUserService userService;  //이후 실제 UserSerice생성된다면 변경되어야 함.
        private final AccommodationQueryService accommodationQueryService;
        private final InventoryService inventoryService;
        private final RefundInfoProvider refundInfoProvider;
        private final ApplicationEventPublisher eventPublisher;
        private final TransactionTemplate transactionTemplate;
        private final ReservationProperties properties;
        private final ObjectMapper objectMapper;


        // ---------- 생성 ----------

        /**
         * 예약 생성. 한 트랜잭션으로 예약 저장(PENDING), 재고 선점, 요금 항목 저장을 한다.
         * 재고 선점이 하루라도 실패하면 전체가 롤백된다.
         */
        @Transactional
        public ReservationResponse create(Long userId, CreateRequest request) {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            if (!request.checkOut().isAfter(request.checkIn()) || request.checkIn().isBefore(today)) {
                throw new BusinessException(ReservationErrorCode.INVALID_DATES);
            }
            if (!userService.isAdult(userId)) {
                throw new BusinessException(ReservationErrorCode.ADULT_ONLY);
            }
            RoomInfo room = accommodationQueryService.getRoomInfo(request.roomId());
            if (!room.sellable()) {
                throw new BusinessException(ReservationErrorCode.ROOM_NOT_AVAILABLE);
            }
            if (request.guestCount() > room.maxCapacity()) {
                throw new BusinessException(ReservationErrorCode.GUEST_COUNT_EXCEEDED);
            }
            RefundPolicyResponse policy = accommodationQueryService.getActiveRefundPolicy(room.accommodationId())
                    .orElseThrow(() -> new BusinessException(ReservationErrorCode.ROOM_NOT_AVAILABLE));

            UserMeResponse me = userService.getMe(userId);
            String guestName = request.guestName() != null ? request.guestName().trim() : me.name();
            String guestPhone = request.guestPhone() != null ? request.guestPhone() : me.phone();
            LocalDateTime holdExpiresAt = now.plus(properties.holdDuration());

            Reservation reservation = reservationRepository.save(Reservation.pending(
                    newReservationNo(today), userId, room.roomId(), policy.id(), request.checkIn(), request.checkOut(),
                    request.guestCount(), guestName, guestPhone, toJson(policy), now, holdExpiresAt));

            List<NightPrice> nights = inventoryService.hold(reservation.getId(), room.roomId(),
                    request.checkIn(), request.checkOut(), holdExpiresAt);

            List<ReservationPriceItem> items = new ArrayList<>();
            BigDecimal roomAmount = nights.stream().map(NightPrice::price).reduce(BigDecimal.ZERO, BigDecimal::add);
            items.add(ReservationPriceItem.of(reservation.getId(), ItemType.ROOM,
                    "객실 요금 (" + nights.size() + "박)", roomAmount));
            int extraPersons = request.guestCount() - room.capacity();
            BigDecimal extraAmount = room.extraPersonFee()
                    .multiply(BigDecimal.valueOf(extraPersons))
                    .multiply(BigDecimal.valueOf(nights.size()));
            if (extraAmount.signum() > 0) {
                items.add(ReservationPriceItem.of(reservation.getId(), ItemType.EXTRA_PERSON,
                        "추가 인원 요금 (" + extraPersons + "명 × " + nights.size() + "박)", extraAmount));
            }
            priceItemRepository.saveAll(items);
            reservation.changeTotalPrice(items.stream().map(ReservationPriceItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            recordHistory(reservation.getId(), userId, null, Status.PENDING, "예약 생성", now);
            return toResponse(reservation, items, room, null);
        }

        // ---------- 조회 ----------

        @Transactional(readOnly = true)
        public PageResponse<ReservationResponse> getMine(Long userId, int page, int size) {
            return toPage(reservationRepository.findByUserId(userId, pageable(page, size)));
        }


        /** 본인 예약만 볼 수 있다. 남의 예약은 없는 예약으로 응답한다. 환불 내역이 있으면 함께 준다. */
        @Transactional(readOnly = true)
        public ReservationResponse getDetail(Long userId, Long reservationId) {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .filter(r -> r.getUserId().equals(userId))
                    .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
            return toResponse(reservation);
        }



        /** 호스트 본인 숙소의 예약 목록. 숙소·상태·체크인 기간으로 거른다. */
        @Transactional(readOnly = true)
        public PageResponse<HostReservationSummary> getForHost(Long hostUserId, HostReservationSearch search,
                                                               int page, int size) {
            if (search.from() != null && search.to() != null && search.from().isAfter(search.to())) {
                throw new BusinessException(ReservationErrorCode.INVALID_SEARCH_RANGE);
            }
            List<Long> roomIds = accommodationQueryService.getRoomIdsOfHost(hostUserId, search.accommodationId());
            if (roomIds.isEmpty()) {
                return PageResponse.of(Page.<Reservation>empty(pageable(page, size)), r->null);
            }

            Page<Reservation> reservations = reservationRepository.searchForHost(
                    roomIds, search.status(), search.from(), search.to(), pageable(page, size));
            Map<Long, RoomInfo> rooms = accommodationQueryService.getRoomInfos(
                    reservations.stream().map(Reservation::getRoomId).distinct().toList());
            return PageResponse.of(reservations, r -> HostReservationSummary.of(r, rooms.get(r.getRoomId())));
        }

        //--------------------내부


        private void recordHistory(Long reservationId, Long changedBy, Status previous, Status next, String reason,
                                   LocalDateTime now) {
            historyRepository.save(ReservationStatusHistory.of(reservationId, changedBy, previous, next, reason, now));
        }

        /** 추측하기 어려운 예약번호. 예: R261111-K7M2Q9XWPB4T */
        private static String newReservationNo(LocalDate today) {
            StringBuilder sb = new StringBuilder("R").append(today.format(DateTimeFormatter.ofPattern("yyMMdd")))
                    .append('-');
            for (int i = 0; i < NO_RANDOM_LENGTH; i++) {
                sb.append(NO_ALPHABET.charAt(RANDOM.nextInt(NO_ALPHABET.length())));
            }
            return sb.toString();
        }



        private String toJson(Object value) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JacksonException e) {
                throw new IllegalStateException(e);
            }
        }


        private PageRequest pageable(int page, int size) {
            return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                    Sort.by(Sort.Direction.DESC, "id"));
        }

        private PageResponse<ReservationResponse> toPage(Page<Reservation> page) {
            List<Long> ids = page.getContent().stream().map(Reservation::getId).toList();
            Map<Long, List<ReservationPriceItem>> items = ids.isEmpty() ? Map.of()
                    : priceItemRepository.findByReservationIdInOrderByIdAsc(ids).stream()
                    .collect(Collectors.groupingBy(ReservationPriceItem::getReservationId));
            Map<Long, RefundInfo> refunds = refundInfoProvider.findByReservationIds(ids);
            return PageResponse.of(page, r -> toResponse(r, items.getOrDefault(r.getId(), List.of()),
                    accommodationQueryService.getRoomInfo(r.getRoomId()), refunds.get(r.getId())));
        }



        private ReservationResponse toResponse(Reservation reservation) {
            return toResponse(reservation,
                    priceItemRepository.findByReservationIdInOrderByIdAsc(List.of(reservation.getId())),
                    accommodationQueryService.getRoomInfo(reservation.getRoomId()),
                    refundInfoProvider.findByReservationIds(List.of(reservation.getId())).get(reservation.getId()));
        }

        private ReservationResponse toResponse(Reservation r, List<ReservationPriceItem> items, RoomInfo room,
                                               RefundInfo refund) {
            JsonNode policy;
            try {
                policy = objectMapper.readTree(r.getRefundPolicySnapshot());
            } catch (JacksonException e) {
                policy = null;
            }
            return new ReservationResponse(r.getId(), r.getReservationNo(), room.accommodationId(),
                    room.accommodationName(), room.roomId(), room.roomName(), r.getCheckIn(), r.getCheckOut(),
                    r.getGuestCount(), r.getGuestName(), r.getGuestPhone(), r.getTotalPrice(), r.getStatus().name(),
                    r.getHoldExpiresAt(), r.getCancelledAt(), r.getCreatedAt(),
                    items.stream().map(i -> new PriceItemResponse(i.getItemType().name(), i.getName(), i.getAmount()))
                            .toList(),
                    policy, refund);
        }



    }