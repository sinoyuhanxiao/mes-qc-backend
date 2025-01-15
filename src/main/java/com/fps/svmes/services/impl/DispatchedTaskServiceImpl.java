package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.models.sql.taskSchedule.DispatchedTask;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
import com.fps.svmes.services.DispatchedTaskService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatchedTaskServiceImpl implements DispatchedTaskService {

    @Autowired
    private DispatchedTaskRepository dispatchedTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<DispatchedTaskDTO> getCurrentTasks(Long userId) {
        ZoneId chinaZone = ZoneId.of("Asia/Shanghai"); // Set timezone to China (UTC+8)
        OffsetDateTime startOfDay = LocalDateTime.now(chinaZone).toLocalDate().atStartOfDay(chinaZone).toOffsetDateTime();
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndDueDateBetweenAndStatus(userId, startOfDay, endOfDay, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<DispatchedTaskDTO> getFutureTasks(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndDueDateAfterAndStatus(userId, now, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<DispatchedTaskDTO> getHistoricalTasks(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndDueDateBeforeAndStatus(userId, now, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
    }

    @Override
    public List<DispatchedTaskDTO> getOverdueTasks(Long userId) {
        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndIsOverdueAndStatus(userId, true, 1);
        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
    }

    @Override
    public void insertDispatchedTasks(DispatchedTaskDTO dispatchedTaskDTO, List<Integer> userIds) {
        for (Integer userId : userIds) {
            DispatchedTask task = modelMapper.map(dispatchedTaskDTO, DispatchedTask.class);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            task.setUser(user);
            dispatchedTaskRepository.save(task);
        }
    }


    @Override
    public void updateDispatchedTask(Long id, DispatchedTaskDTO dispatchedTaskDTO) {
        // Fetch the existing task or throw an exception if not found
        DispatchedTask existingTask = dispatchedTaskRepository.findById(id)
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
        dispatchedTaskRepository.save(existingTask);
    }

    @Override
    public DispatchedTaskDTO getDispatchedTaskById(Long id) {
        DispatchedTask task = dispatchedTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
        return modelMapper.map(task, DispatchedTaskDTO.class);
    }

    @Override
    public void deleteDispatchedTask(Long id) {
        DispatchedTask task = dispatchedTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
        task.setStatus(0); // Set status to 0 for soft delete
        dispatchedTaskRepository.save(task);
    }

}