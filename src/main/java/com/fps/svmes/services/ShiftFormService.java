package com.fps.svmes.services;

import com.fps.svmes.models.nosql.FormNode;

import java.util.List;

public interface ShiftFormService {
    void assignFormToShift(Integer shiftId, String formId);
    void removeFormFromShift(Integer shiftId, String formId);
    List<String> getFormIdsByShift(Integer shiftId);
    void removeAllFormsFromShift(Integer shiftId);
    List<FormNode> getFormTreeByShiftId(Integer shiftId);
}
