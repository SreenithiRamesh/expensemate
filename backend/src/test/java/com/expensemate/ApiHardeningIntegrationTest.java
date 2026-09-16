package com.expensemate;

import com.expensemate.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldBePublic() throws Exception {

        mockMvc.perform(
                        get("/api/v1/health")
                )
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutJwtShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/expenses")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocsShouldBePublic() throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiShouldBePublic() throws Exception {

        mockMvc.perform(
                        get("/swagger-ui.html")
                )
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void corsPreflightFromAllowedOriginShouldSucceed()
            throws Exception {

        mockMvc.perform(
                        options("/api/v1/expenses")
                                .header(
                                        HttpHeaders.ORIGIN,
                                        "http://localhost:5173"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        "GET"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                        "Authorization,Content-Type"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                "http://localhost:5173"
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                                "GET,POST,PUT,PATCH,DELETE,OPTIONS"
                        )
                );
    }

    @Test
    void corsPreflightFromDisallowedOriginShouldBeRejected()
            throws Exception {

        mockMvc.perform(
                        options("/api/v1/expenses")
                                .header(
                                        HttpHeaders.ORIGIN,
                                        "https://not-allowed.example"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        "GET"
                                )
                )
                .andExpect(status().isForbidden());
    }
}