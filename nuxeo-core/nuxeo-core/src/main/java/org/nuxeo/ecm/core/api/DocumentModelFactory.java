/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel.DocumentModelRefresh;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Document.WriteContext;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.Prefetch;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Bridge between a {@link DocumentModel} and a {@link Document} for creation / update.
 */
public class DocumentModelFactory {

    private static final Log log = LogFactory.getLog(DocumentModelFactory.class);

    // Utility class.
    private DocumentModelFactory() {
    }

    /**
     * Creates a document model for an existing document.
     *
     * @param doc the document
     * @param sid the session id for this document
     * @param schemas the schemas to prefetch (deprecated), or {@code null}
     * @return the new document model
     */
    public static DocumentModelImpl createDocumentModel(Document doc, String sid, String[] schemas) {

        DocumentType type = doc.getType();
        if (type == null) {
            throw new NuxeoException("Type not found for doc " + doc);
        }

        DocumentRef docRef = new IdRef(doc.getUUID());
        Document parent = doc.getParent();
        DocumentRef parentRef = parent == null ? null : new IdRef(parent.getUUID());

        // Compute document source id if exists
        Document sourceDoc = doc.getSourceDocument();
        String sourceId = sourceDoc == null ? null : sourceDoc.getUUID();

        // Immutable flag
        boolean immutable = doc.isVersion() || (doc.isProxy() && sourceDoc.isVersion());

        // Instance facets
        Set<String> facets = new HashSet<String>(Arrays.asList(doc.getFacets()));
        if (immutable) {
            facets.add(FacetNames.IMMUTABLE);
        }

        // Compute repository name.
        String repositoryName = doc.getRepositoryName();

        // versions being imported before their live doc don't have a path
        String p = doc.getPath();
        Path path = p == null ? null : new Path(p);

        // create the document model
        // lock is unused
        DocumentModelImpl docModel = new DocumentModelImpl(sid, type.getName(), doc.getUUID(), path, docRef, parentRef,
                null, facets, sourceId, repositoryName, doc.isProxy());

        docModel.setPosInternal(doc.getPos());

        if (doc.isVersion()) {
            docModel.setIsVersion(true);
        }
        if (immutable) {
            docModel.setIsImmutable(true);
        }

        // populate prefetch
        PrefetchInfo prefetchInfo = type.getPrefetchInfo();
        Prefetch prefetch;
        String[] prefetchSchemas;
        if (prefetchInfo != null) {
            Set<String> docSchemas = new HashSet<String>(Arrays.asList(docModel.getSchemas()));
            prefetch = getPrefetch(doc, prefetchInfo, docSchemas);
            prefetchSchemas = prefetchInfo.getSchemas();
        } else {
            prefetch = null;
            prefetchSchemas = null;
        }

        // populate datamodels
        List<String> loadSchemas = new LinkedList<String>();
        if (schemas == null) {
            schemas = prefetchSchemas;
        }
        if (schemas != null) {
            Set<String> validSchemas = new HashSet<String>(Arrays.asList(docModel.getSchemas()));
            for (String schemaName : schemas) {
                if (validSchemas.contains(schemaName)) {
                    loadSchemas.add(schemaName);
                }
            }
        }
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        for (String schemaName : loadSchemas) {
            Schema schema = schemaManager.getSchema(schemaName);
            docModel.addDataModel(createDataModel(doc, schema));
        }

        if (prefetch != null) {
            // ignore prefetches already loaded as datamodels
            for (String schemaName : loadSchemas) {
                prefetch.clearPrefetch(schemaName);
            }
            // set prefetch
            docModel.setPrefetch(prefetch);
        }

        // prefetch lifecycle state
        try {
            String lifeCycleState = doc.getLifeCycleState();
            docModel.prefetchCurrentLifecycleState(lifeCycleState);
            String lifeCyclePolicy = doc.getLifeCyclePolicy();
            docModel.prefetchLifeCyclePolicy(lifeCyclePolicy);
        } catch (LifeCycleException e) {
            log.debug("Cannot prefetch lifecycle for doc: " + doc.getName() + ". Error: " + e.getMessage());
        }

        return docModel;
    }

