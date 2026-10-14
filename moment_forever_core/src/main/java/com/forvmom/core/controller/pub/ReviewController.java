package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.response.ReviewResponseDto;
import com.forvmom.common.dto.response.ReviewSummaryDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public endpoints for approved experience reviews.
 *
 * <p>
 * Mapped under {@code /public/reviews}; everything here serves only
 * {@code approved=true} rows and never exposes reviewer emails.
 */
@RestController
@RequestMapping("/public/reviews")
@Tag(name = "Public Review API", description = "Endpoints for approved experience reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * Global approved-review feed for surfaces like the homepage.
     *
     * @param limit max rows, newest first (default 20, capped at 100)
     */
    @GetMapping
    @Operation(summary = "Get Recent Reviews", description = "Approved reviews across all experiences, newest first")
    public ResponseEntity<ApiResponse<?>> getRecent(
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<ReviewResponseDto> response = reviewService.getApprovedGlobal(limit);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Approved reviews of one experience, newest first.
     *
     * @param experienceId identifier of the experience
     */
    @GetMapping("/experience/{experienceId}")
    @Operation(summary = "Get Experience Reviews", description = "Approved reviews of one experience")
    public ResponseEntity<ApiResponse<?>> getByExperience(@PathVariable Long experienceId) {
        List<ReviewResponseDto> response = reviewService.getApprovedByExperienceId(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Average rating and approved-review count of one experience.
     *
     * @param experienceId identifier of the experience
     */
    @GetMapping("/experience/{experienceId}/summary")
    @Operation(summary = "Get Experience Rating Summary", description = "Average rating and review count")
    public ResponseEntity<ApiResponse<?>> getSummary(@PathVariable Long experienceId) {
        ReviewSummaryDto response = reviewService.getSummary(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }
}
