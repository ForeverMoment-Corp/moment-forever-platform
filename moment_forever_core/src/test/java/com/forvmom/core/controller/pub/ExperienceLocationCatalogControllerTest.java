package com.forvmom.core.controller.pub;

import com.forvmom.core.services.ExperienceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route tests for the location + category / subcategory experience endpoints
 * added Oct-2026 (see
 * C:\manishshrma\EXPERIENCE-LOCATION-CATEGORY-APIS-PROPOSAL.md).
 *
 * <p>Verifies each new route resolves (no clash with {@code /{id}},
 * {@code /featured} or {@code /slug/{slug}}) and delegates to the matching
 * {@link ExperienceService} method with path variables forwarded.
 */
class ExperienceLocationCatalogControllerTest {

    private ExperienceService experienceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        experienceService = mock(ExperienceService.class);
        ExperienceController controller = new ExperienceController();
        ReflectionTestUtils.setField(controller, "experienceService", experienceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByLocation_resolves() throws Exception {
        when(experienceService.getByLocation(eq(3L), isNull())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/public/experiences/location/3"))
                .andExpect(status().isOk());

        verify(experienceService).getByLocation(eq(3L), isNull());
    }

    @Test
    void getFeaturedByLocation_resolves() throws Exception {
        when(experienceService.getFeaturedByLocation(eq(3L), isNull())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/public/experiences/location/3/featured"))
                .andExpect(status().isOk());

        verify(experienceService).getFeaturedByLocation(eq(3L), isNull());
    }

    @Test
    void getByLocationAndCategory_resolvesWithPincode() throws Exception {
        when(experienceService.getByLocationAndCategory(eq(3L), eq(11L), eq("411001")))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/public/experiences/location/3/category/11").param("pincode", "411001"))
                .andExpect(status().isOk());

        verify(experienceService).getByLocationAndCategory(eq(3L), eq(11L), eq("411001"));
    }

    @Test
    void getFeaturedByLocationAndCategory_resolves() throws Exception {
        when(experienceService.getFeaturedByLocationAndCategory(eq(3L), eq(11L), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/public/experiences/location/3/category/11/featured"))
                .andExpect(status().isOk());

        verify(experienceService).getFeaturedByLocationAndCategory(eq(3L), eq(11L), isNull());
    }

    @Test
    void getByLocationAndSubCategory_resolves() throws Exception {
        when(experienceService.getByLocationAndSubCategory(eq(3L), eq(21L), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/public/experiences/location/3/subcategory/21"))
                .andExpect(status().isOk());

        verify(experienceService).getByLocationAndSubCategory(eq(3L), eq(21L), isNull());
    }

    @Test
    void getFeaturedByLocationAndSubCategory_resolves() throws Exception {
        when(experienceService.getFeaturedByLocationAndSubCategory(eq(3L), eq(21L), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/public/experiences/location/3/subcategory/21/featured"))
                .andExpect(status().isOk());

        verify(experienceService).getFeaturedByLocationAndSubCategory(eq(3L), eq(21L), isNull());
    }
}
