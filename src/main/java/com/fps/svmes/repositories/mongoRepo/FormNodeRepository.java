package com.fps.svmes.repositories.mongoRepo;

import com.fps.svmes.models.nosql.FormNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormNodeRepository extends MongoRepository<FormNode, String> {
}