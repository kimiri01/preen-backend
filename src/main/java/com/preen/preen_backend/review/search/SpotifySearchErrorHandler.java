package com.preen.preen_backend.review.search;

import com.preen.preen_backend.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=SpotifySearchController.class)
public class SpotifySearchErrorHandler {
    @ExceptionHandler(SpotifySearchService.Failure.class)
    ResponseEntity<ApiResponse<Void>> failure(SpotifySearchService.Failure error) {
        return ResponseEntity.status(error.status).body(ApiResponse.failure(error.code,error.getMessage()));
    }
}
