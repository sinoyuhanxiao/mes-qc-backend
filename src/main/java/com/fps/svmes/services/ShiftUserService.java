package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.user.ShiftUserDTO;
import java.util.List;

public interface ShiftUserService {
    void assignUserToShifts(Integer userId, List<Integer> shiftIds); // Changed Integer to Long
    void assignUsersToShift(Integer shiftId, List<Integer> userIds); // Changed Integer to Long
    void removeUserFromShift(Integer userId, Integer shiftId);
    void removeUserFromAllShifts(Integer userId);
    void removeUsersFromShift(Integer shiftId, List<Integer> userIds);
    List<ShiftUserDTO> getShiftsForUser(Integer userId);
    List<ShiftUserDTO> getUsersForShift(Integer shiftId);
    List<ShiftUserDTO> getAllShiftUsers();
//    void setShiftLeader(Long shiftId, Long leaderId);
//    List<ShiftUserDTO> getLedShiftsForUser(Long userId);
}
