package org.nuxeo.template.context;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.rendering.fm.adapters.SchemaTemplate;

public class SimpleSchemaWrapper {

    private final DocumentModel doc;

    private final String schemaName;

    public SimpleSchemaWrapper(SchemaTemplate.DocumentSchema schema) {
        this.doc = schema.doc;
        this.schemaName = schema.schemaName;
    }

    public Object get(String name) {
        try {
            if (doc.isPrefetched(schemaName, name)) {
                // simple value already available, don't load DocumentPart
                return doc.getProperty(schemaName, name);
            } else {
                // use normal Property lookup in Part
                return wrap(doc.getPart(schemaName).get(name));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Object wrap(Property prop) throws Exception {
        if (prop == null || prop.getValue() == null) {
            return null;
        }
        return prop.getValue();
    }

}
