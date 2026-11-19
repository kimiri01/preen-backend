package com.preen.preen_backend.accommodation.availability;

import com.preen.preen_backend.common.response.ApiResponse;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
public class AvailabilityController {
    private final AvailabilityService service;
    public AvailabilityController(AvailabilityService service) { this.service=service; }
    @GetMapping("/api/accommodations/{id}/availability")
    public ApiResponse<AvailabilityService.Result> get(@PathVariable long id,
        @RequestParam String checkIn, @RequestParam String checkOut) {
        return ApiResponse.ok(service.query(id,parse(checkIn),parse(checkOut)));
    }
    private LocalDate parse(String value) {
        try {
            if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
            return LocalDate.parse(value);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"날짜는 실제 존재하는 yyyy-MM-dd 형식이어야 합니다.");
        }
    }
}

