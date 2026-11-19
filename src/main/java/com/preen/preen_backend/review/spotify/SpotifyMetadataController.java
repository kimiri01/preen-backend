package com.preen.preen_backend.review.spotify;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.preen.preen_backend.common.response.ApiResponse;

@RestController
@RequestMapping("/api/spotify/tracks")
public class SpotifyMetadataController {
    private final SpotifyMetadataService service;
    private final JdbcTemplate jdbc;
    public SpotifyMetadataController(SpotifyMetadataService service,JdbcTemplate jdbc) {this.service=service;this.jdbc=jdbc;}
    @GetMapping("/{id}/metadata")
    public ApiResponse<SpotifyMetadataService.Metadata> metadata(@PathVariable String id) {
        if(!id.matches("[a-zA-Z0-9]{22}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        // Only IDs in public completed reviews or active public-room recommendations are accepted.
        Integer count=jdbc.queryForObject("""
          SELECT COUNT(*) FROM (
            SELECT v.id FROM reviews v JOIN accommodations a ON a.id=v.accommodation_id
            JOIN reservations b ON b.id=v.reservation_id AND b.user_id=v.user_id
            JOIN rooms r ON r.id=b.room_id AND r.accommodation_id=a.id
            WHERE v.spotify_track_id=? AND v.status='VISIBLE' AND a.listing_status='LISTED' AND b.status='COMPLETED'
            UNION ALL
            SELECT au.room_id FROM room_audio au JOIN rooms r ON r.id=au.room_id
            JOIN accommodations a ON a.id=r.accommodation_id
            WHERE au.spotify_track_id=? AND r.status='ACTIVE' AND a.listing_status='LISTED'
          ) public_tracks
          """,Integer.class,id,id);
        if(count==null || count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ApiResponse.ok(service.lookup(id));
    }
}
