package org.nuxeo.ecm.automation.core.operations.business.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

/**
 * Nuxeo document model adapter for mapping
 * @since 5.7
 */
public class BeanBusinessAdapter {

    private static final Log log = LogFactory.getLog(BeanBusinessAdapter.class);

    protected final transient DocumentModel doc;

    public BeanBusinessAdapter() {
        this.doc = DocumentModelFactory.createDocumentModel("File");
    }

    public BeanBusinessAdapter(DocumentModel document) {
        this.doc = document;
    }

    public void save(CoreSession session) {
        try {
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            log.error("Cannot save document", e);
        }
    }

    @JsonIgnore
    public DocumentModel getDocument() {
        return doc;
    }

    public String getId() {
        return doc.getId();
    }

    public void setId(String id) {
        return;
    }

    public String getTitle() {
        try {
            return (String) doc.getPropertyValue("dc:title");
        } catch (ClientException e) {
            log.error("cannot get property title", e);
        }
        return null;
    }

    public void setTitle(String value) {
        try {
            doc.setPropertyValue("dc:title", value);
        } catch (ClientException e) {
            log.error("cannot set property title", e);
        }
    }

    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (ClientException e) {
            log.error("cannot get description property", e);
        }
        return null;
    }

    public void setDescription(String value) {
        try {
            doc.setPropertyValue("dc:description", value);
        } catch (ClientException e) {
            log.error("cannot set description property", e);
        }
    }

}
