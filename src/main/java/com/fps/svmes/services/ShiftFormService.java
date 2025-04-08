package com.fps.svmes.services;

import java.util.List;

public interface ShiftFormService {
    void assignFormToShift(Integer shiftId, String formId);
    void removeFormFromShift(Integer shiftId, String formId);
    List<String> getFormIdsByShift(Integer shiftId);
    void removeAllFormsFromShift(Integer shiftId);
}
