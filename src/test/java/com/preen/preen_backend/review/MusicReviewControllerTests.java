package com.preen.preen_backend.review;
import com.preen.preen_backend.accommodation.rooms.RoomRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MusicReviewControllerTests {
    final MusicReviewRepository reviews=mock(MusicReviewRepository.class);
    final RoomRepository stays=mock(RoomRepository.class);
    final MockMvc mvc=MockMvcBuilders.standaloneSetup(new MusicReviewController(reviews,stays)).build();
    @Test void rejectsInvalidIdsAndFiltersBeforeDatabaseAccess() throws Exception {
        for (String path:List.of("0/music-reviews","9223372036854775808/music-reviews","7/music-reviews?rating=6","7/music-reviews?sort=sql","7/music-reviews?page=-1"))
            mvc.perform(get("/api/accommodations/"+path)).andExpect(status().isBadRequest());
        verifyNoInteractions(reviews,stays);
    }
    @Test void hiddenOrMissingStayHasNoPublicReviews() throws Exception {
        when(stays.findListedStay(7)).thenReturn(Optional.empty());
        mvc.perform(get("/api/accommodations/7/music-reviews")).andExpect(status().isNotFound());
        verifyNoInteractions(reviews);
    }
    @Test void returnsRequestedRatingAndPaginationWithoutUserData() throws Exception {
        when(stays.findListedStay(7)).thenReturn(Optional.of(new RoomRepository.Stay(7,"숙소",null,"주소","15:00","11:00","HOTEL","호텔","SEOUL","서울")));
        when(reviews.count(7,0)).thenReturn(4L); when(reviews.count(7,5)).thenReturn(1L);
        when(reviews.list(7,5,"rating-desc",0)).thenReturn(List.of(new MusicReviewRepository.Review("8","스위트","11dFghVXANMlKmJXsNCbNl",5,"2026-11-16T15:00:00","김**","2026-11-15","테스트 곡","아티스트","앨범")));
        mvc.perform(get("/api/accommodations/7/music-reviews?rating=5&sort=rating-desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(4))
            .andExpect(jsonPath("$.data.filteredTotal").value(1)).andExpect(jsonPath("$.data.reviews[0].rating").value(5))
            .andExpect(jsonPath("$.data.reviews[0].userId").doesNotExist());
    }
    @Test void canonicalPageUsesTeamShapeAndValidatesSize() throws Exception {
        for(String query:List.of("size=0","size=51","page=-1","rating=8","sort=unsafe"))
            mvc.perform(get("/api/accommodations/7/reviews?"+query)).andExpect(status().isBadRequest());
        verifyNoInteractions(reviews,stays);
        when(stays.findListedStay(7)).thenReturn(Optional.of(new RoomRepository.Stay(7,"숙소",null,"주소","15:00","11:00","HOTEL","호텔","SEOUL","서울")));
        when(reviews.count(7,5)).thenReturn(21L);
        when(reviews.list(7,5,"latest",0,20)).thenReturn(List.of());
        mvc.perform(get("/api/accommodations/7/reviews?rating=5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(21)).andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.hasNext").value(true)).andExpect(jsonPath("$.data.content").isArray());
        mvc.perform(get("/api/accommodations/7/reviews?rating=5&page=1&size=20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.hasNext").value(false));
    }
    @Test void missingStayDoesNotExposeTracks() throws Exception {
        mvc.perform(get("/api/accommodations/99/tracks")).andExpect(status().isNotFound());
        verifyNoInteractions(reviews);
    }
}
