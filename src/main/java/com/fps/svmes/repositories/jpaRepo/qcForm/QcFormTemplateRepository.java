package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcFormTemplateRepository extends JpaRepository<QcFormTemplate, Long> {
    List<QcFormTemplate> findAllByStatus(Integer status);
}
