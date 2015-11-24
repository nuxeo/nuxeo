package org.nuxeo.opensocial.container.server.layout;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * @author Stéphane Fourrier
 */
public class YUILayoutAdapterFactory implements DocumentAdapterFactory {

    public static final String YUILAYOUT_SCHEMA = "yuilayout";
    @SuppressWarnings("unchecked")
    public Object getAdapter(DocumentModel doc, Class itf) {
        if (doc.hasSchema(YUILAYOUT_SCHEMA)) {
            return new YUILayoutAdapterImpl(doc);
        }
        return null;
    }

}
