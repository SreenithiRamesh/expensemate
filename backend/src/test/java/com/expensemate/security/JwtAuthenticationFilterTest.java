package com.expensemate.security;

import com.expensemate.exception.ApiProblemFactory;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private ApiProblemFactory problemFactory;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        filter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userDetailsService,
                        problemFactory
                );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService,
                problemFactory
        );
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Basic abc123"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService,
                problemFactory
        );
    }

    @Test
    void shouldAuthenticateWhenBearerTokenIsValid()
            throws Exception {

        String token =
                "valid-token";

        String email =
                "sree@example.com";

        UserDetails userDetails =
                User.withUsername(email)
                        .password("unused")
                        .authorities("USER")
                        .build();

        when(
                jwtService.extractEmail(token)
        ).thenReturn(email);

        when(
                userDetailsService.loadUserByUsername(email)
        ).thenReturn(userDetails);

        when(
                jwtService.isTokenValid(
                        token,
                        email
                )
        ).thenReturn(true);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        assertEquals(
                email,
                authentication.getName()
        );

        assertEquals(
                userDetails,
                authentication.getPrincipal()
        );

        verify(filterChain).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                problemFactory
        );
    }

    @Test
    void shouldRejectTokenWhenValidationReturnsFalse()
            throws Exception {

        String token =
                "invalid-token";

        String email =
                "sree@example.com";

        UserDetails userDetails =
                User.withUsername(email)
                        .password("unused")
                        .authorities("USER")
                        .build();

        when(
                jwtService.extractEmail(token)
        ).thenReturn(email);

        when(
                userDetailsService.loadUserByUsername(email)
        ).thenReturn(userDetails);

        when(
                jwtService.isTokenValid(
                        token,
                        email
                )
        ).thenReturn(false);

        MockHttpServletRequest request =
                bearerRequest(token);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verifyInvalidTokenProblem(
                request,
                response
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void shouldRejectTokenWhenProcessingThrowsException()
            throws Exception {

        String token =
                "broken-token";

        when(
                jwtService.extractEmail(token)
        ).thenThrow(
                new RuntimeException(
                        "Sensitive parser detail"
                )
        );

        MockHttpServletRequest request =
                bearerRequest(token);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verifyInvalidTokenProblem(
                request,
                response
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void shouldRejectBlankBearerToken()
            throws Exception {

        MockHttpServletRequest request =
                bearerRequest("");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verifyInvalidTokenProblem(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );
    }

    private MockHttpServletRequest bearerRequest(
            String token
    ) {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );

        return request;
    }

    private void verifyInvalidTokenProblem(
            MockHttpServletRequest request,
            MockHttpServletResponse response
    ) throws Exception {

        verify(problemFactory).write(
                response,
                HttpStatus.UNAUTHORIZED,
                "invalid-access-token",
                "Invalid access token",
                "The access token is invalid or has expired",
                request
        );
    }
}