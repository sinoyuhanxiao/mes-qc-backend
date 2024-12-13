package com.fps.svmes.controllers;


import com.fps.svmes.models.sql.Dispatch;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.services.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing dispatch configurations.
 */
@RestController
@RequestMapping("/api/dispatches")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private DispatchRepository dispatchRepo;

    /**
     * Creates a new dispatch configuration.
     *
     * @param dispatch the dispatch configuration to create
     * @return the created configuration
     */
    @PostMapping
    public Dispatch createDispatch(@RequestBody Dispatch dispatch) {
        Dispatch savedDispatch = dispatchRepo.save(dispatch);
        return savedDispatch;
    }

    /**
     * Retrieves all dispatch configurations.
     *
     * @return the list of configurations
     */
    @GetMapping
    public List<Dispatch> getAllDispatches() {
        return dispatchRepo.findAll();
    }

    /**
     * Manually triggers the dispatch process for a specific configuration.
     *
     * @param id the ID of the configuration to dispatch
     */
    @PostMapping("/dispatch/{id}")
    public void manualDispatch(@PathVariable Long id) {
        dispatchService.executeDispatch(id);
    }
}
