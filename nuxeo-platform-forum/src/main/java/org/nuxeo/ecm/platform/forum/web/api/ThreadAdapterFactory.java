package org.nuxeo.ecm.platform.forum.web.api;


import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class ThreadAdapterFactory implements DocumentAdapterFactory {

    public Object getAdapter(DocumentModel doc, Class itf) {
        if ("Thread".equals(doc.getType()))
        {
            return new ThreadAdapterImpl(doc);
        }
        return null;
    }

}
