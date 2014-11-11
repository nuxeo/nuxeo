package org.nuxeo.ecm.core.model;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;


public class EmptyNameFixer implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        String name = (String)context.getProperty(CoreEventConstants.DESTINATION_NAME);
        if (name != null && name.length() > 0) {
            return;
        }
        context.setProperty(CoreEventConstants.DESTINATION_NAME, IdUtils.generateStringId());
    }


}
