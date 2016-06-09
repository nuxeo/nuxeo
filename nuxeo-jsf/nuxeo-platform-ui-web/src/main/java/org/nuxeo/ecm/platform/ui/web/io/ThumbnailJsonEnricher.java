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

package org.nuxeo.ecm.platform.ui.web.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

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

    public static final String THUMBNAIL_URL_PATTERN = "%s/api/v1/id/%s/@rendition/thumbnail";

    public ThumbnailJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartObject();
        jg.writeStringField(THUMBNAIL_URL_LABEL,
                String.format(THUMBNAIL_URL_PATTERN, ctx.getBaseUrl().replaceAll("/$", ""), document.getId()));
        jg.writeEndObject();
    }

}
