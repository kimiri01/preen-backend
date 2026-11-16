package com.preen.backend.reviews;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class MusicReviewRepository {
    private final JdbcTemplate jdbc;
    public MusicReviewRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Review(String id, String roomName, String spotifyTrackId, int rating, String createdAt, String authorLabel, String stayedOn, String title, String artist, String album) {}
    // Only public reviews for this public accommodation and its completed stays.
    // Reservation/user IDs, names, phone numbers and account data are never returned.
    private static final String FROM = """
        FROM reviews v JOIN accommodations a ON a.id=v.accommodation_id
        JOIN reservations b ON b.id=v.reservation_id AND b.user_id=v.user_id
        JOIN rooms r ON r.id=b.room_id AND r.accommodation_id=a.id
        LEFT JOIN spotify_track_metadata m ON m.spotify_track_id=v.spotify_track_id
        WHERE a.id=? AND a.listing_status='LISTED' AND v.status='VISIBLE'
        AND b.status='COMPLETED'
        """;
    public long count(long id, int rating) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) " + FROM + " AND (?=0 OR v.rating=?)", Long.class, id, rating, rating);
        return count == null ? 0 : count;
    }
    public List<Review> list(long id, int rating, String sort, int page) {
        String order = switch (sort) {
            case "rating-desc" -> "v.rating DESC,v.created_at DESC,v.id DESC";
            case "rating-asc" -> "v.rating ASC,v.created_at DESC,v.id DESC";
            default -> "v.created_at DESC,v.id DESC";
        };
        return jdbc.query("SELECT v.id,r.name,v.spotify_track_id,v.rating,v.created_at,CONCAT(LEFT(TRIM(b.guest_name),1),'**') AS author_label,b.check_out,m.title,m.artist,m.album " + FROM
            + " AND (?=0 OR v.rating=?) ORDER BY " + order + " LIMIT 10 OFFSET ?",
            (rs,row) -> new Review(Long.toString(rs.getLong("id")),rs.getString("name"),
                rs.getString("spotify_track_id"),rs.getInt("rating"),rs.getTimestamp("created_at").toLocalDateTime().toString(),rs.getString("author_label"),rs.getDate("check_out").toLocalDate().toString(),rs.getString("title"),rs.getString("artist"),rs.getString("album")),
            id,rating,rating,page*10);
    }
}
