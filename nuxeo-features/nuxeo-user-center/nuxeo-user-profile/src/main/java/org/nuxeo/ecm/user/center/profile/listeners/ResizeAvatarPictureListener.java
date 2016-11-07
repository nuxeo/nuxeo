/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     rlegall
 */
package org.nuxeo.ecm.user.center.profile.listeners;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_FACET;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author rlegall Listener to handle the maximum dimensions of the avatar picture. This listener is triggered on
 *         DocumentEventTypes.BEFORE_DOC_UPDATE events. It verifies if the picture width is above 300 pixels and its
 *         height above 200. In that case, the picture saved in the rich profile is a resized version of it which fits
 *         those constrains.
 */
public class ResizeAvatarPictureListener implements EventListener {

    protected static final int RESIZED_IMAGE_WIDTH = 300;

    protected static final int RESIZED_IMAGE_HEIGHT = 200;

    private static final Log log = getLog(ResizeAvatarPictureListener.class);

    @Override
    public void handleEvent(Event event) {

        if (isBeforeUpdate(event)) {

            DocumentEventContext ctx = (DocumentEventContext) event.getContext();
            DocumentModel doc = ctx.getSourceDocument();

            if (doc.hasFacet(USER_PROFILE_FACET)) {
                Blob image = (Blob) doc.getPropertyValue(USER_PROFILE_AVATAR_FIELD);
                if (image != null) {
                    resizeAvatar(doc, image);
                }
            }
        }
    }

    protected boolean isBeforeUpdate(Event event) {
        return BEFORE_DOC_UPDATE.equals(event.getName()) && (event.getContext() instanceof DocumentEventContext);
    }

    protected void resizeAvatar(DocumentModel doc, Blob avatarImage) throws PropertyException {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder bh = new SimpleBlobHolder(avatarImage);
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("targetWidth", String.valueOf(RESIZED_IMAGE_WIDTH));
        parameters.put("targetHeight", String.valueOf(RESIZED_IMAGE_HEIGHT));

        try {
            BlobHolder result = conversionService.convert("resizeAvatar", bh, parameters);
            if (result != null) {
                doc.setPropertyValue(USER_PROFILE_AVATAR_FIELD, (Serializable) result.getBlob());
            }
        } catch (NuxeoException e) {
            log.warn("Unable to resize avatar image");
            log.debug(e, e);
        }

    }

}
