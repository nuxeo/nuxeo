/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
package org.nuxeo.ecm.platform.video.extension;

import java.util.Calendar;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.media.streaming.MediaStreamingService;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.media.streaming.MediaStreamingConstants.STREAM_MEDIA_FIELD;

/**
 * @author "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 *
 */
@Name("videoActions")
@Install(precedence = Install.FRAMEWORK)
public class VideoActionsBean {

    protected MediaStreamingService mediaStreamingService;

    protected MediaStreamingService getMediaStreamingService() throws Exception {
        if (mediaStreamingService == null) {
            mediaStreamingService = Framework.getService(MediaStreamingService.class);
        }

        return mediaStreamingService;
    }

    public String getURLForPlayer(DocumentModel videoDoc)
            throws PropertyException, ClientException, Exception {
        if (isDocumentStreamable(videoDoc) && isStreamReady(videoDoc)) {
            return getUrlForStreamingPreview(videoDoc);
        }

        return DocumentModelFunctions.bigFileUrl(videoDoc, "file:content",
                "file:filename");
    }

    public String getURLForStaticPreview(DocumentModel videoDoc)
            throws PropertyException, ClientException {
        String lastModification = "" + (((Calendar) videoDoc.getPropertyValue("dc:modified")).getTimeInMillis());
        String result = DocumentModelFunctions.fileUrl("downloadPicture",
                videoDoc, "StaticPlayerView:content",
                lastModification);

        return result;
    }


    public String getUrlForStreamingPreview(DocumentModel doc)
            throws ClientException, Exception {
        return getMediaStreamingService().getStreamURLFromDocumentModel(doc);
    }

    public boolean isPreviewReady(DocumentModel videoDoc)
            throws PropertyException, ClientException, Exception {
        return isDocumentStreamable(videoDoc) && isStreamReady(videoDoc);
    }

    public boolean isDocumentStreamable(DocumentModel doc) throws Exception {
        boolean isStreamingServerActivated = getMediaStreamingService().isServiceActivated();
        boolean isCurrentDocumentStreamable = getMediaStreamingService().isStreamableMedia(
                doc);

        return isStreamingServerActivated && isCurrentDocumentStreamable;
    }

    public boolean isStreamReady(DocumentModel doc) throws PropertyException,
            ClientException {
        return doc.getPropertyValue(STREAM_MEDIA_FIELD) != null;
    }
}
