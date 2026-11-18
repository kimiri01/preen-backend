package com.preen.preen_backend.accommodation.rooms;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomService {
    private final RoomRepository repository;
    public RoomService(RoomRepository repository) { this.repository = repository; }
    public record ImageView(String id, String imageUrl, int sortOrder) {}
    public record BedView(String type, int count) {}
    public record FacilityView(String code, String name) {}
    public record AudioView(String equipmentName, String connectionGuide, String availableFrom, String availableUntil, String volumeGuide, String spotifyTrackId) {}
    public record RoomView(String id, String name, int capacity, int maxCapacity,
                           String nightlyPrice, String extraPersonFee, List<ImageView> images,
                           String roomType, String areaSqm, List<BedView> beds, List<FacilityView> facilities, AudioView audio) {}
    public record RoomList(String accommodationId, String accommodationName, String description, String address, String checkInTime, String checkOutTime, List<RoomView> rooms, List<ImageView> images, String categoryCode, String categoryName, String regionCode, String regionName) {}

    @Transactional(readOnly = true)
    public RoomList list(long stayId) {
        var stay = repository.findListedStay(stayId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var rooms = repository.findActiveRooms(stayId);
        var images = repository.findActiveRoomImages(stayId).stream()
            .collect(Collectors.groupingBy(RoomRepository.Image::roomId));
        var beds = repository.findBeds(stayId).stream().collect(Collectors.groupingBy(RoomRepository.Bed::roomId));
        var facilities = repository.findFacilities(stayId).stream().collect(Collectors.groupingBy(RoomRepository.Facility::roomId));
        var audio = repository.findAudio(stayId).stream().collect(Collectors.toMap(RoomRepository.Audio::roomId,
            item -> new AudioView(item.equipmentName(), item.connectionGuide(), item.availableFrom(), item.availableUntil(), item.volumeGuide(), item.spotifyTrackId())));
        return new RoomList(Long.toString(stay.id()), stay.name(), stay.description(), stay.address(), stay.checkInTime(), stay.checkOutTime(), rooms.stream().map(room ->
            new RoomView(Long.toString(room.id()), room.name(), room.capacity(), room.maxCapacity(),
                room.nightlyPrice().toPlainString(), room.extraPersonFee().toPlainString(),
                images.getOrDefault(room.id(), List.of()).stream().map(image ->
                    new ImageView(Long.toString(image.id()), image.imageUrl(), image.sortOrder())).toList(),
                room.roomType(), room.areaSqm() == null ? null : room.areaSqm().toPlainString(),
                beds.getOrDefault(room.id(), List.of()).stream().map(bed -> new BedView(bed.bedType(),bed.count())).toList(),
                facilities.getOrDefault(room.id(), List.of()).stream().map(facility -> new FacilityView(facility.code(),facility.name())).toList(), audio.get(room.id()))
        ).toList(),repository.findStayImages(stayId).stream().map(image -> new ImageView(Long.toString(image.id()),image.imageUrl(),image.sortOrder())).toList(), stay.categoryCode(), stay.categoryName(), stay.regionCode(), stay.regionName());
    }
}
