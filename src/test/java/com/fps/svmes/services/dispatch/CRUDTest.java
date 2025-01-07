//package com.fps.svmes.services.dispatch;
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
//import com.fps.svmes.dto.requests.DispatchRequest;
//import com.fps.svmes.models.sql.task_schedule.Dispatch;
//import com.fps.svmes.models.sql.task_schedule.DispatchDay;
//import com.fps.svmes.models.sql.task_schedule.DispatchForm;
//import com.fps.svmes.models.sql.task_schedule.DispatchUser;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTestRepository;
//import com.fps.svmes.services.impl.DispatchServiceImpl;
//import jakarta.persistence.EntityNotFoundException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//public class CRUDTest {
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
//
//    @Nested
//    @DisplayName("CRUD Operations Tests")
//    class CreateDispatch {
//        @Test
//        void testCreateDispatch_SpecificDays() {
//            // Prepare input
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
//            request.setSpecificDays(Arrays.asList("MONDAY", "TUESDAY"));
//            request.setTimeOfDay("08:00");
//            request.setActive(true);
//            request.setFormIds(Arrays.asList(101L, 102L));
//            request.setPersonnelIds(Arrays.asList(501L, 502L));
//
//            // Mock repository save behavior
//            when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> {
//                Dispatch dispatch = invocation.getArgument(0);
//                dispatch.setId(1L);
//                return dispatch;
//            });
//
//            // Call the method
//            DispatchDTO savedDispatchDTO = dispatchService.createDispatch(request);
//
//            // Verify repository interactions
//            verify(dispatchRepo, times(1)).save(any(Dispatch.class));
//
//            // Assertions
//            assertNotNull(savedDispatchDTO);
//            assertEquals(1L, savedDispatchDTO.getId());
//            assertEquals("SPECIFIC_DAYS", savedDispatchDTO.getScheduleType());
//            assertEquals("08:00", savedDispatchDTO.getTimeOfDay());
//            assertEquals(2, savedDispatchDTO.getDispatchDays().size());
//            assertEquals(2, savedDispatchDTO.getFormIds().size());
//            assertEquals(2, savedDispatchDTO.getPersonnelIds().size());
//
//            // Verify DispatchDay DTO entries
//            assertEquals("MONDAY", savedDispatchDTO.getDispatchDays().get(0));
//            assertEquals("TUESDAY", savedDispatchDTO.getDispatchDays().get(1));
//        }
//
//        @Test
//        void testCreateDispatch_Interval() {
//            // Prepare input
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
//            request.setIntervalMinutes(30);
//            request.setRepeatCount(5);
//            request.setActive(true);
//            request.setFormIds(Arrays.asList(201L, 202L));
//            request.setPersonnelIds(Arrays.asList(601L, 602L));
//
//            // Mock repository save behavior
//            when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> {
//                Dispatch dispatch = invocation.getArgument(0);
//                dispatch.setId(2L);
//                return dispatch;
//            });
//
//            // Call the method
//            DispatchDTO savedDispatchDTO = dispatchService.createDispatch(request);
//
//            // Verify repository interactions
//            verify(dispatchRepo, times(1)).save(any(Dispatch.class));
//
//            // Assertions
//            assertNotNull(savedDispatchDTO);
//            assertEquals(2L, savedDispatchDTO.getId());
//            assertEquals("INTERVAL", savedDispatchDTO.getScheduleType());
//            assertEquals(30, savedDispatchDTO.getIntervalMinutes());
//            assertEquals(5, savedDispatchDTO.getRepeatCount());
//            assertEquals(2, savedDispatchDTO.getFormIds().size());
//            assertEquals(2, savedDispatchDTO.getPersonnelIds().size());
//            assertNull(savedDispatchDTO.getDispatchDays()); // Specific days should not be set
//            assertNull(savedDispatchDTO.getTimeOfDay());
//        }
//
//        @Test
//        void testCreateDispatch_InvalidSpecificDays() {
//            // Prepare invalid input for SPECIFIC_DAYS without required fields
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
//            request.setActive(true);
//            request.setSpecificDays(null);
//            request.setTimeOfDay(null);
//
//            // Expect exception
//            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dispatchService.createDispatch(request));
//
//            assertEquals("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule", exception.getMessage());
//        }
//
//        @Test
//        void testCreateDispatch_InvalidInterval() {
//            // Prepare invalid input for INTERVAL without required fields
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
//            request.setActive(true);
//            request.setIntervalMinutes(null);
//            request.setRepeatCount(null);
//
//            // Expect exception
//            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dispatchService.createDispatch(request));
//
//            assertEquals("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule", exception.getMessage());
//        }
//    }
//
//
//    @Nested
//    class UpdateSpecificDaysDispatch {
//        private Dispatch existingDispatch;
//
//        @BeforeEach
//        void initSpecificDaysDispatch() {
//            existingDispatch = new Dispatch();
//            existingDispatch.setId(1L);
//            existingDispatch.setScheduleType("SPECIFIC_DAYS");
//            existingDispatch.setTimeOfDay("09:00");
//            existingDispatch.setActive(true);
//            existingDispatch.setCreatedAt(LocalDateTime.now());
//            existingDispatch.setUpdatedAt(LocalDateTime.now());
//
//            // Use mutable lists
//            existingDispatch.setDispatchDays(new ArrayList<>(Arrays.asList(
//                    new DispatchDay(existingDispatch, "MONDAY"),
//                    new DispatchDay(existingDispatch, "TUESDAY")
//            )));
//            existingDispatch.setDispatchForms(new ArrayList<>(List.of(
//                    new DispatchForm(existingDispatch, 101L)
//            )));
//            existingDispatch.setDispatchUser(new ArrayList<>(List.of(
//                    new DispatchUser(existingDispatch, 201)
//            )));
//        }
//
//        @Test
//        void testUpdateDispatch_SuccessfulSpecificDaysUpdate() {
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
//            request.setSpecificDays(Arrays.asList("WEDNESDAY", "FRIDAY"));
//            request.setTimeOfDay("10:30");
//            request.setActive(true);
//            request.setFormIds(Arrays.asList(103L, 104L));
//            request.setPersonnelIds(Arrays.asList(301L, 302L));
//
//            when(dispatchRepo.findById(1L)).thenReturn(Optional.of(existingDispatch));
//            when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> invocation.<Dispatch>getArgument(0));
//
//            DispatchDTO updatedDispatchDTO = dispatchService.updateDispatch(1L, request);
//
//            assertNotNull(updatedDispatchDTO);
//            assertEquals("SPECIFIC_DAYS", updatedDispatchDTO.getScheduleType());
//            assertEquals("10:30", updatedDispatchDTO.getTimeOfDay());
//            assertEquals(2, updatedDispatchDTO.getDispatchDays().size());
//            assertEquals("WEDNESDAY", updatedDispatchDTO.getDispatchDays().get(0));
//            assertEquals("FRIDAY", updatedDispatchDTO.getDispatchDays().get(1));
//            assertEquals(Arrays.asList(103L, 104L), updatedDispatchDTO.getFormIds());
//            assertEquals(Arrays.asList(301L, 302L), updatedDispatchDTO.getPersonnelIds());
//
//            verify(dispatchRepo, times(1)).save(any(Dispatch.class));
//        }
//
//        @Test
//        void testUpdateDispatch_InvalidSpecificDays() {
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.SPECIFIC_DAYS);
//            request.setSpecificDays(Arrays.asList("MONDAY", "FRIDAY"));
//            request.setTimeOfDay(null);
//
//            when(dispatchRepo.findById(1L)).thenReturn(Optional.of(existingDispatch));
//
//            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dispatchService.updateDispatch(1L, request));
//
//            assertEquals("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule", exception.getMessage());
//            verify(dispatchRepo, never()).save(any());
//        }
//    }
//
//    @Nested
//    class UpdateIntervalDispatch {
//        private Dispatch existingDispatch;
//
//        @BeforeEach
//        void initIntervalDispatch() {
//            existingDispatch = new Dispatch();
//            existingDispatch.setId(2L);
//            existingDispatch.setScheduleType("INTERVAL");
//            existingDispatch.setIntervalMinutes(30);
//            existingDispatch.setRepeatCount(3);
//            existingDispatch.setActive(true);
//            existingDispatch.setCreatedAt(LocalDateTime.now());
//            existingDispatch.setUpdatedAt(LocalDateTime.now());
//        }
//
//        @Test
//        void testUpdateDispatch_SuccessfulIntervalUpdate() {
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
//            request.setIntervalMinutes(45);
//            request.setRepeatCount(5);
//            request.setActive(false);
//            request.setFormIds(Arrays.asList(201L, 202L));
//            request.setPersonnelIds(Arrays.asList(401L, 402L));
//
//            when(dispatchRepo.findById(2L)).thenReturn(Optional.of(existingDispatch));
//            when(dispatchRepo.save(any(Dispatch.class))).thenAnswer(invocation -> invocation.<Dispatch>getArgument(0));
//
//            DispatchDTO updatedDispatchDTO = dispatchService.updateDispatch(2L, request);
//
//            assertNotNull(updatedDispatchDTO);
//            assertEquals("INTERVAL", updatedDispatchDTO.getScheduleType());
//            assertEquals(45, updatedDispatchDTO.getIntervalMinutes());
//            assertEquals(5, updatedDispatchDTO.getRepeatCount());
//            assertEquals(Arrays.asList(201L, 202L), updatedDispatchDTO.getFormIds());
//            assertEquals(Arrays.asList(401L, 402L), updatedDispatchDTO.getPersonnelIds());
//            assertNull(updatedDispatchDTO.getTimeOfDay());
//            assertTrue(updatedDispatchDTO.getDispatchDays() == null || updatedDispatchDTO.getDispatchDays().isEmpty());
//
//            verify(dispatchRepo, times(1)).save(any(Dispatch.class));
//        }
//
//        @Test
//        void testUpdateDispatch_InvalidInterval() {
//            DispatchRequest request = new DispatchRequest();
//            request.setScheduleType(DispatchRequest.ScheduleType.INTERVAL);
//            request.setIntervalMinutes(null);
//            request.setRepeatCount(null);
//
//            when(dispatchRepo.findById(2L)).thenReturn(Optional.of(existingDispatch));
//
//            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dispatchService.updateDispatch(2L, request));
//
//            assertEquals("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule", exception.getMessage());
//            verify(dispatchRepo, never()).save(any());
//        }
//    }
//
//
//    @Nested
//    class GetDispatchTests {
//        @BeforeEach
//        void setUp() {
//            MockitoAnnotations.openMocks(this);
//
//            // Create a mock Dispatch
//            Dispatch mockDispatch = new Dispatch();
//            mockDispatch.setId(1L);
//            mockDispatch.setScheduleType("SPECIFIC_DAYS");
//            mockDispatch.setActive(true);
//        }
//
//
//        @Test
//        void testGetDispatch_NotFound() {
//            when(dispatchRepo.findById(1L)).thenReturn(Optional.empty());
//
//            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> dispatchService.getDispatch(1L));
//
//            assertEquals("Dispatch with ID 1 not found", exception.getMessage());
//            verify(dispatchRepo, times(1)).findById(1L);
//        }
//    }
//
//    @Nested
//    class GetAllDispatchTests {
//        @BeforeEach
//        void setUp() {
//            MockitoAnnotations.openMocks(this);
//
//            // Create a mock Dispatch
//            Dispatch mockDispatch = new Dispatch();
//            mockDispatch.setId(1L);
//            mockDispatch.setScheduleType("SPECIFIC_DAYS");
//            mockDispatch.setActive(true);
//        }
//
//    }
//
//    @Nested
//    class DeleteDispatchTests {
//
//        @Test
//        void testDeleteDispatch_Successful() {
//            // Initialize Dispatch only for this test
//            Dispatch mockDispatch = new Dispatch();
//            mockDispatch.setId(1L);
//            mockDispatch.setScheduleType("SPECIFIC_DAYS");
//            mockDispatch.setActive(true);
//
//            // Mock repository behavior
//            when(dispatchRepo.findById(1L)).thenReturn(Optional.of(mockDispatch));
//            doNothing().when(dispatchRepo).delete(mockDispatch);
//
//            // Call the service method
//            assertDoesNotThrow(() -> dispatchService.deleteDispatch(1L));
//
//            // Verify interactions
//            verify(dispatchRepo, times(1)).findById(1L);
//            verify(dispatchRepo, times(1)).delete(mockDispatch);
//        }
//
//        @Test
//        void testDeleteDispatch_NotFound() {
//            // Mock repository behavior for missing Dispatch
//            when(dispatchRepo.findById(1L)).thenReturn(Optional.empty());
//
//            // Expect EntityNotFoundException
//            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> dispatchService.deleteDispatch(1L));
//
//            assertEquals("Dispatch with ID 1 not found", exception.getMessage());
//
//            // Verify interactions
//            verify(dispatchRepo, times(1)).findById(1L);
//            verify(dispatchRepo, never()).delete(any());
//        }
//    }
//
//
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
