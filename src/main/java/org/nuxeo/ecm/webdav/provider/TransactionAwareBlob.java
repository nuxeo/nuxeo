package org.nuxeo.ecm.webdav.provider;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;

public class TransactionAwareBlob {

    protected final Blob blob;

    protected final CoreSession session;

    protected boolean releaseSession = true;

    public TransactionAwareBlob(CoreSession session, Blob blob) {
        this.blob = blob;
        this.session = session;
    }

    public TransactionAwareBlob(CoreSession session, Blob blob, boolean releaseSession) {
        this.blob = blob;
        this.session = session;
        this.releaseSession = releaseSession;
    }

    public Blob getBlob() {
        return blob;
    }

    public void commitOrRollback() {
        if (session != null && releaseSession) {
            CoreInstance.getInstance().close(session);
        }
        Util.endTransaction();
    }

    public long getLength() {
        if (blob != null) {
            return blob.getLength();
        }
        return 0L;
    }

}