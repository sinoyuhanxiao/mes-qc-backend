package com.fps.svmes.services;

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
}
