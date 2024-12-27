package com.fps.svmes.models.nosql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "qc-form-tree")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormNode {

    @Id
    private String id;

    @Field("label")
    private String label;

    @Field("node_type")
    private String nodeType;

    @Field("qc_form_template_id")
    private Long qcFormTemplateId;

    @Field("children")
    private List<FormNode> children;

}
