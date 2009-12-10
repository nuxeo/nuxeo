package org.nuxeo.ecm.platform.wss.backend;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface WSSListItemFactory {

    NuxeoListItem createItem(DocumentModel doc, String corePathPrefix, String urlRoot);

}
