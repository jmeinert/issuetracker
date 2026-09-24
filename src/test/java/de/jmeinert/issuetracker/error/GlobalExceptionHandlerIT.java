package de.jmeinert.issuetracker.error;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GlobalExceptionHandlerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void httpRequestMethodNotSupportedExceptionIsHandled() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.message").value("Method Not Allowed"));
    }

    @Test
    void httpMediaTypeNotSupportedExceptionIsHandled() throws Exception {
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.TEXT_PLAIN))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.message").value("Unsupported Media Type"));
    }

    @Test
    void noResourceFoundExceptionIsHandled() throws Exception {
        mockMvc.perform(get("/scalar/unknown-path"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Not Found"))
            .andExpect(jsonPath("$.errors").isEmpty());
    }
}
