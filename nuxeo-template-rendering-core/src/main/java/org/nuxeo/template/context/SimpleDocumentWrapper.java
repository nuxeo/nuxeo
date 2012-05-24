package org.nuxeo.template.context;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DefaultDocumentView;
import org.nuxeo.ecm.platform.rendering.fm.adapters.SchemaTemplate;

public class SimpleDocumentWrapper {

    protected final DocumentModel doc;

    public SimpleDocumentWrapper(DocumentModel doc) {
        this.doc = doc;
    }

    public Object get(String key) {
        try {
            Object value = DefaultDocumentView.DEFAULT.get(doc, key);
            if (value != DefaultDocumentView.UNKNOWN) {
                return wrap(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object wrap(Object obj) throws Exception {
        if (obj instanceof SchemaTemplate.DocumentSchema) {
            return new SimpleSchemaWrapper((SchemaTemplate.DocumentSchema) obj);
        }
        return obj;
    }
}
