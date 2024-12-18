package com.fps.svmes.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTestRepository;
import com.fps.svmes.services.impl.DispatchServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class DispatchServiceImplTest {

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

    // === Group: EXECUTE DISPATCH ===
    @Nested
    @DisplayName("Execute Dispatch Tests")
    class ExecuteDispatchTests {

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
                List<DispatchedTest> testList = (List<DispatchedTest>) tests;

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
        @DisplayName("Should not execute when interval exceeds repeat count")
        void testExecuteDispatchIntervalExceedsRepeatCount() {
            Long dispatchId = 2L;
            Dispatch mockDispatch = getIntervalDispatch(dispatchId, 3, 15);
            mockDispatch.setRepeatCount(3);

            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, LocalDateTime.now());
            assertFalse(shouldDispatch);
            verify(testRepo, never()).saveAll(any());
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
                List<DispatchedTest> testList = (List<DispatchedTest>) tests;

                // Verify size
                assertEquals(4, testList.size(), "4 tests should be created (2 personnel x 2 forms)");

                // Verify mapping
                List<Long> personnelIds = testList.stream()
                        .map(DispatchedTest::getPersonnelId)
                        .distinct()
                        .toList();
                assertTrue(personnelIds.containsAll(List.of(201L, 202L)), "Personnel IDs should match.");

                List<Long> formIds = testList.stream()
                        .map(DispatchedTest::getFormId)
                        .distinct()
                        .toList();
                assertTrue(formIds.containsAll(List.of(101L, 102L)), "Form IDs should match.");

                return true;
            }));
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
                List<DispatchedTest> testList = (List<DispatchedTest>) tests;
                return testList.size() == 4; // 2 personnel x 2 forms
            }));
            verify(dispatchRepo, never()).save(any(Dispatch.class)); // No increment for specific_days
        }

        @Test
        void testExecuteDispatchSpecificDaysWrongDay() {
            // Arrange
            Long dispatchId = 6L;
            Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "TUESDAY", "14:00");

            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            // Simulate the current time as Monday at 14:00
            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 14, 0);

            // Act
            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);

            // Assert
            assertFalse(shouldDispatch, "Dispatch should not execute on the wrong day.");
            verify(testRepo, never()).saveAll(any());
        }


        @Test
        void testExecuteDispatchSpecificDaysWithNoDispatchDays() {
            // Arrange
            Long dispatchId = 8L;
            Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, null, "14:00");
            mockDispatch.setDispatchDays(List.of());

            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            // Simulate the current time as Monday at 14:00
            LocalDateTime now = LocalDateTime.of(2024, 12, 16, 14, 0);

            // Act
            boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);

            // Assert
            assertFalse(shouldDispatch, "Dispatch should not execute when no dispatch days are configured.");
            verify(testRepo, never()).saveAll(any());
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

            // Verify log output for the skipped dispatch (if logging is testable)
            // This can be done with a logging framework like LogCaptor or custom log testing.
        }
    }

        // === Group: SHOULD DISPATCH ===
    @Nested
    @DisplayName("Should Dispatch Tests")
    class ShouldDispatchTests {

        @Nested
        @DisplayName("Specific Days Tests")
        class SpecificDaysTests {

            @Test
            @DisplayName("Should not dispatch when scheduleType is null")
            void testShouldDispatchWithNullScheduleType() {
                Dispatch mockDispatch = new Dispatch();
                mockDispatch.setId(10L);
                mockDispatch.setScheduleType(null);

                LocalDateTime now = LocalDateTime.now();

                boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);

                assertFalse(shouldDispatch, "Dispatch should not execute when scheduleType is null.");
            }


            @Test
            @DisplayName("Should dispatch on matching day and time")
            void testShouldDispatchSpecificDaysCorrectDayAndTime() {
                Dispatch mockDispatch = createSpecificDaysDispatch(1L, "MONDAY", "09:00");
                LocalDateTime now = LocalDateTime.of(2024, 12, 16, 9, 0);

                assertTrue(dispatchService.shouldDispatch(mockDispatch, now));
            }

            @Test
            @DisplayName("Should not dispatch on non-matching day")
            void testShouldNotDispatchOnNonMatchingSpecificDay() {
                Dispatch mockDispatch = createSpecificDaysDispatch(1L, "TUESDAY", "09:00");
                LocalDateTime now = LocalDateTime.of(2024, 12, 16, 9, 0);

                assertFalse(dispatchService.shouldDispatch(mockDispatch, now));
            }

            @Test
            @DisplayName("Should not dispatch when specific days are missing")
            void testShouldNotDispatchWhenNoSpecificDays() {
                Dispatch mockDispatch = createSpecificDaysDispatch(1L, null, "09:00");
                mockDispatch.setDispatchDays(List.of());
                LocalDateTime now = LocalDateTime.now();

                assertFalse(dispatchService.shouldDispatch(mockDispatch, now));
            }

            @Test
            @DisplayName("Should dispatch - interval based")
            void testShouldDispatchInterval() {
                Dispatch mockDispatch = getIntervalDispatch(2L, 2, 5);
                LocalDateTime now = LocalDateTime.now();

                boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
                assertTrue(shouldDispatch, "Should allow one more execution.");

                mockDispatch.setExecutedCount(3);
                shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
                assertFalse(shouldDispatch, "Should not allow more executions.");
            }



            @Test
            void testShouldNotDispatchWhenIntervalFieldsAreNull() {
                Dispatch mockDispatch = new Dispatch();
                mockDispatch.setScheduleType("INTERVAL");
                mockDispatch.setStartTime(null); // Missing start time
                mockDispatch.setIntervalMinutes(null); // Missing interval minutes
                mockDispatch.setRepeatCount(5);
                mockDispatch.setExecutedCount(0);

                LocalDateTime now = LocalDateTime.now();

                boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);
                assertFalse(shouldDispatch, "Should not dispatch when interval fields are null.");
            }

            @Test
            @DisplayName("Should not dispatch when DispatchDays contains invalid day")
            void testShouldDispatchWithInvalidDay() {
                Dispatch mockDispatch = createSpecificDaysDispatch(13L, "SOMEDAY", "09:00");
                LocalDateTime now = LocalDateTime.of(2024, 12, 16, 9, 0); // Monday

                boolean shouldDispatch = dispatchService.shouldDispatch(mockDispatch, now);

                assertFalse(shouldDispatch, "Dispatch should not execute when days contain invalid values.");
            }

        }
    }

    // === Group: CRUD OPERATIONS ===
    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudTests {
        @Nested
        class CreateDispatch {
            @Test
            void testCreateDispatch_SpecificDays() {
                // Prepare input
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
                request.setSpecificDays(Arrays.asList("MONDAY", "TUESDAY"));
                request.setTimeOfDay("08:00");
                request.setActive(true);
                request.setFormIds(Arrays.asList(101L, 102L));
                request.setPersonnelIds(Arrays.asList(501L, 502L));

                // Mock repository save behavior
                when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> {
                    Dispatch dispatch = invocation.getArgument(0);
                    dispatch.setId(1L);
                    return dispatch;
                });

                // Call the method
                DispatchDTO savedDispatchDTO = dispatchService.createDispatch(request);

                // Verify repository interactions
                verify(dispatchRepo, times(1)).save(any(Dispatch.class));

                // Assertions
                assertNotNull(savedDispatchDTO);
                assertEquals(1L, savedDispatchDTO.getId());
                assertEquals("SPECIFIC_DAYS", savedDispatchDTO.getScheduleType());
                assertEquals("08:00", savedDispatchDTO.getTimeOfDay());
                assertEquals(2, savedDispatchDTO.getDispatchDays().size());
                assertEquals(2, savedDispatchDTO.getFormIds().size());
                assertEquals(2, savedDispatchDTO.getPersonnelIds().size());

                // Verify DispatchDay DTO entries
                assertEquals("MONDAY", savedDispatchDTO.getDispatchDays().get(0));
                assertEquals("TUESDAY", savedDispatchDTO.getDispatchDays().get(1));
            }

            @Test
            void testCreateDispatch_Interval() {
                // Prepare input
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
                request.setIntervalMinutes(30);
                request.setRepeatCount(5);
                request.setActive(true);
                request.setFormIds(Arrays.asList(201L, 202L));
                request.setPersonnelIds(Arrays.asList(601L, 602L));

                // Mock repository save behavior
                when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> {
                    Dispatch dispatch = invocation.getArgument(0);
                    dispatch.setId(2L);
                    return dispatch;
                });

                // Call the method
                DispatchDTO savedDispatchDTO = dispatchService.createDispatch(request);

                // Verify repository interactions
                verify(dispatchRepo, times(1)).save(any(Dispatch.class));

                // Assertions
                assertNotNull(savedDispatchDTO);
                assertEquals(2L, savedDispatchDTO.getId());
                assertEquals("INTERVAL", savedDispatchDTO.getScheduleType());
                assertEquals(30, savedDispatchDTO.getIntervalMinutes());
                assertEquals(5, savedDispatchDTO.getRepeatCount());
                assertEquals(2, savedDispatchDTO.getFormIds().size());
                assertEquals(2, savedDispatchDTO.getPersonnelIds().size());
                assertNull(savedDispatchDTO.getDispatchDays()); // Specific days should not be set
                assertNull(savedDispatchDTO.getTimeOfDay());
            }

            @Test
            void testCreateDispatch_InvalidSpecificDays() {
                // Prepare invalid input for SPECIFIC_DAYS without required fields
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
                request.setActive(true);
                request.setSpecificDays(null);
                request.setTimeOfDay(null);

                // Expect exception
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dispatchService.createDispatch(request));

                assertEquals("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule", exception.getMessage());
            }

            @Test
            void testCreateDispatch_InvalidInterval() {
                // Prepare invalid input for INTERVAL without required fields
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
                request.setActive(true);
                request.setIntervalMinutes(null);
                request.setRepeatCount(null);

                // Expect exception
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dispatchService.createDispatch(request));

                assertEquals("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule", exception.getMessage());
            }
        }


        @Nested
        class UpdateSpecificDaysDispatch {
            private Dispatch existingDispatch;

            @BeforeEach
            void initSpecificDaysDispatch() {
                existingDispatch = new Dispatch();
                existingDispatch.setId(1L);
                existingDispatch.setScheduleType("SPECIFIC_DAYS");
                existingDispatch.setTimeOfDay("09:00");
                existingDispatch.setActive(true);
                existingDispatch.setCreatedAt(LocalDateTime.now());
                existingDispatch.setUpdatedAt(LocalDateTime.now());

                // Use mutable lists
                existingDispatch.setDispatchDays(new ArrayList<>(Arrays.asList(
                        new DispatchDay(existingDispatch, "MONDAY"),
                        new DispatchDay(existingDispatch, "TUESDAY")
                )));
                existingDispatch.setDispatchForms(new ArrayList<>(List.of(
                        new DispatchForm(existingDispatch, 101L)
                )));
                existingDispatch.setDispatchPersonnel(new ArrayList<>(List.of(
                        new DispatchPersonnel(existingDispatch, 201)
                )));
            }

            @Test
            void testUpdateDispatch_SuccessfulSpecificDaysUpdate() {
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
                request.setSpecificDays(Arrays.asList("WEDNESDAY", "FRIDAY"));
                request.setTimeOfDay("10:30");
                request.setActive(true);
                request.setFormIds(Arrays.asList(103L, 104L));
                request.setPersonnelIds(Arrays.asList(301L, 302L));

                when(dispatchRepo.findById(1L)).thenReturn(Optional.of(existingDispatch));
                when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> {
                    return invocation.<Dispatch>getArgument(0);
                });

                DispatchDTO updatedDispatchDTO = dispatchService.updateDispatch(1L, request);

                assertNotNull(updatedDispatchDTO);
                assertEquals("SPECIFIC_DAYS", updatedDispatchDTO.getScheduleType());
                assertEquals("10:30", updatedDispatchDTO.getTimeOfDay());
                assertEquals(2, updatedDispatchDTO.getDispatchDays().size());
                assertEquals("WEDNESDAY", updatedDispatchDTO.getDispatchDays().get(0));
                assertEquals("FRIDAY", updatedDispatchDTO.getDispatchDays().get(1));
                assertEquals(Arrays.asList(103L, 104L), updatedDispatchDTO.getFormIds());
                assertEquals(Arrays.asList(301L, 302L), updatedDispatchDTO.getPersonnelIds());

                verify(dispatchRepo, times(1)).save(any(Dispatch.class));
            }

            @Test
            void testUpdateDispatch_InvalidSpecificDays() {
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
                request.setSpecificDays(Arrays.asList("MONDAY", "FRIDAY"));
                request.setTimeOfDay(null);

                when(dispatchRepo.findById(1L)).thenReturn(Optional.of(existingDispatch));

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    dispatchService.updateDispatch(1L, request);
                });

                assertEquals("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule", exception.getMessage());
                verify(dispatchRepo, never()).save(any());
            }
        }

        @Nested
        class UpdateIntervalDispatch {
            private Dispatch existingDispatch;

            @BeforeEach
            void initIntervalDispatch() {
                existingDispatch = new Dispatch();
                existingDispatch.setId(2L);
                existingDispatch.setScheduleType("INTERVAL");
                existingDispatch.setIntervalMinutes(30);
                existingDispatch.setRepeatCount(3);
                existingDispatch.setActive(true);
                existingDispatch.setCreatedAt(LocalDateTime.now());
                existingDispatch.setUpdatedAt(LocalDateTime.now());
            }

            @Test
            void testUpdateDispatch_SuccessfulIntervalUpdate() {
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
                request.setIntervalMinutes(45);
                request.setRepeatCount(5);
                request.setActive(false);
                request.setFormIds(Arrays.asList(201L, 202L));
                request.setPersonnelIds(Arrays.asList(401L, 402L));

                when(dispatchRepo.findById(2L)).thenReturn(Optional.of(existingDispatch));
                when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> {
                    return invocation.<Dispatch>getArgument(0);
                });

                DispatchDTO updatedDispatchDTO = dispatchService.updateDispatch(2L, request);

                assertNotNull(updatedDispatchDTO);
                assertEquals("INTERVAL", updatedDispatchDTO.getScheduleType());
                assertEquals(45, updatedDispatchDTO.getIntervalMinutes());
                assertEquals(5, updatedDispatchDTO.getRepeatCount());
                assertEquals(Arrays.asList(201L, 202L), updatedDispatchDTO.getFormIds());
                assertEquals(Arrays.asList(401L, 402L), updatedDispatchDTO.getPersonnelIds());
                assertNull(updatedDispatchDTO.getTimeOfDay());
                assertTrue(updatedDispatchDTO.getDispatchDays() == null || updatedDispatchDTO.getDispatchDays().isEmpty());

                verify(dispatchRepo, times(1)).save(any(Dispatch.class));
            }

            @Test
            void testUpdateDispatch_InvalidInterval() {
                DispatchRequest request = new DispatchRequest();
                request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
                request.setIntervalMinutes(null);
                request.setRepeatCount(null);

                when(dispatchRepo.findById(2L)).thenReturn(Optional.of(existingDispatch));

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    dispatchService.updateDispatch(2L, request);
                });

                assertEquals("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule", exception.getMessage());
                verify(dispatchRepo, never()).save(any());
            }
        }


        @Nested
        class GetDispatchTests {
            private Dispatch mockDispatch;
            @BeforeEach
            void setUp() {
                MockitoAnnotations.openMocks(this);

                // Create a mock Dispatch
                mockDispatch = new Dispatch();
                mockDispatch.setId(1L);
                mockDispatch.setScheduleType("SPECIFIC_DAYS");
                mockDispatch.setActive(true);
            }

//            @Test
//            void testGetDispatch_Successful() {
//                when(dispatchRepo.findById(1L)).thenReturn(Optional.of(mockDispatch));
//
//                Dispatch result = dispatchService.getDispatch(1L);
//
//                assertNotNull(result);
//                assertEquals(1L, result.getId());
//                assertEquals("SPECIFIC_DAYS", result.getScheduleType());
//                assertTrue(result.getActive());
//                verify(dispatchRepo, times(1)).findById(1L);
//            }

            @Test
            void testGetDispatch_NotFound() {
                when(dispatchRepo.findById(1L)).thenReturn(Optional.empty());

                EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                    dispatchService.getDispatch(1L);
                });

                assertEquals("Dispatch with ID 1 not found", exception.getMessage());
                verify(dispatchRepo, times(1)).findById(1L);
            }
        }

        @Nested
        class GetAllDispatchTests {
            private Dispatch mockDispatch;
            @BeforeEach
            void setUp() {
                MockitoAnnotations.openMocks(this);

                // Create a mock Dispatch
                mockDispatch = new Dispatch();
                mockDispatch.setId(1L);
                mockDispatch.setScheduleType("SPECIFIC_DAYS");
                mockDispatch.setActive(true);
            }

//            @Test
//            void testGetAllDispatches_Successful() {
//                Dispatch secondDispatch = new Dispatch();
//                secondDispatch.setId(2L);
//                secondDispatch.setScheduleType("INTERVAL");
//                secondDispatch.setActive(false);
//
//                List<Dispatch> mockDispatches = Arrays.asList(mockDispatch, secondDispatch);
//
//                when(dispatchRepo.findAll()).thenReturn(mockDispatches);
//
//                List<Dispatch> result = dispatchService.getAllDispatches();
//
//                assertNotNull(result);
//                assertEquals(2, result.size());
//
//                assertEquals(1L, result.get(0).getId());
//                assertEquals("SPECIFIC_DAYS", result.get(0).getScheduleType());
//
//                assertEquals(2L, result.get(1).getId());
//                assertEquals("INTERVAL", result.get(1).getScheduleType());
//
//                verify(dispatchRepo, times(1)).findAll();
//            }

//            @Test
//            void testGetAllDispatches_EmptyList() {
//                when(dispatchRepo.findAll()).thenReturn(List.of());
//
//                List<Dispatch> result = dispatchService.getAllDispatches();
//
//                assertNotNull(result);
//                assertTrue(result.isEmpty());
//
//                verify(dispatchRepo, times(1)).findAll();
//            }
        }

        @Nested
        class DeleteDispatchTests {

            @Test
            void testDeleteDispatch_Successful() {
                // Initialize Dispatch only for this test
                Dispatch mockDispatch = new Dispatch();
                mockDispatch.setId(1L);
                mockDispatch.setScheduleType("SPECIFIC_DAYS");
                mockDispatch.setActive(true);

                // Mock repository behavior
                when(dispatchRepo.findById(1L)).thenReturn(Optional.of(mockDispatch));
                doNothing().when(dispatchRepo).delete(mockDispatch);

                // Call the service method
                assertDoesNotThrow(() -> dispatchService.deleteDispatch(1L));

                // Verify interactions
                verify(dispatchRepo, times(1)).findById(1L);
                verify(dispatchRepo, times(1)).delete(mockDispatch);
            }

            @Test
            void testDeleteDispatch_NotFound() {
                // Mock repository behavior for missing Dispatch
                when(dispatchRepo.findById(1L)).thenReturn(Optional.empty());

                // Expect EntityNotFoundException
                EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                    dispatchService.deleteDispatch(1L);
                });

                assertEquals("Dispatch with ID 1 not found", exception.getMessage());

                // Verify interactions
                verify(dispatchRepo, times(1)).findById(1L);
                verify(dispatchRepo, never()).delete(any());
            }
        }

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
