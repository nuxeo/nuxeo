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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.preview.io.PreviewJsonEnricher;
import org.nuxeo.runtime.api.Framework;

/**
 * This content enricher adds a document Preview URL.
 *
 * @since 6.0
 * @deprecated This enricher was migrated to {@link PreviewJsonEnricher}. The content enricher service doesn't work
 *             anymore.
 */
@Deprecated
public class PreviewContentEnricher extends AbstractContentEnricher {

    public static final String PREVIEW_URL_LABEL = "url";

    public static final String PREVIEW_CONTENT_ID = "preview";

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
        DocumentModel doc = ec.getDocumentModel();
        String relativeUrl = PreviewHelper.getPreviewURL(doc);
        jg.writeStartObject();
        if (relativeUrl != null && !relativeUrl.isEmpty()) {
            String url = Framework.getProperty("nuxeo.url") + "/" + PreviewHelper.getPreviewURL(doc);
            jg.writeStringField(PREVIEW_URL_LABEL, url);
        } else {
            writeEmptyURL(jg);
        }
        jg.writeEndObject();
        jg.flush();
    }

    private void writeEmptyURL(JsonGenerator jg) throws IOException {
        jg.writeStringField(PREVIEW_URL_LABEL, null);
    }

}
