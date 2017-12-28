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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.preview.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s preview url as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers-document=preview is present.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "preview": {
 *       "url": "PREVIEW_URL"
 *     },
 *   }
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class PreviewJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "preview";

    private static final String PREVIEW_URL_LABEL = "url";

    public PreviewJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        String relativeUrl = PreviewHelper.getPreviewURL(document);
        jg.writeFieldName(NAME);
        jg.writeStartObject();
        if (relativeUrl != null && !relativeUrl.isEmpty()) {
            String url = ctx.getBaseUrl() + PreviewHelper.getPreviewURL(document);
            jg.writeStringField(PREVIEW_URL_LABEL, url);
        } else {
            jg.writeNullField(PREVIEW_URL_LABEL);
        }
        jg.writeEndObject();
    }

}
