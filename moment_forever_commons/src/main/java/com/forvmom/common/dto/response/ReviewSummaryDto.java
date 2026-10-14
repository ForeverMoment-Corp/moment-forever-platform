package com.forvmom.common.dto.response;

/**
 * Aggregate rating of one experience over its approved reviews.
 */
public class ReviewSummaryDto {

    private Long experienceId;
    private double averageRating;
    private long reviewCount;

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(long reviewCount) {
        this.reviewCount = reviewCount;
    }
}
