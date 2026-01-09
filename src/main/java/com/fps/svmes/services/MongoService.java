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

    /**
     * Replaces one document in a MongoDB collection (upsert if not exists).
     *
     * @param collectionName Name of the collection
     * @param filter Filter to find the document
     * @param newDoc The new document to replace with
     */
    void replaceOne(String collectionName, Document filter, Document newDoc);

    /**
     * Deletes one document from a MongoDB collection.
     *
     * @param collectionName Name of the collection
     * @param filter Filter to find the document to delete
     */
    void deleteOne(String collectionName, Document filter);

}
