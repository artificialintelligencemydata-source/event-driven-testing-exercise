package com.acuver.autwit.internal.config;

public final class FileReaderManager {

    private static final FileReaderManager INSTANCE = new FileReaderManager();
    private volatile ConfigFileReader configFileReader;

    private FileReaderManager(){}

    public static FileReaderManager getInstance(){ return INSTANCE; }

    public ConfigFileReader getConfigReader(){
        if (configFileReader == null) {
            synchronized (this) {
                if (configFileReader == null) {
                    configFileReader = new ConfigFileReader(null);
                }
            }
        }
        return configFileReader;
    }
}
