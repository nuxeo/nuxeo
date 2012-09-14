package org.nuxeo.ecm.csv;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class DefaultDocumentModelFactory implements DocumentModelFactory {

    @Override
    public void createDocument(CoreSession session, String parentPath,
            String name, String type, Map<String, Serializable> values) throws ClientException {
        DocumentModel doc = session.createDocumentModel(parentPath, name, type);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }
        session.createDocument(doc);
    }

    @Override
    public void updateDocument(CoreSession session, DocumentRef docRef,
            Map<String, Serializable> values) throws ClientException {
        DocumentModel doc = session.getDocument(docRef);
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }
        session.saveDocument(doc);
    }

}
