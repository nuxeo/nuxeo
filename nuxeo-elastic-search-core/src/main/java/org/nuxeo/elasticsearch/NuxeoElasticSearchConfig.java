package org.nuxeo.elasticsearch;

public class NuxeoElasticSearchConfig {

    protected boolean local;

    protected String dataPath;

    protected String logPath;

    protected String indexStorageType = "memory";

    protected boolean enableHttp;

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getIndexStorageType() {
        return indexStorageType;
    }

    public void setIndexStorageType(String indexStorageType) {
        this.indexStorageType = indexStorageType;
    }

    public boolean enableHttp() {
        return enableHttp;
    }

    public void enableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
    }


}
