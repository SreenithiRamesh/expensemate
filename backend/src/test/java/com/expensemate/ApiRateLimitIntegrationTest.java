package com.expensemate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.auth.requests-per-minute=2",
        "app.rate-limit.api.requests-per-minute=2"
})
class ApiRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authEndpointShouldReturn429AfterLimitIsExceeded()
            throws Exception {

        /*
         * Invalid request data deliberately causes validation
         * to return 400 before AuthService accesses the database.
         *
         * The purpose of this test is the HTTP rate limiter,
         * not authentication or database behaviour.
         */
        String body = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(body)
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.1");
                                    return request;
                                })
                )
                .andExpect(
                        status().isBadRequest()
                );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(body)
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.1");
                                    return request;
                                })
                )
                .andExpect(
                        status().isBadRequest()
                );

        /*
         * Third request from the same client exceeds
         * the configured auth limit of 2 requests/minute.
         */
        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType("application/json")
                                        .content(body)
                                        .with(request -> {
                                            request.setRemoteAddr("10.0.0.1");
                                            return request;
                                        })
                        )
                        .andExpect(
                                status().isTooManyRequests()
                        )
                        .andExpect(
                                header().exists(
                                        "Retry-After"
                                )
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value(429)
                        )
                        .andExpect(
                                jsonPath("$.error")
                                        .value(
                                                "Too Many Requests"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.message")
                                        .value(
                                                "Rate limit exceeded. Please try again later."
                                        )
                        )
                        .andReturn();

        assertValidRetryAfterHeader(result);
    }

    @Test
    void differentClientIpsShouldHaveIndependentAuthLimits()
            throws Exception {

        String body = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        /*
         * Consume the full auth allowance for client 10.0.0.2.
         */
        for (int i = 0; i < 2; i++) {

            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType("application/json")
                                    .content(body)
                                    .with(request -> {
                                        request.setRemoteAddr(
                                                "10.0.0.2"
                                        );

                                        return request;
                                    })
                    )
                    .andExpect(
                            status().isBadRequest()
                    );
        }

        /*
         * A different client must have its own
         * independent rate-limit window.
         */
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(body)
                                .with(request -> {
                                    request.setRemoteAddr(
                                            "10.0.0.3"
                                    );

                                    return request;
                                })
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void generalApiShouldReturn429AfterLimitIsExceeded()
            throws Exception {

        /*
         * The endpoint is protected.
         *
         * Without a JWT, the first two requests reach
         * Spring Security and return 401.
         *
         * The rate limiter executes before JWT authentication,
         * so the third request should be rejected with 429.
         */
        mockMvc.perform(
                        get("/api/v1/expenses")
                                .with(request -> {
                                    request.setRemoteAddr(
                                            "10.0.0.4"
                                    );

                                    return request;
                                })
                )
                .andExpect(
                        status().isUnauthorized()
                );

        mockMvc.perform(
                        get("/api/v1/expenses")
                                .with(request -> {
                                    request.setRemoteAddr(
                                            "10.0.0.4"
                                    );

                                    return request;
                                })
                )
                .andExpect(
                        status().isUnauthorized()
                );

        MvcResult result =
                mockMvc.perform(
                                get("/api/v1/expenses")
                                        .with(request -> {
                                            request.setRemoteAddr(
                                                    "10.0.0.4"
                                            );

                                            return request;
                                        })
                        )
                        .andExpect(
                                status().isTooManyRequests()
                        )
                        .andExpect(
                                header().exists(
                                        "Retry-After"
                                )
                        )
                        .andReturn();

        assertValidRetryAfterHeader(result);
    }

    @Test
    void healthEndpointShouldNotConsumeRateLimit()
            throws Exception {

        /*
         * Health is deliberately excluded from
         * application API rate limiting.
         *
         * More than two requests must therefore
         * continue to succeed.
         */
        for (int i = 0; i < 5; i++) {

            mockMvc.perform(
                            get("/api/v1/health")
                                    .with(request -> {
                                        request.setRemoteAddr(
                                                "10.0.0.5"
                                        );

                                        return request;
                                    })
                    )
                    .andExpect(
                            status().isOk()
                    );
        }
    }

    @Test
    void optionsRequestShouldNotConsumeRateLimit()
            throws Exception {

        /*
         * Browser CORS preflight requests must not consume
         * the normal application rate-limit allowance.
         */
        for (int i = 0; i < 5; i++) {

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
                                    .with(request -> {
                                        request.setRemoteAddr(
                                                "10.0.0.6"
                                        );

                                        return request;
                                    })
                    )
                    .andExpect(
                            status().isOk()
                    );
        }
    }

    /*
     * Retry-After is dynamic because the limiter uses
     * fixed one-minute windows.
     *
     * Therefore the correct value is between
     * 1 and 60 seconds rather than always exactly 60.
     */
    private void assertValidRetryAfterHeader(
            MvcResult result
    ) {

        String retryAfter =
                result.getResponse()
                        .getHeader(
                                "Retry-After"
                        );

        assertNotNull(
                retryAfter,
                "Retry-After header must be present"
        );

        long retryAfterSeconds =
                Long.parseLong(
                        retryAfter
                );

        assertTrue(
                retryAfterSeconds >= 1
                        && retryAfterSeconds <= 60,
                "Retry-After must be between 1 and 60 seconds"
        );
    }
}