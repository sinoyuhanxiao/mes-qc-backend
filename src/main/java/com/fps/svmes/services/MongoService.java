package com.fps.svmes.services;

import org.bson.Document;

public interface MongoService {

    /**
     * Creates a MongoDB collection if it does not already exist.
     *
     * @param collectionName Name of the collection to create
     */
    void createCollection(String collectionName);

    /**
     * Checks if a MongoDB collection exists.
     *
     * @param collectionName Name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    boolean collectionExists(String collectionName);

    /**
     * Inserts one document into a MongoDB collection.
     *
     * @param collectionName Name of the collection
     * @param document The document to insert
     */
    void insertOne(String collectionName, org.bson.Document document);


    void replaceOne(String collectionName, Document filter, Document newDoc);

}
