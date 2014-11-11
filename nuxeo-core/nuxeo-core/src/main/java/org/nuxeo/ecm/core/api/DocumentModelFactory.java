/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Collection;
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
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

/**
 * DocumentModel factory for document models initialization.
 *
 * @author bstefanescu
 */
public class DocumentModelFactory {

    private static final Log log = LogFactory.getLog(DocumentModelFactory.class);

    private static final SecureRandom random = new SecureRandom();

    public static DocumentModel newDocument(DocumentModel parent, String type) {
        DocumentType docType = ((DocumentModelImpl) parent).getClient().getDocumentType(
                type);
        return newDocument(parent, docType);
    }

    public static DocumentModel newDocument(DocumentModel parent, String name,
            String type) {
        DocumentType docType = ((DocumentModelImpl) parent).getClient().getDocumentType(
                type);
        return newDocument(parent, name, docType);
    }

    public static DocumentModel newDocument(DocumentModel parent,
            DocumentType type) {
        return newDocument(parent, "Untitled_"
                + Long.toHexString(random.nextLong()), type);
    }

    public static DocumentModel newDocument(DocumentModel parent, String name,
            DocumentType type) {
        return new DocumentModelImpl(null, type.getName(), null,
                parent.getPath(), null, null, parent.getRef(),
                type.getSchemaNames(), type.getFacets(), null,
                parent.getRepositoryName());
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
                Object value = docModel.getProperty(typeName, typeLocalName);
                prefetchMap.put(fieldName, (Serializable) value);
            }
        }

        return prefetchMap;
    }

    public static DocumentModelImpl createDocumentModel(Document doc,
            String[] schemas) throws DocumentException {

        DocumentType type = doc.getType();
        if (type == null) {
            throw new DocumentException("Type not found for doc " + doc);
        }

        Session session = doc.getSession();
        String sid = session.getUserSessionId();

        DocumentRef docRef = new IdRef(doc.getUUID());
        DocumentRef parentRef = null;
        Document parent = doc.getParent();
        if (null != parent) {
            parentRef = new IdRef(parent.getUUID());
        }

        Set<String> typeFacets = type.getFacets();
        if (doc.isProxy() || doc.isVersion()) {
            if (typeFacets != null) {
                // clone facets to avoid modifying doc type facets
                typeFacets = new HashSet<String>(typeFacets);
            } else {
                typeFacets = new HashSet<String>();
            }
            typeFacets.add("Immutable");
        }

        // Compute document source id if exists.
        String sourceId = null;
        Document sourceDoc = doc.getSourceDocument();
        if (sourceDoc != null) {
            sourceId = sourceDoc.getUUID();
        }

        // Compute repository name.
        String repositoryName = null;
        Repository repository = doc.getRepository();
        if (repository != null) {
            repositoryName = repository.getName();
        }
        DocumentModelImpl docModel = new DocumentModelImpl(sid, type.getName(),
                doc.getUUID(), new Path(doc.getPath()), doc.getLock(), docRef,
                parentRef, type.getSchemaNames(), typeFacets, sourceId,
                repositoryName);

        if (doc.isVersion()) {
            docModel.setIsVersion(true);
            docModel.putContextData("version.label",
                    ((DocumentVersion) doc).getLabel());
        }
        if (doc.isProxy()) {
            docModel.setIsProxy(true);
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
                } catch (DocumentException e) {
                    log.error("Error while building prefetch fields, "
                            + "check the document configuration", e);
                }
            }
        }

        if (schemas != null) {
            for (String schema : schemas) {
                DataModel dataModel = exportSchema(sid, docRef, doc,
                        type.getSchema(schema));
                docModel.addDataModel(dataModel);
            }
        } else if (prefetchSchemas != null && prefetchSchemas.length > 0) {
            for (Schema schema : prefetchSchemas) {
                DataModel dataModel = exportSchema(sid, docRef, doc, schema);
                docModel.addDataModel(dataModel);
            }
        }

        // prefetch lifecycle state
        try {
            String lifecycle = doc.getCurrentLifeCycleState();
            docModel.prefetchCurrentLifecycleState(lifecycle);
            String lifeCyclePolicy = doc.getLifeCyclePolicy();
            docModel.prefetchLifeCyclePolicy(lifeCyclePolicy);
        } catch (LifeCycleException e) {
            log.debug("Cannot prefetch lifecycle for doc: " + doc.getName()
                    + ". Error: " + e.getMessage());
        }

        return docModel;
    }

    /**
     * Creates a new document model using only required information to be used
     * on client side.
     *
     * @param docType The document type
     * @return the document model initialized thanks to the type definition
     * @throws DocumentException
     */
    public static DocumentModelImpl createDocumentModel(DocumentType docType)
            throws DocumentException {
        return createDocumentModel(null, docType);
    }

    /**
     * Creates a new document model using only required information to be used
     * on client side.
     *
     * @param sessionId the CoreSession id
     * @param docType The document type
     * @return the document model initialized thanks to the type definition
     * @throws DocumentException
     */
    public static DocumentModelImpl createDocumentModel(String sessionId,
            DocumentType docType) throws DocumentException {
        DocumentModelImpl docModel = new DocumentModelImpl(sessionId,
                docType.getName());
        for (Schema schema : docType.getSchemas()) {
            DataModel dataModel = exportSchema(null, null, null, schema);
            docModel.addDataModel(dataModel);
        }
        return docModel;
    }

    /**
     * Creates a new document model using only required information to be used
     * on client side.
     *
     * @param parentPath the document parent path
     * @param id the document id
     * @param docType the document type
     * @param schemas schemas to take into account when initializing
     * @return the document model initialized thanks to the type definition
     * @throws DocumentException
     */
    public static DocumentModelImpl createDocumentModel(String parentPath,
            String id, DocumentType docType, String[] schemas)
            throws DocumentException {
        DocumentModelImpl docModel = new DocumentModelImpl(parentPath, id,
                docType.getName());
        // populate models
        if (schemas != null) {
            for (String schema : schemas) {
                DataModel dataModel = exportSchema(null, null, null,
                        docType.getSchema(schema));
                docModel.addDataModel(dataModel);
            }
        }
        return docModel;
    }

    public static DataModel exportSchema_OLD(String sid, DocumentRef docRef,
            Document doc, Schema schema) throws DocumentException {
        Collection<Field> fields = schema.getFields();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields) {
            String name = field.getName().getLocalName();
            if (doc == null) {
                // init
                map.put(name, field.getDefaultValue());
            } else {
                Type type = field.getType();
                Property prop = doc.getProperty(name);
                map.put(name, exportProperty(sid, docRef, type, prop));
            }
        }
        return new DataModelImpl(schema.getName(), map);
    }

    public static DataModel exportSchema(String sid, DocumentRef docRef,
            Document doc, Schema schema) throws DocumentException {
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

    public static Map<String, Object> exportComplexProperty(String sid,
            DocumentRef docRef, Property property) throws DocumentException {
        Collection<Field> fields = ((ComplexType) property.getType()).getFields();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields) {
            Type type = field.getType();
            String name = field.getName().getLocalName();
            Property prop = property.getProperty(name);
            map.put(name, exportProperty(sid, docRef, type, prop));
        }
        return map;
    }

    private static Object exportProperty(String sid, DocumentRef docRef,
            Type type, Property prop) throws DocumentException {
        if (type.isSimpleType() || type.isListType()) {
            return prop.getValue();
        } else if (type.getName().equals(TypeConstants.CONTENT)) {
            // TODO Do not use at core level the ContentSourceObject ->
            // use only complex types!
            // TODO by default all content types are lazy see later how
            // to define them at type manager level
            return prop.getValue();
        } else {
            return exportComplexProperty(sid, docRef, prop);
        }
    }

}
