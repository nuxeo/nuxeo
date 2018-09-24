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

import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.wopi.Constants.ACTION_EDIT;
import static org.nuxeo.wopi.Constants.ACTION_VIEW;
import static org.nuxeo.wopi.Constants.FILE_SCHEMA;
import static org.nuxeo.wopi.Constants.WOPI_SERVLET_PATH;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.wopi.lock.LockHelper;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Gets the WOPI action URLs available for the document's main blob (file:content).
 * <p>
 * To perform an action on another blob than the main one, the blob's xpath can be added to the URLs.
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
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        if (!document.hasSchema(FILE_SCHEMA)) {
            return;
        }

        Blob blob = Helpers.getEditableBlob(document, "file:content");
        if (blob == null) {
            return;
        }

        String viewURL = null;
        String editURL = null;
        String extension = FilenameUtils.getExtension(blob.getFilename());
        if (isExtensionSupported(extension, ACTION_VIEW)) {
            viewURL = getWOPIURL(document, ACTION_VIEW);
        }
        if (isExtensionSupported(extension, ACTION_EDIT)) {
            try (SessionWrapper wrapper = ctx.getSession(document)) {
                if (wrapper.getSession().hasPermission(document.getRef(), WRITE_PROPERTIES)) {
                    editURL = getWOPIURL(document, ACTION_EDIT);
                }
            }
        }
        if (viewURL != null || editURL != null) {
            jg.writeFieldName(NAME);
            jg.writeStartObject();
            jg.writeStringField(APP_NAME_FIELD, getAppName(extension, ACTION_VIEW));
            if (viewURL != null) {
                jg.writeStringField(ACTION_VIEW, viewURL);
            }
            if (editURL != null) {
                jg.writeStringField(ACTION_EDIT, editURL);
            }
            jg.writeBooleanField(LOCKED_FIELD, LockHelper.isLocked(document.getRepositoryName(), document.getId()));
            jg.writeEndObject();
        }
    }

    protected boolean isExtensionSupported(String extension, String action) {
        return WOPIServlet.ACTIONS_TO_URLS.containsKey(Pair.of(action, extension));
    }

    protected String getWOPIURL(DocumentModel doc, String action) {
        return String.format("%s%s/%s/%s/%s", ctx.getBaseUrl(), WOPI_SERVLET_PATH, action, doc.getRepositoryName(),
                doc.getId());
    }

    // TODO NXP-25381 - to be removed when using the discovery XML file
    protected String getAppName(String extension, String action) {
        Pair<String, String> pair = WOPIServlet.ACTIONS_TO_URLS.get(Pair.of(action, extension));
        if (pair != null) {
            return pair.getLeft();
        }
        // default to Word
        return "Word";
    }

}
