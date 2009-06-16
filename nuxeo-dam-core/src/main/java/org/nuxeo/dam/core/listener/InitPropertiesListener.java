package org.nuxeo.dam.core.listener;

import org.nuxeo.dam.api.Constants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class InitPropertiesListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession coreSession = docCtx.getCoreSession();

            if (doc.hasSchema(Constants.DAM_COMMON_SCHEMA)
                    && !Constants.IMPORT_SET_TYPE.equals(doc.getType())) {
                DocumentModel parent = coreSession.getDocument(doc.getParentRef());
                DocumentModel importSet = docCtx.getCoreSession().getSuperSpace(
                        parent);

                // Override document dam_common DM with the importSet's one
                doc.getDataModel(Constants.DAM_COMMON_SCHEMA).setMap(
                        importSet.getDataModel(Constants.DAM_COMMON_SCHEMA).getMap());
            }
        }
    }

}
