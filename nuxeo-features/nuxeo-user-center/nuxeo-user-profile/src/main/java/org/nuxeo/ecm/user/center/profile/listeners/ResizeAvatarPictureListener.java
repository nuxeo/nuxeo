/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rlegall
 */
package org.nuxeo.ecm.user.center.profile.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_FACET;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
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
        ImagingService service = Framework.getService(ImagingService.class);
        ImageInfo info = service.getImageInfo(avatarImage);
        int width = info.getWidth();
        int height = info.getHeight();
        float wScale = (float) RESIZED_IMAGE_WIDTH / width;
        float hscale = (float) RESIZED_IMAGE_HEIGHT / height;
        float scale = Math.min(wScale, hscale);

        if (scale < 1) {
            avatarImage = service.resize(avatarImage, "jpg", (int) (width * scale), (int) (height * scale),
                    info.getDepth());
            avatarImage.setMimeType("image/jpeg");// XXX : Should be automatic
            doc.setPropertyValue(USER_PROFILE_AVATAR_FIELD, (Serializable) avatarImage);
        }
    }

}
