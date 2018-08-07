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

package org.nuxeo.ecm.platform.thumbnail.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.net.URLEncoder;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s thumbnail url as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers-document=thumbnail is present.
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
 *     "thumbnail": {
 *       "url": "THUMBNAIL_URL"
 *     },
 *   }
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ThumbnailJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "thumbnail";

    public static final String THUMBNAIL_URL_LABEL = "url";

    public static final String THUMBNAIL_URL_PATTERN = "%s/api/v1/repo/%s/id/%s/@rendition/thumbnail?"
            + CoreSession.CHANGE_TOKEN + "=%s";

    public ThumbnailJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartObject();
        jg.writeStringField(THUMBNAIL_URL_LABEL,
                String.format(THUMBNAIL_URL_PATTERN, ctx.getBaseUrl().replaceAll("/$", ""),
                        document.getRepositoryName(), document.getId(),
                        URLEncoder.encode(document.getChangeToken(), "UTF-8")));
        jg.writeEndObject();

    }
}
