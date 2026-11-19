package com.preen.preen_backend.review;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("moon-verification")
@Transactional
@EnabledIfEnvironmentVariable(named="PREEN_VERIFY_LOCAL_DB",matches="true")
class MusicReviewRepositoryTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MusicReviewRepository repository;
    @BeforeEach void fixture() {
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(-9101,'HOST','ACTIVE',NOW(),NOW())");
        jdbc.update("INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no,approval_status,created_at) VALUES(-9101,-9101,'리뷰 테스트','music-review-test','APPROVED',NOW())");
        jdbc.update("INSERT INTO regions(id,name,level) VALUES(-9101,'리뷰 테스트 지역',1)");
        jdbc.update("INSERT INTO accommodation_categories(id,name,sort_order) VALUES(-9101,'리뷰 테스트 유형',999)");
        jdbc.update("INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status,created_at) VALUES(-9101,-9101,-9101,-9101,'리뷰 테스트 숙소','주소','15:00','11:00','LISTED',NOW())");
        jdbc.update("INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,nightly_price,extra_person_fee,status) VALUES(-9101,-9101,'리뷰 객실',2,2,100000,0,'ACTIVE')");
        jdbc.update("INSERT INTO refund_policies(id,accommodation_id,version,policy_text,active) VALUES(-9101,-9101,'test','테스트',FALSE)");
        for (int i=1;i<=4;i++) {
            long id=-9200-i;
            jdbc.update("INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot,hold_expires_at,created_at) VALUES(?,?,-9101,-9101,-9101,'2026-10-01','2026-10-02',2,'반환하지 않는 이름',100000,?,'{}',NOW(),NOW())",id,"music-test-"+i,i==4?"CONFIRMED":"COMPLETED");
            jdbc.update("INSERT INTO reviews(id,reservation_id,accommodation_id,user_id,spotify_track_id,rating,status,created_at) VALUES(?,?,-9101,-9101,'11dFghVXANMlKmJXsNCbNl',?,?,?)",id,id,i==1?5:3,i==3?"HIDDEN":"VISIBLE","2026-10-0"+i+" 12:00:00");
        }
    }
    @Test void filtersPublicCompletedReviewsAndSortsWithinThisStay() {
        assertEquals(2,repository.count(-9101,0));
        assertEquals(1,repository.count(-9101,5));
        assertEquals(0,repository.count(-9101,2));
        assertTrue(repository.list(-9101,2,"latest",0).isEmpty());
        assertTrue(repository.list(-9101,5,"latest",0).stream().allMatch(review -> review.rating()==5));
        assertEquals(3,repository.list(-9101,0,"rating-asc",0).getFirst().rating());
        assertEquals(0,repository.count(-999999,0));
        assertEquals(3,repository.list(-9101,0,"latest",0).getFirst().rating());
        assertEquals(5,repository.list(-9101,0,"rating-desc",0).getFirst().rating());
        assertTrue(repository.list(-9101,0,"latest",1).isEmpty());
        jdbc.update("UPDATE accommodations SET listing_status='HIDDEN' WHERE id=-9101");
        assertEquals(0,repository.count(-9101,0));
    }
    @Test void tracksDeduplicateAndIgnoreHiddenOrIncompleteReviews() {
        var tracks=repository.tracks(-9101);
        assertEquals(1,tracks.size());assertEquals(2,tracks.getFirst().reviewCount());
        assertTrue(tracks.getFirst().latestCreatedAt().startsWith("2026-10-02"));
        jdbc.update("UPDATE accommodations SET listing_status='HIDDEN' WHERE id=-9101");
        assertTrue(repository.tracks(-9101).isEmpty());
    }
    @Test void tracksAreLimitedToFiftyMostRecentlyAttachedSongsAndPagesUseRequestedSize() {
        for(int i=1;i<=51;i++) {
            long id=-9400-i;
            jdbc.update("INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot,hold_expires_at,created_at) VALUES(?,?,-9101,-9101,-9101,'2026-10-01','2026-10-02',2,'테스트',100000,'COMPLETED','{}',NOW(),NOW())",id,"track-limit-"+i);
            jdbc.update("INSERT INTO reviews(id,reservation_id,accommodation_id,user_id,spotify_track_id,rating,status,created_at) VALUES(?,?,-9101,-9101,?,5,'VISIBLE',?)",id,id,String.format("%022d",i),java.time.LocalDateTime.of(2026,10,10,12,0).plusSeconds(i));
        }
        var tracks=repository.tracks(-9101);
        assertEquals(50,tracks.size());assertEquals(String.format("%022d",51),tracks.getFirst().spotifyTrackId());
        assertEquals(20,repository.list(-9101,5,"latest",0,20).size());
        assertEquals(12,repository.list(-9101,5,"latest",2,20).size());
    }
}
