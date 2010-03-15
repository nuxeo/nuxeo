package org.nuxeo.ecm.platform.video.listener;

import java.io.IOException;

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
 * Core event listener to compute / update the story board of a Video document
 *
 * @author ogrisel
 */
public class VideoPreviewListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(VideoConstants.HAS_VIDEO_PREVIEW_FACET)) {
            Property origVideoProperty = doc.getProperty("file:content");
            if (origVideoProperty.isDirty()) {
                try {
                    VideoHelper.updatePreviews(doc,
                            origVideoProperty.getValue(Blob.class));
                } catch (IOException e) {
                    throw ClientException.wrap(e);
                }
            }
        }
    }
}
