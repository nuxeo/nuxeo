package org.nuxeo.ecm.automation.server.jaxrs.batch.handler;

public class BatchFileInfo {
    private String name;
    private String md5;
    private String key;
    private String mimeType;
    private long fileSize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public BatchFileInfo(String name, String md5, String key, String mimeType, long fileSize) {
        this.name = name;
        this.md5 = md5;
        this.key = key;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String md5;
        private String key;
        private String mimeType;
        private long fileSize;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withMd5(String md5) {
            this.md5 = md5;
            return this;
        }

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder withFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public BatchFileInfo build() {
            return new BatchFileInfo(name, md5, key, mimeType, fileSize);
        }
    }
}
