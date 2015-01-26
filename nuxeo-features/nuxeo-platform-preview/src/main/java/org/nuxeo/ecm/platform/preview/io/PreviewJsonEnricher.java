/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.preview.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s preview url as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers:document=preview is present.
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
