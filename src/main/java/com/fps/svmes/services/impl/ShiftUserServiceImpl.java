package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.user.ShiftUserDTO;
import com.fps.svmes.models.sql.user.ShiftUser;
import com.fps.svmes.models.sql.user.ShiftUserId;
import com.fps.svmes.repositories.jpaRepo.user.ShiftRepository;
import com.fps.svmes.repositories.jpaRepo.user.ShiftUserRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.ShiftUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftUserServiceImpl implements ShiftUserService {

    private final ShiftUserRepository shiftUserRepository;

    private final ShiftRepository shiftRepository;

    private final UserRepository userRepository;

    @Override
    public void assignUserToShifts(Integer userId, List<Integer> shiftIds) {
        List<ShiftUser> shiftUsers = shiftIds.stream()
                .map(shiftId -> {
                    ShiftUser shiftUser = new ShiftUser(new ShiftUserId(shiftId, userId));
                    shiftUser.setShift(shiftRepository.findById(Math.toIntExact(shiftId))
                            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId)));
                    shiftUser.setUser(userRepository.findById(Math.toIntExact(userId))
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
                    return shiftUser;
                })
                .collect(Collectors.toList());
        shiftUserRepository.saveAll(shiftUsers);
        log.info("Assigned user {} to shifts {}", userId, shiftIds);
    }

    @Override
    public void assignUsersToShift(Integer shiftId, List<Integer> userIds) {
        List<ShiftUser> shiftUsers = userIds.stream()
                .map(userId -> {
                    ShiftUser shiftUser = new ShiftUser(new ShiftUserId(shiftId, userId));
                    shiftUser.setShift(shiftRepository.findById(Math.toIntExact(shiftId))
                            .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId)));
                    shiftUser.setUser(userRepository.findById(Math.toIntExact(userId))
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
                    return shiftUser;
                })
                .collect(Collectors.toList());
        shiftUserRepository.saveAll(shiftUsers);
        log.info("Assigned users {} to shift {}", userIds, shiftId);
    }


    @Override
    @Transactional
    public void removeUserFromShift(Integer userId, Integer shiftId) {
        shiftUserRepository.deleteById(new ShiftUserId(shiftId, userId));
        log.info("Removed user {} from shift {}", userId, shiftId);
    }

    @Override
    @Transactional
    public void removeUserFromAllShifts(Integer userId) {
        shiftUserRepository.deleteByIdUserId(userId);
        log.info("Removed user {} from all shifts", userId);
    }

    @Override
    @Transactional
    public void removeUsersFromShift(Integer shiftId, List<Integer> userIds) {
        userIds.forEach(userId ->
                shiftUserRepository.deleteById(new ShiftUserId(shiftId, userId))
        );
        log.info("Removed users {} from shift {}", userIds, shiftId);
    }

    @Override
    public List<ShiftUserDTO> getShiftsForUser(Integer userId) {
        List<ShiftUser> shiftUsers = shiftUserRepository.findByIdUserId(userId);
        log.info("Retrieved shifts for user {}: {}", userId, shiftUsers.size());
        return shiftUsers.stream()
                .map(shiftUser -> new ShiftUserDTO(
                        shiftUser.getId().getUserId(),
                        shiftUser.getId().getShiftId()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ShiftUserDTO> getUsersForShift(Integer shiftId) {
        List<ShiftUser> shiftUsers = shiftUserRepository.findByIdShiftId(shiftId);
        log.info("Retrieved users for shift {}: {}", shiftId, shiftUsers.size());
        return shiftUsers.stream()
                .map(shiftUser -> new ShiftUserDTO(
                        shiftUser.getId().getUserId(),
                        shiftUser.getId().getShiftId()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ShiftUserDTO> getAllShiftUsers() {
        List<ShiftUser> shiftUsers = shiftUserRepository.findAll();
        log.info("Retrieved all shift-user relationships: {}", shiftUsers.size());
        return shiftUsers.stream()
                .map(shiftUser -> {
                    ShiftUserId id = shiftUser.getId(); // Retrieve the embedded ID
                    if (id != null) {
                        return new ShiftUserDTO(id.getUserId(), id.getShiftId());
                    } else {
                        log.warn("ShiftUser entity with null ID found: {}", shiftUser);
                        return new ShiftUserDTO(null, null);
                    }
                })
                .collect(Collectors.toList());
    }
}