package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accommodation_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccommodationImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accommodationId;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder;

    public static AccommodationImage of(Long accommodationId, String imageUrl, int sortOrder) {
        AccommodationImage image = new AccommodationImage();
        image.accommodationId = accommodationId;
        image.imageUrl = imageUrl;
        image.sortOrder = sortOrder;
        return image;
    }
}
