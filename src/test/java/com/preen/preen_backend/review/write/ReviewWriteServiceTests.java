package com.preen.preen_backend.review.write;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="PREEN_VERIFY_LOCAL_DB",matches="true")
class ReviewWriteServiceTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired ReviewWriteService service;
    @Autowired com.preen.preen_backend.review.MusicReviewRepository publicReviews;
    private static final long OWNER=982001,OTHER=982002,BOOKING=982010;
    private static final String TRACK="4Dr2hJ3EnVh2Aaot6fRwDO";
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(?,'GUEST','ACTIVE',NOW(),NOW()),(?,'GUEST','ACTIVE',NOW(),NOW())",OWNER,OTHER);
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(-982001,'HOST','ACTIVE',NOW(),NOW())");
        jdbc.update("INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no,approval_status,created_at) VALUES(-982001,-982001,'review-write-test','review-write-test','APPROVED',NOW())");
        jdbc.update("INSERT INTO regions(id,name,level) VALUES(-982001,'review-write-region',1)");
        jdbc.update("INSERT INTO accommodation_categories(id,name,sort_order) VALUES(-982001,'review-write-category',999)");
        jdbc.update("INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status,created_at) VALUES(-982001,-982001,-982001,-982001,'review-write-stay','test address','15:00','11:00','LISTED',NOW())");
        jdbc.update("INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,nightly_price,extra_person_fee,status) VALUES(-982001,-982001,'review-write-room',2,2,100000,0,'ACTIVE')");
        jdbc.update("INSERT INTO refund_policies(id,accommodation_id,version,policy_text,active) VALUES(-982001,-982001,'test','test',FALSE)");
        jdbc.update("INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot,hold_expires_at,created_at) VALUES(?,'review-write-booking',?,-982001,-982001,'2026-10-01','2026-10-02',2,'테스트',100000,'COMPLETED','{}',NOW(),NOW())",BOOKING,OWNER);
    }
    private void assertStatus(HttpStatus expected,Runnable action) {
        assertEquals(expected,assertThrows(ResponseStatusException.class,action::run).getStatusCode());
    }
    @Test void savesRatingAndTrackForOwnedCompletedReservationAndMakesItPubliclyQueryable() {
        assertTrue(service.getContext(OWNER,BOOKING).eligible());
        assertEquals(1,service.eligible(OWNER,-982001).size());
        var created=service.create(OWNER,BOOKING,5,TRACK);
        assertEquals(5,created.rating());
        assertEquals(TRACK,jdbc.queryForObject("SELECT spotify_track_id FROM reviews WHERE id=?",String.class,Long.parseLong(created.id())));
        assertEquals(1,publicReviews.count(-982001,5));
        assertEquals(TRACK,publicReviews.list(-982001,5,"latest",0).getFirst().spotifyTrackId());
        assertEquals(OWNER,jdbc.queryForObject("SELECT user_id FROM reviews WHERE id=?",Long.class,Long.parseLong(created.id())));
        assertFalse(service.getContext(OWNER,BOOKING).eligible());
        assertTrue(service.eligible(OWNER,-982001).isEmpty());
    }
    @Test void refusesOtherOwnersWithoutDisclosingBooking() {
        assertStatus(HttpStatus.NOT_FOUND,()->service.getContext(OTHER,BOOKING));
        assertStatus(HttpStatus.NOT_FOUND,()->service.create(OTHER,BOOKING,5,TRACK));
        assertTrue(service.eligible(OTHER,-982001).isEmpty());
    }
    @Test void refusesIncompleteAndFutureCompletedStays() {
        for(String status:new String[]{"PENDING","CONFIRMED","CANCELLED"}) {
            jdbc.update("UPDATE reservations SET status=? WHERE id=?",status,BOOKING);
            assertStatus(HttpStatus.CONFLICT,()->service.create(OWNER,BOOKING,5,TRACK));
        }
        jdbc.update("UPDATE reservations SET status='COMPLETED',check_out=? WHERE id=?",LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(2),BOOKING);
        assertStatus(HttpStatus.CONFLICT,()->service.create(OWNER,BOOKING,5,TRACK));
    }
    @Test void rejectsDuplicateEvenIfReviewWasHidden() {
        var created=service.create(OWNER,BOOKING,4,TRACK);
        jdbc.update("UPDATE reviews SET status='HIDDEN' WHERE id=?",Long.parseLong(created.id()));
        assertStatus(HttpStatus.CONFLICT,()->service.create(OWNER,BOOKING,5,TRACK));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE reservation_id=?",Integer.class,BOOKING));
    }
    @Test void rejectsInvalidRatingTrackAndInactiveAccount() {
        assertStatus(HttpStatus.BAD_REQUEST,()->service.create(OWNER,BOOKING,6,TRACK));
        assertStatus(HttpStatus.BAD_REQUEST,()->service.create(OWNER,BOOKING,5,"not-a-track"));
        jdbc.update("UPDATE users SET status='WITHDRAWN' WHERE id=?",OWNER);
        assertStatus(HttpStatus.UNAUTHORIZED,()->service.create(OWNER,BOOKING,5,TRACK));
    }
}
