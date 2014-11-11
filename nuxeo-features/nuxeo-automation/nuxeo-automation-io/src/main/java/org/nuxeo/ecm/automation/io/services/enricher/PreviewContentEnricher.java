/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;

/**
 * This content enricher adds a document Preview URL.
 *
 * @since 5.9.6
 */
public class PreviewContentEnricher extends AbstractContentEnricher {

    public static final String PREVIEW_URL_LABEL = "url";

    public static final String PREVIEW_CONTENT_ID = "preview";

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec)
            throws ClientException, IOException {
        DocumentModel doc = ec.getDocumentModel();
        String relativeUrl = PreviewHelper.getPreviewURL(doc);
        jg.writeStartObject();
        if (relativeUrl != null && !relativeUrl.isEmpty()) {
            String url = Framework.getProperty("nuxeo.url") + "/"
                    + PreviewHelper.getPreviewURL(doc);
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
