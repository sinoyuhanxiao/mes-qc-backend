package com.fps.svmes.dto.requests;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import lombok.Data;

import java.util.List;

@Data
public class TemplateFormRequest {
    private QcFormTemplateDTO form; // The form to be created
    private List<String> parentFolderIds; // List of parent folder IDs to add nodes under
}
