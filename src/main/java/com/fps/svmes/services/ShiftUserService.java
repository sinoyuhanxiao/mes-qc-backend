package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.user.ShiftUserDTO;
import java.util.List;

public interface ShiftUserService {
    void assignUserToShifts(Long userId, List<Long> shiftIds); // Changed Integer to Long
    void assignUsersToShift(Long shiftId, List<Long> userIds); // Changed Integer to Long
    void removeUserFromShift(Long userId, Long shiftId);
    void removeUserFromAllShifts(Long userId);
    void removeUsersFromShift(Long shiftId, List<Long> userIds);
    List<ShiftUserDTO> getShiftsForUser(Long userId);
    List<ShiftUserDTO> getUsersForShift(Long shiftId);
    List<ShiftUserDTO> getAllShiftUsers();
//    void setShiftLeader(Long shiftId, Long leaderId);
//    List<ShiftUserDTO> getLedShiftsForUser(Long userId);
}
