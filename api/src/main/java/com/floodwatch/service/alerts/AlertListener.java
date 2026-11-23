/*
 * File:    AlertListener.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Observer interface for components that must react to every newly raised alert.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.Alert;


/**
 * Observer of the alert subject. Every listener is told about every new alert, in the order they are registered.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public interface AlertListener {


    /**
     * Reacts to one newly raised alert.
     *
     * @param alert the alert that was just raised
     */


    void onAlert
    (
        Alert alert
    );
}