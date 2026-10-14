package com.forvmom.core.services;

import com.forvmom.common.dto.request.ReviewAdminRequestDto;
import com.forvmom.common.dto.request.ReviewSubmitRequestDto;
import com.forvmom.common.dto.response.ReviewResponseDto;
import com.forvmom.common.dto.response.ReviewSummaryDto;
import com.forvmom.common.errorhandler.ConflictException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.ReviewBeanMapper;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.dao.ReviewDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed implementation of {@link ReviewService}.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int GLOBAL_FEED_DEFAULT_LIMIT = 20;
    private static final int GLOBAL_FEED_MAX_LIMIT = 100;

    @Autowired
    private ReviewDao reviewDao;

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private ApplicationUserDao applicationUserDao;

    @Override
    @Transactional
    public ReviewResponseDto submitReview(ReviewSubmitRequestDto requestDto, Long userId) {
        ApplicationUser user = resolveApplicationUser(userId);
        Experience experience = resolveExperience(requestDto.getExperienceId());

        if (reviewDao.existsByExperienceIdAndUserId(experience.getId(), user.getId())) {
            throw new ConflictException(
                    "You have already reviewed this experience");
        }

        Review entity = new Review();
        entity.setExperience(experience);
        entity.setApplicationUser(user);
        entity.setRating(requestDto.getRating());
        entity.setTitle(requestDto.getTitle());
        entity.setComment(requestDto.getComment());
        entity.setApproved(false);

        return ReviewBeanMapper.mapEntityToDto(reviewDao.save(entity));
    }

    @Override
    @Transactional
    public ReviewResponseDto createReview(ReviewAdminRequestDto requestDto) {
        Experience experience = resolveExperience(requestDto.getExperienceId());

        Review entity = new Review();
        entity.setExperience(experience);
        entity.setRating(requestDto.getRating());
        entity.setTitle(requestDto.getTitle());
        entity.setComment(requestDto.getComment());
        entity.setReviewerName(requestDto.getReviewerName());
        entity.setReviewerEmail(requestDto.getReviewerEmail());
        entity.setApproved(true);

        return ReviewBeanMapper.mapEntityToDto(reviewDao.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllForAdmin(Long experienceId, Boolean approved) {
        return ReviewBeanMapper.mapEntitiesToDtos(reviewDao.findAllForAdmin(experienceId, approved));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getById(Long id) {
        return ReviewBeanMapper.mapEntityToDto(resolveReview(id));
    }

    @Override
    @Transactional
    public ReviewResponseDto approve(Long id) {
        Review existing = resolveReview(id);
        existing.setApproved(true);
        return ReviewBeanMapper.mapEntityToDto(reviewDao.update(existing));
    }

    @Override
    @Transactional
    public ReviewResponseDto reject(Long id) {
        Review existing = resolveReview(id);
        existing.setApproved(false);
        return ReviewBeanMapper.mapEntityToDto(reviewDao.update(existing));
    }

    @Override
    @Transactional
    public ReviewResponseDto updateReview(Long id, ReviewAdminRequestDto requestDto) {
        Review existing = resolveReview(id);
        existing.setRating(requestDto.getRating());
        existing.setTitle(requestDto.getTitle());
        existing.setComment(requestDto.getComment());
        existing.setReviewerName(requestDto.getReviewerName());
        existing.setReviewerEmail(requestDto.getReviewerEmail());
        return ReviewBeanMapper.mapEntityToDto(reviewDao.update(existing));
    }

    @Override
    @Transactional
    public boolean deleteReview(Long id) {
        Review existing = resolveReview(id);
        reviewDao.delete(existing);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getMyReviews(Long userId) {
        ApplicationUser user = resolveApplicationUser(userId);
        return ReviewBeanMapper.mapEntitiesToDtos(reviewDao.findByApplicationUserId(user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getApprovedByExperienceId(Long experienceId) {
        resolveExperience(experienceId);
        return ReviewBeanMapper.mapEntitiesToDtos(reviewDao.findApprovedByExperienceId(experienceId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getApprovedGlobal(int limit) {
        int effectiveLimit = limit <= 0 ? GLOBAL_FEED_DEFAULT_LIMIT : Math.min(limit, GLOBAL_FEED_MAX_LIMIT);
        return ReviewBeanMapper.mapEntitiesToDtos(reviewDao.findApprovedGlobal(effectiveLimit));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryDto getSummary(Long experienceId) {
        resolveExperience(experienceId);
        ReviewSummaryDto summary = new ReviewSummaryDto();
        summary.setExperienceId(experienceId);
        summary.setAverageRating(reviewDao.avgRatingByExperienceId(experienceId));
        summary.setReviewCount(reviewDao.countApprovedByExperienceId(experienceId));
        return summary;
    }

    private Review resolveReview(Long id) {
        Review existing = reviewDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Review not found: " + id);
        }
        return existing;
    }

    private Experience resolveExperience(Long experienceId) {
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null) {
            throw new ResourceNotFoundException("Experience not found: " + experienceId);
        }
        return experience;
    }

    /**
     * The gateway {@code X-User-Id} header may carry either the
     * application-user id or the auth-user id — same lenient resolution as
     * {@code SupportQueryServiceImpl}.
     */
    private ApplicationUser resolveApplicationUser(Long userId) {
        ApplicationUser userByAppId = applicationUserDao.findById(userId);
        if (userByAppId != null) {
            return userByAppId;
        }
        return applicationUserDao.findByAuthUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
