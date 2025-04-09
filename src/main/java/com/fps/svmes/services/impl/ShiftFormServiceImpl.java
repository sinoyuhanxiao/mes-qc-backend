package com.fps.svmes.services.impl;

import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.models.sql.user.Shift;
import com.fps.svmes.models.sql.user.ShiftForm;
import com.fps.svmes.models.sql.user.ShiftFormId;
import com.fps.svmes.repositories.jpaRepo.user.ShiftFormRepository;
import com.fps.svmes.repositories.jpaRepo.user.ShiftRepository;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.ShiftFormService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftFormServiceImpl implements ShiftFormService {
    private final ShiftFormRepository shiftFormRepository;
    private final ShiftRepository shiftRepository;
    private final FormNodeRepository formNodeRepository;

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

    @Override
    public List<FormNode> getFormTreeByShiftId(Integer shiftId) {
        List<String> formIds = getFormIdsByShift(shiftId);
        List<FormNode> fullTree = formNodeRepository.findAll();

        List<FormNode> filteredTree = new ArrayList<>();
        for (FormNode root: fullTree) {
            FormNode filtered = filterTreeByFormIds(root, formIds);
            if (filtered != null) {
               filteredTree.add(filtered);
            }
        }

        return filteredTree;
    }

    private FormNode filterTreeByFormIds(FormNode node, List<String> allowedId) {
        if ("document".equalsIgnoreCase(node.getNodeType())) {
            return allowedId.contains(node.getId()) ? node : null;
        }

        List<FormNode> filteredChildren = new ArrayList<>();
        for (FormNode childNode: node.getChildren()) {
            FormNode filteredChild = filterTreeByFormIds(childNode, allowedId);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        if (!filteredChildren.isEmpty()) {
            FormNode newNode = new FormNode();
            newNode.setId(node.getId());
            newNode.setLabel(node.getLabel());
            newNode.setNodeType(node.getNodeType());
            newNode.setQcFormTemplateId(node.getQcFormTemplateId());
            newNode.setChildren(filteredChildren);
            return newNode;
        }

        return null;
    }
}
