/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.repository.cache.DirtyUpdateChecker;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * DocumentModel factory for document models initialization.
 */
public class DocumentModelFactory {

    private static final Log log = LogFactory.getLog(DocumentModelFactory.class);

    // Utility class.
    private DocumentModelFactory() {
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModel newDocument(DocumentModel parent, String type) {
        return newDocument(parent, null, type);
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModel newDocument(DocumentModel parent, String name,
            String type) {
        DocumentType docType = parent.getCoreSession().getDocumentType(type);
        return newDocument(parent, name, docType);
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModel newDocument(DocumentModel parent,
            DocumentType type) {
        return newDocument(parent, null, type);
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModel newDocument(DocumentModel parent, String name,
            DocumentType type) {
        return new DocumentModelImpl(null, type.getName(), null,
                parent.getPath(), null, null, parent.getRef(),
                type.getSchemaNames(), type.getFacets(), null,
                parent.getRepositoryName());
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static Map<String, Serializable> updatePrefetch(
            DocumentModel docModel) {
        Map<String, Serializable> prefetchMap = new HashMap<String, Serializable>();
        PrefetchInfo prefetchInfo = docModel.getDocumentType().getPrefetchInfo();
        if (prefetchInfo != null) {
            Field[] prefetchFields = prefetchInfo.getFields();
            for (Field field : prefetchFields) {
                String typeName = field.getDeclaringType().getName();
                String typeLocalName = field.getName().getLocalName();
                String fieldName = typeName + '.' + typeLocalName;
                Object value;
                try {
                    value = docModel.getProperty(typeName, typeLocalName);
                } catch (ClientException e) {
                    continue;
                }
                prefetchMap.put(fieldName, (Serializable) value);
            }
        }
        return prefetchMap;
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModelImpl createDocumentModel(Document doc)
            throws DocumentException {
        DocumentType docType = doc.getType();
        String[] schemas;
        if (docType == null) {
            schemas = null;
        } else {
            schemas = docType.getSchemaNames();
        }
        return createDocumentModel(doc, schemas);
    }

    /**
     * Creates a document model for an existing document.
     *
     * @param doc the document
     * @param schemas the schemas to prefetch (deprecated), or {@code null}
     * @return the new document model
     * @throws DocumentException
     */
    public static DocumentModelImpl createDocumentModel(Document doc,
            String[] schemas) throws DocumentException {

        DocumentType type = doc.getType();
        if (type == null) {
            throw new DocumentException("Type not found for doc " + doc);
        }

        String sid = doc.getSession().getUserSessionId();

        DocumentRef docRef = new IdRef(doc.getUUID());
        Document parent = doc.getParent();
        DocumentRef parentRef = parent == null ? null : new IdRef(
                parent.getUUID());

        // Compute document source id if exists
        Document sourceDoc = doc.getSourceDocument();
        String sourceId = sourceDoc == null ? null : sourceDoc.getUUID();

        // Immutable flag
        boolean immutable = doc.isVersion()
                || (doc.isProxy() && sourceDoc.isVersion());

        Set<String> typeFacets = type.getFacets();
        if (immutable) {
            // clone facets to avoid modifying doc type facets
            if (typeFacets != null) {
                typeFacets = new HashSet<String>(typeFacets);
            } else {
                typeFacets = new HashSet<String>();
            }
            typeFacets.add(FacetNames.IMMUTABLE);
        }

        // Compute repository name.
        Repository repository = doc.getRepository();
        String repositoryName = repository == null ? null
                : repository.getName();

        // versions being imported before their live doc don't have a path
        String p = doc.getPath();
        Path path = p == null ? null : new Path(p);

        // create the document model
        DocumentModelImpl docModel = new DocumentModelImpl(sid, type.getName(),
                doc.getUUID(), path, doc.getLock(), docRef, parentRef,
                type.getSchemaNames(), typeFacets, sourceId, repositoryName);

        if (doc.isVersion()) {
            docModel.setIsVersion(true);
        }
        if (doc.isProxy()) {
            docModel.setIsProxy(true);
        }
        if (immutable) {
            docModel.setIsImmutable(true);
        }

        // populate models
        Schema[] prefetchSchemas = null;
        PrefetchInfo prefetchInfo = type.getPrefetchInfo();
        if (prefetchInfo != null) {
            prefetchSchemas = prefetchInfo.getSchemas();
            Field[] prefetchFields = prefetchInfo.getFields();
            for (Field field : prefetchFields) {
                // TODO: the document model don't know to work using prefixed
                // names -this should be fixed and register the property here
                // directly
                // by its prefixed name and not by the "schema.field" id
                try {
                    Object value = doc.getPropertyValue(field.getName().getPrefixedName());
                    docModel.prefetchProperty(
                            field.getDeclaringType().getName() + '.'
                                    + field.getName().getLocalName(), value);
                } catch (NoSuchPropertyException e) {
                    // skip
                } catch (DocumentException e) {
                    log.error("Error while building prefetch fields, "
                            + "check the document configuration", e);
                }
            }
        }

        if (schemas != null) {
            for (String schemaName : schemas) {
                Schema schema = type.getSchema(schemaName);
                if (schema == null) {
                    continue;
                }
                DataModel dataModel = exportSchema(doc, schema);
                docModel.addDataModel(dataModel);
            }
        } else if (prefetchSchemas != null) {
            for (Schema schema : prefetchSchemas) {
                DataModel dataModel = exportSchema(doc, schema);
                docModel.addDataModel(dataModel);
            }
        }

        // prefetch lifecycle state
        try {
            String lifeCycleState = doc.getLifeCycleState();
            docModel.prefetchCurrentLifecycleState(lifeCycleState);
            String lifeCyclePolicy = doc.getLifeCyclePolicy();
            docModel.prefetchLifeCyclePolicy(lifeCyclePolicy);
        } catch (LifeCycleException e) {
            log.debug("Cannot prefetch lifecycle for doc: " + doc.getName()
                    + ". Error: " + e.getMessage());
        }

        DirtyUpdateChecker.check(docModel);

        return docModel;
    }

    /**
     * Creates a document model for a new document.
     * <p>
     * Initializes the proper data models according to the type info.
     *
     * @param sessionId the CoreSession id
     * @param docType the document type
     * @return the document model
     * @throws DocumentException
     */
    public static DocumentModelImpl createDocumentModel(String sessionId,
            DocumentType docType) throws DocumentException {
        Set<String> facets = docType.getFacets();
        String[] schemas = docType.getSchemaNames();
        DocumentModelImpl docModel = new DocumentModelImpl(sessionId,
                docType.getName(), null, null, null, null, null, schemas,
                facets, null, null);
        for (Schema schema : docType.getSchemas()) {
            DataModel dataModel = exportSchema(null, schema);
            docModel.addDataModel(dataModel);
        }
        return docModel;
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModelImpl createDocumentModel(DocumentType docType)
            throws DocumentException {
        return createDocumentModel(null, docType);
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public static DocumentModelImpl createDocumentModel(String parentPath,
            String id, DocumentType docType, String[] schemas)
            throws DocumentException {
        DocumentModelImpl docModel = new DocumentModelImpl(parentPath, id,
                docType.getName());
        // populate models
        if (schemas != null) {
            for (String schema : schemas) {
                DataModel dataModel = exportSchema(null,
                        docType.getSchema(schema));
                docModel.addDataModel(dataModel);
            }
        }
        return docModel;
    }

    public static DataModel exportSchema(Document doc, Schema schema)
            throws DocumentException {
        DocumentPart part = new DocumentPartImpl(schema);
        if (doc != null) {
            try {
                doc.readDocumentPart(part);
            } catch (DocumentException e) {
                throw e;
            } catch (Exception e) {
                throw new DocumentException("failed to read document part", e);
            }
        }
        return new DataModelImpl(part);
    }

}
