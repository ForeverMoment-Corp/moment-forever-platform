package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.ReviewAdminRequestDto;
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
 * Admin endpoints for seeding and moderating experience reviews.
 *
 * <p>
 * Mapped under {@code /admin/reviews} and intended for administrators only.
 * Created reviews go live immediately ({@code approved=true}); user
 * submissions arrive unapproved and are published via the approve endpoint.
 */
@RestController
@RequestMapping("/admin/reviews")
@Tag(name = "Admin Review API", description = "Endpoints for seeding and moderating reviews (Admin only)")
public class ReviewControllerAdmin {

    @Autowired
    private ReviewService reviewService;

    /**
     * Creates a review (seed/showcase); live immediately.
     *
     * @param requestDto experience, rating and display name
     * @return {@code 201 CREATED} wrapping the created review
     */
    @PostMapping
    @Operation(summary = "Create Review", description = "Seed a review; goes live immediately")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody ReviewAdminRequestDto requestDto) {
        ReviewResponseDto response = reviewService.createReview(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Lists reviews including unapproved ones, optionally filtered.
     *
     * @param experienceId optional experience filter
     * @param approved     optional approval-state filter
     */
    @GetMapping
    @Operation(summary = "Get All Reviews", description = "Fetch all reviews including unapproved, optionally filtered")
    public ResponseEntity<ApiResponse<?>> getAll(
            @RequestParam(required = false) Long experienceId,
            @RequestParam(required = false) Boolean approved) {
        List<ReviewResponseDto> response = reviewService.getAllForAdmin(experienceId, approved);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns a single review in any state.
     *
     * @param id identifier of the review
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Review by ID", description = "Fetch a single review")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable Long id) {
        ReviewResponseDto response = reviewService.getById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Approves a review so it appears publicly.
     *
     * @param id identifier of the review
     */
    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve Review", description = "Approve a review so it goes public")
    public ResponseEntity<ApiResponse<?>> approve(@PathVariable Long id) {
        ReviewResponseDto response = reviewService.approve(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Un-approves a review so it is hidden but kept.
     *
     * @param id identifier of the review
     */
    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject Review", description = "Hide a review without deleting it")
    public ResponseEntity<ApiResponse<?>> reject(@PathVariable Long id) {
        ReviewResponseDto response = reviewService.reject(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Edits a review's content; approval state is left untouched.
     *
     * @param id         identifier of the review
     * @param requestDto the new content values
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Review", description = "Edit a review's content")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Long id,
            @Valid @RequestBody ReviewAdminRequestDto requestDto) {
        ReviewResponseDto response = reviewService.updateReview(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Soft-deletes a review.
     *
     * @param id identifier of the review
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Review (soft)", description = "Soft-delete a review")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }
}
