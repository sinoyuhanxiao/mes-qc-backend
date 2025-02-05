package com.fps.svmes.services.impl;

import com.fps.svmes.dto.requests.InstrumentRequest;
import com.fps.svmes.models.sql.taskSchedule.Instrument;
import com.fps.svmes.repositories.jpaRepo.dispatch.InstrumentRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.InstrumentService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InstrumentServiceImpl implements InstrumentService {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Instrument createInstrument(InstrumentRequest instrumentRequest, Integer userId) {
        Instrument instrument = modelMapper.map(instrumentRequest, Instrument.class);
        instrument.setCreationDetails(userId, 1);
        return instrumentRepository.save(instrument);
    }

    @Override
    public Optional<Instrument> getInstrumentById(Long id) {
        return instrumentRepository.findByIdAndStatus(id, 1); // Only fetch active instruments
    }

    @Override
    public List<Instrument> getAllInstruments() {
        return instrumentRepository.findByStatus(1); // Only return active instruments
    }

    @Override
    public Instrument updateInstrument(Long id, Instrument instrument, Integer userId) {
        return instrumentRepository.findById(id).map(existingInstrument -> {
            existingInstrument.setType(instrument.getType());
            existingInstrument.setName(instrument.getName());
            existingInstrument.setDescription(instrument.getDescription());
            existingInstrument.setManufacturer(instrument.getManufacturer());
            existingInstrument.setModelNumber(instrument.getModelNumber());
            existingInstrument.setUpdateDetails(userId, 1);
            return instrumentRepository.save(existingInstrument);
        }).orElseThrow(() -> new RuntimeException("Instrument not found with id " + id));
    }

    @Override
    public void deleteInstrument(Long id, Integer userId) {
        instrumentRepository.findById(id).ifPresent(existingInstrument -> {
            existingInstrument.setStatus(0); // Soft delete by setting status to 0
            existingInstrument.setUpdatedAt(OffsetDateTime.now());
            existingInstrument.setUpdatedBy(userId);
            instrumentRepository.save(existingInstrument);
        });
    }
}
