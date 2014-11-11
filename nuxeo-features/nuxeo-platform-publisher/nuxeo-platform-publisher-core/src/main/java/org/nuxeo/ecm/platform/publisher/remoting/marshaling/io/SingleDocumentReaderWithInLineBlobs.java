package org.nuxeo.ecm.platform.publisher.remoting.marshaling.io;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelReader;

import java.io.IOException;

/**
 * 
 * {@link DocumentModelReader} implementation that uses inline blobs
 * 
 * @author tiry
 * 
 */
public class SingleDocumentReaderWithInLineBlobs extends DocumentModelReader {

    protected DocumentModel doc;

    private boolean readDone = false;

    public SingleDocumentReaderWithInLineBlobs(CoreSession session,
            DocumentModel doc) {
        super(session);
        this.doc = doc;
    }

    @Override
    public void close() {
        super.close();
        session = null;
        doc = null;
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (doc != null) {
            if (readDone) {
                return null;
            } else {
                readDone = true;
                return new ExportedDocumentImpl(doc, true);
            }
        }
        doc = null;
        return null;
    }
}
