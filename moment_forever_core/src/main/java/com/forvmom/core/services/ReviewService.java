package com.forvmom.core.services;

import com.forvmom.common.dto.request.ReviewAdminRequestDto;
import com.forvmom.common.dto.request.ReviewSubmitRequestDto;
import com.forvmom.common.dto.response.ReviewResponseDto;
import com.forvmom.common.dto.response.ReviewSummaryDto;

import java.util.List;

/**
 * Contract for experience reviews: user submissions, admin seeding and
 * moderation, and public approved-only reads.
 *
 * <p>
 * User reviews enter moderation ({@code approved=false}); admin-created
 * reviews go live immediately. Only approved rows are served publicly.
 */
public interface ReviewService {

    /**
     * Submits a review for the caller's own account; held for moderation.
     *
     * @param requestDto the rating (comment/title optional)
     * @param userId     gateway user id (application-user or auth-user id)
     * @return the stored, unapproved review
     * @throws com.forvmom.common.errorhandler.ResourceNotFoundException if the
     *         experience or user does not exist
     * @throws com.forvmom.common.errorhandler.ConflictException         if this
     *         user already reviewed the experience
     */
    ReviewResponseDto submitReview(ReviewSubmitRequestDto requestDto, Long userId);

    /**
     * Creates a review as admin (seed/showcase); live immediately.
     *
     * @param requestDto experience, rating and display name
     * @return the stored, approved review
     */
    ReviewResponseDto createReview(ReviewAdminRequestDto requestDto);

    /**
     * Admin listing with optional filters.
     *
     * @param experienceId optional experience filter
     * @param approved     optional approval-state filter
     */
    List<ReviewResponseDto> getAllForAdmin(Long experienceId, Boolean approved);

    ReviewResponseDto getById(Long id);

    /** Approves a review so it goes public. */
    ReviewResponseDto approve(Long id);

    /** Un-approves a review so it is hidden but kept. */
    ReviewResponseDto reject(Long id);

    /**
     * Edits content fields of a review; approval state is left untouched —
     * use {@link #approve(Long)} / {@link #reject(Long)} for that.
     */
    ReviewResponseDto updateReview(Long id, ReviewAdminRequestDto requestDto);

    /** Soft-deletes a review. */
    boolean deleteReview(Long id);

    /** The caller's own reviews, any state. */
    List<ReviewResponseDto> getMyReviews(Long userId);

    /** Approved reviews of one experience, newest first. */
    List<ReviewResponseDto> getApprovedByExperienceId(Long experienceId);

    /** Approved reviews across all experiences, newest first (homepage feed). */
    List<ReviewResponseDto> getApprovedGlobal(int limit);

    /** Average rating + approved count of one experience. */
    ReviewSummaryDto getSummary(Long experienceId);
}
