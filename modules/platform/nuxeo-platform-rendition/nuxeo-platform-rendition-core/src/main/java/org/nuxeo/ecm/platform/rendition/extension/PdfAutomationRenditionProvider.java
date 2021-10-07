/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */

package org.nuxeo.ecm.platform.rendition.extension;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * Rendition provider for Pdf conversion which checks that we have a suitable converter given a source mime-type.
 *
 * @since 2021.10
 */
public class PdfAutomationRenditionProvider extends DefaultAutomationRenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        if (super.isAvailable(doc, def)) {
            ConversionService cs = Framework.getService(ConversionService.class);
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            return cs.getConverterName(bh.getBlob().getMimeType(), MimetypeRegistry.PDF_MIMETYPE) != null;
        }
        return false;
    }

}
