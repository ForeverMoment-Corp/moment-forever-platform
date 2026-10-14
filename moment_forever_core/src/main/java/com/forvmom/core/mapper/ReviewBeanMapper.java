package com.forvmom.core.mapper;

import com.forvmom.common.dto.response.ReviewResponseDto;
import com.forvmom.data.entities.Review;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewBeanMapper {

    private ReviewBeanMapper() {
    }

    /**
     * Maps an entity to its public-safe projection.
     *
     * <p>
     * The display name prefers the stored {@code reviewerName} (admin seeds);
     * for user reviews it falls back to the linked profile's full name.
     * Must be called inside a transaction — both associations are lazy.
     */
    public static ReviewResponseDto mapEntityToDto(Review entity) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(entity.getId());
        if (entity.getExperience() != null) {
            dto.setExperienceId(entity.getExperience().getId());
            dto.setExperienceName(entity.getExperience().getName());
        }
        dto.setRating(entity.getRating());
        dto.setTitle(entity.getTitle());
        dto.setComment(entity.getComment());
        dto.setReviewerName(resolveReviewerName(entity));
        dto.setHelpfulCount(entity.getHelpfulCount());
        dto.setApproved(entity.getApproved());
        dto.setCreatedOn(entity.getCreatedOn());
        return dto;
    }

    public static List<ReviewResponseDto> mapEntitiesToDtos(List<Review> entities) {
        return entities.stream().map(ReviewBeanMapper::mapEntityToDto).collect(Collectors.toList());
    }

    private static String resolveReviewerName(Review entity) {
        if (entity.getReviewerName() != null && !entity.getReviewerName().trim().isEmpty()) {
            return entity.getReviewerName();
        }
        if (entity.getApplicationUser() != null && entity.getApplicationUser().getFullName() != null) {
            return entity.getApplicationUser().getFullName();
        }
        return "Guest";
    }
}
