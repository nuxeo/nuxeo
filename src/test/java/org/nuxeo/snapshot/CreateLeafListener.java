package org.nuxeo.snapshot;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import static org.nuxeo.snapshot.Snapshotable.ABOUT_TO_CREATE_LEAF_VERSION_EVENT;
import static org.nuxeo.snapshot.Snapshotable.ROOT_DOCUMENT_PROPERTY;

public class CreateLeafListener implements EventListener {

    public static final String DO_NOT_CHANGE_CHILD_FLAG = "hold";

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!event.getName().equals(ABOUT_TO_CREATE_LEAF_VERSION_EVENT)) {
            return;
        }

        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel source = ctx.getSourceDocument();
        DocumentModel root = (DocumentModel) ctx.getProperty(ROOT_DOCUMENT_PROPERTY);

        String rootDescription = (String) root.getPropertyValue("dc:description");
        if (StringUtils.isEmpty(rootDescription) || !rootDescription.contains(DO_NOT_CHANGE_CHILD_FLAG)) {
            source.setPropertyValue("dc:description", "XOXO");
        }
    }
}
