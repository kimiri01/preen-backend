package com.preen.preen_backend.accommodation.rooms;
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
@EnabledIfEnvironmentVariable(named="PREEN_VERIFY_LOCAL_DB", matches="true")
class RoomRepositoryTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired RoomRepository repository;
    @Autowired RoomService service;
    @BeforeEach void fixtures() {
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(-9001,'HOST','ACTIVE',NOW(),NOW())");
        jdbc.update("INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no,approval_status,created_at) VALUES(-9001,-9001,'객실 테스트','room-spec-test-only','APPROVED',NOW())");
        jdbc.update("INSERT INTO regions(id,name,level) VALUES(-9001,'테스트 지역',1)");
        jdbc.update("INSERT INTO accommodation_categories(id,name,sort_order) VALUES(-9001,'객실 테스트 카테고리',998)");
        jdbc.update("INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status,created_at) VALUES(-9001,-9001,-9001,-9001,'테스트 숙소','테스트 주소','15:00','11:00','LISTED',NOW())");
        jdbc.update("INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,nightly_price,extra_person_fee,status) VALUES(-9001,-9001,'활성 객실',2,4,150000,20000,'ACTIVE'),(-9002,-9001,'비활성 객실',2,2,100000,0,'INACTIVE')");
        jdbc.update("INSERT INTO room_images(id,room_id,image_url,sort_order) VALUES(-9001,-9001,'https://example.com/second.jpg',2),(-9002,-9001,'https://example.com/first.jpg',1),(-9003,-9002,'https://example.com/hidden.jpg',0)");
    }
    @Test void listsOnlyActiveRoomsAndOrderedImages() {
        var result=service.list(-9001);
        assertEquals(1,result.rooms().size());
        var room=result.rooms().getFirst();
        assertEquals("150000.00",room.nightlyPrice());
        assertEquals(2,room.capacity());assertEquals(4,room.maxCapacity());
        assertEquals(2,room.images().size());
        assertTrue(room.images().getFirst().imageUrl().endsWith("first.jpg"));
    }
    @Test void hiddenStaysCannotBeRead() {
        jdbc.update("UPDATE accommodations SET listing_status='HIDDEN' WHERE id=-9001");
        assertTrue(repository.findListedStay(-9001).isEmpty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.list(-9001));
    }
    @Test void missingAndEmptyStayAreDifferent() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.list(-999999));
        jdbc.update("UPDATE rooms SET status='INACTIVE' WHERE accommodation_id=-9001");
        assertTrue(service.list(-9001).rooms().isEmpty());
    }
    @Test void absentSpecsRemainUnknownInsteadOfHidingRoom() {
        var room=service.list(-9001).rooms().getFirst();
        assertNull(room.roomType());assertNull(room.areaSqm());assertNull(room.audio());
        assertTrue(room.beds().isEmpty());assertTrue(room.facilities().isEmpty());
    }
    @Test void multipleBedsAndFacilitiesDoNotDuplicateRoomOrMixInactiveData() {
        jdbc.update("INSERT INTO room_specs(room_id,room_type,area_sqm) VALUES(-9001,'트윈',38.50)");
        jdbc.update("INSERT INTO room_beds(room_id,bed_type,bed_count,sort_order) VALUES(-9001,'DOUBLE',1,1),(-9001,'SINGLE',2,2),(-9002,'KING',1,1)");
        jdbc.update("INSERT INTO room_facilities(id,code,name) VALUES(-9001,'TEST_ROOM_WIFI','와이파이'),(-9002,'TEST_ROOM_TUB','욕조')");
        jdbc.update("INSERT INTO room_facility_links(room_id,facility_id) VALUES(-9001,-9001),(-9001,-9002),(-9002,-9002)");
        var result=service.list(-9001);
        assertEquals(1,result.rooms().size());
        var room=result.rooms().getFirst();
        assertEquals("38.50",room.areaSqm());assertEquals("트윈",room.roomType());
        assertEquals(2,room.beds().size());assertEquals("DOUBLE",room.beds().getFirst().type());
        assertEquals(2,room.beds().get(1).count());assertEquals(2,room.facilities().size());
    }
    @Test void audioBelongsOnlyToVisibleActiveRooms() {
        jdbc.update("INSERT INTO room_audio(room_id,equipment_name,connection_guide,available_from,available_until,volume_guide) VALUES(-9001,'스테레오','Bluetooth','09:00','22:00','적정 음량'),(-9002,'미노출 장비','AUX','10:00','20:00','안내')");
        jdbc.update("UPDATE room_audio SET spotify_track_id='0Lr4kGOYn9l83EjuK6cZFQ' WHERE room_id=-9001");
        var rooms=service.list(-9001).rooms();
        assertEquals(1,rooms.size());assertNotNull(rooms.getFirst().audio());
        assertEquals("스테레오",rooms.getFirst().audio().equipmentName());
        assertEquals("09:00",rooms.getFirst().audio().availableFrom());
        assertEquals("22:00",rooms.getFirst().audio().availableUntil());
        assertEquals("0Lr4kGOYn9l83EjuK6cZFQ",rooms.getFirst().audio().spotifyTrackId());
        assertEquals(1,repository.findAudio(-9001).size());
        jdbc.update("UPDATE accommodations SET listing_status='HIDDEN' WHERE id=-9001");
        assertTrue(repository.findAudio(-9001).isEmpty());
    }
    @Test void readsActualStayAmenitiesCoordinatesAndOnlyActiveRefundPolicy() {
        jdbc.update("UPDATE accommodations SET latitude=33.1234567,longitude=126.1234567 WHERE id=-9001");
        jdbc.update("INSERT INTO amenities(id,name) VALUES(-9001,'숙소 공용 주차')");
        jdbc.update("INSERT INTO accommodation_amenities(accommodation_id,amenity_id) VALUES(-9001,-9001)");
        jdbc.update("INSERT INTO refund_policies(id,accommodation_id,version,policy_text,active) VALUES(-9102,-9001,'active-v1','실제 환불 안내',TRUE)");
        jdbc.update("INSERT INTO refund_policy_rules(refund_policy_id,days_before,refund_rate) VALUES(-9102,7,1.00),(-9102,1,0.50)");
        var result=service.list(-9001);
        assertEquals("33.1234567",result.latitude());
        assertEquals("126.1234567",result.longitude());
        assertEquals(java.util.List.of("숙소 공용 주차"),result.amenities());
        assertEquals("active-v1",result.refundPolicy().version());
        assertEquals(7,result.refundPolicy().rules().getFirst().daysBefore());
        assertEquals(0,result.refundPolicy().rules().getFirst().refundRate().compareTo(java.math.BigDecimal.ONE));
        jdbc.update("UPDATE refund_policies SET active=FALSE WHERE id=-9102");
        assertNull(service.list(-9001).refundPolicy());
    }
}
