package com.preen.backend.reviews;
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
@ActiveProfiles("local")
@Transactional
@EnabledIfEnvironmentVariable(named="PREEN_VERIFY_LOCAL_DB",matches="true")
class MusicReviewRepositoryTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MusicReviewRepository repository;
    @BeforeEach void fixture() {
        jdbc.update("INSERT INTO users(id,role) VALUES(-9101,'HOST')");
        jdbc.update("INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no) VALUES(-9101,-9101,'리뷰 테스트','music-review-test')");
        jdbc.update("INSERT INTO regions(id,name,level) VALUES(-9101,'리뷰 테스트 지역',1)");
        jdbc.update("INSERT INTO accommodation_categories(id,name) VALUES(-9101,'리뷰 테스트 유형')");
        jdbc.update("INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status) VALUES(-9101,-9101,-9101,-9101,'리뷰 테스트 숙소','주소','15:00','11:00','LISTED')");
        jdbc.update("INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,nightly_price,extra_person_fee,status) VALUES(-9101,-9101,'리뷰 객실',2,2,100000,0,'ACTIVE')");
        jdbc.update("INSERT INTO refund_policies(id,accommodation_id,version,policy_text) VALUES(-9101,-9101,'test','테스트')");
        for (int i=1;i<=4;i++) {
            long id=-9200-i;
            jdbc.update("INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot) VALUES(?,?,-9101,-9101,-9101,'2026-10-01','2026-10-02',2,'반환하지 않는 이름',100000,?,'{}')",id,"music-test-"+i,i==4?"CONFIRMED":"COMPLETED");
            jdbc.update("INSERT INTO reviews(id,reservation_id,accommodation_id,user_id,spotify_track_id,rating,status,created_at) VALUES(?,?,-9101,-9101,'11dFghVXANMlKmJXsNCbNl',?,?,?)",id,id,i==1?5:3,i==3?"HIDDEN":"VISIBLE","2026-10-0"+i+" 12:00:00");
        }
    }
    @Test void filtersPublicCompletedReviewsAndSortsWithinThisStay() {
        assertEquals(2,repository.count(-9101,0));
        assertEquals(1,repository.count(-9101,5));
        assertEquals(0,repository.count(-999999,0));
        assertEquals(3,repository.list(-9101,0,"latest",0).getFirst().rating());
        assertEquals(5,repository.list(-9101,0,"rating-desc",0).getFirst().rating());
        assertTrue(repository.list(-9101,0,"latest",1).isEmpty());
        jdbc.update("UPDATE accommodations SET listing_status='HIDDEN' WHERE id=-9101");
        assertEquals(0,repository.count(-9101,0));
    }
}
