package com.preen.preen_backend.review.write;

import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewWriteService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public ReviewWriteService(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }
    public record Context(String reservationId, String accommodationId, String accommodationName,
                          String roomName, String checkIn, String checkOut,
                          boolean eligible, String reasonCode, String reviewId) {}
    public record Created(String id, String reservationId, String accommodationId, int rating, String spotifyTrackId) {}
    private static final String CONTEXT_SQL = """
        SELECT b.id,b.status,b.check_in,b.check_out,a.id AS accommodation_id,a.name AS accommodation_name,
               r.name AS room_name,v.id AS review_id
        FROM reservations b JOIN rooms r ON r.id=b.room_id
        JOIN accommodations a ON a.id=r.accommodation_id
        LEFT JOIN reviews v ON v.reservation_id=b.id
        WHERE b.user_id=?
        """;
    private void requireActive(long userId) {
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",Integer.class,userId);
        if (count==null || count==0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"활성 회원 로그인이 필요합니다.");
    }
    private Context context(java.sql.ResultSet rs) throws java.sql.SQLException {
        LocalDate checkout=rs.getDate("check_out").toLocalDate();
        String reviewId=rs.getString("review_id");
        String reason=reviewId!=null?"ALREADY_REVIEWED":
            !"COMPLETED".equals(rs.getString("status")) || checkout.isAfter(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))?"STAY_NOT_COMPLETED":"READY";
        return new Context(rs.getString("id"),rs.getString("accommodation_id"),rs.getString("accommodation_name"),
            rs.getString("room_name"),rs.getDate("check_in").toLocalDate().toString(),checkout.toString(),"READY".equals(reason),reason,reviewId);
    }
    @Transactional(readOnly=true)
    public Context getContext(long userId,long reservationId) {
        requireActive(userId);
        return jdbc.query(CONTEXT_SQL+" AND b.id=?",(rs,row)->context(rs),userId,reservationId).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"예약을 찾을 수 없습니다."));
    }
    @Transactional(readOnly=true)
    public List<Context> eligible(long userId,long accommodationId) {
        requireActive(userId);
        return jdbc.query(CONTEXT_SQL+" AND a.id=? AND b.status='COMPLETED' AND v.id IS NULL AND b.check_out<=? ORDER BY b.check_out DESC,b.id DESC LIMIT 50",
            (rs,row)->context(rs),userId,accommodationId,LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))));
    }
    @Transactional
    public Created create(long userId,long reservationId,int rating,String trackId) {
        if (reservationId<=0 || rating<1 || rating>5 || trackId==null || !trackId.matches("[A-Za-z0-9]{22}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"별점과 Spotify 곡을 확인해주세요.");
        requireActive(userId);
        // Serialize writers on the reservation, and keep the DB unique constraint as the final guard.
        var owned=jdbc.query("SELECT id FROM reservations WHERE id=? AND user_id=? FOR UPDATE",(rs,row)->rs.getLong("id"),reservationId,userId);
        if (owned.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"예약을 찾을 수 없습니다.");
        Context booking=getContext(userId,reservationId);
        if (!booking.eligible()) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "ALREADY_REVIEWED".equals(booking.reasonCode())?"이미 이 예약의 리뷰를 작성했습니다.":"투숙이 끝난 예약에만 리뷰를 쓸 수 있습니다.");
        var keys=new GeneratedKeyHolder();
        try {
            jdbc.update(connection->{
                var statement=connection.prepareStatement("INSERT INTO reviews(reservation_id,accommodation_id,user_id,spotify_track_id,rating,status,created_at) VALUES(?,?,?,?,?,'VISIBLE',?)",Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1,reservationId); statement.setLong(2,Long.parseLong(booking.accommodationId()));
                statement.setLong(3,userId); statement.setString(4,trackId);statement.setInt(5,rating);
                statement.setTimestamp(6,java.sql.Timestamp.from(clock.instant()));return statement;
            },keys);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 이 예약의 리뷰를 작성했습니다.");
        }
        if (keys.getKey()==null) throw new IllegalStateException("Missing review key");
        return new Created(keys.getKey().toString(),Long.toString(reservationId),booking.accommodationId(),rating,trackId);
    }
}
