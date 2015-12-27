/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.platform.ui.web.io.ThumbnailJsonEnricher;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.runtime.api.Framework;

/**
 * This contributor adds a document Thumbnail Download URL
 *
 * @since 5.9.5
 * @deprecated This enricher was migrated to {@link ThumbnailJsonEnricher}. The content enricher service doesn't work
 *             anymore.
 */
@Deprecated
public class ThumbnailContentEnricher extends AbstractContentEnricher {

    public static final String THUMBNAIL_URL_LABEL = "url";

    public static final String THUMBNAIL_CONTENT_ID = "thumbnail";

    public static final String THUMB_THUMBNAIL = "thumb:thumbnail";

    public static final String DOWNLOAD_THUMBNAIL = "downloadThumbnail";

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
        DocumentModel doc = ec.getDocumentModel();
        ThumbnailAdapter thumbnailAdapter = doc.getAdapter(ThumbnailAdapter.class);
        jg.writeStartObject();
        if (thumbnailAdapter != null) {
            Blob thumbnail = thumbnailAdapter.getThumbnail(doc.getCoreSession());
            if (thumbnail != null) {
                String url = DocumentModelFunctions.fileUrl(Framework.getProperty("nuxeo.url"), DOWNLOAD_THUMBNAIL, doc,
                        THUMB_THUMBNAIL, thumbnail.getFilename());
                jg.writeStringField(THUMBNAIL_URL_LABEL, url);
            } else {
                writeEmptyThumbnail(jg);
            }
        } else {
            writeEmptyThumbnail(jg);
        }
        jg.writeEndObject();
        jg.flush();
    }

    private void writeEmptyThumbnail(JsonGenerator jg) throws IOException {
        jg.writeStringField(THUMBNAIL_URL_LABEL, null);
    }

}
