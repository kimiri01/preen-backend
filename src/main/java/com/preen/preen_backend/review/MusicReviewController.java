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

    // Team contract: keep the legacy music-reviews payload unchanged for existing callers.
    @GetMapping("/{accommodationId}/reviews")
    @Transactional(readOnly=true)
    public ApiResponse<com.preen.preen_backend.common.response.PageResponse<MusicReviewRepository.Review>> page(
        @PathVariable String accommodationId,@RequestParam(defaultValue="0") int rating,
        @RequestParam(defaultValue="latest") String sort,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) {
        if(rating<0||rating>5||page<0||page>10000||size<1||size>50||!Set.of("latest","rating-desc","rating-asc").contains(sort))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        long id=listedId(accommodationId);
        long total=reviews.count(id,rating);
        return ApiResponse.ok(new com.preen.preen_backend.common.response.PageResponse<>(reviews.list(id,rating,sort,page,size),
            page,size,total,(int)((total+size-1)/size),(long)(page+1)*size<total));
    }
    @GetMapping("/{accommodationId}/tracks")
    @Transactional(readOnly=true)
    public ApiResponse<List<MusicReviewRepository.Track>> tracks(@PathVariable String accommodationId) {
        return ApiResponse.ok(reviews.tracks(listedId(accommodationId)));
    }
    private long listedId(String value) {
        long id;
        try {
            if(!value.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
            id=Long.parseLong(value);
        } catch(NumberFormatException error) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}
        if(stays.findListedStay(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return id;
    }
}
