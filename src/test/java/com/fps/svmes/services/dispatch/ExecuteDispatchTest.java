package com.fps.svmes.services.dispatch;

import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTestRepository;
import com.fps.svmes.services.impl.DispatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExecuteDispatchTest {

    @Mock
    private DispatchRepository dispatchRepo;

    @Mock
    private DispatchedTestRepository testRepo;

    @InjectMocks
    private DispatchServiceImpl dispatchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSpecificDaysDispatchSchedulesAtCorrectTime() {
        // Arrange
        Long dispatchId = 1L;
        Dispatch dispatch = new Dispatch();
        dispatch.setId(dispatchId);
        dispatch.setScheduleType("SPECIFIC_DAYS");
        dispatch.setTimeOfDay("10:30"); // Specific time
        dispatch.setDispatchDays(Arrays.asList(
                new DispatchDay(dispatch, "MONDAY"),
                new DispatchDay(dispatch, "WEDNESDAY")
        ));
        dispatch.setDispatchPersonnel(List.of(
                new DispatchPersonnel(dispatch, 101),
                new DispatchPersonnel(dispatch, 102)
        ));
        dispatch.setDispatchForms(List.of(
                new DispatchForm(dispatch, 201L),
                new DispatchForm(dispatch, 202L)
        ));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(dispatch));
        when(testRepo.saveAll(any())).thenReturn(null); // Simulate saving tests

        // Simulate current date and time as Monday, 10:30 AM
        LocalDateTime now = LocalDateTime.now()
                .withHour(10).withMinute(30)
                .withSecond(0).withNano(0);

        try (var mockTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockTime.when(LocalDateTime::now).thenReturn(now);

            // Act
            dispatchService.executeDispatch(dispatchId);

            // Assert
            verify(testRepo, times(1)).saveAll(argThat(dispatchedTests -> {
                List<DispatchedTask> tests = (List<DispatchedTask>) dispatchedTests;
                assertEquals(4, tests.size()); // 2 personnel x 2 forms
                for (DispatchedTask test : tests) {
                    assertEquals(now, test.getDispatchTime()); // Ensure proper time
                }
                return true;
            }));
        }
    }

    @Test
    void testExecuteDispatchSpecificDaysCreatesCorrectTests() {
        // Arrange
        Long dispatchId = 1L;
        Dispatch dispatch = new Dispatch();
        dispatch.setId(dispatchId);
        dispatch.setScheduleType("SPECIFIC_DAYS");
        dispatch.setTimeOfDay("10:30");
        dispatch.setDispatchDays(List.of(
                new DispatchDay(dispatch, "MONDAY")
        ));
        dispatch.setDispatchPersonnel(List.of(
                new DispatchPersonnel(dispatch, 101),
                new DispatchPersonnel(dispatch, 102)
        ));
        dispatch.setDispatchForms(List.of(
                new DispatchForm(dispatch, 201L),
                new DispatchForm(dispatch, 202L)
        ));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(dispatch));

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert
        verify(testRepo, times(1)).saveAll(argThat(dispatchedTests -> {
            List<DispatchedTask> tests = (List<DispatchedTask>) dispatchedTests;
            assertEquals(4, tests.size()); // 2 personnel x 2 forms
            return true;
        }));
    }

    @Test
    void testExecuteDispatchIntervalIncrementsCount() {
        // Arrange
        Long dispatchId = 2L;
        Dispatch dispatch = new Dispatch();
        dispatch.setId(dispatchId);
        dispatch.setScheduleType("INTERVAL");
        dispatch.setStartTime(LocalDateTime.now().minusMinutes(10));
        dispatch.setIntervalMinutes(5);
        dispatch.setExecutedCount(1);
        dispatch.setDispatchPersonnel(List.of(
                new DispatchPersonnel(dispatch, 101),
                new DispatchPersonnel(dispatch, 102)
        ));
        dispatch.setDispatchForms(List.of(
                new DispatchForm(dispatch, 201L),
                new DispatchForm(dispatch, 202L)
        ));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(dispatch));

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert
        verify(testRepo, times(1)).saveAll(any());
        assertEquals(2, dispatch.getExecutedCount(), "Executed count should be incremented.");
    }

    @Test
    @DisplayName("Should create batch tests successfully")
    void testExecuteDispatchCreatesBatchTests() {
        Long dispatchId = 1L;

        Dispatch mockDispatch = getIntervalDispatch(dispatchId, 2, 15);
        mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202));
        mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        dispatchService.executeDispatch(dispatchId);

        verify(testRepo, times(1)).saveAll(any());
        assertEquals(3, mockDispatch.getExecutedCount(), "Executed count should increment to 3.");
    }

    @Test
    @DisplayName("Should skip execution when no personnel or forms")
    void testExecuteDispatchNoPersonnelOrForms() {
        Long dispatchId = 3L;

        Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", "09:00");
        mockDispatch.setDispatchPersonnel(List.of());
        mockDispatch.setDispatchForms(List.of());

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        dispatchService.executeDispatch(dispatchId);

        verify(testRepo, times(0)).saveAll(any());
    }

    @Test
    @DisplayName("Should skip execution for missing timeOfDay in SPECIFIC_DAYS")
    void testExecuteDispatchSpecificDaysWithNullTimeOfDay() {
        Long dispatchId = 7L;

        Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", null); // Null timeOfDay
        mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201));
        mockDispatch.setDispatchForms(createForms(mockDispatch, 101L));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert: Ensure no tests are saved, and dispatch is not updated
        verify(testRepo, never()).saveAll(any());
        verify(dispatchRepo, never()).save(mockDispatch);
    }

    @Test
    @DisplayName("Should create correct dispatch time for interval-based dispatch")
    void testExecuteDispatchIntervalCreatesCorrectDispatchTime() {
        Long dispatchId = 1L;
        int original_count = 15;
        // Arrange: Interval-based dispatch with start time and interval configuration
        Dispatch mockDispatch = getIntervalDispatch(dispatchId, original_count, 2);
        mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202));
        mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        // Capture the current executed count for the calculation
        int simulatedExecutedCount = mockDispatch.getExecutedCount() + 1;

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert: Verify that the correct dispatch time is used
        verify(testRepo, times(1)).saveAll(argThat(tests -> {
            List<DispatchedTask> testList = (List<DispatchedTask>) tests;

            // Calculate the expected time using the simulated count
            LocalDateTime expectedTime = mockDispatch.getStartTime()
                    .plusMinutes((long) mockDispatch.getIntervalMinutes() * simulatedExecutedCount);

            // Ensure all dispatched tests have the correct `dispatchTime`
            return testList.stream().allMatch(test -> expectedTime.equals(test.getDispatchTime()));
        }));

        // Ensure executedCount is incremented after successful execution
        assertEquals(original_count + 1, mockDispatch.getExecutedCount());

    }

    @Test
    @DisplayName("Should skip execution for interval-based dispatch with missing start time")
    void testExecuteDispatchIntervalWithMissingStartTime() {
        Long dispatchId = 3L;

        // Arrange: Create an interval-based dispatch missing start time
        Dispatch mockDispatch = new Dispatch();
        mockDispatch.setId(dispatchId);
        mockDispatch.setScheduleType("INTERVAL");
        mockDispatch.setIntervalMinutes(15);
        mockDispatch.setExecutedCount(0);
        mockDispatch.setStartTime(null); // Missing start time
        mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202));
        mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert: Ensure no tests are saved, and dispatch is not updated
        verify(testRepo, never()).saveAll(any());
        verify(dispatchRepo, never()).save(mockDispatch);
    }

    @Test
    @DisplayName("ExecuteDispatchIntervalPersonnelFormsMapping")
    void testExecuteDispatchIntervalPersonnelFormsMapping() {
        // Arrange
        Long dispatchId = 4L;

        Dispatch mockDispatch = new Dispatch();
        mockDispatch.setId(dispatchId);
        mockDispatch.setScheduleType("INTERVAL");
        mockDispatch.setStartTime(LocalDateTime.of(2024, 12, 16, 14, 0));
        mockDispatch.setIntervalMinutes(15);
        mockDispatch.setExecutedCount(1);

        List<DispatchPersonnel> personnelList = List.of(
                new DispatchPersonnel(1L, mockDispatch, 201),
                new DispatchPersonnel(2L, mockDispatch, 202)
        );
        List<DispatchForm> formList = List.of(
                new DispatchForm(1L, mockDispatch, 101L),
                new DispatchForm(2L, mockDispatch, 102L)
        );

        mockDispatch.setDispatchPersonnel(personnelList);
        mockDispatch.setDispatchForms(formList);

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));
        when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert
        verify(testRepo, times(1)).saveAll(argThat(tests -> {
            List<DispatchedTask> testList = (List<DispatchedTask>) tests;

            // Verify size
            assertEquals(4, testList.size(), "4 tests should be created (2 personnel x 2 forms)");

            // Verify mapping
            List<Long> personnelIds = testList.stream()
                    .map(DispatchedTask::getPersonnelId)
                    .distinct()
                    .toList();
            assertTrue(personnelIds.containsAll(List.of(201L, 202L)), "Personnel IDs should match.");

            List<Long> formIds = testList.stream()
                    .map(DispatchedTask::getFormId)
                    .distinct()
                    .toList();
            assertTrue(formIds.containsAll(List.of(101L, 102L)), "Form IDs should match.");

            return true;
        }));
    }

    @Test
    @DisplayName("Should skip execution when personnel list is null")
    void testExecuteDispatchWithNullPersonnelList() {
        Long dispatchId = 11L;

        Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", "09:00");
        mockDispatch.setDispatchPersonnel(null); // Null personnel list
        mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L));

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        dispatchService.executeDispatch(dispatchId);

        verify(testRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should skip execution when forms list is null")
    void testExecuteDispatchWithNullFormsList() {
        Long dispatchId = 12L;

        Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", "09:00");
        mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202));
        mockDispatch.setDispatchForms(null); // Null forms list

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        dispatchService.executeDispatch(dispatchId);

        verify(testRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should skip execution for empty timeOfDay in SPECIFIC_DAYS")
    void testExecuteDispatchSpecificDaysWithEmptyTimeOfDay() {
        Long dispatchId = 9L;

        // Arrange: Create a dispatch with valid personnel and forms but empty timeOfDay
        Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", ""); // Empty timeOfDay
        mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202)); // Add valid personnel
        mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L)); // Add valid forms

        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

        // Act
        dispatchService.executeDispatch(dispatchId);

        // Assert: Ensure no tests are saved, and dispatch is not updated
        verify(testRepo, never()).saveAll(any());
        verify(dispatchRepo, never()).save(mockDispatch);
    }

    @Test
    void testManualDispatchNotFound() {
        // Arrange
        when(dispatchRepo.existsById(1L)).thenReturn(false);

        // Act
        boolean result = dispatchService.manualDispatch(1L);

        // Assert
        assertFalse(result);
        verify(dispatchRepo, times(1)).existsById(1L);
    }

    @Test
    void testExecuteDispatchSpecificDaysCreatesTests() {
        // Arrange
        Long dispatchId = 5L;
        Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", "14:00");
        List<DispatchPersonnel> personnelList = createPersonnel(mockDispatch, 201, 202);
        List<DispatchForm> formList = createForms(mockDispatch, 101L, 102L);

        mockDispatch.setDispatchPersonnel(personnelList);
        mockDispatch.setDispatchForms(formList);

        // Set up the mock behavior
        when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));
        when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Simulate the current time as Monday at 14:00
        LocalDateTime now = LocalDateTime.of(2024, 12, 16, 14, 0);

        // Act
        boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
        dispatchService.executeDispatch(dispatchId);

        // Assert
        assertTrue(shouldDispatch, "Dispatch should execute on specific day and time.");
        verify(testRepo, times(1)).saveAll(argThat(tests -> {
            List<DispatchedTask> testList = (List<DispatchedTask>) tests;
            return testList.size() == 4; // 2 personnel x 2 forms
        }));
        verify(dispatchRepo, never()).save(any(Dispatch.class)); // No increment for specific_days
    }


















    // === Utility Methods for Test Setup ===
    private Dispatch getIntervalDispatch(Long id, int executedCount, int intervalMinutes) {
        Dispatch dispatch = new Dispatch();
        dispatch.setId(id);
        dispatch.setScheduleType("INTERVAL");
        dispatch.setExecutedCount(executedCount);
        dispatch.setStartTime(LocalDateTime.now().minusMinutes((long) intervalMinutes * executedCount));
        dispatch.setIntervalMinutes(intervalMinutes);
        return dispatch;
    }

    private Dispatch createSpecificDaysDispatch(Long id, String day, String timeOfDay) {
        Dispatch dispatch = new Dispatch();
        dispatch.setId(id);
        dispatch.setScheduleType("SPECIFIC_DAYS");
        dispatch.setTimeOfDay(timeOfDay);
        if (day != null) {
            DispatchDay specificDay = new DispatchDay();
            specificDay.setDay(day);
            dispatch.setDispatchDays(List.of(specificDay));
        }
        return dispatch;
    }

    private List<DispatchPersonnel> createPersonnel(Dispatch dispatch, Integer... userIds) {
        return Stream.of(userIds)
                .map(userId -> new DispatchPersonnel(1L, dispatch, userId))
                .toList();
    }

    private List<DispatchForm> createForms(Dispatch dispatch, Long... formIds) {
        return Stream.of(formIds)
                .map(formId -> new DispatchForm(1L, dispatch, formId))
                .toList();
    }

}
