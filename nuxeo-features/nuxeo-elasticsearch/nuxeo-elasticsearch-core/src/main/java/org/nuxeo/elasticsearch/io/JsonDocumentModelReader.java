/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.elasticsearch.io;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.search.lookup.SourceLookup;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
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
        String type = (String) getProperty("ecm:primaryType");
        @SuppressWarnings("unchecked")
        List<String> mixinTypes = (List<String>) getProperty("ecm:mixinType");
        String id = (String) getProperty("ecm:uuid");
        String path = (String) getProperty("ecm:path");
        String parentId = (String) getProperty("ecm:parentId");
        String repositoryName = (String) getProperty("ecm:repository");
        boolean isProxy = Boolean.TRUE.equals(getProperty("ecm:isProxy"));
        String sourceId = null; // TODO write this in JsonESDocumentWriter

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        DocumentType docType = schemaManager.getDocumentType(type);

        // fixup facets, keep only instance facets
        Set<String> facets = new HashSet<>(mixinTypes == null ? Collections.emptyList() : mixinTypes);
        facets.remove(FacetNames.IMMUTABLE); // system facet
        facets.removeAll(docType.getFacets());

        Path pathObj = path == null ? null : new Path(path);
        DocumentRef docRef = new IdRef(id);
        DocumentRef parentRef = parentId == null ? null : new IdRef(parentId);
        DocumentModelImpl doc = new DocumentModelImpl(sid, type, id, pathObj, docRef, parentRef, null, facets, sourceId,
                repositoryName, isProxy);

        // preload DataModel to prevent DB access
        for (String schemaName : doc.getSchemas()) { // all schemas including from facets
            Schema schema = schemaManager.getSchema(schemaName);
            doc.addDataModel(DocumentModelFactory.createDataModel(null, schema));
        }

        for (String prop : source.keySet()) {
            String schema = prop.split(":")[0];
            Serializable value = getProperty(prop);
            if (value == null) {
                continue;
            }
            if ("ecm".equals(schema)) {
                switch (prop) {
                case "ecm:currentLifeCycleState":
                    doc.prefetchCurrentLifecycleState((String) value);
                    break;
                default:
                    // others not taken in account
                }
                continue;
            }
            // regular property
            try {
                doc.setPropertyValue(prop, value);
            } catch (PropertyException e) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("fetchDocFromEs can not set property %s to %s", prop, value));
                }
            }
        }
        doc.setIsImmutable(true);
        return doc;
    }

    protected Serializable getProperty(String name) {
        return (Serializable) source.get(name);
    }

}
