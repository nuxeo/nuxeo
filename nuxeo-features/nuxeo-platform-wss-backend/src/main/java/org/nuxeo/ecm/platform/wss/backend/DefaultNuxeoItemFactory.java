package org.nuxeo.ecm.platform.wss.backend;

import org.nuxeo.ecm.core.api.DocumentModel;

public class DefaultNuxeoItemFactory implements WSSListItemFactory {

    public NuxeoListItem createItem(DocumentModel doc,
            String corePathPrefix, String urlRoot) {
        return new NuxeoListItem(doc,corePathPrefix,urlRoot);
    }

}
