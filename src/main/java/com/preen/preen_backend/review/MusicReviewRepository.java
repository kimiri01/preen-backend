package com.preen.preen_backend.review;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
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
        WHERE a.id=? AND a.listing_status='LISTED' AND v.status='VISIBLE'
        AND b.status='COMPLETED'
        """;
    public long count(long id, int rating) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) " + FROM + " AND (?=0 OR v.rating=?)", Long.class, id, rating, rating);
        return count == null ? 0 : count;
    }
    public List<Review> list(long id, int rating, String sort, int page) {
        return list(id,rating,sort,page,10);
    }
    public List<Review> list(long id, int rating, String sort, int page, int size) {
        String order = switch (sort) {
            case "rating-desc" -> "v.rating DESC,v.created_at DESC,v.id DESC";
            case "rating-asc" -> "v.rating ASC,v.created_at DESC,v.id DESC";
            default -> "v.created_at DESC,v.id DESC";
        };
        return jdbc.query("SELECT v.id,r.name,v.spotify_track_id,v.rating,v.created_at,CONCAT(LEFT(TRIM(b.guest_name),1),'**') AS author_label,b.check_out " + FROM
            + " AND (?=0 OR v.rating=?) ORDER BY " + order + " LIMIT ? OFFSET ?",
            (rs,row) -> new Review(Long.toString(rs.getLong("id")),rs.getString("name"),
                rs.getString("spotify_track_id"),rs.getInt("rating"),rs.getTimestamp("created_at").toLocalDateTime().toString(),rs.getString("author_label"),rs.getDate("check_out").toLocalDate().toString(),null,null,null),
            id,rating,rating,size,(long)page*size);
    }

    public record Track(String spotifyTrackId, long reviewCount, String latestCreatedAt) {}
    public List<Track> tracks(long id) {
        return jdbc.query("SELECT v.spotify_track_id,COUNT(*) AS review_count,MAX(v.created_at) AS latest_created_at " + FROM
            + " GROUP BY v.spotify_track_id ORDER BY MAX(v.created_at) DESC,v.spotify_track_id LIMIT 50",
            (rs,row)->new Track(rs.getString("spotify_track_id"),rs.getLong("review_count"),rs.getTimestamp("latest_created_at").toLocalDateTime().toString()),id);
    }
}
