package com.preen.backend.rooms;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("local")
@RequestMapping("/api/accommodations")
public class RoomController {
    private final RoomService service;
    public RoomController(RoomService service) { this.service = service; }

    @GetMapping("/{accommodationId}/rooms")
    public RoomService.RoomList list(@PathVariable String accommodationId) {
        long id;
        try {
            if (!accommodationId.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
            id = Long.parseLong(accommodationId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return service.list(id);
    }
}
