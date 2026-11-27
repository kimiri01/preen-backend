package com.preen.preen_backend.inventory.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** inventory 요청·응답 모음. */
public final class InventoryDtos {

    private InventoryDtos() {
    }


    /** 선점 결과의 하루치. 예약이 요금 항목을 계산할 때 쓴다. */
    public record NightPrice(LocalDate stayDate, BigDecimal price) {
    }
}