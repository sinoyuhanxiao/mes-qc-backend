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
    public ShiftDTO createShift(ShiftRequest shiftRequest, Integer userId) {
        // Map the ShiftRequest to the Shift entity
        Shift shift = modelMapper.map(shiftRequest, Shift.class);

        // Set audit fields
        OffsetDateTime now = OffsetDateTime.now();
        shift.setCreatedAt(now);
        shift.setCreatedBy(userId);
        shift.setUpdatedAt(now);
        shift.setUpdatedBy(userId);

        // Set the leader based on leaderId
        if (shiftRequest.getLeaderId() != null) {
            User leader = userRepository.findById(shiftRequest.getLeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Leader not found with ID: " + shiftRequest.getLeaderId()));
            shift.setLeader(leader);
        }

        // Save the Shift entity
        shift = shiftRepository.save(shift);

        // Map the saved Shift entity to the ShiftDTO
        return modelMapper.map(shift, ShiftDTO.class);
    }

    @Override
    public ShiftDTO updateShift(Integer id, @Valid ShiftRequest shiftRequest, Integer userId) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Shift not found"));

        // Map only non-null properties from DTO to entity
        if (shiftRequest.getName() != null) shift.setName(shiftRequest.getName());
        if (shiftRequest.getType() != null) shift.setType(shiftRequest.getType());
        if (shiftRequest.getLeaderId() != null) {
            User leader = userRepository.findById(shiftRequest.getLeaderId())
                    .orElseThrow(() -> new RuntimeException("Leader not found"));
            shift.setLeader(leader);
        }
        if (shiftRequest.getStartTime() != null) shift.setStartTime(shiftRequest.getStartTime());
        if (shiftRequest.getEndTime() != null) shift.setEndTime(shiftRequest.getEndTime());
        if (shiftRequest.getDescription() != null) shift.setDescription(shiftRequest.getDescription());

        if (shiftRequest.getStatus() != null) {
            shift.setStatus(shiftRequest.getStatus()); // Soft delete
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

