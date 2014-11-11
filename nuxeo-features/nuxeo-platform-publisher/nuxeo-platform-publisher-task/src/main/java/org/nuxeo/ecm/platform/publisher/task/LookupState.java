package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface LookupState {

    boolean isPublished(DocumentModel doc, CoreSession session)
            throws ClientException;

}