package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.user.ShiftDTO;
import com.fps.svmes.dto.requests.ShiftRequest;
import com.fps.svmes.models.sql.user.Shift;
import com.fps.svmes.repositories.jpaRepo.user.ShiftRepository;
import com.fps.svmes.services.ShiftService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.models.sql.user.User;

@Service
public class ShiftServiceImpl implements ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ShiftDTO createShift(ShiftDTO shiftDTO, Integer userId) {
        Shift shift = modelMapper.map(shiftDTO, Shift.class);
        shift.setCreatedAt(OffsetDateTime.now());
        shift.setCreatedBy(userId);
        shift.setUpdatedAt(OffsetDateTime.now());
        shift.setUpdatedBy(userId);

        shift = shiftRepository.save(shift);
        return modelMapper.map(shift, ShiftDTO.class);
    }

    @Override
    public ShiftDTO updateShift(Integer id, @Valid ShiftRequest shiftDTO, Integer userId) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Shift not found"));

        // Map only non-null properties from DTO to entity
        if (shiftDTO.getName() != null) shift.setName(shiftDTO.getName());
        if (shiftDTO.getType() != null) shift.setType(shiftDTO.getType());
        if (shiftDTO.getLeader() != null && shiftDTO.getLeader().getId() != null) {
            User leader = userRepository.findById(shiftDTO.getLeader().getId())
                    .orElseThrow(() -> new RuntimeException("Leader not found"));
            shift.setLeader(leader);
        }
        if (shiftDTO.getStartTime() != null) shift.setStartTime(shiftDTO.getStartTime());
        if (shiftDTO.getEndTime() != null) shift.setEndTime(shiftDTO.getEndTime());
        if (shiftDTO.getDescription() != null) shift.setDescription(shiftDTO.getDescription());

        if (shiftDTO.getStatus() != null && shiftDTO.getStatus() == 0) {
            shift.setStatus(0); // Soft delete
        }

        shift.setUpdatedAt(OffsetDateTime.now());
        shift.setUpdatedBy(userId);

        shift = shiftRepository.save(shift);
        return modelMapper.map(shift, ShiftDTO.class);
    }

    @Override
    public ShiftDTO getShiftById(Integer id) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Shift not found"));
        return modelMapper.map(shift, ShiftDTO.class);
    }

    @Override
    public List<ShiftDTO> getAllShifts() {
        List<Shift> shifts = shiftRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return shifts.stream()
                .map(shift -> modelMapper.map(shift, ShiftDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void softDeleteShift(Integer id, Integer userId) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Shift not found"));
        shift.setStatus(0); // Soft delete
        shift.setUpdatedAt(OffsetDateTime.now());
        shift.setUpdatedBy(userId);
        shiftRepository.save(shift);
    }

    @Override
    public void hardDeleteShift(Integer id) {
        if (!shiftRepository.existsById(id)) {
            throw new RuntimeException("Shift not found");
        }
        shiftRepository.deleteById(id); // Permanently deletes the record
    }


    @Override
    public void activateShift(Integer id, Integer updatedBy) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Shift not found"));

        if (shift.getStatus() == 1) {
            throw new RuntimeException("Shift is already active");
        }

        shift.setStatus(1); // Reactivate the shift
        shift.setUpdatedAt(OffsetDateTime.now());
        shift.setUpdatedBy(updatedBy);

        shiftRepository.save(shift);
    }

}

