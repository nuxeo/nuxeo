/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin (aka matic)
 */

package org.nuxeo.ecm.core.storage.sql.net;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides info about a client connection. Mainly used by the management for reporting.
 * Each invokers hold a client info. Client infos are created and updated by the servlet mapper.
 *
 * @author matic
 *
 */
public class MapperClientInfo {

    protected final String remoteIP;
    protected final String remoteUser;

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
