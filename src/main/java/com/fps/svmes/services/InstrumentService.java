package com.fps.svmes.services;

import com.fps.svmes.dto.requests.InstrumentRequest;
import com.fps.svmes.models.sql.taskSchedule.Instrument;

import java.util.List;
import java.util.Optional;

public interface InstrumentService {
    Instrument createInstrument(InstrumentRequest instrumentRequest, Integer userId);
    Optional<Instrument> getInstrumentById(Long id);
    List<Instrument> getAllInstruments();
    Instrument updateInstrument(Long id, Instrument instrument, Integer userId);
    void deleteInstrument(Long id, Integer userId);
}
