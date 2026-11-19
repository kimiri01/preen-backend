package com.preen.preen_backend.review.search;

import java.util.List;
import com.preen.preen_backend.common.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class SpotifySearchController {
    private final SpotifySearchService service;
    public SpotifySearchController(SpotifySearchService service){this.service=service;}
    @GetMapping("/api/spotify/tracks/search")
    public ResponseEntity<ApiResponse<List<SpotifySearchService.Track>>> search(@RequestParam String q) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(service.search(q)));
    }
}
