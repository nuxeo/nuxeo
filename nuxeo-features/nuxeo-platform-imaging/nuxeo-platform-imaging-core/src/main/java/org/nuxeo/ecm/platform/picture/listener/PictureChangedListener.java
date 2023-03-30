/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.picture.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CHECKIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.CTX_FORCE_VIEWS_GENERATION;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTUREBOOK_TYPE_NAME;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener updating pre-filling the views of a Picture if the main Blob has changed.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class PictureChangedListener implements EventListener {

    public static final String EMPTY_PICTURE_PATH = "nuxeo.war/img/empty_picture.png";

    private static final Log log = LogFactory.getLog(PictureChangedListener.class);

    private static ImageInfo emptyPictureImageInfo;

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(PICTURE_FACET) && !doc.isProxy()) {
            if (triggersPictureViewsGeneration(event, doc)) {
                preFillPictureViews(docCtx.getCoreSession(), doc);
            } else {
                docCtx.setProperty(PictureViewsGenerationListener.DISABLE_PICTURE_VIEWS_GENERATION_LISTENER, true);
            }
        }
    }

    protected boolean triggersPictureViewsGeneration(Event event, DocumentModel doc) {
        Property fileProp = doc.getProperty("file:content");
        Property viewsProp = doc.getProperty(AbstractPictureAdapter.VIEWS_PROPERTY);

        boolean forceGeneration = Boolean.TRUE.equals(doc.getContextData(CTX_FORCE_VIEWS_GENERATION));
        boolean emptyPictureViews = viewsProp.size() == 0;
        boolean emptyOrNotDirtyPictureViews = !viewsProp.isDirty() || emptyPictureViews;
        boolean fileChanged = ABOUT_TO_CREATE.equals(event.getName()) || fileProp.isDirty();
        boolean aboutToCheckIn = ABOUT_TO_CHECKIN.equals(event.getName());

        return forceGeneration || (emptyOrNotDirtyPictureViews && fileChanged) || (emptyPictureViews && aboutToCheckIn);
    }

    protected void preFillPictureViews(CoreSession session, DocumentModel doc) {
        PictureResourceAdapter adapter = doc.getAdapter(PictureResourceAdapter.class);
        adapter.clearInfo();
        try {
            URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(getEmptyPicturePath());
            if (fileUrl == null) {
                return;
            }

            Blob blob = Blobs.createBlob(FileUtils.getFileFromURL(fileUrl));
            MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
            String mimeType = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(blob.getFilename(), blob,
                    null);
            blob.setMimeType(mimeType);

            DocumentModel parentDoc = getParentDocument(session, doc);

            List<Map<String, Object>> pictureConversions = null;
            if (parentDoc != null && PICTUREBOOK_TYPE_NAME.equals(parentDoc.getType())) {
                // use PictureBook Properties
                pictureConversions = (ArrayList<Map<String, Object>>) parentDoc.getPropertyValue(
                        "picturebook:picturetemplates");
                if (pictureConversions.isEmpty()) {
                    pictureConversions = null;
                }
            }

            if (emptyPictureImageInfo == null) {
                ImagingService imagingService = Framework.getService(ImagingService.class);
                emptyPictureImageInfo = imagingService.getImageInfo(blob);
            }

            adapter.preFillPictureViews(blob, pictureConversions, emptyPictureImageInfo);
        } catch (IOException e) {
            log.error("Error while pre-filling picture views: " + e.getMessage(), e);
        }
    }

    protected String getEmptyPicturePath() {
        return EMPTY_PICTURE_PATH;
    }

    protected DocumentModel getParentDocument(CoreSession session, DocumentModel doc) {
        DocumentModel parent;
        if (session.exists(doc.getRef())) {
            parent = session.getParentDocument(doc.getRef());
        } else {
            Path parentPath = doc.getPath().removeLastSegments(1);
            parent = session.getDocument(new PathRef(parentPath.toString()));
        }
        return parent;
    }

}
