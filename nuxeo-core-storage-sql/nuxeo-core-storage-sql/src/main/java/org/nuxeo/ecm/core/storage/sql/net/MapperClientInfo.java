package org.nuxeo.ecm.core.storage.sql.net;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

public class MapperClientInfo {

    protected String remoteIP;
    protected String remoteUser;
    protected long requestCount;
    protected long lastRequestTime;

    protected MapperClientInfo(String remoteIP, String remoteUser) {
        this.remoteIP = remoteIP;
        this.remoteUser = remoteUser;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public long getLastRequestTime() {
        return lastRequestTime;
    }

    void handledRequest(HttpServletRequest request) {
        requestCount += 1;
        lastRequestTime = Calendar.getInstance().getTimeInMillis();
    }

}
