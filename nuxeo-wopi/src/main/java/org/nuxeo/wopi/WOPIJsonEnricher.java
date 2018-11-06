/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.wopi;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Gets the WOPI action URLs available for each blob of the document.
 * <p>
 * Enabled when parameter {@code enrichers.blob:wopi} is present.
 * <p>
 * Blob format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"document",
 *   "properties": {
 *     "file:content": {
 *       "name": "...",
 *       "mime-type": "...",
 *       ...,
 *       "wopi": {
 *         "appName": "Word",
 *         "view": "http://localhost:8080/nuxeo/wopi/view/REPOSITORY/DOC_ID/file:content",
 *         "edit": "http://localhost:8080/nuxeo/wopi/edit/REPOSITORY/DOC_ID/file:content"
 *       }
 *     },
 *     "other:xpath": {
 *       "name": "...",
 *       "mime-type": "...",
 *       ...,
 *       "wopi": {
 *         "appName": "Excel",
 *         "view": "http://localhost:8080/nuxeo/wopi/view/REPOSITORY/DOC_ID/other:xpath",
 *         "edit": "http://localhost:8080/nuxeo/wopi/edit/REPOSITORY/DOC_ID/other:xpath"
 *       }
 *     }
 *   }
 * }
 * }
 * </pre>
 *
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WOPIJsonEnricher extends AbstractJsonEnricher<BlobProperty> {

    public static final String NAME = "wopi";

    public static final String APP_NAME_FIELD = "appName";

    public WOPIJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, BlobProperty blobProperty) throws IOException {
        DocumentModel doc = ctx.getParameter(DocumentModelJsonWriter.ENTITY_TYPE);
        if (doc == null) {
            return;
        }

        Blob blob = (Blob) blobProperty.getValue();
        WOPIBlobInfo info = Framework.getService(WOPIService.class).getWOPIBlobInfo(blob);
        if (info == null) {
            return;
        }

        jg.writeFieldName(NAME);
        jg.writeStartObject();
        writeWOPIBlobInfo(jg, info, doc, getXPath(blobProperty));
        jg.writeEndObject();
    }

    protected void writeWOPIBlobInfo(JsonGenerator jg, WOPIBlobInfo info, DocumentModel doc, String xpath)
            throws IOException {
        jg.writeStringField(APP_NAME_FIELD, info.appName);
        for (String action : info.actions) {
            jg.writeStringField(action, Helpers.getWOPIURL(ctx.getBaseUrl(), action, doc, xpath));
        }
    }

    protected String getXPath(Property property) {
        String xpath = property.getXPath();
        // if no prefix, use schema name as prefix
        if (!xpath.contains(":")) {
            xpath = property.getSchema().getName() + ":" + xpath;
        }
        return xpath;
    }

}
