package com.fps.svmes.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fps.svmes.models.sql.User;
import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.DispatchedTestRepository;
import com.fps.svmes.services.impl.DispatchServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        @DisplayName("Should throw exception for missing timeOfDay in SPECIFIC_DAYS")
        void testExecuteDispatchSpecificDaysWithNullTimeOfDay() {
            Long dispatchId = 7L;

            Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", null);
            mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201));
            mockDispatch.setDispatchForms(createForms(mockDispatch, 101L));

            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            Exception exception = assertThrows(IllegalStateException.class, () -> {
                dispatchService.executeDispatch(dispatchId);
            });

            assertEquals("Time of day is missing for SPECIFIC_DAYS schedule.", exception.getMessage());
            verify(testRepo, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should create correct dispatch time for interval-based dispatch")
        void testExecuteDispatchIntervalCreatesCorrectDispatchTime() {
            Long dispatchId = 1L;

            Dispatch mockDispatch = getIntervalDispatch(dispatchId, 2, 15);
            mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202));
            mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L));

            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            dispatchService.executeDispatch(dispatchId);

            verify(testRepo, times(1)).saveAll(argThat(tests -> {
                List<DispatchedTest> testList = (List<DispatchedTest>) tests;
                LocalDateTime expectedTime = mockDispatch.getStartTime()
                        .plusMinutes((long) mockDispatch.getIntervalMinutes() * mockDispatch.getExecutedCount());
                return testList.stream().allMatch(test -> expectedTime.equals(test.getDispatchTime()));
            }));
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
        @DisplayName("Execute Dispatch Interval With Missing Start Time")
        void testExecuteDispatchIntervalWithMissingStartTime() {
            // Arrange
            Long dispatchId = 3L;

            // Create a mock Dispatch with missing start time
            Dispatch mockDispatch = new Dispatch();
            mockDispatch.setId(dispatchId);
            mockDispatch.setScheduleType("INTERVAL");
            mockDispatch.setIntervalMinutes(15);
            mockDispatch.setExecutedCount(0);
            mockDispatch.setStartTime(null); // Missing start time
            List<DispatchPersonnel> personnelList = List.of(
                    new DispatchPersonnel(mockDispatch, 201),
                    new DispatchPersonnel(mockDispatch, 202)
            );

            // Add sample forms to DispatchForm list
            List<DispatchForm> formList = List.of(
                    new DispatchForm(mockDispatch, 101L),
                    new DispatchForm(mockDispatch, 102L)
            );

            // Assign personnel and forms to mockDispatch
            mockDispatch.setDispatchPersonnel(personnelList);
            mockDispatch.setDispatchForms(formList);

            // Mock repository behavior
            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            // Act & Assert
            Exception exception = assertThrows(IllegalStateException.class, () -> {
                dispatchService.executeDispatch(dispatchId);
            });

            // Verify exception message
            assertEquals("Invalid INTERVAL configuration: Missing start time or interval minutes.", exception.getMessage());

            // Verify that no tests are created
            verify(testRepo, never()).save(any(DispatchedTest.class));

            // Verify that the Dispatch entity is not updated
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
        @DisplayName("Should throw exception for empty timeOfDay in SPECIFIC_DAYS")
        void testExecuteDispatchSpecificDaysWithEmptyTimeOfDay() {
            Long dispatchId = 9L;

            // Arrange: Create a dispatch with valid personnel and forms but empty timeOfDay
            Dispatch mockDispatch = createSpecificDaysDispatch(dispatchId, "MONDAY", ""); // Empty timeOfDay
            mockDispatch.setDispatchPersonnel(createPersonnel(mockDispatch, 201, 202)); // Add valid personnel
            mockDispatch.setDispatchForms(createForms(mockDispatch, 101L, 102L)); // Add valid forms

            when(dispatchRepo.findById(dispatchId)).thenReturn(Optional.of(mockDispatch));

            // Act & Assert: Ensure the exception is thrown for empty timeOfDay
            Exception exception = assertThrows(IllegalStateException.class, () -> {
                dispatchService.executeDispatch(dispatchId);
            });

            // Verify the expected exception and message
            assertEquals("Time of day is missing for SPECIFIC_DAYS schedule.", exception.getMessage());

            // Ensure no tests are saved
            verify(testRepo, never()).saveAll(any());
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

        @Test
        @DisplayName("Should create dispatch successfully")
        void testCreateDispatch() {
            Dispatch newDispatch = new Dispatch();
            Dispatch savedDispatch = new Dispatch();
            savedDispatch.setId(1L);

            when(dispatchRepo.save(any())).thenReturn(savedDispatch);

            Dispatch result = dispatchService.createDispatch(newDispatch);
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should delete dispatch successfully")
        void testDeleteDispatchSuccess() {
            when(dispatchRepo.existsById(1L)).thenReturn(true);
            assertTrue(dispatchService.deleteDispatch(1L));
        }

        @Test
        @DisplayName("testCreateDispatchWithNullLists")
        void testCreateDispatchWithNullLists() {
            // Arrange
            Dispatch inputDispatch = new Dispatch();
            inputDispatch.setId(2L);
            inputDispatch.setDispatchDays(null); // Null list
            inputDispatch.setDispatchForms(null);
            inputDispatch.setDispatchPersonnel(null);

            Dispatch savedDispatch = new Dispatch();
            savedDispatch.setId(2L);

            when(dispatchRepo.save(any(Dispatch.class))).thenReturn(savedDispatch);

            // Act
            Dispatch result = dispatchService.createDispatch(inputDispatch);

            // Assert
            assertNotNull(result);
            assertEquals(2L, result.getId());
            verify(dispatchRepo, times(2)).save(any(Dispatch.class)); // Initial and final saves
        }
        @Test
        void testGetDispatchByIdSuccess() {
            // Arrange
            Dispatch mockDispatch = new Dispatch();
            mockDispatch.setId(1L);

            when(dispatchRepo.findById(1L)).thenReturn(Optional.of(mockDispatch));

            // Act
            Optional<Dispatch> result = dispatchService.getDispatchById(1L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
            verify(dispatchRepo, times(1)).findById(1L);
        }

        @Test
        void testGetDispatchByIdNotFound() {
            // Arrange
            when(dispatchRepo.findById(1L)).thenReturn(Optional.empty());

            // Act
            Optional<Dispatch> result = dispatchService.getDispatchById(1L);

            // Assert
            assertFalse(result.isPresent());
            verify(dispatchRepo, times(1)).findById(1L);
        }

        @Test
        void testGetAllDispatches() {
            // Arrange
            List<Dispatch> mockDispatches = List.of(
                    new Dispatch(),
                    new Dispatch()
            );

            when(dispatchRepo.findAll()).thenReturn(mockDispatches);

            // Act
            List<Dispatch> result = dispatchService.getAllDispatches();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(dispatchRepo, times(1)).findAll();
        }

        @Test
        void testUpdateDispatchSuccess() {
            // Arrange
            Dispatch existingDispatch = new Dispatch();
            existingDispatch.setId(1L);

            Dispatch updatedDispatch = new Dispatch();
            updatedDispatch.setScheduleType("SPECIFIC_DAYS");

            when(dispatchRepo.findById(1L)).thenReturn(Optional.of(existingDispatch));
            when(dispatchRepo.save(any(Dispatch.class))).thenReturn(updatedDispatch);

            // Act
            Optional<Dispatch> result = dispatchService.updateDispatch(1L, updatedDispatch);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("SPECIFIC_DAYS", result.get().getScheduleType());
            verify(dispatchRepo, times(1)).findById(1L);
            verify(dispatchRepo, times(1)).save(any(Dispatch.class));
        }

        @Test
        void testUpdateDispatchNotFound() {
            // Arrange
            Dispatch updatedDispatch = new Dispatch();
            updatedDispatch.setScheduleType("SPECIFIC_DAYS");

            when(dispatchRepo.findById(1L)).thenReturn(Optional.empty());

            // Act
            Optional<Dispatch> result = dispatchService.updateDispatch(1L, updatedDispatch);

            // Assert
            assertFalse(result.isPresent());
            verify(dispatchRepo, times(1)).findById(1L);
            verify(dispatchRepo, times(0)).save(any(Dispatch.class));
        }



        @Test
        void testDeleteDispatchNotFound() {
            // Arrange
            when(dispatchRepo.existsById(1L)).thenReturn(false);

            // Act
            boolean result = dispatchService.deleteDispatch(1L);

            // Assert
            assertFalse(result);
            verify(dispatchRepo, times(1)).existsById(1L);
            verify(dispatchRepo, times(0)).deleteById(1L);
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
        return List.of(userIds).stream()
                .map(userId -> new DispatchPersonnel(1L, dispatch, userId))
                .toList();
    }

    private List<DispatchForm> createForms(Dispatch dispatch, Long... formIds) {
        return List.of(formIds).stream()
                .map(formId -> new DispatchForm(1L, dispatch, formId))
                .toList();
    }

    }
}
