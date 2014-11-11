package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

public class SpacesAdapterFactory implements DocumentAdapterFactory {

    @SuppressWarnings("unchecked")
    public Object getAdapter(DocumentModel doc, Class itf) {
        SpacesAdapterComponent comp = (SpacesAdapterComponent) Framework.getRuntime().getComponent(
                SpacesAdapterComponent.NAME);
        return comp.getAdapter(doc, itf);
    }

}
