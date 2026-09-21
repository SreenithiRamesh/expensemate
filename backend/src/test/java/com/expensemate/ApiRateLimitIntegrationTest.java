package com.expensemate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    private static final String PROBLEM_TYPE =
            "https://api.expensemate.com/problems/"
                    + "rate-limit-exceeded";

    private static final String RATE_LIMIT_DETAIL =
            "Rate limit exceeded. Please try again later.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authEndpointShouldReturn429AfterLimitIsExceeded()
            throws Exception {

        /*
         * Invalid data deliberately returns 400 before the
         * authentication service accesses the database.
         *
         * This test validates rate limiting rather than
         * authentication or database behaviour.
         */
        String body = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(body)
                                .with(request -> {
                                    request.setRemoteAddr(
                                            "10.0.0.1"
                                    );

                                    return request;
                                })
                )
                .andExpect(
                        status().isBadRequest()
                );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(body)
                                .with(request -> {
                                    request.setRemoteAddr(
                                            "10.0.0.1"
                                    );

                                    return request;
                                })
                )
                .andExpect(
                        status().isBadRequest()
                );

        /*
         * The third request from the same client exceeds
         * the configured limit of two requests per minute.
         */
        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(body)
                                        .with(request -> {
                                            request.setRemoteAddr(
                                                    "10.0.0.1"
                                            );

                                            return request;
                                        })
                        )
                        .andExpect(
                                status().isTooManyRequests()
                        )
                        .andExpect(
                                content().contentTypeCompatibleWith(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                        )
                        .andExpect(
                                header().exists(
                                        HttpHeaders.RETRY_AFTER
                                )
                        )
                        .andExpect(
                                header().exists(
                                        "X-Correlation-ID"
                                )
                        )
                        .andExpect(
                                jsonPath("$.type")
                                        .value(PROBLEM_TYPE)
                        )
                        .andExpect(
                                jsonPath("$.title")
                                        .value(
                                                "Too many requests"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value(429)
                        )
                        .andExpect(
                                jsonPath("$.detail")
                                        .value(
                                                RATE_LIMIT_DETAIL
                                        )
                        )
                        .andExpect(
                                jsonPath("$.instance")
                                        .value(
                                                "/api/v1/auth/login"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.timestamp")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.correlationId")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.retryAfterSeconds")
                                        .isNumber()
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
         * Consume the full authentication allowance for
         * the first client.
         */
        for (int index = 0;
             index < 2;
             index++) {

            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
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
         * A different client must receive an independent
         * rate-limit window.
         */
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
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
         * The rate limiter executes before authentication,
         * so the third request must return 429.
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
                                content().contentTypeCompatibleWith(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                        )
                        .andExpect(
                                header().exists(
                                        HttpHeaders.RETRY_AFTER
                                )
                        )
                        .andExpect(
                                header().exists(
                                        "X-Correlation-ID"
                                )
                        )
                        .andExpect(
                                jsonPath("$.type")
                                        .value(PROBLEM_TYPE)
                        )
                        .andExpect(
                                jsonPath("$.title")
                                        .value(
                                                "Too many requests"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value(429)
                        )
                        .andExpect(
                                jsonPath("$.detail")
                                        .value(
                                                RATE_LIMIT_DETAIL
                                        )
                        )
                        .andExpect(
                                jsonPath("$.instance")
                                        .value(
                                                "/api/v1/expenses"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.timestamp")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.correlationId")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.retryAfterSeconds")
                                        .isNumber()
                        )
                        .andReturn();

        assertValidRetryAfterHeader(result);
    }

    @Test
    void healthEndpointShouldNotConsumeRateLimit()
            throws Exception {

        /*
         * The public health endpoint is excluded from
         * application API rate limiting.
         */
        for (int index = 0;
             index < 5;
             index++) {

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
         * the application rate-limit allowance.
         */
        for (int index = 0;
             index < 5;
             index++) {

            mockMvc.perform(
                            options("/api/v1/expenses")
                                    .header(
                                            HttpHeaders.ORIGIN,
                                            "http://localhost:5173"
                                    )
                                    .header(
                                            HttpHeaders
                                                    .ACCESS_CONTROL_REQUEST_METHOD,
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
     * Therefore, its valid value is between one and
     * sixty seconds.
     */
    private void assertValidRetryAfterHeader(
            MvcResult result
    ) {

        String retryAfter =
                result.getResponse()
                        .getHeader(
                                HttpHeaders.RETRY_AFTER
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