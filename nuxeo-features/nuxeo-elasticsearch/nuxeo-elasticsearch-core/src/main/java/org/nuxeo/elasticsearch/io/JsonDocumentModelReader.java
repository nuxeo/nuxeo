/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.io;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.search.lookup.SourceLookup;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Read a DocumentModel from an ES Json export.
 *
 * @since 5.9.5
 */
public class JsonDocumentModelReader {
    private static final Log log = LogFactory.getLog(JsonDocumentModelReader.class);

    private final Map<String, Object> source;

    private String sid;

    public JsonDocumentModelReader(String json) {
        byte[] bytes = json.getBytes();
        source = SourceLookup.sourceAsMap(new BytesArray(bytes, 0, bytes.length));
    }

    public JsonDocumentModelReader(Map<String, Object> source) {
        this.source = source;
    }

    public JsonDocumentModelReader session(CoreSession session) {
        sid = session.getSessionId();
        return this;
    }

    public JsonDocumentModelReader sid(String sid) {
        this.sid = sid;
        return this;
    }

    public DocumentModel getDocumentModel() {
        assert (source != null);
        String type = getType();
        String name = getPropertyAsString("ecm:name");
        String id = getPropertyAsString("ecm:uuid");
        String path = getPropertyAsString("ecm:path");
        String parentId = getPropertyAsString("ecm:parentId");
        String repository = getPropertyAsString("ecm:repository");

        DocumentModelImpl doc = new DocumentModelImpl(sid, getType(), id, new Path(path), new IdRef(id),
                new IdRef(parentId), null, null, null, repository, false);

        // pre load datamodel to prevent DB access
        DocumentType docType = Framework.getService(SchemaManager.class).getDocumentType(type);
        for (Schema schema : docType.getSchemas()) {
            doc.addDataModel(DocumentModelFactory.createDataModel(null, schema));
        }

        for (String prop : source.keySet()) {
            String schema = prop.split(":")[0];
            // schema = schema.replace("dc", "dublincore");
            String key = prop.split(":")[1];
            if (source.get(prop) == null) {
                continue;
            }
            String value = getPropertyAsString(prop);
            if (value.isEmpty() || "[]".equals(value)) {
                continue;
            }
            // System.out.println( String.format("schema: %s, key %s = %s", schema, key, value));
            if ("ecm".equals(schema)) {
                switch (key) {
                case "isProxy":
                    doc.setIsProxy(Boolean.valueOf(value));
                    break;
                case "currentLifeCycleState":
                    doc.prefetchCurrentLifecycleState(value);
                    break;
                case "versionLabel":
                case "mixinType":
                    // Can not be done via API
                    break;
                default:
                    // not taken in account
                }
            } else {
                try {
                    doc.setPropertyValue(prop, value);
                    // doc.setProperty(schema, key, value);
                } catch (PropertyException e) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("fetchDocFromEs can not set property %s to %s", key, value));
                    }
                }
            }
        }
        doc.setIsImmutable(true);
        return doc;
    }

    private String getType() {
        return getPropertyAsString("ecm:primaryType");
    }

    private String getPropertyAsString(String name) {
        Object prop = source.get(name);
        return (prop == null) ? "" : prop.toString();
    }
}
