/*
 * File:    StartupDataCheck.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Reads every data file at startup and refuses to start on a damaged file.
 */


package com.floodwatch.repository;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * Reads every data file once at startup. A damaged file stops the application with a clear message,
 * so it is never served or overwritten; storage that cannot be reached only logs a warning, because
 * the files are read again on the first request.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class StartupDataCheck implements ApplicationRunner {


    private static final Logger LOG = LoggerFactory.getLogger(StartupDataCheck.class);

    private final List<JsonFileRepository<?>> repositories;

    /**
     * Creates the check.
     *
     * @param repositories every JSON-file repository in the application
     */


    public StartupDataCheck
    (
        List<JsonFileRepository<?>> repositories
    ) {
        this.repositories = List.copyOf(repositories);
    }

    /**
     * Runs the check after the application has started.
     *
     * @param args application arguments, not used
     * @throws CorruptDataException when any data file is damaged
     */


    @Override
    public void run
    (
        ApplicationArguments args
    ) {
        checkAll();
    }

    /**
     * Loads every repository once.
     *
     * @return how many files were read successfully
     * @throws CorruptDataException when any data file is damaged
     */


    int checkAll() {
        int readable = 0;
        for (JsonFileRepository<?> repository : repositories)
        {
            try
            {
                repository.findAll();
                readable++;
            }
            catch (CorruptDataException e)
            {
                LOG.error(e.getMessage());
                throw e;
            }
            catch (RuntimeException e)
            {
                LOG.warn("Could not read {} at startup ({}); it will be read again on first use.", repository.fileName(), e.getMessage());
            }
        }
        LOG.info("Startup data check: {} of {} data files readable", readable, repositories.size());
        return readable;
    }
}