package com.forvmom.data.dao;

import com.forvmom.data.entities.Experience;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ExperienceDaoImpl extends GenericDaoImpl<Experience, Long> implements ExperienceDao {

    public ExperienceDaoImpl() {
        super(Experience.class);
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = em.createQuery(
                "SELECT COUNT(e) FROM Experience e WHERE e.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Experience findBySlug(String slug) {
        try {
            return em.createQuery(
                    "FROM Experience e WHERE e.slug = :slug", Experience.class)
                    .setParameter("slug", slug)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Experience findByIdWithDetail(Long id) {
        try {
            // Query 1 of 2: Experience + detail + subCategory + inclusionMappers
            return em.createQuery(
                    "SELECT DISTINCT e FROM Experience e " +
                            "LEFT JOIN FETCH e.detail " +
                            "LEFT JOIN FETCH e.subCategory " +
                            "LEFT JOIN FETCH e.inclusionMappers im " +
                            "LEFT JOIN FETCH im.inclusion " +
                            "WHERE e.id = :id",
                    Experience.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Query 2 of 2 for detail fetch: loads policyMappers onto an already-loaded
     * Experience (Hibernate 2nd-level cache / same session merges the result).
     */
    @Override
    public Experience findByIdWithPolicies(Long id) {
        try {
            return em.createQuery(
                    "SELECT DISTINCT e FROM Experience e " +
                            "LEFT JOIN FETCH e.policyMappers pm " +
                            "LEFT JOIN FETCH pm.policy " +
                            "WHERE e.id = :id",
                    Experience.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Experience findBySlugWithDetail(String slug) {
        try {
            // Query 1 of 2: Experience + detail + subCategory + inclusionMappers
            return em.createQuery(
                    "SELECT DISTINCT e FROM Experience e " +
                            "LEFT JOIN FETCH e.detail " +
                            "LEFT JOIN FETCH e.subCategory " +
                            "LEFT JOIN FETCH e.inclusionMappers im " +
                            "LEFT JOIN FETCH im.inclusion " +
                            "WHERE e.slug = :slug",
                    Experience.class)
                    .setParameter("slug", slug)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    /** Query 2 of 2 for detail fetch: loads policyMappers for a given slug. */
    @Override
    public Experience findBySlugWithPolicies(String slug) {
        try {
            return em.createQuery(
                    "SELECT DISTINCT e FROM Experience e " +
                            "LEFT JOIN FETCH e.policyMappers pm " +
                            "LEFT JOIN FETCH pm.policy " +
                            "WHERE e.slug = :slug",
                    Experience.class)
                    .setParameter("slug", slug)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Query 3 of 3 for the detail fetch.
     * Loads locationMappers + location master data + timeslot mappers + timeslot
     * master.
     * Kept separate from queries 1 & 2 to avoid Cartesian product explosion.
     */
    @Override
    public Experience findByIdWithLocations(Long id) {
        try {
            return em.createQuery(
                    "SELECT DISTINCT e FROM Experience e " +
                            "LEFT JOIN FETCH e.locationMappers lm " +
                            "LEFT JOIN FETCH lm.location l " +
                            "LEFT JOIN FETCH lm.timeSlotMappers tsm " +
                            "LEFT JOIN FETCH tsm.timeSlot ts " +
                            "WHERE e.id = :id",
                    Experience.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Experience findBySlugWithLocations(String slug) {
        try {
            return em.createQuery(
                    "SELECT DISTINCT e FROM Experience e " +
                            "LEFT JOIN FETCH e.locationMappers lm " +
                            "LEFT JOIN FETCH lm.location l " +
                            "LEFT JOIN FETCH lm.timeSlotMappers tsm " +
                            "LEFT JOIN FETCH tsm.timeSlot ts " +
                            "WHERE e.slug = :slug",
                    Experience.class)
                    .setParameter("slug", slug)
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public List<Experience> findAllWithDetail() {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .getResultList();
    }

    @Override
    public List<Experience> findBySubCategoryId(Long subCategoryId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "LEFT JOIN FETCH e.detail " +
                        "WHERE e.subCategory.id = :subCategoryId " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("subCategoryId", subCategoryId)
                .getResultList();
    }

    @Override
    public List<Experience> findFeatured() {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE e.isFeatured = true AND e.isActive = true " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .getResultList();
    }

    @Override
    public List<Experience> findAllActive() {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory " +
                        "WHERE e.isActive = true " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .getResultList();
    }

    // ---------------------------------------------------------------------------
    // LOCATION + CATEGORY / SUBCATEGORY CATALOG QUERIES (added Oct-2026)
    // See C:\manishshrma\EXPERIENCE-LOCATION-CATEGORY-APIS-PROPOSAL.md
    // Pattern mirrors findAllActive/findFeatured: JOIN the locationMappers
    // junction (mapper must be active), filter on the experience side, fetch
    // detail + subCategory (+ parent category) to avoid N+1 on card mapping.
    // ---------------------------------------------------------------------------

    @Override
    public List<Experience> findActiveByLocationId(Long locationId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "JOIN e.locationMappers lm " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE lm.location.id = :locationId " +
                        "AND lm.isActive = true " +
                        "AND e.isActive = true " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("locationId", locationId)
                .getResultList();
    }

    @Override
    public List<Experience> findFeaturedByLocationId(Long locationId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "JOIN e.locationMappers lm " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE lm.location.id = :locationId " +
                        "AND lm.isActive = true " +
                        "AND e.isActive = true " +
                        "AND e.isFeatured = true " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("locationId", locationId)
                .getResultList();
    }

    @Override
    public List<Experience> findActiveByLocationAndCategory(Long locationId, Long categoryId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "JOIN e.locationMappers lm " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE lm.location.id = :locationId " +
                        "AND lm.isActive = true " +
                        "AND e.isActive = true " +
                        "AND sc.category.id = :categoryId " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("locationId", locationId)
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    @Override
    public List<Experience> findFeaturedByLocationAndCategory(Long locationId, Long categoryId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "JOIN e.locationMappers lm " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE lm.location.id = :locationId " +
                        "AND lm.isActive = true " +
                        "AND e.isActive = true " +
                        "AND e.isFeatured = true " +
                        "AND sc.category.id = :categoryId " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("locationId", locationId)
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    @Override
    public List<Experience> findActiveByLocationAndSubCategory(Long locationId, Long subCategoryId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "JOIN e.locationMappers lm " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE lm.location.id = :locationId " +
                        "AND lm.isActive = true " +
                        "AND e.isActive = true " +
                        "AND e.subCategory.id = :subCategoryId " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("locationId", locationId)
                .setParameter("subCategoryId", subCategoryId)
                .getResultList();
    }

    @Override
    public List<Experience> findFeaturedByLocationAndSubCategory(Long locationId, Long subCategoryId) {
        return em.createQuery(
                "SELECT DISTINCT e FROM Experience e " +
                        "JOIN e.locationMappers lm " +
                        "LEFT JOIN FETCH e.detail " +
                        "LEFT JOIN FETCH e.subCategory sc " +
                        "LEFT JOIN FETCH sc.category c " +
                        "WHERE lm.location.id = :locationId " +
                        "AND lm.isActive = true " +
                        "AND e.isActive = true " +
                        "AND e.isFeatured = true " +
                        "AND e.subCategory.id = :subCategoryId " +
                        "ORDER BY e.displayOrder ASC",
                Experience.class)
                .setParameter("locationId", locationId)
                .setParameter("subCategoryId", subCategoryId)
                .getResultList();
    }
}
