package com.fps.svmes.services;

import com.fps.svmes.models.sql.Dispatch;
import com.fps.svmes.models.sql.ScheduleType;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.DispatchedTestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DispatchServiceTest {

    @Autowired
    private DispatchService dispatchService;

    @MockBean
    private DispatchRepository configRepo;

    @MockBean
    private DispatchedTestRepository testRepo;

    @Test
    public void testExecuteDispatch() {
        // Add mock configurations and verify the logic
    }

    @Test
    public void testShouldDispatchRespectsRepeatCount() {
        Dispatch dispatch = new Dispatch();
        dispatch.setScheduleType(ScheduleType.INTERVAL);
        dispatch.setStartTime(LocalDateTime.now().minusMinutes(10));
        dispatch.setIntervalMinutes(5);
        dispatch.setRepeatCount(3);
        dispatch.setExecutedCount(2);

        boolean shouldDispatch = dispatchService.shouldDispatch(dispatch, LocalDateTime.now());
        assertTrue(shouldDispatch); // Should allow one more execution

        dispatch.setExecutedCount(3);
        shouldDispatch = dispatchService.shouldDispatch(dispatch, LocalDateTime.now());
        assertFalse(shouldDispatch); // Should not allow more executions
    }
}