    /**
     * Returns a document model computed from its type, querying the {@link SchemaManager} service.
     * <p>
     * The created document model is not linked to any core session.
     *
     * @since 5.4.2
     */
    public static DocumentModelImpl createDocumentModel(String docType) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        DocumentType type = schemaManager.getDocumentType(docType);
        return createDocumentModel(null, type);
    }

    /**
     * Creates a document model for a new document.
     * <p>
     * Initializes the proper data models according to the type info.
     *
     * @param sessionId the CoreSession id
     * @param docType the document type
     * @return the document model
     */
    public static DocumentModelImpl createDocumentModel(String sessionId, DocumentType docType) {
        DocumentModelImpl docModel = new DocumentModelImpl(sessionId, docType.getName(), null, null, null, null, null,
                null, null, null, null);
        for (Schema schema : docType.getSchemas()) {
            docModel.addDataModel(createDataModel(null, schema));
        }
        return docModel;
    }

    /**
     * Creates a data model from a document and a schema. If the document is null, just creates empty data models.
     */
    public static DataModel createDataModel(Document doc, Schema schema) {
        DocumentPart part = new DocumentPartImpl(schema);
        if (doc != null) {
            doc.readDocumentPart(part);
        }
        return new DataModelImpl(part);
    }

    /**
     * Writes a document model to a document. Returns the re-read document model.
     */
    public static DocumentModel writeDocumentModel(DocumentModel docModel, Document doc) {
        if (!(docModel instanceof DocumentModelImpl)) {
            throw new NuxeoException("Must be a DocumentModelImpl: " + docModel);
        }

        boolean changed = false;

        // change token
        String changeToken = (String) docModel.getContextData(CoreSession.CHANGE_TOKEN);
        if (!doc.validateChangeToken(changeToken)) {
            throw new ConcurrentUpdateException(doc.getUUID());
        }

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
        // check only the loaded ones to find the dirty ones
        WriteContext writeContext = doc.getWriteContext();
        for (DataModel dm : docModel.getDataModelsCollection()) { // only loaded
            if (dm.isDirty()) {
                DocumentPart part = ((DataModelImpl) dm).getDocumentPart();
                changed = doc.writeDocumentPart(part, writeContext) || changed;
            }
        }
        // write the blobs last, so that blob providers have access to the new doc state
        writeContext.flush(doc);

        if (!changed) {
            return docModel;
        }

        // TODO: here we can optimize document part doesn't need to be read
        DocumentModel newModel = createDocumentModel(doc, docModel.getSessionId(), null);
        newModel.copyContextData(docModel);
        return newModel;
    }

    /**
     * Gets what's to refresh in a model (except for the ACPs, which need the session).
     */
    public static DocumentModelRefresh refreshDocumentModel(Document doc, int flags, String[] schemas)
            throws LifeCycleException {
        DocumentModelRefresh refresh = new DocumentModelRefresh();

        refresh.instanceFacets = new HashSet<String>(Arrays.asList(doc.getFacets()));
        Set<String> docSchemas = DocumentModelImpl.computeSchemas(doc.getType(), refresh.instanceFacets, doc.isProxy());

        if ((flags & DocumentModel.REFRESH_PREFETCH) != 0) {
            PrefetchInfo prefetchInfo = doc.getType().getPrefetchInfo();
            if (prefetchInfo != null) {
                refresh.prefetch = getPrefetch(doc, prefetchInfo, docSchemas);
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
            // TODO change token
        }

        if ((flags & DocumentModel.REFRESH_CONTENT) != 0) {
            if (schemas == null) {
                schemas = docSchemas.toArray(new String[0]);
            }
            TypeProvider typeProvider = Framework.getLocalService(SchemaManager.class);
            DocumentPart[] parts = new DocumentPart[schemas.length];
            for (int i = 0; i < schemas.length; i++) {
                DocumentPart part = new DocumentPartImpl(typeProvider.getSchema(schemas[i]));
                doc.readDocumentPart(part);
                parts[i] = part;
            }
            refresh.documentParts = parts;
        }

        return refresh;
    }

    /**
     * Prefetches from a document.
     */
    protected static Prefetch getPrefetch(Document doc, PrefetchInfo prefetchInfo, Set<String> docSchemas) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);

        // individual fields
        Set<String> xpaths = new HashSet<String>();
        String[] prefetchFields = prefetchInfo.getFields();
        if (prefetchFields != null) {
            xpaths.addAll(Arrays.asList(prefetchFields));
        }

        // whole schemas (but NOT their complex properties)
        String[] prefetchSchemas = prefetchInfo.getSchemas();
        if (prefetchSchemas != null) {
            for (String schemaName : prefetchSchemas) {
                if (docSchemas.contains(schemaName)) {
                    Schema schema = schemaManager.getSchema(schemaName);
                    if (schema != null) {
                        for (Field field : schema.getFields()) {
                            if (isScalarField(field)) {
                                xpaths.add(field.getName().getPrefixedName());
                            }
                        }
                    }
                }
            }
        }

        // do the prefetch
        Prefetch prefetch = new Prefetch();
        for (String schemaName : docSchemas) {
            Schema schema = schemaManager.getSchema(schemaName);
            // find xpaths for this schema
            Set<String> schemaXpaths = new HashSet<String>();
            for (String xpath : xpaths) {
                String sn = DocumentModelImpl.getXPathSchemaName(xpath, docSchemas, null);
                if (schemaName.equals(sn)) {
                    schemaXpaths.add(xpath);
                }
            }
            if (schemaXpaths.isEmpty()) {
                continue;
            }
            Map<String, Serializable> map = doc.readPrefetch(schema, schemaXpaths);
            for (Entry<String, Serializable> en : map.entrySet()) {
                String xpath = en.getKey();
                Serializable value = en.getValue();
                String[] returnName = new String[1];
                String sn = DocumentModelImpl.getXPathSchemaName(xpath, docSchemas, returnName);
                String name = returnName[0];
                prefetch.put(xpath, sn, name, value);
            }
        }

        return prefetch;
    }

    /**
     * Checks if a field is a primitive type or array.
     */
    protected static boolean isScalarField(Field field) {
        Type type = field.getType();
        if (type.isComplexType()) {
            // complex type
            return false;
        }
        if (!type.isListType()) {
            // primitive type
            return true;
        }
        // array or complex list?
        return ((ListType) type).getFieldType().isSimpleType();
    }

    /**
     * Create an empty documentmodel for a given type with its id already setted. This can be useful when trying to
     * attach a documentmodel that has been serialized and modified.
     *
     * @param type
     * @param id
     * @return
     * @since 5.7.2
     */
    public static DocumentModel createDocumentModel(String type, String id) {
        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        DocumentType docType = sm.getDocumentType(type);
        DocumentModel doc = new DocumentModelImpl(null, docType.getName(), id, null, null, new IdRef(id), null, null,
                null, null, null);
        for (Schema schema : docType.getSchemas()) {
            ((DocumentModelImpl) doc).addDataModel(createDataModel(null, schema));
        }
        return doc;
    }

}
