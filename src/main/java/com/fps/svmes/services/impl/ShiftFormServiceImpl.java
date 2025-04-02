package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.user.Shift;
import com.fps.svmes.models.sql.user.ShiftForm;
import com.fps.svmes.models.sql.user.ShiftFormId;
import com.fps.svmes.repositories.jpaRepo.user.ShiftFormRepository;
import com.fps.svmes.repositories.jpaRepo.user.ShiftRepository;
import com.fps.svmes.services.ShiftFormService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftFormServiceImpl implements ShiftFormService {
    private final ShiftFormRepository shiftFormRepository;
    private final ShiftRepository shiftRepository;

    @Transactional
    @Override
    public void assignFormToShift(Integer shiftId, String formId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found"));

        ShiftFormId id = new ShiftFormId(shiftId, formId);

        if (!shiftFormRepository.existsById(id)) {
            ShiftForm shiftForm = new ShiftForm(id, shift);
            shiftFormRepository.save(shiftForm);
        }
    }

    @Transactional
    @Override
    public void removeFormFromShift(Integer shiftId, String formId) {
        shiftFormRepository.deleteById(new ShiftFormId(shiftId, formId));
    }

    @Override
    public List<String> getFormIdsByShift(Integer shiftId) {
        return shiftFormRepository.findByShiftId(shiftId)
                .stream()
                .map(sf -> sf.getId().getFormId())
                .toList();
    }

    @Transactional
    @Override
    public void removeAllFormsFromShift(Integer shiftId) {
        shiftFormRepository.deleteByShiftId(shiftId);
    }
}
