package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskTestDTO;
import com.fps.svmes.models.sql.task_schedule.DispatchedTaskTest;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskTestRepository;
import com.fps.svmes.services.DispatchedTaskTestService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatchedTaskTestServiceImpl implements DispatchedTaskTestService {

    @Autowired
    private DispatchedTaskTestRepository dispatchedTaskTestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<DispatchedTaskTestDTO> getCurrentTasks(Long userId) {
        OffsetDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);
        List<DispatchedTaskTest> tasks = dispatchedTaskTestRepository.findByUserIdAndDueDateBetweenAndStatus(userId, startOfDay, endOfDay, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskTestDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<DispatchedTaskTestDTO> getFutureTasks(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<DispatchedTaskTest> tasks = dispatchedTaskTestRepository.findByUserIdAndDueDateAfterAndStatus(userId, now, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskTestDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<DispatchedTaskTestDTO> getHistoricalTasks(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<DispatchedTaskTest> tasks = dispatchedTaskTestRepository.findByUserIdAndDueDateBeforeAndStatus(userId, now, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskTestDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<DispatchedTaskTestDTO> getOverdueTasks(Long userId) {
        List<DispatchedTaskTest> tasks = dispatchedTaskTestRepository.findByUserIdAndIsOverdueAndStatus(userId, true, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskTestDTO.class)).collect(Collectors.toList());
    }

    @Override
    public void insertDispatchedTasks(DispatchedTaskTestDTO dispatchedTaskDTO, List<Integer> userIds) {
        for (Integer userId : userIds) {
            DispatchedTaskTest task = modelMapper.map(dispatchedTaskDTO, DispatchedTaskTest.class);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            task.setUser(user);
            dispatchedTaskTestRepository.save(task);
        }
    }


    @Override
    public void updateDispatchedTask(Long id, DispatchedTaskTestDTO dispatchedTaskDTO) {
        // Fetch the existing task or throw an exception if not found
        DispatchedTaskTest existingTask = dispatchedTaskTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        if (dispatchedTaskDTO.getName() != null) {
            existingTask.setName(dispatchedTaskDTO.getName());
        }
        if (dispatchedTaskDTO.getDescription() != null) {
            existingTask.setDescription(dispatchedTaskDTO.getDescription());
        }
        if (dispatchedTaskDTO.getDueDate() != null) {
            existingTask.setDueDate(dispatchedTaskDTO.getDueDate());
        }
        if (dispatchedTaskDTO.getStateId() != null) {
            existingTask.setStateId(dispatchedTaskDTO.getStateId());
        }
        if (dispatchedTaskDTO.getFinishedAt() != null) {
            existingTask.setFinishedAt(dispatchedTaskDTO.getFinishedAt());
        }
        if (dispatchedTaskDTO.getNotes() != null) {
            existingTask.setNotes(dispatchedTaskDTO.getNotes());
        }
        if (dispatchedTaskDTO.getIsOverdue() != null) {
            existingTask.setIsOverdue(dispatchedTaskDTO.getIsOverdue());
        }

        // Save the updated task back to the database
        dispatchedTaskTestRepository.save(existingTask);
    }

    @Override
    public DispatchedTaskTestDTO getDispatchedTaskById(Long id) {
        DispatchedTaskTest task = dispatchedTaskTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
        return modelMapper.map(task, DispatchedTaskTestDTO.class);
    }

    @Override
    public void deleteDispatchedTask(Long id) {
        DispatchedTaskTest task = dispatchedTaskTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
        task.setStatus(0); // Set status to 0 for soft delete
        dispatchedTaskTestRepository.save(task);
    }

}