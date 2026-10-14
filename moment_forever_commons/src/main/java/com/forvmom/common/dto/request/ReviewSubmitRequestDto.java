package com.forvmom.common.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for a logged-in user submitting a review through
 * {@code POST /user/reviews}.
 *
 * <p>
 * Identity comes from the gateway {@code X-User-Id} header, never from this
 * body. Only the rating is mandatory — a user may rate without writing
 * anything.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ReviewSubmitRequestDto {

    @NotNull(message = "Experience id is required")
    private Long experienceId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    private String comment;

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
