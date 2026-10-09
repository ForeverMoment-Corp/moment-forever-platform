package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.dto.response.ExperienceResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only catalog browsing endpoints for experiences (the bookable decoration
 * packages).
 *
 * <p>
 * Mapped under {@code /public/experiences} and reachable without
 * authentication. List endpoints return the lightweight
 * {@link ExperienceHighlightResponseDto} projection used for cards and
 * carousels, while the single-item lookups (by id or by slug) return the full
 * {@link ExperienceResponseDto} including experience detail.
 */
@RestController
@RequestMapping("/public/experiences")
@Tag(name = "Public Experience API", description = "Endpoints for browsing experiences")
public class ExperienceController {

    @Autowired
    private ExperienceService experienceService;

    /**
     * Lists all active experiences as highlight cards.
     *
     * @param pincode optional pincode to restrict results to experiences
     *                serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All Active Experiences", description = "Optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getAll(@RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getAllActive(pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns one experience with its full detail section.
     *
     * @param id identifier of the experience
     * @return {@code 200 OK} wrapping the {@link ExperienceResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Experience by ID (with full detail)")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable Long id) {
        ExperienceResponseDto response = experienceService.getById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns one experience with its full detail section, addressed by the
     * URL-friendly slug used in storefront links.
     *
     * @param slug unique slug of the experience
     * @return {@code 200 OK} wrapping the {@link ExperienceResponseDto}
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get Experience by Slug (with full detail)")
    public ResponseEntity<ApiResponse<?>> getBySlug(@PathVariable String slug) {
        ExperienceResponseDto response = experienceService.getBySlug(slug);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists the experiences that belong to a sub-category, as highlight cards.
     *
     * @param subCategoryId identifier of the owning sub-category
     * @param pincode       optional pincode to restrict results to experiences
     *                      serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/subcategory/{subCategoryId}")
    @Operation(summary = "Get Experiences by SubCategory", description = "Optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getBySubCategory(@PathVariable Long subCategoryId,
                                                            @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getBySubCategory(subCategoryId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists the experiences flagged as featured, used for homepage banners.
     *
     * @param pincode optional pincode to restrict results to experiences
     *                serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/featured")
    @Operation(summary = "Get Featured Experiences", description = "Returns featured active experiences for homepage banners; optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getFeatured(@RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getFeatured(pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Checks whether an experience is serviceable at a specific pincode.
     *
     * @param id      identifier of the experience
     * @param pincode the pincode to check
     * @return {@code 200 OK} wrapping a boolean serviceability flag
     */
    @GetMapping("/{id}/serviceable")
    @Operation(summary = "Check Experience Serviceability at Pincode")
    public ResponseEntity<ApiResponse<?>> isServiceableAtPincode(@PathVariable Long id,
                                                                   @RequestParam String pincode) {
        boolean serviceable = experienceService.isExperienceServiceableAtPincode(id, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(serviceable, AppConstants.MSG_FETCHED));
    }

    // ---------------------------------------------------------------------------
    // LOCATION + CATEGORY / SUBCATEGORY EXPERIENCE APIS (added Oct-2026)
    // Proposal: C:\manishshrma\EXPERIENCE-LOCATION-CATEGORY-APIS-PROPOSAL.md
    // All endpoints return highlight cards (ExperienceHighlightResponseDto) for
    // the storefront rails. Optional ?pincode= narrows to serviceable items.
    // ---------------------------------------------------------------------------

    /**
     * Lists active experiences attached to a location, as highlight cards.
     *
     * @param locationId identifier of the location
     * @param pincode    optional pincode to restrict results to experiences
     *                   serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get Experiences by Location", description = "Lists active experiences attached to a location; optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getByLocation(@PathVariable Long locationId,
                                                        @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getByLocation(locationId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists featured experiences attached to a location, as highlight cards.
     *
     * @param locationId identifier of the location
     * @param pincode    optional pincode to restrict results to experiences
     *                   serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/location/{locationId}/featured")
    @Operation(summary = "Get Featured Experiences by Location", description = "Returns featured active experiences for a location; optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getFeaturedByLocation(@PathVariable Long locationId,
                                                                @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getFeaturedByLocation(locationId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists active experiences for a location + parent category, as highlight cards.
     *
     * @param locationId identifier of the location
     * @param categoryId identifier of the parent category
     * @param pincode    optional pincode to restrict results to experiences
     *                   serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/location/{locationId}/category/{categoryId}")
    @Operation(summary = "Get Experiences by Location and Category", description = "Optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getByLocationAndCategory(@PathVariable Long locationId,
                                                                   @PathVariable Long categoryId,
                                                                   @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getByLocationAndCategory(locationId, categoryId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists featured experiences for a location + parent category, as highlight cards.
     *
     * @param locationId identifier of the location
     * @param categoryId identifier of the parent category
     * @param pincode    optional pincode to restrict results to experiences
     *                   serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/location/{locationId}/category/{categoryId}/featured")
    @Operation(summary = "Get Featured Experiences by Location and Category", description = "Optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getFeaturedByLocationAndCategory(@PathVariable Long locationId,
                                                                           @PathVariable Long categoryId,
                                                                           @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getFeaturedByLocationAndCategory(locationId, categoryId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists active experiences for a location + sub-category, as highlight cards.
     * Location-scoped twin of {@code GET /subcategory/{subCategoryId}}.
     *
     * @param locationId    identifier of the location
     * @param subCategoryId identifier of the sub-category
     * @param pincode       optional pincode to restrict results to experiences
     *                      serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/location/{locationId}/subcategory/{subCategoryId}")
    @Operation(summary = "Get Experiences by Location and SubCategory", description = "Optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getByLocationAndSubCategory(@PathVariable Long locationId,
                                                                      @PathVariable Long subCategoryId,
                                                                      @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getByLocationAndSubCategory(locationId, subCategoryId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists featured experiences for a location + sub-category, as highlight cards.
     *
     * @param locationId    identifier of the location
     * @param subCategoryId identifier of the sub-category
     * @param pincode       optional pincode to restrict results to experiences
     *                      serviceable there
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/location/{locationId}/subcategory/{subCategoryId}/featured")
    @Operation(summary = "Get Featured Experiences by Location and SubCategory", description = "Optionally pass a pincode to only return experiences serviceable there")
    public ResponseEntity<ApiResponse<?>> getFeaturedByLocationAndSubCategory(@PathVariable Long locationId,
                                                                              @PathVariable Long subCategoryId,
                                                                              @RequestParam(required = false) String pincode) {
        List<ExperienceHighlightResponseDto> response = experienceService.getFeaturedByLocationAndSubCategory(locationId, subCategoryId, pincode);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }
}
