package org.nuxeo.ecm.platform.thumbnail.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;

public class UpdateThumbnailCounter implements EventListener {

    protected static int count;

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext context = (DocumentEventContext)event.getContext();
        DocumentModel doc = context.getSourceDocument();
        Property prop = doc.getProperty(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME);
        if (prop.isDirty()) {
            count += 1;
        }
    }

}
