/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.rendition;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
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
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        if (picture == null) {
            return Collections.emptyList();
        }

        Blob blob = picture.getPictureFromTitle(definition.getName());
        return blob != null ? Collections.singletonList(blob) : Collections.emptyList();
    }
}
