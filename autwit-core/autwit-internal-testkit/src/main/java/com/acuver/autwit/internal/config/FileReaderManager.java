package com.acuver.autwit.internal.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FileReaderManager - Singleton for ConfigFileReader access.
 *
 * <h2>LOCATION</h2>
 * Module: autwit-internal
 * Package: com.acuver.autwit.internal.config
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public class FileReaderManager {

    private static final Logger logger = LogManager.getLogger(FileReaderManager.class);

    private static FileReaderManager fileReaderManager = new FileReaderManager();
    private ConfigFileReader configFileReader;

    private FileReaderManager() {
    }

    public static FileReaderManager getInstance() {
        if (fileReaderManager == null) {
            fileReaderManager = new FileReaderManager();
        }
        return fileReaderManager;
    }

    public synchronized ConfigFileReader getConfigReader() {
        long threadId = Thread.currentThread().getId();
        configFileReader = new ConfigFileReader(threadId);
        return configFileReader;
    }
}