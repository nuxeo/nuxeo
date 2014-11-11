/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Null;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel.DocumentModelRefresh;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.repository.cache.DirtyUpdateChecker;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Bridge between a {@link DocumentModel} and a {@link Document} for creation /
 * update.
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
                parent.getPath(), null, null, parent.getRef(), null, null,
                null, parent.getRepositoryName());
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

        // Instance facets
        Set<String> facets = new HashSet<String>(Arrays.asList(doc.getFacets()));
        if (immutable) {
            facets.add(FacetNames.IMMUTABLE);
        }

        // Compute repository name.
        Repository repository = doc.getRepository();
        String repositoryName = repository == null ? null
                : repository.getName();

        // versions being imported before their live doc don't have a path
        String p = doc.getPath();
        Path path = p == null ? null : new Path(p);

        // create the document model
        // lock is unused
        DocumentModelImpl docModel = new DocumentModelImpl(sid, type.getName(),
                doc.getUUID(), path, null, docRef, parentRef, null, facets,
                sourceId, repositoryName);

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
                docModel.addDataModel(createDataModel(doc, schema));
            }
        } else if (prefetchSchemas != null) {
            for (Schema schema : prefetchSchemas) {
                docModel.addDataModel(createDataModel(doc, schema));
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
     * Returns a document model computed from its type, querying the
     * {@link SchemaManager} service.
     * <p>
     * The created document model is not linked to any core session.
     *
     * @since 5.4.2
     */
    public static DocumentModelImpl createDocumentModel(String docType) {
        try {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            if (schemaManager == null) {
                throw new ClientRuntimeException("SchemaManager is null");
            }
            DocumentType type = schemaManager.getDocumentType(docType);
            return createDocumentModel(type);
        } catch (ClientRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
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
        DocumentModelImpl docModel = new DocumentModelImpl(sessionId,
                docType.getName(), null, null, null, null, null, null, null,
                null, null);
        for (Schema schema : docType.getSchemas()) {
            docModel.addDataModel(createDataModel(null, schema));
        }
        return docModel;
    }

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
            for (String schemaName : schemas) {
                Schema schema = docType.getSchema(schemaName);
                docModel.addDataModel(createDataModel(null, schema));
            }
        }
        return docModel;
    }

    /**
     * Creates a data model from a document and a schema. If the document is
     * null, just creates empty data models.
     */
    public static DataModel createDataModel(Document doc, Schema schema)
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

    /**
     * Writes a document model to a document. Returns the re-read document
     * model.
     */
    public static DocumentModel writeDocumentModel(DocumentModel docModel,
            Document doc) throws DocumentException, ClientException {
        if (!(docModel instanceof DocumentModelImpl)) {
            throw new ClientRuntimeException("Must be a DocumentModelImpl: "
                    + docModel);
        }

        boolean changed = false;

        // facets added/removed
        Set<String> instanceFacets = ((DocumentModelImpl) docModel).instanceFacets;
        Set<String> instanceFacetsOrig = ((DocumentModelImpl) docModel).instanceFacetsOrig;
        Set<String> addedFacets = new HashSet<String>(instanceFacets);
        addedFacets.removeAll(instanceFacetsOrig);
        for (String facet : addedFacets) {
            changed = doc.addFacet(facet) || changed;
        }
        Set<String> removedFacets = new HashSet<String>(instanceFacetsOrig);
        removedFacets.removeAll(instanceFacets);
        for (String facet : removedFacets) {
            changed = doc.removeFacet(facet) || changed;
        }

        // write data models
        DocumentPart[] parts = docModel.getParts(); // TODO only loaded ones
        for (DocumentPart part : parts) {
            if (part.isDirty()) {
                try {
                    doc.writeDocumentPart(part);
                } catch (ClientException e) {
                    throw e;
                } catch (DocumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientException("failed to write document part",
                            e);
                }
                changed = true;
            }
        }

        if (!changed) {
            return docModel;
        }

        // TODO: here we can optimize document part doesn't need to be read
        DocumentModel newModel = createDocumentModel(doc, null);
        newModel.copyContextData(docModel);
        return newModel;
    }

    /**
     * Gets what's to refresh in a model (except for the ACPs, which need the
     * session).
     */
    public static DocumentModelRefresh refreshDocumentModel(Document doc,
            int flags, String[] schemas) throws DocumentException,
            LifeCycleException, Exception {
        DocumentModelRefresh refresh = new DocumentModelRefresh();

        if ((flags & DocumentModel.REFRESH_PREFETCH) != 0) {
            PrefetchInfo info = doc.getType().getPrefetchInfo();
            if (info != null) {
                Schema[] pschemas = info.getSchemas();
                if (pschemas != null) {
                    // TODO: this should be returned as document parts of
                    // the document
                }
                Field[] fields = info.getFields();
                if (fields != null) {
                    Map<String, Serializable> prefetch = new HashMap<String, Serializable>();
                    // TODO : should use documentpartreader
                    for (Field field : fields) {
                        Object value = doc.getPropertyValue(field.getName().getPrefixedName());
                        prefetch.put(field.getDeclaringType().getName() + '.'
                                + field.getName().getLocalName(),
                                value == null ? Null.VALUE
                                        : (Serializable) value);
                    }
                    refresh.prefetch = prefetch;
                }
            }
        }

        if ((flags & DocumentModel.REFRESH_STATE) != 0) {
            refresh.lifeCycleState = doc.getLifeCycleState();
            refresh.lifeCyclePolicy = doc.getLifeCyclePolicy();
            refresh.isCheckedOut = doc.isCheckedOut();
            refresh.isLatestVersion = doc.isLatestVersion();
            refresh.isMajorVersion = doc.isMajorVersion();
            refresh.isLatestMajorVersion = doc.isLatestMajorVersion();
            refresh.isVersionSeriesCheckedOut = doc.isVersionSeriesCheckedOut();
            refresh.versionSeriesId = doc.getVersionSeriesId();
            refresh.checkinComment = doc.getCheckinComment();
        }

        if ((flags & DocumentModel.REFRESH_CONTENT) != 0) {
            if (schemas == null) {
                schemas = doc.getType().getSchemaNames();
            }
            DocumentType type = doc.getType();
            DocumentPart[] parts = new DocumentPart[schemas.length];
            for (int i = 0; i < schemas.length; i++) {
                DocumentPart part = new DocumentPartImpl(
                        type.getSchema(schemas[i]));
                doc.readDocumentPart(part);
                parts[i] = part;
            }
            refresh.documentParts = parts;
        }

        return refresh;
    }

}
