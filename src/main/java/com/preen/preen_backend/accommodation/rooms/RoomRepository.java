package com.preen.preen_backend.accommodation.rooms;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("moonRoomQueryRepository")
public class RoomRepository {
    private final JdbcTemplate jdbc;
    public RoomRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Stay(long id, String name, String description, String address, String checkInTime, String checkOutTime, String categoryCode, String categoryName, String regionCode, String regionName) {}
    public record Room(long id, String name, int capacity, int maxCapacity,
                       BigDecimal nightlyPrice, BigDecimal extraPersonFee, String roomType, BigDecimal areaSqm) {}
    public record Image(long id, long roomId, String imageUrl, int sortOrder) {}

    public Optional<Stay> findListedStay(long id) {
        return jdbc.query("""
            SELECT a.id,a.name,a.description,a.address,a.check_in_time,a.check_out_time,
                   c.code AS category_code,c.name AS category_name,g.code AS region_code,g.name AS region_name
            FROM accommodations a
            JOIN accommodation_categories c ON c.id=a.category_id
            JOIN regions g ON g.id=a.region_id
            WHERE a.id=? AND a.listing_status='LISTED'
            """,
            (rs, row) -> new Stay(rs.getLong("id"), rs.getString("name"), rs.getString("description"), rs.getString("address"), rs.getTime("check_in_time").toLocalTime().toString(), rs.getTime("check_out_time").toLocalTime().toString(), rs.getString("category_code"), rs.getString("category_name"), rs.getString("region_code"), rs.getString("region_name")), id).stream().findFirst();
    }
    public List<Image> findStayImages(long stayId) {
        return jdbc.query("""
            SELECT i.id,i.image_url,i.sort_order FROM accommodation_images i
            JOIN accommodations a ON a.id=i.accommodation_id
            WHERE a.id=? AND a.listing_status='LISTED' ORDER BY i.sort_order,i.id
            """, (rs,row)->new Image(rs.getLong("id"),0,rs.getString("image_url"),rs.getInt("sort_order")),stayId);
    }
    public List<Room> findActiveRooms(long stayId) {
        return jdbc.query("""
            SELECT r.id, r.name, r.capacity, r.max_capacity, r.nightly_price, r.extra_person_fee, s.room_type, s.area_sqm
            FROM rooms r JOIN accommodations a ON a.id = r.accommodation_id
            LEFT JOIN room_specs s ON s.room_id = r.id
            WHERE a.id = ? AND a.listing_status = 'LISTED' AND r.status = 'ACTIVE'
            ORDER BY r.id
            """, (rs, row) -> new Room(rs.getLong("id"), rs.getString("name"),
                rs.getInt("capacity"), rs.getInt("max_capacity"),
                rs.getBigDecimal("nightly_price"), rs.getBigDecimal("extra_person_fee"),
                rs.getString("room_type"), rs.getBigDecimal("area_sqm")), stayId);
    }
    public List<Image> findActiveRoomImages(long stayId) {
        return jdbc.query("""
            SELECT i.id, i.room_id, i.image_url, i.sort_order
            FROM room_images i JOIN rooms r ON r.id = i.room_id
            JOIN accommodations a ON a.id = r.accommodation_id
            WHERE a.id = ? AND a.listing_status = 'LISTED' AND r.status = 'ACTIVE'
            ORDER BY i.room_id, i.sort_order, i.id
            """, (rs, row) -> new Image(rs.getLong("id"), rs.getLong("room_id"),
                rs.getString("image_url"), rs.getInt("sort_order")), stayId);
    }
    public record Bed(long roomId, String bedType, int count) {}
    public record Facility(long roomId, String code, String name) {}
    public List<Bed> findBeds(long stayId) {
        return jdbc.query("""
            SELECT b.room_id,b.bed_type,b.bed_count FROM room_beds b
            JOIN rooms r ON r.id=b.room_id JOIN accommodations a ON a.id=r.accommodation_id
            WHERE a.id=? AND a.listing_status='LISTED' AND r.status='ACTIVE'
            ORDER BY b.room_id,b.sort_order,b.id
            """, (rs,row)->new Bed(rs.getLong("room_id"),rs.getString("bed_type"),rs.getInt("bed_count")),stayId);
    }
    public List<Facility> findFacilities(long stayId) {
        return jdbc.query("""
            SELECT l.room_id,f.code,f.name FROM room_facility_links l
            JOIN room_facilities f ON f.id=l.facility_id
            JOIN rooms r ON r.id=l.room_id JOIN accommodations a ON a.id=r.accommodation_id
            WHERE a.id=? AND a.listing_status='LISTED' AND r.status='ACTIVE'
            ORDER BY l.room_id,f.id
            """, (rs,row)->new Facility(rs.getLong("room_id"),rs.getString("code"),rs.getString("name")),stayId);
    }
    public record Audio(long roomId, String equipmentName, String connectionGuide,
                        String availableFrom, String availableUntil, String volumeGuide, String spotifyTrackId) {}
    public List<Audio> findAudio(long stayId) {
        return jdbc.query("""
            SELECT au.* FROM room_audio au
            JOIN rooms r ON r.id=au.room_id JOIN accommodations a ON a.id=r.accommodation_id
            WHERE a.id=? AND a.listing_status='LISTED' AND r.status='ACTIVE'
            ORDER BY au.room_id
            """, (rs,row)->new Audio(rs.getLong("room_id"),rs.getString("equipment_name"),
                rs.getString("connection_guide"),rs.getTime("available_from").toLocalTime().toString(),
                rs.getTime("available_until").toLocalTime().toString(),rs.getString("volume_guide"),rs.getString("spotify_track_id")),stayId);
    }

    public record Coordinates(String latitude, String longitude) {}
    public record RefundRule(int daysBefore, BigDecimal refundRate) {}
    public record RefundPolicy(String id, String version, String policyText, List<RefundRule> rules) {}

    public Coordinates findCoordinates(long stayId) {
        return jdbc.queryForObject("SELECT latitude,longitude FROM accommodations WHERE id=?", (rs,row) -> {
            BigDecimal latitude=rs.getBigDecimal("latitude"), longitude=rs.getBigDecimal("longitude");
            return new Coordinates(latitude==null?null:latitude.toPlainString(),longitude==null?null:longitude.toPlainString());
        },stayId);
    }
    public List<String> findStayAmenities(long stayId) {
        return jdbc.query("SELECT m.name FROM amenities m JOIN accommodation_amenities l ON l.amenity_id=m.id WHERE l.accommodation_id=? ORDER BY m.id",
            (rs,row)->rs.getString("name"),stayId);
    }
    public RefundPolicy findRefundPolicy(long stayId) {
        var policies=jdbc.query("SELECT id,version,policy_text FROM refund_policies WHERE accommodation_id=? AND active=TRUE ORDER BY id DESC LIMIT 1",
            (rs,row)->new RefundPolicy(rs.getString("id"),rs.getString("version"),rs.getString("policy_text"),List.of()),stayId);
        if(policies.isEmpty()) return null;
        var policy=policies.getFirst();
        var rules=jdbc.query("SELECT days_before,refund_rate FROM refund_policy_rules WHERE refund_policy_id=? ORDER BY days_before DESC,id",
            (rs,row)->new RefundRule(rs.getInt("days_before"),rs.getBigDecimal("refund_rate")),Long.parseLong(policy.id()));
        return new RefundPolicy(policy.id(),policy.version(),policy.policyText(),rules);
    }
}
