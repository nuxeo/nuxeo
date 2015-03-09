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

package org.nuxeo.ecm.platform.url.io;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList.CODEC_PARAMETER_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s document url as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers.document=documentURL is present.
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
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentUrlJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "documentURL";

    @Inject
    private DocumentViewCodecManager viewCodecManager;

    public DocumentUrlJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        DocumentLocation docLoc = new DocumentLocationImpl(document);
        String pCodecName = ctx.getParameter(CODEC_PARAMETER_NAME);
        String codecName = isBlank(pCodecName) ? viewCodecManager.getDefaultCodecName() : pCodecName;
        TypeInfo adapter = document.getAdapter(TypeInfo.class);
        if (adapter == null) {
            jg.writeNullField(NAME);
            return;
        }
        DocumentView docView = new DocumentViewImpl(docLoc, adapter.getDefaultView());
        String url = ctx.getBaseUrl() + viewCodecManager.getUrlFromDocumentView(codecName, docView, false, null);
        jg.writeStringField(NAME, url);
    }

}
