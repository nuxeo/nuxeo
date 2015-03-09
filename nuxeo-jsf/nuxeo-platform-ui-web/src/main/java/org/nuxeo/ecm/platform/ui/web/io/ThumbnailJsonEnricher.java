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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s thumbnail url as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers.document=thumbnail is present.
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
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ThumbnailJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "thumbnail";

    public static final String THUMBNAIL_URL_LABEL = "url";

    public static final String THUMB_THUMBNAIL = "thumb:thumbnail";

    public static final String DOWNLOAD_THUMBNAIL = "downloadThumbnail";

    public ThumbnailJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        ThumbnailAdapter thumbnailAdapter = document.getAdapter(ThumbnailAdapter.class);
        jg.writeFieldName(NAME);
        jg.writeStartObject();
        if (thumbnailAdapter != null) {
            try {
                Blob thumbnail = null;
                try (SessionWrapper wrapper = ctx.getSession(document)) {
                    thumbnail = thumbnailAdapter.getThumbnail(wrapper.getSession());
                }
                if (thumbnail != null) {
                    String url = DocumentModelFunctions.fileUrl(ctx.getBaseUrl().replaceAll("/$", ""),
                            DOWNLOAD_THUMBNAIL, document, THUMB_THUMBNAIL, thumbnail.getFilename());
                    jg.writeStringField(THUMBNAIL_URL_LABEL, url);
                } else {
                    jg.writeNullField(THUMBNAIL_URL_LABEL);
                }
            } catch (ClientException e) {
                jg.writeNullField(THUMBNAIL_URL_LABEL);
            }
        } else {
            jg.writeNullField(THUMBNAIL_URL_LABEL);
        }
        jg.writeEndObject();
    }

}
