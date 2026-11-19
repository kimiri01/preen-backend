package com.preen.preen_backend.review.write;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.preen.preen_backend.auth.service.AuthHashService;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginReviewFlowTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthHashService hashes;
    @Autowired ObjectMapper mapper;
    @Autowired StringRedisTemplate redis;
    private static final long OWNER=982001, OTHER=982002, BOOKING=982010;
    private static final String TRACK="4Dr2hJ3EnVh2Aaot6fRwDO";
    private static final String ORIGIN="http://127.0.0.1:5178";
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(?,'GUEST','ACTIVE',NOW(),NOW()),(?,'GUEST','ACTIVE',NOW(),NOW())",OWNER,OTHER);
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(985001,'HOST','ACTIVE',NOW(),NOW())");
        jdbc.update("INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no,approval_status,created_at) VALUES(985001,985001,'login-review-test','login-review-test','APPROVED',NOW())");
        jdbc.update("INSERT INTO regions(id,name,level) VALUES(985001,'login-review-region',1)");
        jdbc.update("INSERT INTO accommodation_categories(id,name,sort_order) VALUES(985001,'login-review-category',999)");
        jdbc.update("INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status,created_at) VALUES(985001,985001,985001,985001,'login-review-stay','test address','15:00','11:00','LISTED',NOW())");
        jdbc.update("INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,nightly_price,extra_person_fee,status) VALUES(985001,985001,'login-review-room',2,2,100000,0,'ACTIVE')");
        jdbc.update("INSERT INTO refund_policies(id,accommodation_id,version,policy_text,active) VALUES(985001,985001,'test','test',FALSE)");
        jdbc.update("INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot,hold_expires_at,created_at) VALUES(?,'login-review-booking',?,985001,985001,'2026-10-01','2026-10-02',2,'테스트',100000,'COMPLETED','{}',NOW(),NOW())",BOOKING,OWNER);

        for(long id : new long[]{OWNER,OTHER}) {
            jdbc.update("INSERT INTO user_personal_info(user_id,name,email_hash) VALUES(?,?,?)",id,"login-review-test",hashes.hash("login-review-"+id+"@example.invalid"));
            jdbc.update("INSERT INTO user_credentials(user_id,password_hash,password_changed_at,failed_login_count) VALUES(?,?,NOW(),0)",id,encoder.encode("LocalTest!2026"));
        }
    }
    @AfterEach void cleanupOwnSessions() {
        redis.delete(java.util.List.of("auth:session:"+OWNER,"auth:session:"+OTHER));
    }
    private String login(long id) throws Exception {
        var response=mvc.perform(post("/api/auth/login").header("Origin",ORIGIN).contentType("application/json")
            .content("{\"email\":\"login-review-"+id+"@example.invalid\",\"password\":\"LocalTest!2026\",\"rememberMe\":false}"))
            .andExpect(status().isOk()).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString()).path("data").path("accessToken").asText();
    }
    private String body() { return "{\"reservationId\":\""+BOOKING+"\",\"rating\":5,\"spotifyTrackId\":\""+TRACK+"\"}"; }
    @Test void realLoginJwtRedisReviewWritePublicListAndLogout() throws Exception {
        String token=login(OWNER);
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/review-reservations").param("accommodationId","985001").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].eligible").value(true));
        mvc.perform(post("/api/reviews").header("Authorization","Bearer "+token).contentType("application/json").content(body()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.rating").value(5));
        mvc.perform(get("/api/accommodations/985001/reviews").param("rating","5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(post("/api/reviews").header("Authorization","Bearer "+token).contentType("application/json").content(body()))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/logout").header("Origin",ORIGIN).header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/review-reservations").param("accommodationId","985001").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void anonymousForgedTokenOtherOwnerAndWrongPasswordCannotWrite() throws Exception {
        mvc.perform(get("/api/review-reservations").param("accommodationId","985001")).andExpect(status().isUnauthorized());
        String ownerToken=login(OWNER);
        mvc.perform(get("/api/review-reservations").param("accommodationId","985001").header("Authorization","Bearer "+ownerToken+"x")).andExpect(status().isUnauthorized());
        String otherToken=login(OTHER);
        mvc.perform(post("/api/reviews").header("Authorization","Bearer "+otherToken).contentType("application/json").content(body())).andExpect(status().isNotFound());
        mvc.perform(get("/api/review-reservations").param("accommodationId","985001").header("Authorization","Bearer invalid"))
            .andExpect(status().isUnauthorized());
        String other=login(OTHER);
        mvc.perform(post("/api/reviews").header("Authorization","Bearer "+other).contentType("application/json").content(body())).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login").header("Origin",ORIGIN).contentType("application/json")
            .content("{\"email\":\"login-review-982001@example.invalid\",\"password\":\"Incorrect!2026\",\"rememberMe\":false}"))
            .andExpect(status().isUnauthorized());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE reservation_id=?",Integer.class,BOOKING));
    }
    @Test void musicSearchRequiresLoginAndMissingProviderKeysAreReportedClearly() throws Exception {
        mvc.perform(get("/api/spotify/tracks/search").param("q","test")).andExpect(status().isUnauthorized());
        String token=login(OWNER);
        mvc.perform(get("/api/spotify/tracks/search").param("q","test").header("Authorization","Bearer "+token))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SPOTIFY_NOT_CONFIGURED"));
        mvc.perform(get("/api/spotify/tracks/search").param("q"," ").header("Authorization","Bearer "+token))
            .andExpect(status().isBadRequest());
    }

}
