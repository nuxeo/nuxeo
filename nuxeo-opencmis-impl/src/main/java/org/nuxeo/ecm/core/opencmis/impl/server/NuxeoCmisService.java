/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyHtmlDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIdDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyUriDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Converter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyData;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.jaxb.CmisTypeContainer;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoFolder;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Nuxeo implementation of the CMIS Services, on top of a {@link CoreSession}.
 */
public class NuxeoCmisService extends AbstractCmisService {

    private static final Log log = LogFactory.getLog(NuxeoCmisService.class);

    protected final BindingsObjectFactory objectFactory = new BindingsObjectFactoryImpl();

    protected final NuxeoRepository repository;

    protected final CoreSession coreSession;

    /** When false, we don't own the core session and shouldn't close it. */
    protected final boolean coreSessionOwned;

    /** Filter that hides HiddenInNavigation and deleted objects. */
    protected final Filter documentFilter;

    protected final CallContext context;

    /** Constructor called by binding. */
    public NuxeoCmisService(NuxeoRepository repository, CallContext context) {
        this.repository = repository;
        this.context = context;
        this.coreSession = repository == null ? null
                : openCoreSession(repository.getId());
        coreSessionOwned = true;
        documentFilter = getDocumentFilter();
    }

    /** Constructor called by high-level session from existing core session. */
    public NuxeoCmisService(NuxeoRepository repository, CallContext context,
            CoreSession coreSession) {
        this.repository = repository;
        this.context = context;
        this.coreSession = coreSession;
        coreSessionOwned = false;
        documentFilter = getDocumentFilter();
    }

    // called in a finally block from dispatcher
    @Override
    public void close() {
        if (coreSession != null && coreSessionOwned) {
            closeCoreSession();
        }
    }

