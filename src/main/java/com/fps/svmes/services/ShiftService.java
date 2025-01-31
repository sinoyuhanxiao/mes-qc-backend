package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.user.ShiftDTO;
import com.fps.svmes.dto.requests.ShiftRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface ShiftService {
    ShiftDTO createShift(ShiftRequest ShiftRequest, Integer userId);
    ShiftDTO updateShift(Integer id, @Valid ShiftRequest shiftDTO, Integer userId);
    ShiftDTO getShiftById(Integer id);
    List<ShiftDTO> getAllShifts();
    void activateShift(Integer id, Integer updatedBy);
    void softDeleteShift(Integer id, Integer userId);
    void hardDeleteShift(Integer id);
}