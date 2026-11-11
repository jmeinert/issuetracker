package de.jmeinert.issuetracker.openapi;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OpenApiSmokeIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scalarIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/scalar"))
            .andExpect(status().isOk());
    }

    @Test
    void apiDocsAreReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    @Test
    void responsePropertiesAreRequired() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.components.schemas.ProjectResponse.required",
                containsInAnyOrder("id", "name", "description", "createdAt", "updatedAt")
            ))
            .andExpect(jsonPath(
                "$.components.schemas.ErrorResponse.required",
                containsInAnyOrder("message", "errors")
            ));
    }
}
