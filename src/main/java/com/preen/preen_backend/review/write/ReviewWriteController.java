package com.preen.preen_backend.review.write;

import com.preen.preen_backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReviewWriteController {
    private final ReviewWriteService service;
    private final ReviewUserResolver users;
    public ReviewWriteController(ReviewWriteService service,ReviewUserResolver users) {this.service=service;this.users=users;}
    public record CreateRequest(@NotBlank @Pattern(regexp="[1-9][0-9]{0,18}") String reservationId,
                                @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer=1,fraction=0) BigDecimal rating,
                                @NotBlank @Pattern(regexp="[A-Za-z0-9]{22}") String spotifyTrackId) {}
    public record CsrfResponse(String headerName,String token) {}
    @GetMapping("/api/review-reservations/{reservationId}")
    public ApiResponse<ReviewWriteService.Context> context(Authentication authentication,@PathVariable long reservationId) {
        if(reservationId<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return ApiResponse.ok(service.getContext(users.requireUserId(authentication),reservationId));
    }
    @GetMapping("/api/review-reservations")
    public ApiResponse<List<ReviewWriteService.Context>> eligible(Authentication authentication,@RequestParam long accommodationId) {
        if(accommodationId<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return ApiResponse.ok(service.eligible(users.requireUserId(authentication),accommodationId));
    }
    @GetMapping("/api/reviews/csrf")
    public ApiResponse<CsrfResponse> csrf(Authentication authentication,HttpServletRequest request) {
        users.requireUserId(authentication);
        // The team's temporary mode disables CSRF; the normal mode still returns its token.
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ApiResponse.ok(token == null ? new CsrfResponse(null,null)
                : new CsrfResponse(token.getHeaderName(),token.getToken()));
    }
    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewWriteService.Created> create(Authentication authentication,@Valid @RequestBody CreateRequest request) {
        long reservationId;
        try { reservationId=Long.parseLong(request.reservationId()); }
        catch(NumberFormatException exception) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}
        return ApiResponse.ok(service.create(users.requireUserId(authentication),reservationId,request.rating().intValueExact(),request.spotifyTrackId()));
    }
}
