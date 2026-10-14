package com.forvmom.core.controller.user;

import com.forvmom.common.dto.request.ReviewSubmitRequestDto;
import com.forvmom.common.dto.response.ReviewResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Logged-in user endpoints for submitting and viewing own experience reviews.
 *
 * <p>
 * Mapped under {@code /user/reviews}. Caller identity is supplied by the API
 * gateway as the {@code X-User-Id} header (same pattern as
 * {@code SupportQueryControllerUser}). Submissions enter moderation and are
 * not public until an admin approves them.
 */
@RestController
@RequestMapping("/user/reviews")
@Tag(name = "User Review API", description = "Endpoints for a logged-in user's own reviews")
public class ReviewControllerUser {

    @Autowired
    private ReviewService reviewService;

    /**
     * Submits a review for the caller's own account; held for moderation.
     * Only the rating is mandatory.
     *
     * @param userId     gateway user id from {@code X-User-Id} header
     * @param requestDto experience id and rating (title/comment optional)
     * @return {@code 201 CREATED} wrapping the stored, unapproved review
     */
    @PostMapping
    @Operation(summary = "Submit Review", description = "Submit a review; visible publicly after admin approval")
    public ResponseEntity<ApiResponse<?>> submit(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReviewSubmitRequestDto requestDto) {
        ReviewResponseDto response = reviewService.submitReview(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Lists the caller's own reviews in any state.
     *
     * @param userId gateway user id from {@code X-User-Id} header
     */
    @GetMapping
    @Operation(summary = "Get My Reviews", description = "Fetch the logged-in user's own reviews")
    public ResponseEntity<ApiResponse<?>> getMine(@RequestHeader("X-User-Id") Long userId) {
        List<ReviewResponseDto> response = reviewService.getMyReviews(userId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }
}
