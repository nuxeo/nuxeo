package org.nuxeo.ecm.platform.video.listener;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.ecm.platform.video.VideoHelper;

/**
 * Core event listener to compute / update the storyboard of a Video document
 *
 * @author ogrisel
 */
public class VideoStoryboardListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(VideoConstants.HAS_STORYBOARD_FACET)) {
            Property origVideoProperty = doc.getProperty("file:content");
            if (origVideoProperty.isDirty()) {
                VideoHelper.updateStoryboard(doc,
                        origVideoProperty.getValue(Blob.class));
            }
        }
    }
}
