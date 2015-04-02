/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.rendition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinitionProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides rendition definitions based on the existing picture views.
 *
 * @since 7.2
 */
public class PictureRenditionDefinitionProvider implements RenditionDefinitionProvider {

    public static final String PICTURE_RENDITION_KIND = "nuxeo:picture:conversion";

    @Override
    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        MultiviewPicture multiviewPicture = doc.getAdapter(MultiviewPicture.class);
        if (multiviewPicture == null) {
            return Collections.emptyList();
        }

        List<RenditionDefinition> renditionDefinitions = new ArrayList<>();
        ImagingService imagingService = Framework.getService(ImagingService.class);
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        for (PictureView pictureView : multiviewPicture.getViews()) {
            PictureConversion pictureConversion = imagingService.getPictureConversion(pictureView.getTitle());
            if (pictureConversion != null && pictureConversion.isRendition()) {
                Blob blob = pictureView.getBlob();
                if (blob != null) {
                    RenditionDefinition renditionDefinition = new RenditionDefinition();
                    renditionDefinition.setEnabled(true);
                    renditionDefinition.setName(pictureView.getTitle());
                    renditionDefinition.setKind(PICTURE_RENDITION_KIND);
                    renditionDefinition.setProvider(new PictureRenditionProvider());
                    renditionDefinition.setVisible(pictureConversion.isRenditionVisible());
                    renditionDefinition.setLabel(pictureView.getTitle());
                    MimetypeEntry mimeType = mimetypeRegistry.getMimetypeEntryByMimeType(blob.getMimeType());
                    renditionDefinition.setIcon("/icons/" + mimeType.getIconPath());
                    renditionDefinitions.add(renditionDefinition);
                }
            }
        }
        return renditionDefinitions;
    }

}
