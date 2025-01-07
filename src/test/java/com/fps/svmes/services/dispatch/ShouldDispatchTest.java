//package com.fps.svmes.services.dispatch;
//
//import com.fps.svmes.models.sql.task_schedule.*;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTestRepository;
//import com.fps.svmes.services.impl.DispatchServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//
//public class ShouldDispatchTest {
//
//    @Mock
//    private DispatchRepository dispatchRepo;
//
//    @Mock
//    private DispatchedTestRepository testRepo;
//
//    @InjectMocks
//    private DispatchServiceImpl dispatchService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    @DisplayName("Should not dispatch when scheduleType is null")
//    void testShouldDispatchWithNullScheduleType() {
//        Dispatch mockDispatch = new Dispatch();
//        mockDispatch.setId(10L);
//        mockDispatch.setScheduleType(null);
//
//        LocalDateTime now = LocalDateTime.now();
//
//        boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//
//        assertFalse(shouldDispatch, "Dispatch should not execute when scheduleType is null.");
//    }
//
//    @Nested
//    @DisplayName("Interval Tests")
//    class IntervalTests {
//
//        @Test
//        void testShouldDispatchIntervalWithValidNextDispatchTime() {
//            // Arrange
//            Dispatch dispatch = new Dispatch();
//            dispatch.setScheduleType("INTERVAL");
//            dispatch.setStartTime(LocalDateTime.now().minusMinutes(10));
//            dispatch.setIntervalMinutes(5);
//            dispatch.setExecutedCount(1);
//
//            LocalDateTime now = LocalDateTime.now();
//
//            // Act
//            boolean result = dispatchService.shouldDispatch(dispatch, now);
//
//            // Assert
//            assertTrue(result, "Dispatch should be eligible for execution based on the interval configuration.");
//        }
//
//        @Test
//        void testShouldDispatchIntervalExceedsRepeatCount() {
//            // Arrange
//            Dispatch dispatch = new Dispatch();
//            dispatch.setScheduleType("INTERVAL");
//            dispatch.setStartTime(LocalDateTime.now().minusMinutes(10));
//            dispatch.setIntervalMinutes(5);
//            dispatch.setRepeatCount(1);
//            dispatch.setExecutedCount(1);
//
//            LocalDateTime now = LocalDateTime.now();
//
//            // Act
//            boolean result = dispatchService.shouldDispatch(dispatch, now);
//
//            // Assert
//            assertFalse(result, "Dispatch should not execute if executed count exceeds repeat count.");
//        }
//
//
//        @Test
//        void testExecuteDispatchIntervalExceedsRepeatCount() {
//            Long dispatchId = 2L;
//            Dispatch mockDispatch = getIntervalDispatch(dispatchId, 3, 15);
//            mockDispatch.setRepeatCount(3);
//
//            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));
//
//            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, LocalDateTime.now());
//            assertFalse(shouldDispatch);
//            verify(testRepo, never()).saveAll(any());
//        }
//
//        @Test
//        @DisplayName("Should dispatch - interval based")
//        void testShouldDispatchInterval() {
//            Dispatch mockDispatch = getIntervalDispatch(2L, 2, 5);
//            LocalDateTime now = LocalDateTime.now();
//
//            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//            assertTrue(shouldDispatch, "Should allow one more execution.");
//
//            mockDispatch.setExecutedCount(3);
//            shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//            assertFalse(shouldDispatch, "Should not allow more executions.");
//        }
//
//        @Test
//        void testShouldNotDispatchWhenIntervalFieldsAreNull() {
//            Dispatch mockDispatch = new Dispatch();
//            mockDispatch.setScheduleType("INTERVAL");
//            mockDispatch.setStartTime(null); // Missing start time
//            mockDispatch.setIntervalMinutes(null); // Missing interval minutes
//            mockDispatch.setRepeatCount(5);
//            mockDispatch.setExecutedCount(0);
//
//            LocalDateTime now = LocalDateTime.now();
//
//            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//            assertFalse(shouldDispatch, "Should not dispatch when interval fields are null.");
//        }
//    }
//
//    @Nested
//    @DisplayName("Specific Days Tests")
//    class SpecificDaysTests {
//
//        @Test
//        void testExecuteDispatchSpecificDaysWithNoDispatchDays() {
//            // Arrange
//            Long dispatchId = 8L;
//            Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, null, "14:00");
//            mockDispatch.setDispatchDays(List.of());
//
//            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));
//
//            // Simulate the current time as Monday at 14:00
//            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 14, 0);
//
//            // Act
//            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//
//            // Assert
//            assertFalse(shouldDispatch, "Dispatch should not execute when no dispatch days are configured.");
//            verify(testRepo, never()).saveAll(any());
//        }
//
//        @Test
//        void testExecuteDispatchSpecificDaysWrongDay() {
//            // Arrange
//            Long dispatchId = 6L;
//            Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "TUESDAY", "14:00");
//
//            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));
//
//            // Simulate the current time as Monday at 14:00
//            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 14, 0);
//
//            // Act
//            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//
//            // Assert
//            assertFalse(shouldDispatch, "Dispatch should not execute on the wrong day.");
//            verify(testRepo, never()).saveAll(any());
//        }
//
//        @Test
//        void testShouldDispatchSpecificDaysWithMatchingDayAndTime() {
//            // Arrange
//            Dispatch dispatch = new Dispatch();
//            dispatch.setScheduleType("SPECIFIC_DAYS");
//            dispatch.setTimeOfDay("10:30");
//            dispatch.setDispatchDays(List.of(
//                    new DispatchDay(dispatch, "MONDAY"),
//                    new DispatchDay(dispatch, "WEDNESDAY")
//            ));
//
//            LocalDateTime now = LocalDateTime.now()
//                    .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
//                    .with(LocalTime.of(10, 30));
//
//            // Act
//            boolean result = dispatchService.shouldDispatch(dispatch, now);
//
//            // Assert
//            assertTrue(result, "Dispatch should be eligible for execution at the matching day and time.");
//        }
//
//        @Test
//        void testShouldDispatchSpecificDaysWithNonMatchingDay() {
//            // Arrange
//            Dispatch dispatch = new Dispatch();
//            dispatch.setScheduleType("SPECIFIC_DAYS");
//            dispatch.setTimeOfDay("10:30");
//            dispatch.setDispatchDays(List.of(
//                    new DispatchDay(dispatch, "MONDAY"),
//                    new DispatchDay(dispatch, "WEDNESDAY")
//            ));
//
//            LocalDateTime now = LocalDateTime.now()
//                    .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.TUESDAY))
//                    .with(LocalTime.of(10, 30));
//
//            // Act
//            boolean result = dispatchService.shouldDispatch(dispatch, now);
//
//            // Assert
//            assertFalse(result, "Dispatch should not execute on a non-matching day.");
//        }
//
//        @Test
//        void testShouldDispatchSpecificDaysWithoutTimeOfDay() {
//            // Arrange
//            Dispatch dispatch = new Dispatch();
//            dispatch.setScheduleType("SPECIFIC_DAYS");
//            dispatch.setDispatchDays(List.of(new DispatchDay(dispatch, "MONDAY")));
//            dispatch.setTimeOfDay(null);
//
//            LocalDateTime now = LocalDateTime.now()
//                    .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
//
//            // Act
//            boolean result = dispatchService.shouldDispatch(dispatch, now);
//
//            // Assert
//            assertFalse(result, "Dispatch should not execute without a valid `timeOfDay`.");
//        }
//
//
//
//
//        @Test
//        @DisplayName("Should dispatch on matching day and time")
//        void testShouldDispatchSpecificDaysCorrectDayAndTime() {
//            Dispatch mockDispatch = createSpecificDaysDispatch(1L, "MONDAY", "09:00");
//            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 9, 0);
//
//            assertTrue(dispatchService.shouldDispatch(mockDispatch, now));
//        }
//
//        @Test
//        @DisplayName("Should not dispatch on non-matching day")
//        void testShouldNotDispatchOnNonMatchingSpecificDay() {
//            Dispatch mockDispatch = createSpecificDaysDispatch(1L, "TUESDAY", "09:00");
//            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 9, 0);
//
//            assertFalse(dispatchService.shouldDispatch(mockDispatch, now));
//        }
//
//        @Test
//        @DisplayName("Should not dispatch when specific days are missing")
//        void testShouldNotDispatchWhenNoSpecificDays() {
//            Dispatch mockDispatch = createSpecificDaysDispatch(1L, null, "09:00");
//            mockDispatch.setDispatchDays(List.of());
//            LocalDateTime now = LocalDateTime.now();
//
//            assertFalse(dispatchService.shouldDispatch(mockDispatch, now));
//        }
//
//
//
//
//
//
//
//        @Test
//        @DisplayName("Should not dispatch when DispatchDays contains invalid day")
//        void testShouldDispatchWithInvalidDay() {
//            Dispatch mockDispatch = createSpecificDaysDispatch(13L, "SOMEDAY", "09:00");
//            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 9, 0); // Monday
//
//            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
//
//            assertFalse(shouldDispatch, "Dispatch should not execute when days contain invalid values.");
//        }
//
//    }
//
//    // === Utility Methods for Test Setup ===
//    private Dispatch getIntervalDispatch(Long id, int executedCount, int intervalMinutes) {
//        Dispatch dispatch = new Dispatch();
//        dispatch.setId(id);
//        dispatch.setScheduleType("INTERVAL");
//        dispatch.setExecutedCount(executedCount);
//        dispatch.setStartTime(LocalDateTime.now().minusMinutes((long) intervalMinutes * executedCount));
//        dispatch.setIntervalMinutes(intervalMinutes);
//        return dispatch;
//    }
//
//    private Dispatch createSpecificDaysDispatch(Long id, String day, String timeOfDay) {
//        Dispatch dispatch = new Dispatch();
//        dispatch.setId(id);
//        dispatch.setScheduleType("SPECIFIC_DAYS");
//        dispatch.setTimeOfDay(timeOfDay);
//        if (day != null) {
//            DispatchDay specificDay = new DispatchDay();
//            specificDay.setDay(day);
//            dispatch.setDispatchDays(List.of(specificDay));
//        }
//        return dispatch;
//    }
//
//    private List<DispatchUser> createPersonnel(Dispatch dispatch, Integer... userIds) {
//        return Stream.of(userIds)
//                .map(userId -> new DispatchUser(1L, dispatch, userId))
//                .toList();
//    }
//
//    private List<DispatchForm> createForms(Dispatch dispatch, Long... formIds) {
//        return Stream.of(formIds)
//                .map(formId -> new DispatchForm(1L, dispatch, formId))
//                .toList();
//    }
//}
