/*
 * File:    CorruptDataException.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Signals that a stored JSON data file cannot be read as valid data.
 */


package com.floodwatch.repository;


/**
 * Thrown when a stored JSON file exists but is not valid data for its entity. The file is never
 * replaced automatically, so no records are lost; an administrator must fix or restore it.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class CorruptDataException extends RuntimeException {


    private final String fileName;

    /**
     * Creates the exception.
     *
     * @param fileName name of the damaged file
     * @param cause    the parse error
     */


    public CorruptDataException
    (
        String fileName,
        Throwable cause
    ) {
        super("Data file " + fileName + " is damaged and could not be read. Fix or restore it; it was not changed.", cause);
        this.fileName = fileName;
    }

    /**
     * Returns the name of the damaged file.
     *
     * @return the file name
     */


    public String getFileName() {
        return fileName;
    }
}