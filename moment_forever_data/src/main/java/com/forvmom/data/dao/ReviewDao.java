package com.forvmom.data.dao;

import com.forvmom.data.entities.Review;

import java.util.List;

public interface ReviewDao extends GenericDao<Review, Long> {

    /** Approved reviews of one experience, newest first (public listing). */
    List<Review> findApprovedByExperienceId(Long experienceId);

    /** Approved reviews across all experiences, newest first (homepage feed). */
    List<Review> findApprovedGlobal(int limit);

    /**
     * Admin listing with optional filters; both filters {@code null} returns
     * everything newest first.
     */
    List<Review> findAllForAdmin(Long experienceId, Boolean approved);

    /** One user's own reviews, newest first. */
    List<Review> findByApplicationUserId(Long applicationUserId);

    /** Duplicate guard: one review per user per experience. */
    boolean existsByExperienceIdAndUserId(Long experienceId, Long applicationUserId);

    long countApprovedByExperienceId(Long experienceId);

    /** Average of approved ratings; {@code 0.0} when no approved reviews. */
    double avgRatingByExperienceId(Long experienceId);
}
