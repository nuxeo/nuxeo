package org.nuxeo.ecm.webdav.provider;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;

import java.io.Closeable;
import java.io.IOException;

public class CoreSessionWrapper implements Closeable {

    private final CoreSession session;

    public CoreSessionWrapper(CoreSession session) {
        this.session = session;
    }

    @Override
    public void close() throws IOException {
        CoreInstance.getInstance().close(session);
        Util.endTransaction();
    }

}
