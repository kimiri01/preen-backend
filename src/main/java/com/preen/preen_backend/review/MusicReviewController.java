package com.preen.preen_backend.review;

import java.util.List;
import java.util.Set;
import com.preen.preen_backend.accommodation.rooms.RoomRepository;
import org.springframework.http.HttpStatus;
import com.preen.preen_backend.common.response.ApiResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accommodations")
public class MusicReviewController {
    private final MusicReviewRepository reviews;
    private final RoomRepository stays;
    public MusicReviewController(MusicReviewRepository reviews, RoomRepository stays) { this.reviews=reviews; this.stays=stays; }
    public record ReviewList(String accommodationId, long total, long filteredTotal, int page, int pageSize, List<MusicReviewRepository.Review> reviews) {}
    @GetMapping("/{accommodationId}/music-reviews")
    @Transactional(readOnly=true)
    public ApiResponse<ReviewList> list(@PathVariable String accommodationId,
        @RequestParam(defaultValue="0") int rating, @RequestParam(defaultValue="latest") String sort,
        @RequestParam(defaultValue="0") int page) {
        long id;
        try {
            if (!accommodationId.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
            id=Long.parseLong(accommodationId);
        } catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        if (rating<0 || rating>5 || page<0 || page>10000 || !Set.of("latest","rating-desc","rating-asc").contains(sort))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (stays.findListedStay(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        long total=reviews.count(id,0);
        return ApiResponse.ok(new ReviewList(accommodationId,total,rating==0?total:reviews.count(id,rating),page,10,reviews.list(id,rating,sort,page)));
    }
}
