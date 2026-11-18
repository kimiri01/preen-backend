package com.preen.preen_backend.accommodation.rooms;

import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.review.MusicReviewController;
import com.preen.preen_backend.review.spotify.SpotifyMetadataController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Feature-scoped advice: does not replace the team's handlers for other endpoints. */
@RestControllerAdvice(assignableTypes={RoomController.class,MusicReviewController.class,SpotifyMetadataController.class})
public class RoomFeatureErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException ex) {
        int code=ex.getStatusCode().value();
        String message=code==404?"요청한 공개 정보를 찾을 수 없습니다.":code==400?"요청 값을 확인해주세요.":"일시적으로 요청을 처리하지 못했습니다.";
        return ResponseEntity.status(code).body(new ApiResponse<>(false,"ROOM_"+code,message,null));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> badParameter() {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false,"ROOM_400","요청 값을 확인해주세요.",null));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> failure() {
        return ResponseEntity.internalServerError().body(new ApiResponse<>(false,"ROOM_500","일시적으로 요청을 처리하지 못했습니다.",null));
    }
}
