package com.forvmom.data.dao;

import com.forvmom.data.entities.Review;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ReviewDaoImpl extends GenericDaoImpl<Review, Long> implements ReviewDao {

    public ReviewDaoImpl() {
        super(Review.class);
    }

    @Override
    public List<Review> findApprovedByExperienceId(Long experienceId) {
        return em.createQuery(
                "SELECT r FROM Review r WHERE r.deleted = false "
                        + "AND r.approved = true AND r.experience.id = :experienceId "
                        + "ORDER BY r.createdOn DESC",
                Review.class)
                .setParameter("experienceId", experienceId)
                .getResultList();
    }

    @Override
    public List<Review> findApprovedGlobal(int limit) {
        return em.createQuery(
                "SELECT r FROM Review r WHERE r.deleted = false "
                        + "AND r.approved = true "
                        + "ORDER BY r.createdOn DESC",
                Review.class)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Review> findAllForAdmin(Long experienceId, Boolean approved) {
        String jpql = "SELECT r FROM Review r WHERE r.deleted = false"
                + (experienceId != null ? " AND r.experience.id = :experienceId" : "")
                + (approved != null ? " AND r.approved = :approved" : "")
                + " ORDER BY r.createdOn DESC";
        TypedQuery<Review> query = em.createQuery(jpql, Review.class);
        if (experienceId != null) {
            query.setParameter("experienceId", experienceId);
        }
        if (approved != null) {
            query.setParameter("approved", approved);
        }
        return query.getResultList();
    }

    @Override
    public List<Review> findByApplicationUserId(Long applicationUserId) {
        return em.createQuery(
                "SELECT r FROM Review r WHERE r.deleted = false "
                        + "AND r.applicationUser.id = :userId "
                        + "ORDER BY r.createdOn DESC",
                Review.class)
                .setParameter("userId", applicationUserId)
                .getResultList();
    }

    @Override
    public boolean existsByExperienceIdAndUserId(Long experienceId, Long applicationUserId) {
        Long count = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.deleted = false "
                        + "AND r.experience.id = :experienceId "
                        + "AND r.applicationUser.id = :userId",
                Long.class)
                .setParameter("experienceId", experienceId)
                .setParameter("userId", applicationUserId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public long countApprovedByExperienceId(Long experienceId) {
        Long count = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.deleted = false "
                        + "AND r.approved = true AND r.experience.id = :experienceId",
                Long.class)
                .setParameter("experienceId", experienceId)
                .getSingleResult();
        return count != null ? count : 0;
    }

    @Override
    public double avgRatingByExperienceId(Long experienceId) {
        Double avg = em.createQuery(
                "SELECT AVG(r.rating) FROM Review r WHERE r.deleted = false "
                        + "AND r.approved = true AND r.experience.id = :experienceId",
                Double.class)
                .setParameter("experienceId", experienceId)
                .getSingleResult();
        return avg != null ? avg : 0.0;
    }
}
