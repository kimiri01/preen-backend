package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 객실의 날짜별 재고. room_inventories 테이블과 연결된다. (room_id, stay_date)는 유일하다. */
@Entity
@Table(name = "room_inventories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private LocalDate stayDate;

    /** 총 재고 */
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int totalCount;

    /** 확정된 예약 수 */
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int bookedCount;

    /** 결제 대기 중 선점 수 */
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int heldCount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** 판매 중지 여부 */
    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    /** 판매 가능한 재고가 남아 있는가 */
    public boolean isSellable() {
        return !closed && totalCount - bookedCount - heldCount > 0;
    }
}
