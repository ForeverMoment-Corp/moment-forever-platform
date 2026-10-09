package com.forvmom.data.dao;

import com.forvmom.data.entities.Experience;

import java.util.List;

public interface ExperienceDao extends GenericDao<Experience, Long> {

    boolean existsBySlug(String slug);

    Experience findBySlug(String slug);

    Experience findByIdWithDetail(Long id);

    Experience findByIdWithPolicies(Long id);

    Experience findBySlugWithDetail(String slug);

    Experience findBySlugWithPolicies(String slug);

    /**
     * Query 3 of 3: loads locationMappers + their timeslot mappers (separate to
     * avoid Cartesian product)
     */
    Experience findByIdWithLocations(Long id);

    Experience findBySlugWithLocations(String slug);

    List<Experience> findAllWithDetail();

    List<Experience> findBySubCategoryId(Long subCategoryId);

    List<Experience> findFeatured();

    List<Experience> findAllActive();

    // ---------------------------------------------------------------------------
    // LOCATION + CATEGORY / SUBCATEGORY CATALOG QUERIES (added Oct-2026)
    // See C:\manishshrma\EXPERIENCE-LOCATION-CATEGORY-APIS-PROPOSAL.md
    // All queries return active experiences attached (active mapper) to the
    // given location, ordered by displayOrder. Featured variants add
    // e.isFeatured = true. Empty result = empty list (never null).
    // ---------------------------------------------------------------------------

    List<Experience> findActiveByLocationId(Long locationId);

    List<Experience> findFeaturedByLocationId(Long locationId);

    List<Experience> findActiveByLocationAndCategory(Long locationId, Long categoryId);

    List<Experience> findFeaturedByLocationAndCategory(Long locationId, Long categoryId);

    List<Experience> findActiveByLocationAndSubCategory(Long locationId, Long subCategoryId);

    List<Experience> findFeaturedByLocationAndSubCategory(Long locationId, Long subCategoryId);
}
