package com.expensemate.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        filter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userDetailsService
                );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenAuthorizationHeaderMissing()
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
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenHeaderIsNotBearer()
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
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );
    }

    @Test
    void shouldAuthenticateWhenBearerTokenIsValid()
            throws Exception {

        String token = "valid-token";
        String email = "sree@example.com";

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
                SecurityContextHolder.getContext()
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
    }

    @Test
    void shouldNotAuthenticateWhenTokenValidationReturnsFalse()
            throws Exception {

        String token = "invalid-token";
        String email = "sree@example.com";

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

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(
                request,
                response
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenProcessingThrowsException()
            throws Exception {

        String token = "broken-token";

        when(
                jwtService.extractEmail(token)
        ).thenThrow(
                new RuntimeException(
                        "Invalid token"
                )
        );

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

        assertEquals(
                401,
                response.getStatus()
        );

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );
    }
}