package com.forvmom.core.services;

import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.dao.ExperienceDetailDao;
import com.forvmom.data.dao.ExperienceLocationMapperDao;
import com.forvmom.data.dao.ExperienceLocationPincodeMapperDao;
import com.forvmom.data.dao.ExperienceMediaMapperDao;
import com.forvmom.data.dao.PincodeDao;
import com.forvmom.data.dao.SubCategoryDao;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.Location;
import com.forvmom.data.entities.Pincode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the location + category / subcategory catalog queries added
 * Oct-2026 (see C:\manishshrma\EXPERIENCE-LOCATION-CATEGORY-APIS-PROPOSAL.md).
 *
 * <p>All DAO/cache collaborators are mocked: each test fixes the cache to a
 * miss (null) unless the cache-hit path itself is under test, and the media
 * mapper DAO returns no rows so card-image enrichment is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class ExperienceLocationCatalogServiceTest {

    private static final Long LOCATION_ID = 3L;
    private static final Long CATEGORY_ID = 11L;
    private static final Long SUB_CATEGORY_ID = 21L;

    @Mock
    private ExperienceDao experienceDao;
    @Mock
    private ExperienceDetailDao experienceDetailDao;
    @Mock
    private SubCategoryDao subCategoryDao;
    @Mock
    private CatalogCacheService catalogCacheService;
    @Mock
    private ExperienceMediaMapperDao experienceMediaMapperDao;
    @Mock
    private ImageUrlConfig imageUrlConfig;
    @Mock
    private ImageFlowCacheService imageFlowCacheService;
    @Mock
    private ExperienceMediaService experienceMediaService;
    @Mock
    private ImageVariantService imageVariantService;
    @Mock
    private PincodeDao pincodeDao;
    @Mock
    private ExperienceLocationMapperDao experienceLocationMapperDao;
    @Mock
    private ExperienceLocationPincodeMapperDao experienceLocationPincodeMapperDao;

    @InjectMocks
    private ExperienceServiceImpl service;

    @Test
    void getByLocation_cacheMiss_returnsHighlightsAndWarmsCache() {
        when(imageFlowCacheService.getExperienceListByLocation(LOCATION_ID)).thenReturn(null);
        when(experienceDao.findActiveByLocationId(LOCATION_ID))
                .thenReturn(Arrays.asList(experience(1L), experience(2L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result = service.getByLocation(LOCATION_ID);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(imageFlowCacheService).putExperienceListByLocation(eq(LOCATION_ID), anyList());
    }

    @Test
    void getByLocation_cacheHit_skipsDao() {
        ExperienceHighlightResponseDto cached = new ExperienceHighlightResponseDto();
        cached.setId(9L);
        when(imageFlowCacheService.getExperienceListByLocation(LOCATION_ID))
                .thenReturn(Collections.singletonList(cached));

        List<ExperienceHighlightResponseDto> result = service.getByLocation(LOCATION_ID);

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getId());
        verify(experienceDao, never()).findActiveByLocationId(LOCATION_ID);
    }

    @Test
    void getByLocation_emptyDao_returnsEmptyListWithoutWarmingCache() {
        when(imageFlowCacheService.getExperienceListByLocation(LOCATION_ID)).thenReturn(null);
        when(experienceDao.findActiveByLocationId(LOCATION_ID))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result = service.getByLocation(LOCATION_ID);

        assertTrue(result.isEmpty());
        verify(imageFlowCacheService, never()).putExperienceListByLocation(eq(LOCATION_ID), anyList());
    }

    @Test
    void getByLocationAndCategory_cacheMiss_delegatesToDao() {
        when(imageFlowCacheService.getExperienceListByLocationAndCategory(LOCATION_ID, CATEGORY_ID))
                .thenReturn(null);
        when(experienceDao.findActiveByLocationAndCategory(LOCATION_ID, CATEGORY_ID))
                .thenReturn(Collections.singletonList(experience(1L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result =
                service.getByLocationAndCategory(LOCATION_ID, CATEGORY_ID);

        assertEquals(1, result.size());
        verify(imageFlowCacheService)
                .putExperienceListByLocationAndCategory(eq(LOCATION_ID), eq(CATEGORY_ID), anyList());
    }

    @Test
    void getFeaturedByLocationAndCategory_cacheMiss_delegatesToDao() {
        when(imageFlowCacheService.getExperienceListFeaturedByLocationAndCategory(LOCATION_ID, CATEGORY_ID))
                .thenReturn(null);
        when(experienceDao.findFeaturedByLocationAndCategory(LOCATION_ID, CATEGORY_ID))
                .thenReturn(Collections.singletonList(experience(1L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result =
                service.getFeaturedByLocationAndCategory(LOCATION_ID, CATEGORY_ID);

        assertEquals(1, result.size());
        verify(imageFlowCacheService)
                .putExperienceListFeaturedByLocationAndCategory(eq(LOCATION_ID), eq(CATEGORY_ID), anyList());
    }

    @Test
    void getByLocationAndSubCategory_cacheMiss_delegatesToDao() {
        when(imageFlowCacheService.getExperienceListByLocationAndSubCategory(LOCATION_ID, SUB_CATEGORY_ID))
                .thenReturn(null);
        when(experienceDao.findActiveByLocationAndSubCategory(LOCATION_ID, SUB_CATEGORY_ID))
                .thenReturn(Collections.singletonList(experience(2L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result =
                service.getByLocationAndSubCategory(LOCATION_ID, SUB_CATEGORY_ID);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        verify(imageFlowCacheService)
                .putExperienceListByLocationAndSubCategory(eq(LOCATION_ID), eq(SUB_CATEGORY_ID), anyList());
    }

    @Test
    void getFeaturedByLocationAndSubCategory_cacheMiss_delegatesToDao() {
        when(imageFlowCacheService.getExperienceListFeaturedByLocationAndSubCategory(LOCATION_ID, SUB_CATEGORY_ID))
                .thenReturn(null);
        when(experienceDao.findFeaturedByLocationAndSubCategory(LOCATION_ID, SUB_CATEGORY_ID))
                .thenReturn(Collections.singletonList(experience(2L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result =
                service.getFeaturedByLocationAndSubCategory(LOCATION_ID, SUB_CATEGORY_ID);

        assertEquals(1, result.size());
        verify(imageFlowCacheService)
                .putExperienceListFeaturedByLocationAndSubCategory(eq(LOCATION_ID), eq(SUB_CATEGORY_ID), anyList());
    }

    @Test
    void getFeaturedByLocation_cacheMiss_delegatesToDao() {
        when(imageFlowCacheService.getExperienceListFeaturedByLocation(LOCATION_ID)).thenReturn(null);
        when(experienceDao.findFeaturedByLocationId(LOCATION_ID))
                .thenReturn(Collections.singletonList(experience(1L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        List<ExperienceHighlightResponseDto> result = service.getFeaturedByLocation(LOCATION_ID);

        assertEquals(1, result.size());
        verify(imageFlowCacheService).putExperienceListFeaturedByLocation(eq(LOCATION_ID), anyList());
    }

    @Test
    void getByLocation_unknownPincode_throws404() {
        when(imageFlowCacheService.getExperienceListByLocation(LOCATION_ID)).thenReturn(null);
        when(experienceDao.findActiveByLocationId(LOCATION_ID))
                .thenReturn(Collections.singletonList(experience(1L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());
        when(pincodeDao.findByPincodeCode("999999")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByLocation(LOCATION_ID, "999999"));
    }

    @Test
    void getByLocationAndCategory_knownPincode_filtersToServiceable() {
        when(imageFlowCacheService.getExperienceListByLocationAndCategory(LOCATION_ID, CATEGORY_ID))
                .thenReturn(null);
        when(experienceDao.findActiveByLocationAndCategory(LOCATION_ID, CATEGORY_ID))
                .thenReturn(Arrays.asList(experience(1L), experience(2L)));
        when(experienceMediaMapperDao.findActiveByExperienceIdsOrdered(anyList()))
                .thenReturn(Collections.emptyList());

        Location location = new Location();
        location.setId(LOCATION_ID);
        Pincode pincode = new Pincode();
        pincode.setId(7L);
        pincode.setPincodeCode("411001");
        pincode.setLocation(location);
        when(pincodeDao.findByPincodeCode("411001")).thenReturn(pincode);
        when(experienceLocationPincodeMapperDao.findServiceableExperienceIds(LOCATION_ID, 7L))
                .thenReturn(Collections.singletonList(1L));

        List<ExperienceHighlightResponseDto> result =
                service.getByLocationAndCategory(LOCATION_ID, CATEGORY_ID, "411001");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    private static Experience experience(Long id) {
        Experience e = new Experience();
        e.setId(id);
        e.setName("Experience " + id);
        e.setSlug("experience-" + id);
        e.setActive(true);
        e.setDisplayOrder(0);
        e.setIsFeatured(false);
        return e;
    }
}
