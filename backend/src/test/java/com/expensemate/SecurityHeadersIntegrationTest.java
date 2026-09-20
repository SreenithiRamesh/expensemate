package com.expensemate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldContainDefaultSecurityHeaders()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/health")
                )
                .andExpect(status().isOk())

                /*
                 * Spring Security default:
                 * prevents MIME-type sniffing.
                 */
                .andExpect(
                        header().string(
                                "X-Content-Type-Options",
                                "nosniff"
                        )
                )

                /*
                 * Spring Security default:
                 * prevents the response from being framed.
                 */
                .andExpect(
                        header().string(
                                "X-Frame-Options",
                                "DENY"
                        )
                )

                /*
                 * Sensitive API responses should not
                 * be cached by browsers/intermediaries.
                 */
                .andExpect(
                        header().string(
                                "Cache-Control",
                                "no-cache, no-store, max-age=0, must-revalidate"
                        )
                );
    }

    @Test
    void healthEndpointShouldContainExpenseMateSecurityPolicies()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/health")
                )
                .andExpect(status().isOk())

                .andExpect(
                        header().string(
                                "Referrer-Policy",
                                "strict-origin-when-cross-origin"
                        )
                )

                .andExpect(
                        header().string(
                                "Permissions-Policy",
                                "camera=(), microphone=(), geolocation=()"
                        )
                );
    }

    @Test
    void protectedEndpointShouldContainSecurityHeadersEvenWhenUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/expenses")
                )
                .andExpect(status().isUnauthorized())

                .andExpect(
                        header().string(
                                "X-Content-Type-Options",
                                "nosniff"
                        )
                )

                .andExpect(
                        header().string(
                                "X-Frame-Options",
                                "DENY"
                        )
                )

                .andExpect(
                        header().string(
                                "Referrer-Policy",
                                "strict-origin-when-cross-origin"
                        )
                )

                .andExpect(
                        header().string(
                                "Permissions-Policy",
                                "camera=(), microphone=(), geolocation=()"
                        )
                );
    }

    @Test
    void hstsShouldNotBeWrittenForPlainHttpRequest()
            throws Exception {

        /*
         * HSTS is meaningful only over HTTPS.
         * Spring Security should therefore not send it
         * for an ordinary HTTP request.
         */
        mockMvc.perform(
                        get("/api/v1/health")
                                .secure(false)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().doesNotExist(
                                "Strict-Transport-Security"
                        )
                );
    }

    @Test
    void hstsShouldBeWrittenForHttpsRequest()
            throws Exception {

        /*
         * Simulate HTTPS and verify Spring Security's
         * built-in HSTS protection.
         */
        mockMvc.perform(
                        get("/api/v1/health")
                                .secure(true)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().exists(
                                "Strict-Transport-Security"
                        )
                );
    }
}