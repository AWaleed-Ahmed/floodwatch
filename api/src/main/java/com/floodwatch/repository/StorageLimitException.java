/*
 * File:    StorageLimitException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that a data file would grow beyond the size limit.
 */


package com.floodwatch.repository;


/**
 * Thrown when saving would make a data file larger than the size limit. Nothing is written,
 * and the in-memory copy stays as it was.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class StorageLimitException extends RuntimeException {


    /**
     * Creates the exception.
     *
     * @param fileName name of the file that would be too large
     * @param maxBytes the size limit in bytes
     */


    public StorageLimitException
    (
        String fileName,
        long maxBytes
    ) {
        super("Data file " + fileName + " would be larger than the " + maxBytes / (1024 * 1024) + " MB limit. Remove old records first.");
    }
}