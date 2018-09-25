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
import static org.nuxeo.wopi.Constants.WOPI_SERVLET_PATH;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
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

    protected static final BlobsExtractor BLOBS_EXTRACTOR = new BlobsExtractor();

    public WOPIJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel doc) throws IOException {
        List<Property> blobsProperties = BLOBS_EXTRACTOR.getBlobsProperties(doc);
        List<WOPIBlobInfo> infos = blobsProperties.stream()
                                                  .map(p -> getWOPIBlobInfo(p, doc))
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());

        if (!infos.isEmpty()) {
            jg.writeFieldName(NAME);
            jg.writeStartObject();
            infos.forEach(info -> writeWOPIBlobInfo(jg, info));
            jg.writeBooleanField(LOCKED_FIELD, LockHelper.isLocked(doc.getRepositoryName(), doc.getId()));
            jg.writeEndObject();
        }
    }

    // TODO NXP-25381 - to be moved to a WOPIService when using the discovery XML file
    protected boolean isExtensionSupported(String extension, String action) {
        return WOPIServlet.ACTIONS_TO_URLS.containsKey(Pair.of(action, extension));
    }

    // TODO NXP-25381 - to be moved to a WOPIService/Helper when using the discovery XML file
    protected String getWOPIURL(String action, DocumentModel doc, String xpath) {
        return String.format("%s%s/%s/%s/%s/%s", ctx.getBaseUrl(), WOPI_SERVLET_PATH, action, doc.getRepositoryName(),
                doc.getId(), xpath);
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

    protected WOPIBlobInfo getWOPIBlobInfo(Property blobProperty, DocumentModel doc) {
        String blobXPath = blobProperty.getXPath();
        if (!blobXPath.contains(":")) {
            // for schema without prefix: we need to add schema name as prefix
            blobXPath = blobProperty.getSchema().getName() + ":" + blobXPath;
        }

        Blob blob = Helpers.getEditableBlob(doc, blobXPath);
        if (blob == null) {
            return null;
        }

        String viewURL = null;
        String editURL = null;
        String appName = null;

        String extension = FilenameUtils.getExtension(blob.getFilename());
        if (isExtensionSupported(extension, ACTION_EDIT)) {
            try (SessionWrapper wrapper = ctx.getSession(doc)) {
                if (wrapper.getSession().hasPermission(doc.getRef(), WRITE_PROPERTIES)) {
                    editURL = getWOPIURL(ACTION_EDIT, doc, blobXPath);
                    appName = getAppName(extension, ACTION_EDIT);
                }
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }

        if (isExtensionSupported(extension, ACTION_VIEW)) {
            viewURL = getWOPIURL(ACTION_VIEW, doc, blobXPath);
            if (appName == null) {
                appName = getAppName(extension, ACTION_VIEW);
            }
        }

        if (viewURL != null || editURL != null) {
            return new WOPIBlobInfo(blobXPath, viewURL, editURL, appName);
        }
        return null;
    }

    protected void writeWOPIBlobInfo(JsonGenerator jg, WOPIBlobInfo info) {
        try {
            jg.writeFieldName(info.xpath);
            jg.writeStartObject();
            jg.writeStringField(APP_NAME_FIELD, info.appName);
            if (info.viewURL != null) {
                jg.writeStringField(ACTION_VIEW, info.viewURL);
            }
            if (info.editURL != null) {
                jg.writeStringField(ACTION_EDIT, info.editURL);
            }
            jg.writeEndObject();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    // TODO NXP-25381 - to be moved to a WOPIService when using the discovery XML file
    public static class WOPIBlobInfo {

        public final String xpath;

        public final String viewURL;

        public final String editURL;

        public final String appName;

        public WOPIBlobInfo(String xpath, String viewURL, String editURL, String appName) {
            this.xpath = xpath;
            this.viewURL = viewURL;
            this.editURL = editURL;
            this.appName = appName;
        }
    }

}
