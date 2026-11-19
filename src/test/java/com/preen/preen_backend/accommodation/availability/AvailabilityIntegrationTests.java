package com.preen.preen_backend.accommodation.availability;

import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration") @SpringBootTest @AutoConfigureMockMvc @Transactional
class AvailabilityIntegrationTests {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock testClock(){ return Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"),ZoneOffset.UTC); }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired AvailabilityService service;
    final LocalDate start=LocalDate.of(2030,1,2);
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO users(id,role,status,created_at,updated_at) VALUES(985801,'HOST','ACTIVE',NOW(),NOW())");
        jdbc.update("INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no,approval_status,created_at) VALUES(985801,985801,'availability-test','availability-test','APPROVED',NOW())");
        jdbc.update("INSERT INTO regions(id,name,level) VALUES(985801,'availability-region',1)");
        jdbc.update("INSERT INTO accommodation_categories(id,name,sort_order) VALUES(985801,'availability-category',999)");
        jdbc.update("INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status,created_at) VALUES(985801,985801,985801,985801,'availability-stay','test','15:00','11:00','LISTED',NOW())");
        for(long id=985802;id<=985807;id++) {
            jdbc.update("INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,nightly_price,extra_person_fee,status) VALUES(?,985801,?,2,4,999999,20000,?)",id,"room-"+id,id==985807?"INACTIVE":"ACTIVE");
        }
        for(long id=985802;id<=985807;id++) for(int day=0;day<2;day++) {
            if(id==985805 && day==1)continue;
            jdbc.update("INSERT INTO room_inventories(room_id,stay_date,total_count,booked_count,held_count,price,is_closed) VALUES(?,?,4,?,?,?,?)",id,start.plusDays(day),id==985803?3:1+day,id==985803?1:1,day==0?100000.25:120000.50,id==985804 && day==1);
        }
        jdbc.update("DELETE FROM room_inventories WHERE room_id=985806");
    }
    @Test void sumsDailyPricesAndUsesMinimumNetStockCheckoutExcluded() {
        // Checkout itself is closed and sold out: it must not affect this two-night stay.
        jdbc.update("INSERT INTO room_inventories(room_id,stay_date,total_count,booked_count,held_count,price,is_closed) VALUES(985802,?,0,0,0,999999,TRUE)",start.plusDays(2));
        var result=service.query(985801,start,start.plusDays(2));
        assertEquals(5,result.rooms().size());
        var room=result.rooms().getFirst();
        assertTrue(room.available());assertEquals("220000.75",room.totalPrice());assertEquals(1,room.remainingRooms());
        assertEquals("20000.00",room.extraPersonFee());assertEquals(2,room.nights());
    }
    @Test void holdsClosedMissingAndNoInventoryFailClosedWithoutFallbackPrice() {
        var rooms=service.query(985801,start,start.plusDays(2)).rooms();
        assertEquals("SOLD_OUT",rooms.get(1).reason());assertEquals("CLOSED",rooms.get(2).reason());
        assertEquals("MISSING_INVENTORY",rooms.get(3).reason());assertEquals("MISSING_INVENTORY",rooms.get(4).reason());
        rooms.stream().skip(1).forEach(r->{assertFalse(r.available());assertNull(r.totalPrice());});
    }
    @Test void anonymouslyReadsActualInventoryAndNeverChangesCounts() throws Exception {
        Long before=jdbc.queryForObject("SELECT SUM(held_count) FROM room_inventories WHERE room_id BETWEEN 985802 AND 985807",Long.class);
        mvc.perform(get("/api/accommodations/985801/availability").param("checkIn",start.toString()).param("checkOut",start.plusDays(2).toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.rooms[0].totalPrice").value("220000.75"));
        assertEquals(before,jdbc.queryForObject("SELECT SUM(held_count) FROM room_inventories WHERE room_id BETWEEN 985802 AND 985807",Long.class));
    }
    @Test void rejectsBadDatesPastDatesZeroAndOverThirtyNights() throws Exception {
        for(String[] dates:new String[][]{{"2030-02-30","2030-03-02"},{"2030-1-02","2030-01-03"},{"2029-12-31","2030-01-02"},{"2030-01-02","2030-01-02"},{"2030-01-02","2030-02-02"}})
            mvc.perform(get("/api/accommodations/985801/availability").param("checkIn",dates[0]).param("checkOut",dates[1])).andExpect(status().isBadRequest());
        mvc.perform(get("/api/accommodations/985801/availability").param("checkIn","2030-01-02")).andExpect(status().isBadRequest());
        assertEquals(30,service.query(985801,start,start.plusDays(30)).nights());
    }
    @Test void unpublishedAndUnknownStayAre404EmptyActiveRoomsIsEmpty() throws Exception {
        jdbc.update("UPDATE accommodations SET listing_status='DRAFT' WHERE id=985801");
        mvc.perform(get("/api/accommodations/985801/availability").param("checkIn",start.toString()).param("checkOut",start.plusDays(1).toString())).andExpect(status().isNotFound());
        jdbc.update("UPDATE accommodations SET listing_status='LISTED' WHERE id=985801");
        jdbc.update("UPDATE rooms SET status='INACTIVE' WHERE accommodation_id=985801");
        assertTrue(service.query(985801,start,start.plusDays(1)).rooms().isEmpty());
    }
}
