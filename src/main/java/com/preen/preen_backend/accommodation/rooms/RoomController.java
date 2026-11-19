package com.preen.preen_backend.accommodation.rooms;

import org.springframework.http.HttpStatus;
import com.preen.preen_backend.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accommodations")
public class RoomController {
    private final RoomService service;
    public RoomController(RoomService service) { this.service = service; }

    @GetMapping("/{accommodationId}/rooms")
    public ApiResponse<RoomService.RoomList> list(@PathVariable String accommodationId) {
        long id;
        try {
            if (!accommodationId.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
            id = Long.parseLong(accommodationId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return ApiResponse.ok(service.list(id));
    }
}
