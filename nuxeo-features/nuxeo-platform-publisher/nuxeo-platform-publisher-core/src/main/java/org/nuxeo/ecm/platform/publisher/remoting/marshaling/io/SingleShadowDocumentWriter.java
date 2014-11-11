package org.nuxeo.ecm.platform.publisher.remoting.marshaling.io;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;

import java.io.IOException;

/**
 * {@link DocumentModelWriter} that creates a shallow DocumentModel (ie: not
 * path and no uuid)
 * 
 * @author tiry
 * 
 */
public class SingleShadowDocumentWriter extends AbstractDocumentModelWriter {

    protected DocumentModel dm;

    public SingleShadowDocumentWriter(CoreSession session, String parentPath) {
        super(session, "/");
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc)
            throws IOException {

        try {
            dm = createDocument(doc, null);
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // keep location unchanged
        DocumentLocation oldLoc = doc.getSourceLocation();
        String oldServerName = oldLoc.getServerName();
        DocumentRef oldDocRef = oldLoc.getDocRef();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(
                oldServerName, oldServerName);
        map.put(oldDocRef, oldDocRef);
        return map;
    }

    @Override
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath)
            throws ClientException {
        String docType = xdoc.getType();
        dm = session.createDocumentModel(docType);
        // then load schemas data
        loadSchemas(xdoc, dm, xdoc.getDocument());
        return dm;
    }

    public DocumentModel getShadowDocument() {
        return dm;
    }
}
