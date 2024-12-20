package com.fps.svmes.services.impl;

import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.FormNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FormNodeServiceImpl implements FormNodeService {

    @Autowired
    private FormNodeRepository repository;

    // Save a top-level node (new document)
    @Override
    public FormNode saveNode(FormNode node) {
        return repository.save(node);
    }

    // Get all top-level nodes
    @Override
    public List<FormNode> getAllNodes() {
        return repository.findAll();
    }

    // Add a child node to an existing node
    @Override
    public Optional<FormNode> addChildNode(String parentId, FormNode childNode) {
        List<FormNode> nodes = repository.findAll();
        for (FormNode node : nodes) {
            Optional<FormNode> createdChild = addChildNodeRecursively(node, parentId, childNode);
            if (createdChild.isPresent()) {
                repository.save(node); // Save the updated top-level node
                return createdChild;   // Return the newly created child node
            }
        }
        return Optional.empty();
    }

    // Get a node by ID or UUID (traverse if necessary)
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

    // Delete a node by ID or UUID (traverse if necessary)
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

    // Recursive method to add a child node to a node with the given parentId
    private Optional<FormNode> addChildNodeRecursively(FormNode currentNode, String parentId, FormNode childNode) {
        if (currentNode.getId().equals(parentId)) {
            if (currentNode.getChildren() == null) {
                currentNode.setChildren(new ArrayList<>());
            }
            // Assign a UUID to the child node if it doesn't already have an ID
            if (childNode.getId() == null) {
                childNode.setId(UUID.randomUUID().toString());
            }
            currentNode.getChildren().add(childNode);
            return Optional.of(childNode); // Return the newly added child node
        }
        if (currentNode.getChildren() != null) {
            for (FormNode child : currentNode.getChildren()) {
                Optional<FormNode> result = addChildNodeRecursively(child, parentId, childNode);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }
}
