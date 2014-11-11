/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.picture.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTUREBOOK_TYPE_NAME;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.UPDATE_PICTURE_VIEW_EVENT;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener updating pre-filling the views of a Picture if the main Blob has
 * changed.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class PictureChangedListener implements EventListener {

    public static final String EMPTY_PICTURE_PATH = "nuxeo.war/img/empty_picture.png";

    private static final Log log = LogFactory.getLog(PictureChangedListener.class);

    private static ImageInfo emptyPictureImageInfo;

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(PICTURE_FACET) && !doc.isProxy()) {
            Property fileProp = doc.getProperty("file:content");
            if (DOCUMENT_CREATED.equals(event.getName()) || fileProp.isDirty()) {
                Property viewsProp = doc.getProperty(AbstractPictureAdapter.VIEWS_PROPERTY);
                // if the views are dirty, assume they're up to date
                if (viewsProp == null || !viewsProp.isDirty()) {
                    preFillPictureViews(docCtx.getCoreSession(), doc);
                    // mark the document as needing picture views generation
                    Event trigger = docCtx.newEvent(UPDATE_PICTURE_VIEW_EVENT);
                    EventService eventService = Framework.getLocalService(EventService.class);
                    eventService.fireEvent(trigger);
                }
            }
        }
    }

    protected void preFillPictureViews(CoreSession session, DocumentModel doc) {
        try {
            URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(
                    getEmptyPicturePath());
            if (fileUrl == null) {
                return;
            }

            Blob blob = new FileBlob(FileUtils.getFileFromURL(fileUrl));
            MimetypeRegistry mimetypeRegistry = Framework.getLocalService(MimetypeRegistry.class);
            String mimeType = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                    blob.getFilename(), blob, null);
            blob.setMimeType(mimeType);

            DocumentModel parentDoc = getParentDocument(session, doc);

            List<Map<String, Object>> pictureTemplates = null;
            if (parentDoc != null
                    && PICTUREBOOK_TYPE_NAME.equals(parentDoc.getType())) {
                // use PictureBook Properties
                pictureTemplates = (ArrayList<Map<String, Object>>) parentDoc.getPropertyValue("picturebook:picturetemplates");
                if (pictureTemplates.isEmpty()) {
                    pictureTemplates = null;
                }
            }

            if (emptyPictureImageInfo == null) {
                ImagingService imagingService = Framework.getLocalService(ImagingService.class);
                emptyPictureImageInfo = imagingService.getImageInfo(blob);
            }

            PictureResourceAdapter adapter = doc.getAdapter(PictureResourceAdapter.class);
            adapter.preFillPictureViews(blob, pictureTemplates,
                    emptyPictureImageInfo);
        } catch (Exception e) {
            log.debug(e, e);
            log.error("Error while pre-filling picture views: "
                    + e.getMessage());
        }
    }

    protected String getEmptyPicturePath() {
        return EMPTY_PICTURE_PATH;
    }

    protected DocumentModel getParentDocument(CoreSession session,
            DocumentModel doc) throws ClientException {
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
