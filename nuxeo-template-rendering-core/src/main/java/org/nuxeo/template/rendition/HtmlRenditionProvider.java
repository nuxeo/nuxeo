/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.rendition;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

public class HtmlRenditionProvider implements RenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        if (holder != null) {
            return true;
        }
        return false;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        HtmlPreviewAdapter preview = doc.getAdapter(HtmlPreviewAdapter.class);
        return preview.getFilePreviewBlobs();
    }

}