    protected CoreSession openCoreSession(String repositoryId) {
        try {
            Repository repository = Framework.getService(
                    RepositoryManager.class).getRepository(repositoryId);
            if (repository == null) {
                throw new CmisRuntimeException("Cannot get repository: "
                        + repositoryId);
            }
            return repository.open();
        } catch (CmisRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    protected void closeCoreSession() {
        try {
            Repository.close(coreSession);
        } catch (Exception e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    public NuxeoRepository getNuxeoRepository() {
        return repository;
    }

    public CoreSession getCoreSession() {
        return coreSession;
    }

    public BindingsObjectFactory getObjectFactory() {
        return objectFactory;
    }

    /** Gets the filter that hides HiddenInNavigation and deleted objects. */
    protected Filter getDocumentFilter() {
        Filter facetFilter = new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION,
                false);
        Filter lcFilter = new LifeCycleFilter(LifeCycleConstants.DELETED_STATE,
                false);
        return new CompoundFilter(facetFilter, lcFilter);
    }

    /* This is the only method that does not have a repositoryId / coreSession. */
    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        List<NuxeoRepository> repos = NuxeoRepositories.getRepositories();
        List<RepositoryInfo> infos = new ArrayList<RepositoryInfo>(repos.size());
        for (NuxeoRepository repo : repos) {
            infos.add(repo.getRepositoryInfo());
        }
        return infos;
    }

    @Override
    public RepositoryInfo getRepositoryInfo(String repositoryId,
            ExtensionsData extension) {
        // TODO link LatestChangeLogToken to session state
        return NuxeoRepositories.getRepository(repositoryId).getRepositoryInfo();
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        TypeDefinition type = repository.getTypeDefinition(typeId);
        if (type == null) {
            throw new CmisInvalidArgumentException("No such type: " + typeId);
        }
        // TODO copy only when local binding
        // clone
        return Converter.convert(Converter.convert(type));

    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId,
            String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        TypeDefinitionList types = repository.getTypeChildren(typeId,
                includePropertyDefinitions, maxItems, skipCount);
        // TODO copy only when local binding
        // clone
        return Converter.convert(Converter.convert(types));
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(
            String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {
        int d = depth == null ? 2 : depth.intValue(); // default 2
        List<TypeDefinitionContainer> types = repository.getTypeDescendants(
                typeId, d, includePropertyDefinitions);
        // clone
        // TODO copy only when local binding
        List<CmisTypeContainer> tmp = new ArrayList<CmisTypeContainer>(
                types.size());
        Converter.convertTypeContainerList(types, tmp);
        return Converter.convertTypeContainerList(tmp);
    }

    protected DocumentModel getDocumentModel(String id) {
        DocumentRef docRef = new IdRef(id);
        try {
            if (!coreSession.exists(docRef)) {
                throw new CmisObjectNotFoundException(docRef.toString());
            }
            DocumentModel doc = coreSession.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new CmisObjectNotFoundException(docRef.toString());
            }
            return doc;
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public NuxeoObjectData getObject(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData data = new NuxeoObjectData(repository, doc, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
        if (context.isObjectInfoRequired()) {
            addObjectInfo(getObjectInfo(repositoryId, data));
        }
        return data;
    }

    /**
     * Checks if the doc should be ignored because it is "invisible" (deleted,
     * hidden in navigation).
     */
    public boolean isFilteredOut(DocumentModel doc) {
        return !documentFilter.accept(doc);
    }

    protected DocumentModel createDocumentModel(ObjectId folder,
            TypeDefinition type) {
        DocumentModel doc;
        String typeId = type.getId();
        String nuxeoTypeId = type.getLocalName();
        if (BaseTypeId.CMIS_DOCUMENT.value().equals(typeId)) {
            nuxeoTypeId = NuxeoTypeHelper.NUXEO_FILE;
        } else if (BaseTypeId.CMIS_FOLDER.value().equals(typeId)) {
            nuxeoTypeId = NuxeoTypeHelper.NUXEO_FOLDER;
        }
        try {
            doc = coreSession.createDocumentModel(nuxeoTypeId);
        } catch (ClientException e) {
            throw new IllegalArgumentException(typeId);
        }
        if (folder != null) {
            DocumentModel parentDoc;
            if (folder instanceof NuxeoFolder) {
                parentDoc = ((NuxeoFolder) folder).data.doc;
            } else {
                try {
                    parentDoc = coreSession.getDocument(new IdRef(
                            folder.getId()));
                } catch (ClientException e) {
                    throw new CmisRuntimeException("Cannot create object", e);
                }
            }
            String pathSegment = nuxeoTypeId; // default path segment based on id
            doc.setPathInfo(parentDoc.getPathAsString(), pathSegment);
        }
        return doc;
    }

    // create and save session
    protected NuxeoObjectData createObject(Properties properties,
            ObjectId folder, BaseTypeId baseType, ContentStream contentStream) {
        String typeId;
        Map<String, PropertyData<?>> p;
        PropertyData<?> d;
        TypeDefinition type = null;
        if (properties != null //
                && (p = properties.getProperties()) != null //
                && (d = p.get(PropertyIds.OBJECT_TYPE_ID)) != null) {
            typeId = (String) d.getFirstValue();
            if (baseType == null) {
                type = repository.getTypeDefinition(typeId);
                if (type == null) {
                    throw new IllegalArgumentException(typeId);
                }
                baseType = type.getBaseTypeId();
            }
        } else {
            typeId = null;
        }
        if (typeId == null) {
            switch (baseType) {
            case CMIS_DOCUMENT:
                typeId = "File"; // TODO constant
                break;
            case CMIS_FOLDER:
                typeId = BaseTypeId.CMIS_FOLDER.value();
                break;
            case CMIS_POLICY:
                throw new CmisRuntimeException("Cannot create policy");
            case CMIS_RELATIONSHIP:
                throw new CmisRuntimeException("Cannot create relationship");
            default:
                throw new CmisRuntimeException("No base type");
            }
        }
        if (type == null) {
            type = repository.getTypeDefinition(typeId);
        }
        if (type == null || type.getBaseTypeId() != baseType) {
            throw new CmisInvalidArgumentException(typeId);
        }
        if (type.isCreatable() == Boolean.FALSE) {
            throw new CmisInvalidArgumentException("Not creatable: " + typeId);
        }
        DocumentModel doc = createDocumentModel(folder, type);
        NuxeoObjectData data = new NuxeoObjectData(repository, doc);
        updateProperties(data, null, properties, true);
        try {
            if (contentStream != null) {
                if (contentStream.getFileName() == null) {
                    // infer filename from properties
                    PropertyData<?> pd = properties.getProperties().get(
                            PropertyIds.NAME);
                    if (pd != null) {
                        String filename = (String) pd.getFirstValue();
                        contentStream = new ContentStreamImpl(filename,
                                contentStream.getBigLength(),
                                contentStream.getMimeType(),
                                contentStream.getStream());
                    }
                }
                try {
                    NuxeoPropertyData.setContentStream(doc, contentStream, true);
                } catch (CmisContentAlreadyExistsException e) {
                    // cannot happen, overwrite = true
                }
            }
            // set path segment from title
            String pathSegment = IdUtils.generatePathSegment(doc.getTitle());
            doc.setPathInfo(doc.getPath().removeLastSegments(1).toString(),
                    pathSegment);
            data.doc = coreSession.createDocument(doc);
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException("Cannot create", e);
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        return data;
    }

    protected <T> void updateProperties(NuxeoObjectData object,
            String changeToken, Properties properties, boolean creation) {
        TypeDefinition type = object.getTypeDefinition();
        // TODO changeToken
        Map<String, PropertyData<?>> p;
        if (properties == null || (p = properties.getProperties()) == null) {
            return;
        }
        for (Entry<String, PropertyData<?>> en : p.entrySet()) {
            String key = en.getKey();
            PropertyData<?> d = en.getValue();
            setObjectProperty(object, key, d, type, creation);
        }
    }

    protected <T> void updateProperties(NuxeoObjectData object,
            String changeToken, Map<String, ?> properties, TypeDefinition type,
            boolean creation) {
        // TODO changeToken
        if (properties == null) {
            return;
        }
        for (Entry<String, ?> en : properties.entrySet()) {
            String key = en.getKey();
            Object value = en.getValue();
            @SuppressWarnings("unchecked")
            PropertyDefinition<T> pd = (PropertyDefinition<T>) type.getPropertyDefinitions().get(
                    key);
            if (pd == null) {
                throw new CmisRuntimeException("Unknown property: " + key);
            }
            setObjectProperty(object, key, value, pd, creation);
        }
    }

    protected <T> void setObjectProperty(NuxeoObjectData object, String key,
            PropertyData<T> d, TypeDefinition type, boolean creation) {
        @SuppressWarnings("unchecked")
        PropertyDefinition<T> pd = (PropertyDefinition<T>) type.getPropertyDefinitions().get(
                key);
        if (pd == null) {
            throw new CmisRuntimeException("Unknown property: " + key);
        }
        Object value;
        if (d == null) {
            value = null;
        } else if (pd.getCardinality() == Cardinality.SINGLE) {
            value = d.getFirstValue();
        } else {
            value = d.getValues();
        }
        setObjectProperty(object, key, value, pd, creation);
    }

    protected <T> void setObjectProperty(NuxeoObjectData object, String key,
            Object value, PropertyDefinition<T> pd, boolean creation) {
        Updatability updatability = pd.getUpdatability();
        if (updatability == Updatability.READONLY
                || (updatability == Updatability.ONCREATE && !creation)) {
            // log.error("Read-only property, ignored: " + key);
            return;
        }
        if (PropertyIds.OBJECT_TYPE_ID.equals(key)
                || PropertyIds.LAST_MODIFICATION_DATE.equals(key)) {
            return;
        }
        // TODO avoid constructing property object just to set value
        NuxeoPropertyDataBase<T> np = (NuxeoPropertyDataBase<T>) NuxeoPropertyData.construct(
                object, pd);
        np.setValue(value);
    }

    @Override
    public String create(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            ExtensionsData extension) {
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), null, contentStream);
        return object.getId();
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        // TODO versioningState, policies, addAces, removeAces
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), BaseTypeId.CMIS_DOCUMENT, contentStream);
        return object.getId();
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO policies, addAces, removeAces
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), BaseTypeId.CMIS_FOLDER, null);
        return object.getId();
    }

    @Override
    public String createPolicy(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String createRelationship(String repositoryId,
            Properties properties, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String createDocumentFromSource(String repositoryId,
            String sourceId, Properties properties, String folderId,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        if (folderId == null) {
            // no unfileable objects for now
            throw new CmisInvalidArgumentException("Invalid null folder ID");
        }
        DocumentModel doc = getDocumentModel(sourceId);
        DocumentModel folder = getDocumentModel(folderId);
        try {
            DocumentModel copyDoc = coreSession.copy(doc.getRef(),
                    folder.getRef(), null);
            NuxeoObjectData copy = new NuxeoObjectData(repository, copyDoc);
            if (properties != null && properties.getPropertyList() != null
                    && !properties.getPropertyList().isEmpty()) {
                updateProperties(copy, null, properties, false);
                copy.doc = coreSession.saveDocument(copyDoc);
            }
            coreSession.save();
            return copy.getId();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    public NuxeoObjectData copy(String sourceId, String targetId,
            Map<String, ?> properties, TypeDefinition type,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addACEs, List<Ace> removeACEs, OperationContext context) {
        DocumentModel doc = getDocumentModel(sourceId);
        DocumentModel folder = getDocumentModel(targetId);
        try {
            DocumentModel copyDoc = coreSession.copy(doc.getRef(),
                    folder.getRef(), null);
            NuxeoObjectData copy = new NuxeoObjectData(repository, copyDoc,
                    context);
            if (properties != null && !properties.isEmpty()) {
                updateProperties(copy, null, properties, type, false);
                copy.doc = coreSession.saveDocument(copyDoc);
            }
            coreSession.save();
            return copy;
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void deleteContentStream(String repositoryId,
            Holder<String> objectIdHolder, Holder<String> changeTokenHolder,
            ExtensionsData extension) {
        setContentStream(repositoryId, objectIdHolder, Boolean.TRUE,
                changeTokenHolder, null, extension);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects,
            Boolean continueOnFailure, ExtensionsData extension) {
        if (unfileObjects == UnfileObject.UNFILE) {
            throw new CmisConstraintException("Unfiling not supported");
        }
        if (repository.getRootFolderId().equals(folderId)) {
            throw new CmisInvalidArgumentException("Cannot delete root");
        }
        try {
            DocumentModel doc = getDocumentModel(folderId);
            if (!doc.isFolder()) {
                throw new CmisInvalidArgumentException("Not a folder: "
                        + folderId);
            }
            coreSession.removeDocument(new IdRef(folderId));
            coreSession.save();
            // TODO returning null fails in opencmis 0.1.0 due to
            // org.apache.chemistry.opencmis.client.runtime.PersistentFolderImpl.deleteTree
            return new FailedToDeleteDataImpl();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public AllowableActions getAllowableActions(String repositoryId,
            String objectId, ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        return NuxeoObjectData.getAllowableActions(doc, false);
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId,
            String streamId, BigInteger offset, BigInteger length,
            ExtensionsData extension) {

        if (streamId != null) {
            throw new CmisInvalidArgumentException("Invalid stream id: "
                    + streamId);
        }
        DocumentModel doc = getDocumentModel(objectId);
        ContentStream cs = NuxeoPropertyData.getContentStream(doc);
        if (cs != null) {
            return cs;
        }
        throw new CmisConstraintException("No content stream: " + objectId);
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        DocumentModel doc;
        try {
            DocumentRef pathRef = new PathRef(path);
            if (coreSession.exists(pathRef)) {
                doc = coreSession.getDocument(pathRef);
                if (isFilteredOut(doc)) {
                    throw new CmisObjectNotFoundException(path);
                }
            } else {
                // Adobe Drive 2 confuses cmis:name and path segment
                // try using sequence of titles
                doc = getObjectByPathOfNames(path);
            }
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        ObjectData data = new NuxeoObjectData(repository, doc, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
        if (context.isObjectInfoRequired()) {
            addObjectInfo(getObjectInfo(repositoryId, data));
        }
        return data;
    }

    /**
     * Gets a document given a path built out of dc:title components.
     * <p>
     * Filtered out docs are ignored.
     */
    protected DocumentModel getObjectByPathOfNames(String path)
            throws ClientException, CmisObjectNotFoundException {
        DocumentModel doc = coreSession.getRootDocument();
        for (String name : new Path(path).segments()) {
            String query = String.format(
                    "SELECT * FROM Document WHERE ecm:parentId = %s AND dc:title = %s",
                    escapeStringForNXQL(doc.getId()), escapeStringForNXQL(name));
            DocumentModelList docs = coreSession.query(query);
            if (docs.isEmpty()) {
                throw new CmisObjectNotFoundException(path);
            }
            doc = null;
            for (DocumentModel d : docs) {
                if (isFilteredOut(d)) {
                    continue;
                }
                if (doc == null) {
                    doc = d;
                } else {
                    log.warn(String.format(
                            "Path '%s' returns several documents for '%s'",
                            path, name));
                    break;
                }
            }
            if (doc == null) {
                throw new CmisObjectNotFoundException(path);
            }
        }
        return doc;
    }

    protected static String REPLACE_QUOTE = Matcher.quoteReplacement("\\'");

    protected static String escapeStringForNXQL(String s) {
        return "'" + s.replaceAll("'", REPLACE_QUOTE) + "'";
    }

    @Override
    public Properties getProperties(String repositoryId, String objectId,
            String filter, ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData data = new NuxeoObjectData(repository, doc, filter,
                null, null, null, null, null, null);
        return data.getProperties();
    }

    @Override
    public List<RenditionData> getRenditions(String repositoryId,
            String objectId, String renditionFilter, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectInfo getObjectInfo(String repositoryId, String objectId) {
        return super.getObjectInfo(repositoryId, objectId);
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectIdHolder,
            String targetFolderId, String sourceFolderId,
            ExtensionsData extension) {
        String objectId;
        if (objectIdHolder == null
                || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        if (repository.getRootFolderId().equals(objectId)) {
            throw new CmisConstraintException("Cannot move root");
        }
        if (targetFolderId == null) {
            throw new CmisInvalidArgumentException("Missing target folder ID");
        }
        try {
            getDocumentModel(objectId); // check exists and not deleted
            DocumentRef docRef = new IdRef(objectId);
            DocumentModel parent = coreSession.getParentDocument(docRef);
            if (isFilteredOut(parent)) {
                throw new CmisObjectNotFoundException("No parent: " + objectId);
            }
            if (sourceFolderId == null) {
                sourceFolderId = parent.getId();
            } else {
                // check it's the actual parent
                if (!parent.getId().equals(sourceFolderId)) {
                    throw new CmisInvalidArgumentException("Object " + objectId
                            + " is not filed in " + sourceFolderId);
                }
            }
            DocumentModel target = getDocumentModel(targetFolderId);
            if (!target.isFolder()) {
                throw new CmisInvalidArgumentException(
                        "Target is not a folder: " + targetFolderId);
            }
            coreSession.move(docRef, new IdRef(targetFolderId), null);
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void setContentStream(String repositoryId,
            Holder<String> objectIdHolder, Boolean overwriteFlag,
            Holder<String> changeTokenHolder, ContentStream contentStream,
            ExtensionsData extension) {
        String objectId;
        if (objectIdHolder == null
                || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }

        DocumentModel doc = getDocumentModel(objectId);
        // TODO test doc checkout state
        try {
            NuxeoPropertyData.setContentStream(doc, contentStream,
                    !Boolean.FALSE.equals(overwriteFlag));
            coreSession.saveDocument(doc);
            coreSession.save();
        } catch (CmisBaseException e) {
            throw e;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void updateProperties(String repositoryId,
            Holder<String> objectIdHolder, Holder<String> changeTokenHolder,
            Properties properties, ExtensionsData extension) {
        String objectId;
        if (objectIdHolder == null
                || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData object = new NuxeoObjectData(repository, doc);
        String changeToken = changeTokenHolder == null ? null
                : changeTokenHolder.getValue();
        updateProperties(object, changeToken, properties, false);
        try {
            coreSession.saveDocument(doc);
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl addAces,
            Acl removeAces, AclPropagation aclPropagation,
            ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl aces,
            AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl getAcl(String repositoryId, String objectId,
            Boolean onlyBasicPermissions, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectList getContentChanges(String repositoryId,
            Holder<String> changeLogTokenHolder, Boolean includeProperties,
            String filter, Boolean includePolicyIds, Boolean includeAcl,
            BigInteger maxItems, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectList query(String repositoryId, String statement,
            Boolean searchAllVersions, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        List<ObjectData> list;
        IterableQueryResult res = null;
        try {
            Map<String, PropertyDefinition<?>> typeInfo = new HashMap<String, PropertyDefinition<?>>();
            res = coreSession.queryAndFetch(statement, CMISQLQueryMaker.TYPE,
                    this, typeInfo);

            // convert from Nuxeo to CMIS format
            list = new ArrayList<ObjectData>();
            for (Map<String, Serializable> map : res) {
                ObjectDataImpl od = new ObjectDataImpl();

                // properties (kept in list form)
                PropertiesImpl properties = new PropertiesImpl();
                for (Entry<String, Serializable> en : map.entrySet()) {
                    String queryName = en.getKey();
                    PropertyDefinition<?> pd = typeInfo.get(queryName);
                    if (pd == null) {
                        throw new NullPointerException("Cannot get "
                                + queryName);
                    }
                    PropertyData<?> p = createPropertyData(pd, en.getValue(),
                            queryName);
                    properties.addProperty(p);
                }
                od.setProperties(properties);

                // optional stuff
                if (Boolean.TRUE.equals(includeAllowableActions)) {
                    // od.setAllowableActions(allowableActions);
                }
                if (includeRelationships != null
                        && includeRelationships != IncludeRelationships.NONE) {
                    // od.setRelationships(relationships);
                }
                if (renditionFilter != null && renditionFilter.length() > 0) {
                    // od.setRenditions(renditions);
                }

                list.add(od);
            }
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        } finally {
            if (res != null) {
                res.close();
            }
        }
        ObjectListImpl objList = new ObjectListImpl();
        objList.setObjects(list);
        objList.setNumItems(BigInteger.valueOf(list.size())); // TODO-batching
        objList.setHasMoreItems(Boolean.FALSE);
        return objList;
    }

    // TODO extract and move to BindingsObjectFactoryImpl
    @SuppressWarnings("unchecked")
    protected <T> PropertyData<T> createPropertyData(PropertyDefinition<T> pd,
            Serializable value, String queryName) {
        AbstractPropertyData<T> p;
        String id = pd.getId();
        if (pd instanceof PropertyIdDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyIdData(
                        id, (String) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyIdData(
                        id, (List<String>) value);
            }
        } else if (pd instanceof PropertyStringDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyStringData(
                        id, (String) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyStringData(
                        id, (List<String>) value);
            }
        } else if (pd instanceof PropertyDateTimeDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyDateTimeData(
                        id, (GregorianCalendar) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyDateTimeData(
                        id, (List<GregorianCalendar>) value);
            }
        } else if (pd instanceof PropertyBooleanDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyBooleanData(
                        id, (Boolean) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyBooleanData(
                        id, (List<Boolean>) value);
            }
        } else if (pd instanceof PropertyIntegerDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyIntegerData(
                        id, (BigInteger) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyIntegerData(
                        id, (List<BigInteger>) value);
            }
        } else if (pd instanceof PropertyDecimalDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyDecimalData(
                        id, (BigDecimal) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyDecimalData(
                        id, (List<BigDecimal>) value);
            }
        } else if (pd instanceof PropertyHtmlDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyHtmlData(
                        id, (String) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyHtmlData(
                        id, (List<String>) value);
            }
        } else if (pd instanceof PropertyUriDefinition) {
            if (pd.getCardinality() == Cardinality.SINGLE) {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyUriData(
                        id, (String) value);
            } else {
                p = (AbstractPropertyData<T>) objectFactory.createPropertyUriData(
                        id, (List<String>) value);
            }
        } else {
            throw new CmisRuntimeException("Unknown property definition: " + pd);
        }
        p.setLocalName(pd.getLocalName());
        p.setDisplayName(pd.getDisplayName());
        p.setQueryName(queryName);
        return p;
    }

    @Override
    public void addObjectToFolder(String repositoryId, String objectId,
            String folderId, Boolean allVersions, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        if (folderId != null) {
            // check it's the actual parent
            try {
                DocumentModel folder = getDocumentModel(folderId);
                DocumentModel parent = coreSession.getParentDocument(new IdRef(
                        objectId));
                if (!parent.getId().equals(folder.getId())) {
                    throw new CmisInvalidArgumentException("Object " + objectId
                            + " is not filed in  " + folderId);
                }
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
        deleteObject(repositoryId, objectId, Boolean.FALSE, extension);
    }

    @Override
    public ObjectList getCheckedOutDocs(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        return getChildrenInternal(repositoryId, folderId, filter, orderBy,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment,
                maxItems == null ? -1 : maxItems.intValue(),
                skipCount == null ? -1 : skipCount.intValue(), false,
                context.isObjectInfoRequired() ? this : null);
    }

    protected ObjectInFolderList getChildrenInternal(String repositoryId,
            String folderId, String filter, String orderBy,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, int maxItems, int skipCount,
            boolean folderOnly, ObjectInfoHandler objectInfos) {
        ObjectInFolderListImpl result = new ObjectInFolderListImpl();
        List<ObjectInFolderData> list = new ArrayList<ObjectInFolderData>();
        DocumentModel folder = getDocumentModel(folderId);
        if (!folder.isFolder()) {
            return null;
        }
        DocumentModelList children;
        try {
            children = coreSession.getChildren(folder.getRef());
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        for (DocumentModel child : children) {
            if (isFilteredOut(child)) {
                continue;
            }
            if (folderOnly && !child.isFolder()) {
                continue;
            }
            NuxeoObjectData data = new NuxeoObjectData(repository, child,
                    filter, includeAllowableActions, includeRelationships,
                    renditionFilter, Boolean.FALSE, Boolean.FALSE, null);
            ObjectInFolderDataImpl oifd = new ObjectInFolderDataImpl();
            oifd.setObject(data);
            if (Boolean.TRUE.equals(includePathSegment)) {
                oifd.setPathSegment(child.getName());
            }
            list.add(oifd);
            if (objectInfos != null) {
                objectInfos.addObjectInfo(getObjectInfo(repositoryId, data));
            }
        }
        result.setObjects(list);
        // TODO orderBy
        // TODO maxItems, skipCount
        result.setHasMoreItems(Boolean.FALSE);
        result.setNumItems(BigInteger.valueOf(list.size()));
        if (objectInfos != null) {
            objectInfos.addObjectInfo(getObjectInfo(repositoryId, folderId));
        }
        return result;
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        int levels = depth == null ? 2 : depth.intValue(); // default 2
        if (levels == 0) {
            throw new CmisInvalidArgumentException("Invalid depth: 0");
        }
        return getDescendantsInternal(repositoryId, folderId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment, 0, levels, false,
                context.isObjectInfoRequired() ? this : null);
    }

    protected List<ObjectInFolderContainer> getDescendantsInternal(
            String repositoryId, String folderId, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegments, int level, int maxLevels,
            boolean folderOnly, ObjectInfoHandler objectInfos) {
        if (maxLevels != -1 && level >= maxLevels) {
            return null;
        }
        ObjectInFolderList children = getChildrenInternal(repositoryId,
                folderId, filter, null, includeAllowableActions,
                includeRelationships, renditionFilter, includePathSegments, -1,
                0, folderOnly, objectInfos);
        if (children == null) {
            return Collections.emptyList();
        }
        List<ObjectInFolderContainer> res = new ArrayList<ObjectInFolderContainer>(
                children.getObjects().size());
        for (ObjectInFolderData child : children.getObjects()) {
            ObjectInFolderContainerImpl oifc = new ObjectInFolderContainerImpl();
            oifc.setObject(child);
            // recurse
            List<ObjectInFolderContainer> subChildren = getDescendantsInternal(
                    repositoryId, child.getObject().getId(), filter,
                    includeAllowableActions, includeRelationships,
                    renditionFilter, includePathSegments, level + 1, maxLevels,
                    folderOnly, objectInfos);
            if (subChildren != null) {
                oifc.setChildren(subChildren);
            }
            res.add(oifc);
        }
        return res;
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        List<ObjectParentData> parents = getObjectParentsInternal(repositoryId,
                folderId, filter, null, null, null, Boolean.TRUE, true);
        return parents.isEmpty() ? null : parents.get(0).getObject();
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId,
            String objectId, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        return getObjectParentsInternal(repositoryId, objectId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includeRelativePathSegment, false);
    }

    protected List<ObjectParentData> getObjectParentsInternal(
            String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, boolean folderOnly) {
        String pathSegment;
        String parentId;
        try {
            DocumentRef docRef = new IdRef(objectId);
            if (!coreSession.exists(docRef)) {
                throw new CmisObjectNotFoundException(objectId);
            }
            DocumentModel doc = coreSession.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new CmisObjectNotFoundException(objectId);
            }
            if (folderOnly && !doc.isFolder()) {
                throw new CmisInvalidArgumentException("Not a folder: "
                        + objectId);
            }
            pathSegment = doc.getName();
            DocumentModel parent = coreSession.getParentDocument(docRef);
            if (parent == null || isFilteredOut(parent)) {
                return Collections.emptyList();
            }
            parentId = parent.getId();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }

        ObjectData od = getObject(repositoryId, parentId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                Boolean.FALSE, Boolean.FALSE, null);
        ObjectParentDataImpl opd = new ObjectParentDataImpl(od);
        if (!Boolean.FALSE.equals(includeRelativePathSegment)) {
            opd.setRelativePathSegment(pathSegment);
        }
        return Collections.<ObjectParentData> singletonList(opd);
    }

    @Override
    public void applyPolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectData> getAppliedPolicies(String repositoryId,
            String objectId, String filter, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectList getObjectRelationships(String repositoryId,
            String objectId, Boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, String typeId,
            String filter, Boolean includeAllowableActions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelCheckOut(String repositoryId, String objectId,
            ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkIn(String repositoryId, Holder<String> objectIdHolder,
            Boolean major, Properties properties, ContentStream contentStream,
            String checkinComment, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkOut(String repositoryId, Holder<String> objectIdHolder,
            ExtensionsData extension, Holder<Boolean> contentCopiedHolder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectData> getAllVersions(String repositoryId,
            String objectId, String versionSeriesId, String filter,
            Boolean includeAllowableActions, ExtensionsData extension) {
        return Collections.emptyList();
    }

    @Override
    public ObjectData getObjectOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getPropertiesOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteObject(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        try {
            DocumentModel doc = getDocumentModel(objectId);
            if (doc.isFolder()) {
                // check that there are no children left
                DocumentModelList docs = coreSession.getChildren(new IdRef(
                        objectId), null, documentFilter, null);
                if (docs.size() > 0) {
                    throw new CmisConstraintException(
                            "Cannot delete non-empty folder: " + objectId);
                }
            }
            coreSession.removeDocument(doc.getRef());
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId,
            String objectId, Boolean allVersions, ExtensionsData extension) {
        deleteObject(repositoryId, objectId, allVersions, extension);
    }

}
