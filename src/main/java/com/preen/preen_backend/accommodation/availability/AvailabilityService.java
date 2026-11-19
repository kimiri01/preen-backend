package com.preen.preen_backend.accommodation.availability;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvailabilityService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public AvailabilityService(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }
    public record RoomAvailability(String roomId,String roomName,int capacity,int maxCapacity,
        String extraPersonFee,int nights,boolean available,String reason,Integer remainingRooms,
        String totalPrice) {}
    public record Result(String accommodationId,String checkIn,String checkOut,int nights,
        String currency,String checkedAt,List<RoomAvailability> rooms) {}

    @Transactional(readOnly=true)
    public Result query(long id,LocalDate checkIn,LocalDate checkOut) {
        long nights=ChronoUnit.DAYS.between(checkIn,checkOut);
        if(id<=0 || checkIn.isBefore(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))) || nights<1 || nights>30)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"체크인은 오늘 이후, 숙박은 1~30박이어야 합니다.");
        Integer found=jdbc.queryForObject("SELECT COUNT(*) FROM accommodations WHERE id=? AND listing_status='LISTED'",Integer.class,id);
        if(found==null || found==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"공개 숙소를 찾을 수 없습니다.");
        var rooms=jdbc.query("""
            SELECT r.id,r.name,r.capacity,r.max_capacity,r.extra_person_fee,
              COUNT(i.id) AS coverage,
              COALESCE(SUM(CASE WHEN i.is_closed THEN 1 ELSE 0 END),0) AS closed_days,
              MIN(i.total_count-i.booked_count-i.held_count) AS remaining,
              SUM(i.price) AS period_price
            FROM rooms r LEFT JOIN room_inventories i
              ON i.room_id=r.id AND i.stay_date>=? AND i.stay_date<?
            WHERE r.accommodation_id=? AND r.status='ACTIVE'
            GROUP BY r.id,r.name,r.capacity,r.max_capacity,r.extra_person_fee
            ORDER BY r.id
            """,(rs,row)-> {
                boolean complete=rs.getLong("coverage")==nights;
                int remaining=rs.getInt("remaining");
                String reason=!complete ? "MISSING_INVENTORY" : rs.getLong("closed_days")>0 ? "CLOSED" : remaining<=0 ? "SOLD_OUT" : "AVAILABLE";
                boolean available=reason.equals("AVAILABLE");
                BigDecimal price=rs.getBigDecimal("period_price");
                return new RoomAvailability(rs.getString("id"),rs.getString("name"),rs.getInt("capacity"),rs.getInt("max_capacity"),
                    rs.getBigDecimal("extra_person_fee").toPlainString(),(int)nights,available,reason,
                    complete ? (available ? Math.max(0,remaining) : 0) : null,
                    available && price!=null ? price.toPlainString() : null);
            }, checkIn,checkOut,id);
        return new Result(Long.toString(id),checkIn.toString(),checkOut.toString(),(int)nights,"KRW",clock.instant().toString(),rooms);
    }
}
