package com.expensemate.service.ai;

import com.expensemate.entity.AiUsage;
import com.expensemate.entity.User;
import com.expensemate.exception.AiUsageLimitExceededException;
import com.expensemate.repository.AiUsageRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiUsageServiceImplTest {

    private AiUsageRepository aiUsageRepository;
    private UserRepository userRepository;

    private AiUsageServiceImpl service;

    @BeforeEach
    void setUp() {

        aiUsageRepository =
                mock(AiUsageRepository.class);

        userRepository =
                mock(UserRepository.class);

        service = new AiUsageServiceImpl(
                aiUsageRepository,
                userRepository,
                10
        );
    }

    @Test
    void shouldAllowRequestBelowDailyLimit() {

        User user = mock(User.class);

        AiUsage usage = new AiUsage(
                user,
                LocalDate.now(),
                9,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(aiUsageRepository.findByUserIdAndUsageDate(
                1L,
                LocalDate.now()
        )).thenReturn(Optional.of(usage));

        assertDoesNotThrow(
                () -> service.verifyLimit(1L)
        );
    }

    @Test
    void shouldRejectRequestAtDailyLimit() {

        User user = mock(User.class);

        AiUsage usage = new AiUsage(
                user,
                LocalDate.now(),
                10,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(aiUsageRepository.findByUserIdAndUsageDate(
                1L,
                LocalDate.now()
        )).thenReturn(Optional.of(usage));

        assertThrows(
                AiUsageLimitExceededException.class,
                () -> service.verifyLimit(1L)
        );
    }

    @Test
    void shouldCreateUsageAndRecordFirstSuccessfulRequest() {

        User user = mock(User.class);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(aiUsageRepository.findByUserIdAndUsageDate(
                1L,
                LocalDate.now()
        )).thenReturn(Optional.empty());

        int remaining =
                service.recordSuccessfulRequest(1L);

        assertEquals(9, remaining);

        verify(aiUsageRepository)
                .save(any(AiUsage.class));
    }

    @Test
    void shouldIncrementExistingUsage() {

        User user = mock(User.class);

        AiUsage usage = new AiUsage(
                user,
                LocalDate.now(),
                4,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(aiUsageRepository.findByUserIdAndUsageDate(
                1L,
                LocalDate.now()
        )).thenReturn(Optional.of(usage));

        int remaining =
                service.recordSuccessfulRequest(1L);

        assertEquals(5, usage.getRequestCount());
        assertEquals(5, remaining);

        verify(aiUsageRepository).save(usage);
    }

    @Test
    void shouldReturnFullLimitWhenUserHasNoUsageToday() {

        when(aiUsageRepository.findByUserIdAndUsageDate(
                1L,
                LocalDate.now()
        )).thenReturn(Optional.empty());

        int remaining =
                service.getRemainingRequests(1L);

        assertEquals(10, remaining);
    }
}