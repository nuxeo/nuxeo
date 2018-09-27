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
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wopi.lock.LockHelper;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Gets the WOPI action URLs available for each blob of the document.
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "wopi": {
 *         "file:content": {
 *             "appName": "Word",
 *             "view": "http://localhost:8080/nuxeo/wopi/view/REPOSITORY/DOC_ID/file:content",
 *             "view": "http://localhost:8080/nuxeo/wopi/edit/REPOSITORY/DOC_ID/file:content",
 *         },
 *         "other:xpath": {
 *             "appName": "Excel",
 *             "view": "http://localhost:8080/nuxeo/wopi/view/REPOSITORY/DOC_ID/other:xpath",
 *             "view": "http://localhost:8080/nuxeo/wopi/edit/REPOSITORY/DOC_ID/other:xpath",
 *         },
 *         "locked": true|false
 *     }
 *   }
 * }
 * </pre>
 *
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WOPIJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "wopi";

    public static final String APP_NAME_FIELD = "appName";

    public static final String LOCKED_FIELD = "locked";

    public WOPIJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel doc) throws IOException {
        List<WOPIBlobInfo> infos = Framework.getService(WOPIService.class).getWOPIBlobInfos(doc);
        if (!infos.isEmpty()) {
            jg.writeFieldName(NAME);
            jg.writeStartObject();
            for (WOPIBlobInfo info : infos) {
                writeWOPIBlobInfo(jg, info, doc);
            }
            jg.writeBooleanField(LOCKED_FIELD, LockHelper.isLocked(doc.getRepositoryName(), doc.getId()));
            jg.writeEndObject();
        }
    }

    protected void writeWOPIBlobInfo(JsonGenerator jg, WOPIBlobInfo info, DocumentModel doc) throws IOException {
        jg.writeFieldName(info.xpath);
        jg.writeStartObject();
        jg.writeStringField(APP_NAME_FIELD, info.appName);
        for (String action : info.actions) {
            jg.writeStringField(action, Helpers.getWOPIURL(ctx.getBaseUrl(), action, doc, info.xpath));
        }
        jg.writeEndObject();
    }

}
