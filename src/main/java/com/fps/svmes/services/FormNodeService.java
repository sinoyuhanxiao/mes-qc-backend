package com.fps.svmes.services;

import com.fps.svmes.models.nosql.FormNode;

import java.util.List;
import java.util.Optional;

public interface FormNodeService {
    FormNode saveNode(FormNode node);
    List<FormNode> getAllNodes();
    Optional<FormNode> getNodeByIdOrUuid(String id);
    boolean deleteNodeByIdOrUuid(String id);
}
