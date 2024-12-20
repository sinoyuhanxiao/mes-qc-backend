package com.fps.svmes.services.impl;

import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.FormNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.LinkedList;

@Service
public class FormNodeServiceImpl implements FormNodeService {

    @Autowired
    private FormNodeRepository repository;

    @Override
    public FormNode saveNode(FormNode node) {
        return repository.save(node);
    }

    @Override
    public List<FormNode> getAllNodes() {
        return repository.findAll();
    }

    @Override
    public Optional<FormNode> getNodeByIdOrUuid(String id) {
        List<FormNode> nodes = repository.findAll();
        for (FormNode node : nodes) {
            Optional<FormNode> result = findNodeByIdOrUuid(node, id);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteNodeByIdOrUuid(String id) {
        List<FormNode> nodes = repository.findAll();
        for (FormNode node : nodes) {
            if (deleteNodeByIdOrUuid(node, id)) {
                repository.save(node);
                return true;
            }
        }
        return false;
    }

    // Recursive method to find a node by ID or UUID
    private Optional<FormNode> findNodeByIdOrUuid(FormNode currentNode, String id) {
        if (currentNode.getId().equals(id)) {
            return Optional.of(currentNode);
        }
        if (currentNode.getChildren() != null) {
            for (FormNode child : currentNode.getChildren()) {
                Optional<FormNode> result = findNodeByIdOrUuid(child, id);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    // Recursive method to delete a node by ID or UUID
    private boolean deleteNodeByIdOrUuid(FormNode currentNode, String id) {
        if (currentNode.getChildren() != null) {
            for (int i = 0; i < currentNode.getChildren().size(); i++) {
                if (currentNode.getChildren().get(i).getId().equals(id)) {
                    currentNode.getChildren().remove(i);
                    return true;
                }
                if (deleteNodeByIdOrUuid(currentNode.getChildren().get(i), id)) {
                    return true;
                }
            }
        }
        return false;
    }
}
