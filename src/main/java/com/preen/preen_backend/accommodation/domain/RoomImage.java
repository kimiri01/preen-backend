package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder;

    public static RoomImage of(Long roomId, String imageUrl, int sortOrder) {
        RoomImage image = new RoomImage();
        image.roomId = roomId;
        image.imageUrl = imageUrl;
        image.sortOrder = sortOrder;
        return image;
    }
}
