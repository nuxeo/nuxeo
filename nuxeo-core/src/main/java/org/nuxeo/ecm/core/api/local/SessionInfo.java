package org.nuxeo.ecm.core.api.local;

import org.nuxeo.ecm.core.model.Session;

public final class SessionInfo {
    final Session session;

    Exception openException;

    public SessionInfo(Session session) {
        this.session = session;
        openException = new Exception("Open stack trace");
    }
}