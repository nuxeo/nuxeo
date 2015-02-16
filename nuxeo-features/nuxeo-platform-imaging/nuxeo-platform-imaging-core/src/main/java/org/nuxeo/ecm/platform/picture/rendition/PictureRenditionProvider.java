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

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Provides a rendition based on a picture view (referenced through the rendition definition name).
 *
 * @since 7.2
 */
public class PictureRenditionProvider implements RenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition definition) {
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        return picture != null && picture.getPictureFromTitle(definition.getName()) != null;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) throws RenditionException {
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        if (picture == null) {
            return Collections.emptyList();
        }

        Blob blob = picture.getPictureFromTitle(definition.getName());
        return blob != null ? Collections.singletonList(blob) : Collections.emptyList();
    }
}
