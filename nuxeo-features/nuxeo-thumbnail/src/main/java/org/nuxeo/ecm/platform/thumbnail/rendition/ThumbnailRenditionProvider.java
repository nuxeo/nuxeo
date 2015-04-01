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

package org.nuxeo.ecm.platform.thumbnail.rendition;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class ThumbnailRenditionProvider implements RenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition definition) {
        return true;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) throws RenditionException {
        ThumbnailService thumbnailService = Framework.getService(ThumbnailService.class);
        Blob blob = thumbnailService.getThumbnail(doc, doc.getCoreSession());
        if (blob != null) {
            return Collections.singletonList(blob);
        }

        return Collections.emptyList();
    }
}
