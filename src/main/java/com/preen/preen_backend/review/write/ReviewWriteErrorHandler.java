package com.preen.preen_backend.review.write;

import com.preen.preen_backend.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=ReviewWriteController.class)
public class ReviewWriteErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse<Void>> status(ResponseStatusException exception) {
        int status=exception.getStatusCode().value();
        return ResponseEntity.status(status).body(ApiResponse.failure("REVIEW_"+status,exception.getReason()==null?"요청 값을 확인해주세요.":exception.getReason()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure("REVIEW_400","예약·별점·Spotify 곡을 확인해주세요."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> failure(Exception exception) {
        return ResponseEntity.internalServerError().body(ApiResponse.failure("REVIEW_500","리뷰를 저장하지 못했습니다. 잠시 후 다시 시도해주세요."));
    }
}
