/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.url.io;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList.CODEC_PARAMETER_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s document url as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers-document=documentURL is present.
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
 *     "documentURL": "DOCUMENT_URL"
 *   }
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentUrlJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "documentURL";

    public static final String NOTIFICATION_DOCUMENT_ID_CODEC_NAME = "notificationDocId";

    @Inject
    private DocumentViewCodecManager viewCodecManager;

    public DocumentUrlJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        DocumentLocation docLoc = new DocumentLocationImpl(document);
        String pCodecName = ctx.getParameter(CODEC_PARAMETER_NAME);
        String codecName = isBlank(pCodecName) ? NOTIFICATION_DOCUMENT_ID_CODEC_NAME : pCodecName;
        TypeInfo adapter = document.getAdapter(TypeInfo.class);
        if (adapter == null) {
            jg.writeNullField(NAME);
            return;
        }
        DocumentView docView = new DocumentViewImpl(docLoc, adapter.getDefaultView());
        String url = viewCodecManager.getUrlFromDocumentView(codecName, docView, false, null);
        if (url == null) {
            jg.writeNullField(NAME);
            return;
        }
        jg.writeStringField(NAME, ctx.getBaseUrl() + url);
    }

}
