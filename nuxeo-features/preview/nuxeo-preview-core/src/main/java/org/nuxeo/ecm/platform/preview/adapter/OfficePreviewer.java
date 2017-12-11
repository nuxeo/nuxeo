/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */

package org.nuxeo.ecm.platform.preview.adapter;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.10
 */
public class OfficePreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm) {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder result = conversionService.convert("any2pdf", dm.getAdapter(BlobHolder.class), null);
        PdfPreviewer pdfPreviewer = new PdfPreviewer();
        return pdfPreviewer.getPreview(result.getBlob(), dm);
    }

}
