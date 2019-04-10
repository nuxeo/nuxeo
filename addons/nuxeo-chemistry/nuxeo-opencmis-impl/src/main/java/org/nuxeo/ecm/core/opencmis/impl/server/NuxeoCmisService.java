/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.core.blob.binary.AbstractBinaryManager.MD5_DIGEST;
import static org.nuxeo.ecm.core.opencmis.impl.server.NuxeoContentStream.CONTENT_MD5_DIGEST_ALGORITHM;
import static org.nuxeo.ecm.core.opencmis.impl.server.NuxeoContentStream.CONTENT_MD5_HEADER_NAME;
import static org.nuxeo.ecm.core.opencmis.impl.server.NuxeoContentStream.DIGEST_HEADER_NAME;
import static org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData.REND_STREAM_RENDITION_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.commons.BasicPermissions;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
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
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ChangeType;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.WSConverter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyData;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BulkUpdateObjectIdAndChangeTokenImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChangeEventInfoDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.jaxb.CmisTypeContainer;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.server.ProgressControlCmisService;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.server.support.wrapper.AbstractCmisServiceWrapper;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.opencmis.impl.util.ListUtils;
import org.nuxeo.ecm.core.opencmis.impl.util.ListUtils.BatchedList;
import org.nuxeo.ecm.core.opencmis.impl.util.SimpleImageInfo;
import org.nuxeo.ecm.core.opencmis.impl.util.TypeManagerImpl;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocumentVersion.VersionNotModifiableException;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsIterableQueryResultImpl;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONReader;
import org.nuxeo.elasticsearch.core.EsSearchHitConverter;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Nuxeo implementation of the CMIS Services, on top of a {@link CoreSession}.
 */
public class NuxeoCmisService extends AbstractCmisService
        implements CallContextAwareCmisService, ProgressControlCmisService {

    public static final int DEFAULT_TYPE_LEVELS = 2;

    public static final int DEFAULT_FOLDER_LEVELS = 2;

    public static final int DEFAULT_CHANGE_LOG_SIZE = 100;

    public static final int MAX_CHANGE_LOG_SIZE = 1000 * 1000;

    public static final int DEFAULT_QUERY_SIZE = 100;

    public static final int DEFAULT_MAX_CHILDREN = 100;

    public static final int DEFAULT_MAX_RELATIONSHIPS = 100;

    public static final String PERMISSION_NOTHING = "Nothing";

    /** Synthetic property for change log entries recording the log entry id. */
    public static final String NX_CHANGE_LOG_ID = "nuxeo:changeLogId";

    public static final String ES_AUDIT_ID = "id";

    public static final String ES_AUDIT_REPOSITORY_ID = "repositoryId";

    public static final String ES_AUDIT_EVENT_ID = "eventId";

    private static final Log log = LogFactory.getLog(NuxeoCmisService.class);

    protected final BindingsObjectFactory objectFactory = new BindingsObjectFactoryImpl();

    protected final NuxeoRepository repository;

    /** When false, we don't own the core session and shouldn't close it. */
    protected final boolean coreSessionOwned;

    protected CoreSession coreSession;

    /* To avoid refetching it several times per session. */
    protected String cachedChangeLogToken;

    protected CallContext callContext;

    /** Filter that hides HiddenInNavigation and deleted objects. */
    protected final Filter documentFilter;

    protected final Set<String> readPermissions;

    protected final Set<String> writePermissions;

    public static NuxeoCmisService extractFromCmisService(CmisService service) {
        if (service == null) {
            throw new NullPointerException();
        }
        for (;;) {
            if (service instanceof NuxeoCmisService) {
                return (NuxeoCmisService) service;
            }
            if (!(service instanceof AbstractCmisServiceWrapper)) {
                return null;
            }
            service = ((AbstractCmisServiceWrapper) service).getWrappedService();
        }
    }

    /**
     * Constructs a Nuxeo CMIS Service from an existing {@link CoreSession}.
     *
     * @param coreSession the session
     * @since 6.0
     */
    public NuxeoCmisService(CoreSession coreSession) {
        this(coreSession, coreSession.getRepositoryName());
    }

    /**
     * Constructs a Nuxeo CMIS Service.
     *
     * @param repositoryName the repository name
     * @since 6.0
     */
    public NuxeoCmisService(String repositoryName) {
        this(null, repositoryName);
    }

    protected NuxeoCmisService(CoreSession coreSession, String repositoryName) {
        this.coreSession = coreSession;
        coreSessionOwned = coreSession == null;
        repository = getNuxeoRepository(repositoryName);
        documentFilter = getDocumentFilter();
        SecurityService securityService = Framework.getService(SecurityService.class);
        readPermissions = new HashSet<>(Arrays.asList(securityService.getPermissionsToCheck(SecurityConstants.READ)));
        writePermissions = new HashSet<>(
                Arrays.asList(securityService.getPermissionsToCheck(SecurityConstants.READ_WRITE)));
    }

    // called in a finally block from dispatcher
    @Override
    public void close() {
        if (coreSessionOwned && coreSession != null) {
            coreSession.close();
            coreSession = null;
        }
        clearObjectInfos();
    }

    @Override
    public Progress beforeServiceCall() {
        return Progress.CONTINUE;
    }

    @Override
    public Progress afterServiceCall() {
        // check if there is a transaction timeout
        // if yes, abort and return a 503 (Service Unavailable)
        if (!TransactionHelper.setTransactionRollbackOnlyIfTimedOut()) {
            return Progress.CONTINUE;
        }
        HttpServletResponse response = (HttpServletResponse) getCallContext().get(CallContext.HTTP_SERVLET_RESPONSE);
        if (response != null) {
            try {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Transaction timeout");
            } catch (IOException e) {
                throw new CmisRuntimeException("Failed to set timeout status", e);
            }
        }
        return Progress.STOP;
    }

    protected static NuxeoRepository getNuxeoRepository(String repositoryName) {
        if (repositoryName == null) {
            return null;
        }
        return Framework.getService(NuxeoRepositories.class).getRepository(repositoryName);
    }

    protected static CoreSession openCoreSession(String repositoryName, String username) {
        if (repositoryName == null) {
            return null;
        }
        return CoreInstance.openCoreSession(repositoryName, username);
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

    @Override
    public CallContext getCallContext() {
        return callContext;
    }

    protected TypeManagerImpl getTypeManager() {
        CmisVersion cmisVersion = callContext == null ? CmisVersion.CMIS_1_1 : callContext.getCmisVersion();
        return repository.getTypeManager(cmisVersion);
    }

    @Override
    public void setCallContext(CallContext callContext) {
        close();
        this.callContext = callContext;
        if (coreSessionOwned) {
            // for non-local binding, the principal is found
            // in the login stack
            String username = callContext.getBinding().equals(CallContext.BINDING_LOCAL) ? callContext.getUsername()
                    : null;
            coreSession = repository == null ? null : openCoreSession(repository.getId(), username);
        }
    }

    /** Gets the filter that hides HiddenInNavigation and deleted objects. */
    protected Filter getDocumentFilter() {
        Filter facetFilter = new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION, false);
        Filter lcFilter = new LifeCycleFilter(LifeCycleConstants.DELETED_STATE, false);
        return new CompoundFilter(facetFilter, lcFilter);
    }

    protected String getIdFromDocumentRef(DocumentRef ref) {
        if (ref instanceof IdRef) {
            return ((IdRef) ref).value;
        } else {
            return coreSession.getDocument(ref).getId();
        }
    }

    protected void save() {
        coreSession.save();
        cachedChangeLogToken = null;
    }

    /* This is the only method that does not have a repositoryId / coreSession. */
    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        List<NuxeoRepository> repos = Framework.getService(NuxeoRepositories.class).getRepositories();
        List<RepositoryInfo> infos = new ArrayList<>(repos.size());
        for (NuxeoRepository repo : repos) {
            String latestChangeLogToken = getLatestChangeLogToken(repo.getId());
            infos.add(repo.getRepositoryInfo(latestChangeLogToken, callContext));
        }
        return infos;
    }

    @Override
    public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {
        String latestChangeLogToken;
        if (cachedChangeLogToken != null) {
            latestChangeLogToken = cachedChangeLogToken;
        } else {
            latestChangeLogToken = getLatestChangeLogToken(repositoryId);
            cachedChangeLogToken = latestChangeLogToken;
        }
        NuxeoRepository repository = getNuxeoRepository(repositoryId);
        return repository.getRepositoryInfo(latestChangeLogToken, callContext);
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId, ExtensionsData extension) {
        TypeDefinition type = getTypeManager().getTypeDefinition(typeId);
        if (type == null) {
            throw new CmisInvalidArgumentException("No such type: " + typeId);
        }
        // TODO copy only when local binding
        // clone
        return WSConverter.convert(WSConverter.convert(type));

    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        TypeDefinitionList types = getTypeManager().getTypeChildren(typeId, includePropertyDefinitions, maxItems,
                skipCount);
        // TODO copy only when local binding
        // clone
        return WSConverter.convert(WSConverter.convert(types));
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {
        int d = depth == null ? DEFAULT_TYPE_LEVELS : depth.intValue();
        List<TypeDefinitionContainer> types = getTypeManager().getTypeDescendants(typeId, d,
                includePropertyDefinitions);
        // clone
        // TODO copy only when local binding
        List<CmisTypeContainer> tmp = new ArrayList<>(types.size());
        WSConverter.convertTypeContainerList(types, tmp);
        return WSConverter.convertTypeContainerList(tmp);
    }

    protected DocumentModel getDocumentModel(String id) {
        DocumentRef docRef = new IdRef(id);
        if (!coreSession.exists(docRef)) {
            throw new CmisObjectNotFoundException(docRef.toString());
        }
        DocumentModel doc = coreSession.getDocument(docRef);
        if (isFilteredOut(doc)) {
            throw new CmisObjectNotFoundException(docRef.toString());
        }
        return doc;
    }

    @Override
    public NuxeoObjectData getObject(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData data = new NuxeoObjectData(this, doc, filter, includeAllowableActions, includeRelationships,
                renditionFilter, includePolicyIds, includeAcl, extension);
        collectObjectInfo(repositoryId, objectId);
        return data;
    }

    /**
     * Checks if the doc should be ignored because it is "invisible" (deleted, hidden in navigation).
     */
    public boolean isFilteredOut(DocumentModel doc) {
        // don't filter out relations even though they may be HiddenInNavigation
        if (NuxeoTypeHelper.getBaseTypeId(doc).equals(BaseTypeId.CMIS_RELATIONSHIP)) {
            return false;
        }
        return !documentFilter.accept(doc);
    }

    /** Creates bare unsaved document model. */
    protected DocumentModel createDocumentModel(ObjectId folder, TypeDefinition type) {
        DocumentModel doc;
        String typeId = type.getId();
        String nuxeoTypeId = type.getLocalName();
        if (BaseTypeId.CMIS_DOCUMENT.value().equals(typeId)) {
            nuxeoTypeId = NuxeoTypeHelper.NUXEO_FILE;
        } else if (BaseTypeId.CMIS_FOLDER.value().equals(typeId)) {
            nuxeoTypeId = NuxeoTypeHelper.NUXEO_FOLDER;
        } else if (BaseTypeId.CMIS_RELATIONSHIP.value().equals(typeId)) {
            nuxeoTypeId = NuxeoTypeHelper.NUXEO_RELATION_DEFAULT;
        }
        doc = coreSession.createDocumentModel(nuxeoTypeId);
        if (folder != null) {
            DocumentRef parentRef = new IdRef(folder.getId());
            if (!coreSession.exists(parentRef)) {
                throw new CmisInvalidArgumentException(parentRef.toString());
            }
            DocumentModel parentDoc = coreSession.getDocument(parentRef);
            String pathSegment = nuxeoTypeId; // default path segment based on id
            doc.setPathInfo(parentDoc.getPathAsString(), pathSegment);
        }
        return doc;
    }

    /** Creates and save document model. */
    protected DocumentModel createDocumentModel(ObjectId folder, ContentStream contentStream, String name) {
        FileManager fileManager = Framework.getLocalService(FileManager.class);
        MimetypeRegistryService mtr = (MimetypeRegistryService) Framework.getLocalService(MimetypeRegistry.class);
        if (fileManager == null || mtr == null || name == null || folder == null) {
            return null;
        }

        DocumentModel parent = coreSession.getDocument(new IdRef(folder.getId()));
        String path = parent.getPathAsString();

        Blob blob;
        if (contentStream == null) {
            String mimeType;
            try {
                mimeType = mtr.getMimetypeFromFilename(name);
            } catch (MimetypeNotFoundException e) {
                mimeType = MimetypeRegistry.DEFAULT_MIMETYPE;
            }
            blob = Blobs.createBlob("", mimeType, null, name);
        } else {
            try {
                blob = NuxeoPropertyData.getPersistentBlob(contentStream, null);
            } catch (IOException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }

        try {
            return fileManager.createDocumentFromBlob(coreSession, blob, path, false, name);
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    // create and save session
    protected NuxeoObjectData createObject(String repositoryId, Properties properties, ObjectId folder,
            BaseTypeId baseType, ContentStream contentStream) {
        String typeId;
        Map<String, PropertyData<?>> p;
        PropertyData<?> d;
        TypeDefinition type = null;
        if (properties != null //
                && (p = properties.getProperties()) != null //
                && (d = p.get(PropertyIds.OBJECT_TYPE_ID)) != null) {
            typeId = (String) d.getFirstValue();
            if (baseType == null) {
                type = getTypeManager().getTypeDefinition(typeId);
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
                typeId = BaseTypeId.CMIS_DOCUMENT.value();
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
            type = getTypeManager().getTypeDefinition(typeId);
        }
        if (type == null || type.getBaseTypeId() != baseType) {
            throw new CmisInvalidArgumentException(typeId);
        }
        if (type.isCreatable() == Boolean.FALSE) {
            throw new CmisInvalidArgumentException("Not creatable: " + typeId);
        }

        // name from properties
        PropertyData<?> npd = properties.getProperties().get(PropertyIds.NAME);
        String name = npd == null ? null : (String) npd.getFirstValue();
        if (StringUtils.isBlank(name)) {
            throw new CmisConstraintException("The mandatory property " + PropertyIds.NAME + " is missing");
        }

        // content stream filename default
        if (contentStream != null && StringUtils.isBlank(contentStream.getFileName())) {
            // infer filename from name property
            contentStream = new ContentStreamImpl(name, contentStream.getBigLength(),
                    contentStream.getMimeType().trim(), contentStream.getStream());
        }

        DocumentModel doc = null;
        if (BaseTypeId.CMIS_DOCUMENT.value().equals(typeId)) {
            doc = createDocumentModel(folder, contentStream, name);
        }
        boolean created = doc != null;
        if (!created) {
            doc = createDocumentModel(folder, type);
        }

        NuxeoObjectData data = new NuxeoObjectData(this, doc);
        updateProperties(data, null, properties, true);
        boolean setContentStream = !created && contentStream != null;
        if (setContentStream) {
            try {
                NuxeoPropertyData.setContentStream(doc, contentStream, true);
            } catch (CmisContentAlreadyExistsException e) {
                // cannot happen, overwrite = true
            } catch (IOException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
        if (!created) {
            // set path segment from properties (name/title)
            PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
            String pathSegment = pss.generatePathSegment(doc);
            Path path = doc.getPath();
            doc.setPathInfo(path == null ? null : path.removeLastSegments(1).toString(), pathSegment);
            doc = coreSession.createDocument(doc);
        } else {
            doc = coreSession.saveDocument(doc);
        }
        if (setContentStream) {
            NuxeoPropertyData.validateBlobDigest(doc, callContext);
        }
        data.doc = doc;
        save();
        collectObjectInfo(repositoryId, data.getId());
        return data;
    }

    protected <T> void updateProperties(NuxeoObjectData object, String changeToken, Properties properties,
            boolean creation) {
        List<TypeDefinition> types = object.getTypeDefinitions();
        // TODO changeToken
        Map<String, PropertyData<?>> p;
        if (properties == null || (p = properties.getProperties()) == null) {
            return;
        }
        for (Entry<String, PropertyData<?>> en : p.entrySet()) {
            String key = en.getKey();
            PropertyData<?> d = en.getValue();
            setObjectProperty(object, key, d, types, creation);
        }
    }

    protected <T> void updateProperties(NuxeoObjectData object, String changeToken, Map<String, ?> properties,
            TypeDefinition type, boolean creation) {
        // TODO changeToken
        if (properties == null) {
            return;
        }
        for (Entry<String, ?> en : properties.entrySet()) {
            String key = en.getKey();
            Object value = en.getValue();
            @SuppressWarnings("unchecked")
            PropertyDefinition<T> pd = (PropertyDefinition<T>) type.getPropertyDefinitions().get(key);
            if (pd == null) {
                throw new CmisRuntimeException("Unknown property: " + key);
            }
            setObjectProperty(object, key, value, pd, creation);
        }
    }

    protected <T> void setObjectProperty(NuxeoObjectData object, String key, PropertyData<T> d,
            List<TypeDefinition> types, boolean creation) {
        PropertyDefinition<T> pd = null;
        for (TypeDefinition type : types) {
            pd = (PropertyDefinition<T>) type.getPropertyDefinitions().get(key);
            if (pd != null) {
                break;
            }
        }
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

    protected <T> void setObjectProperty(NuxeoObjectData object, String key, Object value, PropertyDefinition<T> pd,
            boolean creation) {
        Updatability updatability = pd.getUpdatability();
        if (updatability == Updatability.READONLY || (updatability == Updatability.ONCREATE && !creation)) {
            // log.error("Read-only property, ignored: " + key);
            return;
        }
        if (PropertyIds.OBJECT_TYPE_ID.equals(key) || PropertyIds.LAST_MODIFICATION_DATE.equals(key)) {
            return;
        }
        // TODO avoid constructing property object just to set value
        NuxeoPropertyDataBase<T> np = (NuxeoPropertyDataBase<T>) NuxeoPropertyData.construct(object, pd, callContext);
        np.setValue(value);
    }

    /** Sets initial versioning state and returns its id. */
    protected String setInitialVersioningState(NuxeoObjectData object, VersioningState versioningState) {
        if (versioningState == null) {
            // default is MAJOR, per spec
            versioningState = VersioningState.MAJOR;
        }
        String id;
        DocumentRef ref = null;
        switch (versioningState) {
        case NONE: // cannot be made non-versionable in Nuxeo
        case CHECKEDOUT:
            object.doc.setLock();
            save();
            id = object.getId();
            break;
        case MINOR:
            ref = object.doc.checkIn(VersioningOption.MINOR, null);
            save();
            // id = ref.toString();
            id = object.getId();
            break;
        case MAJOR:
            ref = object.doc.checkIn(VersioningOption.MAJOR, null);
            save();
            // id = ref.toString();
            id = object.getId();
            break;
        default:
            throw new AssertionError(versioningState);
        }
        return id;
    }

    @Override
    public String create(String repositoryId, Properties properties, String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies, ExtensionsData extension) {
        // TODO policies
        NuxeoObjectData object = createObject(repositoryId, properties, new ObjectIdImpl(folderId), null,
                contentStream);
        return setInitialVersioningState(object, versioningState);
    }

    @Override
    public String createDocument(String repositoryId, Properties properties, String folderId,
            ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO policies, addAces, removeAces
        NuxeoObjectData object = createObject(repositoryId, properties, new ObjectIdImpl(folderId),
                BaseTypeId.CMIS_DOCUMENT, contentStream);
        return setInitialVersioningState(object, versioningState);
    }

    @Override
    public String createFolder(String repositoryId, Properties properties, String folderId, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        // TODO policies, addAces, removeAces
        NuxeoObjectData object = createObject(repositoryId, properties, new ObjectIdImpl(folderId),
                BaseTypeId.CMIS_FOLDER, null);
        return object.getId();
    }

    @Override
    public String createPolicy(String repositoryId, Properties properties, String folderId, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        throw new CmisNotSupportedException();
    }

    @Override
    public String createRelationship(String repositoryId, Properties properties, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        NuxeoObjectData object = createObject(repositoryId, properties, null, BaseTypeId.CMIS_RELATIONSHIP, null);
        return object.getId();
    }

    @Override
    public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties, String folderId,
            VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {
        if (folderId == null) {
            // no unfileable objects for now
            throw new CmisInvalidArgumentException("Invalid null folder ID");
        }
        DocumentModel doc = getDocumentModel(sourceId);
        DocumentModel folder = getDocumentModel(folderId);
        DocumentModel copyDoc = coreSession.copy(doc.getRef(), folder.getRef(), null);
        NuxeoObjectData copy = new NuxeoObjectData(this, copyDoc);
        if (properties != null && properties.getPropertyList() != null && !properties.getPropertyList().isEmpty()) {
            updateProperties(copy, null, properties, false);
            copy.doc = coreSession.saveDocument(copyDoc);
        }
        save();
        return setInitialVersioningState(copy, versioningState);
    }

    public NuxeoObjectData copy(String sourceId, String targetId, Map<String, ?> properties, TypeDefinition type,
            VersioningState versioningState, List<Policy> policies, List<Ace> addACEs, List<Ace> removeACEs,
            OperationContext context) {
        DocumentModel doc = getDocumentModel(sourceId);
        DocumentModel folder = getDocumentModel(targetId);
        DocumentModel copyDoc = coreSession.copy(doc.getRef(), folder.getRef(), null);
        NuxeoObjectData copy = new NuxeoObjectData(this, copyDoc, context);
        if (properties != null && !properties.isEmpty()) {
            updateProperties(copy, null, properties, type, false);
            copy.doc = coreSession.saveDocument(copyDoc);
        }
        save();
        String id = setInitialVersioningState(copy, versioningState);
        NuxeoObjectData res;
        if (id.equals(copy.getId())) {
            res = copy;
        } else {
            // return the version
            res = new NuxeoObjectData(this, getDocumentModel(id));
        }
        return res;
    }

    @Override
    public void deleteContentStream(String repositoryId, Holder<String> objectIdHolder,
            Holder<String> changeTokenHolder, ExtensionsData extension) {
        setContentStream(repositoryId, objectIdHolder, Boolean.TRUE, changeTokenHolder, null, extension);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions,
            UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {
        if (unfileObjects == UnfileObject.UNFILE) {
            throw new CmisConstraintException("Unfiling not supported");
        }
        if (repository.getRootFolderId().equals(folderId)) {
            throw new CmisInvalidArgumentException("Cannot delete root");
        }
        DocumentModel doc = getDocumentModel(folderId);
        if (!doc.isFolder()) {
            throw new CmisInvalidArgumentException("Not a folder: " + folderId);
        }
        coreSession.removeDocument(new IdRef(folderId));
        save();
        // TODO returning null fails in opencmis 0.1.0 due to
        // org.apache.chemistry.opencmis.client.runtime.PersistentFolderImpl.deleteTree
        return new FailedToDeleteDataImpl();
    }

    @Override
    public AllowableActions getAllowableActions(String repositoryId, String objectId, ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        return NuxeoObjectData.getAllowableActions(doc, false);
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId, String streamId, BigInteger offset,
            BigInteger length, ExtensionsData extension) {
        // TODO offset, length
        ContentStream cs = null;
        HttpServletRequest request = (HttpServletRequest) callContext.get(CallContext.HTTP_SERVLET_REQUEST);
        if (streamId == null) {
            DocumentModel doc = getDocumentModel(objectId);
            cs = NuxeoPropertyData.getContentStream(doc, request);
            if (cs == null) {
                throw new CmisConstraintException("No content stream: " + objectId);
            }
        } else {
            String renditionName = streamId.replaceAll("^" + REND_STREAM_RENDITION_PREFIX, "");
            cs = getRenditionServiceStream(objectId, renditionName);
            if (cs == null) {
                throw new CmisInvalidArgumentException("Invalid stream id: " + streamId);
            }
        }
        if (cs instanceof NuxeoContentStream) {
            NuxeoContentStream ncs = (NuxeoContentStream) cs;
            Blob blob = ncs.blob;
            String blobDigestAlgorithm = blob.getDigestAlgorithm();
            if (MD5_DIGEST.equals(blobDigestAlgorithm)
                    && NuxeoContentStream.hasWantDigestRequestHeader(request, CONTENT_MD5_DIGEST_ALGORITHM)) {
                setResponseHeader(CONTENT_MD5_HEADER_NAME, blob, callContext);
            }
            if (NuxeoContentStream.hasWantDigestRequestHeader(request, blobDigestAlgorithm)) {
                setResponseHeader(DIGEST_HEADER_NAME, blob, callContext);
            }
        }
        return cs;
    }

    protected void setResponseHeader(String headerName, Blob blob, CallContext callContext) {
        String digest = NuxeoPropertyData.transcodeHexToBase64(blob.getDigest());
        HttpServletResponse response = (HttpServletResponse) callContext.get(CallContext.HTTP_SERVLET_RESPONSE);
        if (DIGEST_HEADER_NAME.equalsIgnoreCase(headerName)) {
            digest = blob.getDigestAlgorithm() + "=" + digest;
        }
        response.setHeader(headerName, digest);
    }

    /**
     * @deprecated since 7.3. The thumbnail is now a default rendition, see NXP-16662.
     */
    @Deprecated
    protected ContentStream getIconRenditionStream(String objectId) {
        DocumentModel doc = getDocumentModel(objectId);
        String iconPath;
        try {
            iconPath = (String) doc.getPropertyValue(NuxeoTypeHelper.NX_ICON);
        } catch (PropertyException e) {
            iconPath = null;
        }
        InputStream is = NuxeoObjectData.getIconStream(iconPath, callContext);
        if (is == null) {
            throw new CmisConstraintException("No icon content stream: " + objectId);
        }

        int slash = iconPath.lastIndexOf('/');
        String filename = slash == -1 ? iconPath : iconPath.substring(slash + 1);

        SimpleImageInfo info;
        try {
            info = new SimpleImageInfo(is);
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        // refetch now-consumed stream
        is = NuxeoObjectData.getIconStream(iconPath, callContext);
        return new ContentStreamImpl(filename, BigInteger.valueOf(info.getLength()), info.getMimeType(), is);
    }

    protected ContentStream getRenditionServiceStream(String objectId, String renditionName) {
        RenditionService renditionService = Framework.getLocalService(RenditionService.class);
        DocumentModel doc = getDocumentModel(objectId);
        Rendition rendition = renditionService.getRendition(doc, renditionName);
        if (rendition == null) {
            return null;
        }
        Blob blob = rendition.getBlob();
        if (blob == null) {
            return null;
        }

        Calendar modificationDate = rendition.getModificationDate();
        GregorianCalendar lastModified = (modificationDate instanceof GregorianCalendar)
                ? (GregorianCalendar) modificationDate : null;
        HttpServletRequest request = (HttpServletRequest) getCallContext().get(CallContext.HTTP_SERVLET_REQUEST);
        return NuxeoContentStream.create(doc, null, blob, "cmisRendition",
                Collections.singletonMap("rendition", renditionName), lastModified, request);
    }

    @Override
    public List<RenditionData> getRenditions(String repositoryId, String objectId, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        if (!NuxeoObjectData.needsRenditions(renditionFilter)) {
            return Collections.emptyList();
        }
        DocumentModel doc = getDocumentModel(objectId);
        return NuxeoObjectData.getRenditions(doc, renditionFilter, maxItems, skipCount, callContext);
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
            Boolean includeAcl, ExtensionsData extension) {
        DocumentModel doc;
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
        ObjectData data = new NuxeoObjectData(this, doc, filter, includeAllowableActions, includeRelationships,
                renditionFilter, includePolicyIds, includeAcl, extension);
        collectObjectInfo(repositoryId, data.getId());
        return data;
    }

    /**
     * Gets a document given a path built out of dc:title components.
     * <p>
     * Filtered out docs are ignored.
     */
    protected DocumentModel getObjectByPathOfNames(String path) throws CmisObjectNotFoundException {
        DocumentModel doc = coreSession.getRootDocument();
        for (String name : new Path(path).segments()) {
            String query = String.format("SELECT * FROM Document WHERE " + NXQL.ECM_PARENTID + " = %s AND "
                    + NuxeoTypeHelper.NX_DC_TITLE + " = %s", escapeStringForNXQL(doc.getId()),
                    escapeStringForNXQL(name));
            query = addProxyClause(query);
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
                    log.warn(String.format("Path '%s' returns several documents for '%s'", path, name));
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
    public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData data = new NuxeoObjectData(this, doc, filter, null, null, null, null, null, null);
        return data.getProperties();
    }

    protected boolean collectObjectInfos = true;

    protected Map<String, ObjectInfo> objectInfos;

    // part of CMIS API and of ObjectInfoHandler
    @Override
    public ObjectInfo getObjectInfo(String repositoryId, String objectId) {
        ObjectInfo info = getObjectInfo().get(objectId);
        if (info != null) {
            return info;
        }
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData data = new NuxeoObjectData(this, doc, null, Boolean.TRUE, IncludeRelationships.BOTH, "*",
                Boolean.TRUE, Boolean.TRUE, null);
        return getObjectInfo(repositoryId, data);
    }

    // AbstractCmisService helper
    protected ObjectInfo getObjectInfo(String repositoryId, ObjectData data) {
        ObjectInfo info = getObjectInfo().get(data.getId());
        if (info != null) {
            return info;
        }
        try {
            collectObjectInfos = false;
            info = getObjectInfoIntern(repositoryId, data);
            getObjectInfo().put(info.getId(), info);
        } finally {
            collectObjectInfos = true;
        }
        return info;
    }

    protected Map<String, ObjectInfo> getObjectInfo() {
        if (objectInfos == null) {
            objectInfos = new HashMap<>();
        }
        return objectInfos;
    }

    @Override
    public void clearObjectInfos() {
        objectInfos = null;
    }

    protected void collectObjectInfo(String repositoryId, String objectId) {
        if (collectObjectInfos && callContext.isObjectInfoRequired()) {
            getObjectInfo(repositoryId, objectId);
        }
    }

    @Override
    public void addObjectInfo(ObjectInfo info) {
        // ObjectInfoHandler, unused here
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectIdHolder, String targetFolderId,
            String sourceFolderId, ExtensionsData extension) {
        String objectId;
        if (objectIdHolder == null || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        if (repository.getRootFolderId().equals(objectId)) {
            throw new CmisConstraintException("Cannot move root");
        }
        if (targetFolderId == null) {
            throw new CmisInvalidArgumentException("Missing target folder ID");
        }
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
                throw new CmisInvalidArgumentException("Object " + objectId + " is not filed in " + sourceFolderId);
            }
        }
        DocumentModel target = getDocumentModel(targetFolderId);
        if (!target.isFolder()) {
            throw new CmisInvalidArgumentException("Target is not a folder: " + targetFolderId);
        }
        coreSession.move(docRef, new IdRef(targetFolderId), null);
        save();
    }

    @Override
    public void setContentStream(String repositoryId, Holder<String> objectIdHolder, Boolean overwriteFlag,
            Holder<String> changeTokenHolder, ContentStream contentStream, ExtensionsData extension) {
        String objectId;
        if (objectIdHolder == null || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }

        DocumentModel doc = getDocumentModel(objectId);
        // TODO test doc checkout state
        try {
            NuxeoPropertyData.setContentStream(doc, contentStream, !Boolean.FALSE.equals(overwriteFlag));
            doc = coreSession.saveDocument(doc);
            NuxeoPropertyData.validateBlobDigest(doc, callContext);
            save();
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void updateProperties(String repositoryId, Holder<String> objectIdHolder, Holder<String> changeTokenHolder,
            Properties properties, ExtensionsData extension) {
        updateProperties(objectIdHolder, changeTokenHolder, properties);
        save();
    }

    /* does not save the session */
    protected void updateProperties(Holder<String> objectIdHolder, Holder<String> changeTokenHolder,
            Properties properties) {
        String objectId;
        if (objectIdHolder == null || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        DocumentModel doc = getDocumentModel(objectId);
        NuxeoObjectData object = new NuxeoObjectData(this, doc);
        String changeToken = changeTokenHolder == null ? null : changeTokenHolder.getValue();
        updateProperties(object, changeToken, properties, false);
        coreSession.saveDocument(doc);
    }

    @Override
    public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(String repositoryId,
            List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken, Properties properties,
            List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, ExtensionsData extension) {
        List<BulkUpdateObjectIdAndChangeToken> list = new ArrayList<>(objectIdAndChangeToken.size());
        for (BulkUpdateObjectIdAndChangeToken idt : objectIdAndChangeToken) {
            String id = idt.getId();
            Holder<String> objectIdHolder = new Holder<>(id);
            Holder<String> changeTokenHolder = new Holder<>(idt.getChangeToken());
            updateProperties(objectIdHolder, changeTokenHolder, properties);
            list.add(new BulkUpdateObjectIdAndChangeTokenImpl(id, objectIdHolder.getValue(),
                    changeTokenHolder.getValue()));
        }
        save();
        return list;
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl addAces, Acl removeAces,
            AclPropagation aclPropagation, ExtensionsData extension) {
        return applyAcl(objectId, addAces, removeAces, false, aclPropagation);
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl aces, AclPropagation aclPropagation) {
        return applyAcl(objectId, aces, null, true, aclPropagation);
    }

    protected Acl applyAcl(String objectId, Acl addAces, Acl removeAces, boolean clearFirst,
            AclPropagation aclPropagation) {
        DocumentModel doc = getDocumentModel(objectId); // does filtering
        if (aclPropagation == null) {
            aclPropagation = AclPropagation.REPOSITORYDETERMINED;
        }
        if (aclPropagation == AclPropagation.OBJECTONLY && doc.getDocumentType().isFolder()) {
            throw new CmisInvalidArgumentException("Cannot use ACLPropagation=objectonly on Folder");
        }
        DocumentRef docRef = new IdRef(objectId);

        ACP acp = coreSession.getACP(docRef);

        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        if (clearFirst) {
            acl.clear();
        }

        if (addAces != null) {
            for (Ace ace : addAces.getAces()) {
                String principalId = ace.getPrincipalId();
                for (String permission : ace.getPermissions()) {
                    String perm = permissionToNuxeo(permission);
                    if (PERMISSION_NOTHING.equals(perm)) {
                        // block everything
                        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
                    } else {
                        acl.add(new ACE(principalId, perm, true));
                    }
                }
            }
        }

        if (removeAces != null) {
            for (Iterator<ACE> it = acl.iterator(); it.hasNext();) {
                ACE ace = it.next();
                String username = ace.getUsername();
                String perm = ace.getPermission();
                if (ace.isDenied()) {
                    if (SecurityConstants.EVERYONE.equals(username) && SecurityConstants.EVERYTHING.equals(perm)) {
                        perm = PERMISSION_NOTHING;
                    } else {
                        continue;
                    }
                }
                String permission = permissionFromNuxeo(perm);
                for (Ace race : removeAces.getAces()) {
                    String principalId = race.getPrincipalId();
                    if (!username.equals(principalId)) {
                        continue;
                    }
                    if (race.getPermissions().contains(permission)) {
                        it.remove();
                        break;
                    }
                }
            }
        }
        coreSession.setACP(docRef, acp, true);
        return NuxeoObjectData.getAcl(acp, false, this);
    }

    protected static String permissionToNuxeo(String permission) {
        switch (permission) {
        case BasicPermissions.READ:
            return SecurityConstants.READ;
        case BasicPermissions.WRITE:
            return SecurityConstants.READ_WRITE;
        case BasicPermissions.ALL:
            return SecurityConstants.EVERYTHING;
        default:
            return permission;
        }
    }

    protected static String permissionFromNuxeo(String permission) {
        switch (permission) {
        case SecurityConstants.READ:
            return BasicPermissions.READ;
        case SecurityConstants.READ_WRITE:
            return BasicPermissions.WRITE;
        case SecurityConstants.EVERYTHING:
            return BasicPermissions.ALL;
        default:
            return permission;
        }
    }

    @Override
    public Acl getAcl(String repositoryId, String objectId, Boolean onlyBasicPermissions, ExtensionsData extension) {
        boolean basic = !Boolean.FALSE.equals(onlyBasicPermissions);
        getDocumentModel(objectId); // does filtering
        ACP acp = coreSession.getACP(new IdRef(objectId));
        return NuxeoObjectData.getAcl(acp, basic, this);
    }

    @Override
    public ObjectList getContentChanges(String repositoryId, Holder<String> changeLogTokenHolder,
            Boolean includeProperties, String filter, Boolean includePolicyIds, Boolean includeAcl, BigInteger maxItems,
            ExtensionsData extension) {
        if (changeLogTokenHolder == null) {
            throw new CmisInvalidArgumentException("Missing change log token holder");
        }
        String changeLogToken = changeLogTokenHolder.getValue();
        long minId;
        if (changeLogToken == null) {
            minId = 0;
        } else {
            try {
                minId = Long.parseLong(changeLogToken);
            } catch (NumberFormatException e) {
                throw new CmisInvalidArgumentException("Invalid change log token");
            }
        }
        int max = maxItems == null ? -1 : maxItems.intValue();
        if (max <= 0) {
            max = DEFAULT_CHANGE_LOG_SIZE;
        }
        if (max > MAX_CHANGE_LOG_SIZE) {
            max = MAX_CHANGE_LOG_SIZE;
        }
        List<ObjectData> ods = null;
        // retry with increasingly larger page size if some items are
        // skipped
        for (int scale = 1; scale < 128; scale *= 2) {
            int pageSize = max * scale + 1;
            if (pageSize < 0) { // overflow
                pageSize = Integer.MAX_VALUE;
            }
            ods = readAuditLog(repositoryId, minId, max, pageSize);
            if (ods != null) {
                break;
            }
            if (pageSize == Integer.MAX_VALUE) {
                break;
            }
        }
        if (ods == null) {
            // couldn't find enough, too many items were skipped
            ods = Collections.emptyList();

        }
        boolean hasMoreItems = ods.size() > max;
        if (hasMoreItems) {
            ods = ods.subList(0, max);
        }
        String latestChangeLogToken;
        if (ods.size() == 0) {
            latestChangeLogToken = null;
        } else {
            ObjectData last = ods.get(ods.size() - 1);
            latestChangeLogToken = (String) last.getProperties().getProperties().get(NX_CHANGE_LOG_ID).getFirstValue();
        }
        ObjectListImpl ol = new ObjectListImpl();
        ol.setHasMoreItems(Boolean.valueOf(hasMoreItems));
        ol.setObjects(ods);
        ol.setNumItems(BigInteger.valueOf(-1));
        changeLogTokenHolder.setValue(latestChangeLogToken);
        return ol;
    }

    protected SearchRequestBuilder getElasticsearchBuilder() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        String indexName = esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE);
        return esa.getClient().prepareSearch(indexName).setTypes(ElasticSearchConstants.ENTRY_TYPE).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH);
    }

    /**
     * Reads at most max+1 entries from the audit log.
     *
     * @return null if not enough elements found with the current page size
     */
    protected List<ObjectData> readAuditLog(String repositoryId, long minId, int max, int pageSize) {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        if (reader == null) {
            throw new CmisRuntimeException("Cannot find audit service");
        }
        List<LogEntry> entries;
        if (reader instanceof DefaultAuditBackend) {
            String query = "FROM LogEntry log" //
                    + " WHERE log.id >= :minId" //
                    + "   AND log.eventId IN (:evCreated, :evModified, :evRemoved)" //
                    + "   AND log.repositoryId = :repoId" //
                    + " ORDER BY log.id";
            Map<String, Object> params = new HashMap<>();
            params.put("minId", Long.valueOf(minId));
            params.put("evCreated", DOCUMENT_CREATED);
            params.put("evModified", DOCUMENT_UPDATED);
            params.put("evRemoved", DOCUMENT_REMOVED);
            params.put("repoId", repositoryId);
            entries = (List<LogEntry>) reader.nativeQuery(query, params, 1, pageSize);
        } else if (reader instanceof ESAuditBackend) {
            SearchRequestBuilder builder = getElasticsearchBuilder();
            BoolQueryBuilder query = QueryBuilders.boolQuery()
                                                  .must(QueryBuilders.matchAllQuery())
                                                  .filter(QueryBuilders.termQuery(ES_AUDIT_REPOSITORY_ID, repositoryId))
                                                  .filter(QueryBuilders.termsQuery(ES_AUDIT_EVENT_ID, DOCUMENT_CREATED,
                                                          DOCUMENT_UPDATED, DOCUMENT_REMOVED))
                                                  .filter(QueryBuilders.rangeQuery(ES_AUDIT_ID).gte(minId));
            builder.setQuery(query);
            builder.addSort(ES_AUDIT_ID, SortOrder.ASC);
            entries = new ArrayList<>();
            SearchResponse searchResponse = builder.setSize(pageSize).execute().actionGet();
            for (SearchHit hit : searchResponse.getHits()) {
                try {
                    entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
                } catch (IOException e) {
                    throw new CmisRuntimeException("Failed to parse audit entry: " + hit, e);
                }
            }
        } else {
            throw new CmisRuntimeException("Unknown audit backend: " + reader.getClass().getName());
        }
        List<ObjectData> ods = new ArrayList<>();
        for (LogEntry entry : entries) {
            ObjectData od = getLogEntryObjectData(entry);
            if (od != null) {
                ods.add(od);
                if (ods.size() > max) {
                    // enough collected
                    return ods;
                }
            }
        }
        if (entries.size() < pageSize) {
            // end of audit log
            return ods;
        }
        return null;
    }

    /**
     * Gets object data for a log entry, or null if skipped.
     */
    protected ObjectData getLogEntryObjectData(LogEntry logEntry) {
        String docType = logEntry.getDocType();
        if (!getTypeManager().hasType(docType)) {
            // ignore types present in the log but not exposed through CMIS
            return null;
        }
        // change type
        String eventId = logEntry.getEventId();
        ChangeType changeType;
        if (DOCUMENT_CREATED.equals(eventId)) {
            changeType = ChangeType.CREATED;
        } else if (DOCUMENT_UPDATED.equals(eventId)) {
            changeType = ChangeType.UPDATED;
        } else if (DOCUMENT_REMOVED.equals(eventId)) {
            changeType = ChangeType.DELETED;
        } else {
            return null;
        }
        ChangeEventInfoDataImpl cei = new ChangeEventInfoDataImpl();
        cei.setChangeType(changeType);
        // change time
        GregorianCalendar changeTime = (GregorianCalendar) Calendar.getInstance();
        Date date = logEntry.getEventDate();
        changeTime.setTime(date);
        cei.setChangeTime(changeTime);
        ObjectDataImpl od = new ObjectDataImpl();
        od.setChangeEventInfo(cei);
        // properties: id, doc type, change log id
        PropertiesImpl properties = new PropertiesImpl();
        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_ID, logEntry.getDocUUID()));
        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, docType));
        properties.addProperty(new PropertyStringImpl(NX_CHANGE_LOG_ID, String.valueOf(logEntry.getId())));
        od.setProperties(properties);
        return od;
    }

    protected String getLatestChangeLogToken(String repositoryId) {
        AuditReader reader = Framework.getService(AuditReader.class);
        if (reader == null) {
            log.warn("Audit Service not found. latest change log token will be '0'");
            return "0";
            // throw new CmisRuntimeException("Cannot find audit service");
        }
        long id;
        if (reader instanceof DefaultAuditBackend) {
            String query = "FROM LogEntry log" //
                    + " WHERE log.eventId IN (:evCreated, :evModified, :evRemoved)" //
                    + "   AND log.repositoryId = :repoId" //
                    + " ORDER BY log.id DESC";
            Map<String, Object> params = new HashMap<>();
            params.put("evCreated", DOCUMENT_CREATED);
            params.put("evModified", DOCUMENT_UPDATED);
            params.put("evRemoved", DOCUMENT_REMOVED);
            params.put("repoId", repositoryId);
            @SuppressWarnings("unchecked")
            List<LogEntry> entries = (List<LogEntry>) reader.nativeQuery(query, params, 1, 1);
            id = entries.isEmpty() ? 0 : entries.get(0).getId();
        } else if (reader instanceof ESAuditBackend) {
            SearchRequestBuilder builder = getElasticsearchBuilder();
            BoolQueryBuilder query = QueryBuilders.boolQuery()
                                                  .must(QueryBuilders.matchAllQuery())
                                                  .filter(QueryBuilders.termQuery(ES_AUDIT_REPOSITORY_ID, repositoryId))
                                                  .filter(QueryBuilders.termsQuery(ES_AUDIT_EVENT_ID, DOCUMENT_CREATED,
                                                          DOCUMENT_UPDATED, DOCUMENT_REMOVED));
            builder.setQuery(query);
            builder.addSort(ES_AUDIT_ID, SortOrder.DESC);
            builder.setSize(1);
            // TODO refactor this to use max clause
            SearchResponse searchResponse = builder.execute().actionGet();
            SearchHit[] hits = searchResponse.getHits().hits();
            if (hits.length == 0) {
                id = 0;
            } else {
                String hit = hits[0].getSourceAsString();
                try {
                    id = AuditEntryJSONReader.read(hit).getId();
                } catch (IOException e) {
                    throw new CmisRuntimeException("Failed to parse audit entry: " + hit, e);
                }
            }
        } else {
            throw new CmisRuntimeException("Unknown audit backend: " + reader.getClass().getName());
        }
        return String.valueOf(id);
    }

    protected String addProxyClause(String query) {
        if (!repository.supportsProxies()) {
            query += " AND " + NXQL.ECM_ISPROXY + " = 0";
        }
        return query;
    }

    @Override
    public ObjectList query(String repositoryId, String statement, Boolean searchAllVersions,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        long skip = skipCount == null ? 0 : skipCount.longValue();
        if (skip < 0) {
            skip = 0;
        }
        long max = maxItems == null ? -1 : maxItems.longValue();
        if (max <= 0) {
            max = DEFAULT_QUERY_SIZE;
        }
        Map<String, PropertyDefinition<?>> typeInfo = new HashMap<>();
        // searchAllVersions defaults to false, spec 2.2.6.1.1
        PartialList<Map<String, Serializable>> res = queryProjection(statement, max, skip,
                Boolean.TRUE.equals(searchAllVersions), typeInfo);

        // convert from Nuxeo to CMIS format
        List<ObjectData> list = new ArrayList<>(res.list.size());
        for (Map<String, Serializable> map : res.list) {
            ObjectDataImpl od = makeObjectData(map, typeInfo);

            // optional stuff
            String id = od.getId();
            if (id != null) { // null if JOIN in original query
                DocumentModel doc = null;
                if (Boolean.TRUE.equals(includeAllowableActions)) {
                    doc = getDocumentModel(id);
                    AllowableActions allowableActions = NuxeoObjectData.getAllowableActions(doc, false);
                    od.setAllowableActions(allowableActions);
                }
                if (includeRelationships != null && includeRelationships != IncludeRelationships.NONE) {
                    // TODO get relationships using a JOIN
                    // added to the original query
                    List<ObjectData> relationships = NuxeoObjectData.getRelationships(id, includeRelationships, this);
                    od.setRelationships(relationships);
                }
                if (NuxeoObjectData.needsRenditions(renditionFilter)) {
                    if (doc == null) {
                        doc = getDocumentModel(id);
                    }
                    List<RenditionData> renditions = NuxeoObjectData.getRenditions(doc, renditionFilter, null, null,
                            callContext);
                    od.setRenditions(renditions);
                }
            }

            list.add(od);
        }
        long numItems = res.totalSize;
        ObjectListImpl objList = new ObjectListImpl();
        objList.setObjects(list);
        objList.setNumItems(BigInteger.valueOf(numItems));
        objList.setHasMoreItems(Boolean.valueOf(numItems > skip + list.size()));
        return objList;
    }

    /**
     * Makes a CMISQL query to the repository and returns an {@link IterableQueryResult}, which MUST be closed in a
     * {@code finally} block.
     *
     * @param query the CMISQL query
     * @param searchAllVersions whether to search all versions ({@code true}) or only the latest version ({@code false}
     *            ), for versionable types
     * @param typeInfo a map filled with type information for each returned property, or {@code null} if no such info is
     *            needed
     * @return an {@link IterableQueryResult}, which MUST be closed in a {@code finally} block
     * @throws CmisInvalidArgumentException if the query cannot be parsed or is invalid
     * @since 6.0
     */
    public IterableQueryResult queryAndFetch(String query, boolean searchAllVersions,
            Map<String, PropertyDefinition<?>> typeInfo) {
        if (repository.supportsJoins()) {
            if (repository.supportsProxies()) {
                throw new CmisRuntimeException(
                        "Server configuration error: cannot supports joins and proxies at the same time");
            }
            // straight to CoreSession as CMISQL, relies on proper QueryMaker
            return coreSession.queryAndFetch(query, CMISQLQueryMaker.TYPE, this, typeInfo,
                    Boolean.valueOf(searchAllVersions));
        } else {
            // convert to NXQL for evaluation
            CMISQLtoNXQL converter = new CMISQLtoNXQL(repository.supportsProxies());
            String nxql;
            try {
                nxql = converter.getNXQL(query, this, typeInfo, searchAllVersions);
            } catch (QueryParseException e) {
                throw new CmisInvalidArgumentException(e.getMessage(), e);
            }

            IterableQueryResult it;
            try {
                if (repository.useElasticsearch()) {
                    ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
                    NxQueryBuilder qb = new NxQueryBuilder(coreSession).nxql(nxql)
                                                                       .limit(1000)
                                                                       .onlyElasticsearchResponse();
                    it = new EsIterableQueryResultImpl(ess, ess.scroll(qb, 1000));
                } else {
                    // distinct documents
                    it = coreSession.queryAndFetch(nxql, NXQL.NXQL, true, new Object[0]);
                }
            } catch (QueryParseException e) {
                e.addInfo("Invalid query: CMISQL: " + query);
                throw e;
            }
            // wrap result
            return converter.getIterableQueryResult(it, this);
        }
    }

    /**
     * Makes a CMISQL query to the repository and returns an {@link IterableQueryResult}, which MUST be closed in a
     * {@code finally} block.
     *
     * @param query the CMISQL query
     * @param searchAllVersions whether to search all versions ({@code true}) or only the latest version ({@code false}
     *            ), for versionable types
     * @return an {@link IterableQueryResult}, which MUST be closed in a {@code finally} block
     * @throws CmisRuntimeException if the query cannot be parsed or is invalid
     * @since 6.0
     */
    public IterableQueryResult queryAndFetch(String query, boolean searchAllVersions) {
        return queryAndFetch(query, searchAllVersions, null);
    }

    /**
     * Makes a CMISQL query to the repository and returns a {@link PartialList}.
     *
     * @param query the CMISQL query
     * @param limit the maximum number of documents to retrieve, or 0 for all of them
     * @param offset the offset (starting at 0) into the list of documents
     * @param searchAllVersions whether to search all versions ({@code true}) or only the latest version ({@code false}
     *            ), for versionable types
     * @param typeInfo a map filled with type information for each returned property, or {@code null} if no such info is
     *            needed
     * @return a {@link PartialList}
     * @throws CmisInvalidArgumentException if the query cannot be parsed or is invalid
     * @since 7.10-HF25, 8.10-HF06, 9.2
     */
    public PartialList<Map<String, Serializable>> queryProjection(String query, long limit, long offset,
            boolean searchAllVersions, Map<String, PropertyDefinition<?>> typeInfo) {
        if (repository.supportsJoins()) {
            if (repository.supportsProxies()) {
                throw new CmisRuntimeException(
                        "Server configuration error: cannot supports joins and proxies at the same time");
            }
            // straight to CoreSession as CMISQL, relies on proper QueryMaker
            return coreSession.queryProjection(query, CMISQLQueryMaker.TYPE, false, limit, offset, -1, this, typeInfo,
                    Boolean.valueOf(searchAllVersions));
        } else {
            // convert to NXQL for evaluation
            CMISQLtoNXQL converter = new CMISQLtoNXQL(repository.supportsProxies());
            String nxql;
            try {
                nxql = converter.getNXQL(query, this, typeInfo, searchAllVersions);
            } catch (QueryParseException e) {
                throw new CmisInvalidArgumentException(e.getMessage(), e);
            }

            PartialList<Map<String, Serializable>> pl;
            try {
                if (repository.useElasticsearch()) {
                    ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
                    NxQueryBuilder qb = new NxQueryBuilder(coreSession).nxql(nxql)
                                                                       .limit((int) limit)
                                                                       .offset((int) offset)
                                                                       .onlyElasticsearchResponse();
                    SearchResponse esResponse = ess.queryAndAggregate(qb).getElasticsearchResponse();
                    // Convert response
                    SearchHits esHits = esResponse.getHits();
                    List<Map<String, Serializable>> list = new EsSearchHitConverter(
                            qb.getSelectFieldsAndTypes()).convert(esHits.getHits());
                    pl = new PartialList<>(list, esHits.getTotalHits());
                } else {
                    // distinct documents
                    pl = coreSession.queryProjection(nxql, NXQL.NXQL, true, limit, offset, -1);
                }
            } catch (QueryParseException e) {
                e.addInfo("Invalid query: CMISQL: " + query);
                throw e;
            }
            // wrap result
            return converter.convertToCMIS(pl, this);
        }
    }

    protected ObjectDataImpl makeObjectData(Map<String, Serializable> map,
            Map<String, PropertyDefinition<?>> typeInfo) {
        ObjectDataImpl od = new ObjectDataImpl();
        PropertiesImpl properties = new PropertiesImpl();
        for (Entry<String, Serializable> en : map.entrySet()) {
            String queryName = en.getKey();
            PropertyDefinition<?> pd = typeInfo.get(queryName);
            if (pd == null) {
                throw new NullPointerException("Cannot get " + queryName);
            }
            AbstractPropertyData<?> p = (AbstractPropertyData<?>) objectFactory.createPropertyData(pd, en.getValue());
            p.setLocalName(pd.getLocalName());
            p.setDisplayName(pd.getDisplayName());
            // queryName and pd.getQueryName() may be different
            // for qualified properties
            p.setQueryName(queryName);
            properties.addProperty(p);
        }
        od.setProperties(properties);
        return od;
    }

    @Override
    public void addObjectToFolder(String repositoryId, String objectId, String folderId, Boolean allVersions,
            ExtensionsData extension) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId, String folderId,
            ExtensionsData extension) {
        if (folderId != null) {
            // check it's the actual parent
            DocumentModel folder = getDocumentModel(folderId);
            DocumentModel parent = coreSession.getParentDocument(new IdRef(objectId));
            if (!parent.getId().equals(folder.getId())) {
                throw new CmisInvalidArgumentException("Object " + objectId + " is not filed in  " + folderId);
            }
        }
        deleteObject(repositoryId, objectId, Boolean.FALSE, extension);
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        return getChildrenInternal(repositoryId, folderId, filter, orderBy, includeAllowableActions,
                includeRelationships, renditionFilter, includePathSegment, maxItems, skipCount, false);
    }

    protected ObjectInFolderList getChildrenInternal(String repositoryId, String folderId, String filter,
            String orderBy, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount,
            boolean folderOnly) {
        ObjectInFolderListImpl result = new ObjectInFolderListImpl();
        List<ObjectInFolderData> list = new ArrayList<>();
        DocumentModel folder = getDocumentModel(folderId);
        if (!folder.isFolder()) {
            return null;
        }

        String query = String.format(
                "SELECT * FROM %s WHERE " // Folder/Document
                        + "%s = '%s' AND " // ecm:parentId = 'folderId'
                        + "%s <> '%s' AND " // ecm:mixinType <> 'HiddenInNavigation'
                        + "%s <> '%s'", // ecm:currentLifeCycleState <> 'deleted'
                folderOnly ? "Folder" : "Document", //
                NXQL.ECM_PARENTID, folderId, //
                NXQL.ECM_MIXINTYPE, FacetNames.HIDDEN_IN_NAVIGATION, //
                NXQL.ECM_LIFECYCLESTATE, LifeCycleConstants.DELETED_STATE);
        query = addProxyClause(query);
        if (!StringUtils.isBlank(orderBy)) {
            CMISQLtoNXQL converter = new CMISQLtoNXQL(repository.supportsProxies());
            query += " ORDER BY " + converter.convertOrderBy(orderBy, getTypeManager());
        }

        long limit = maxItems == null ? 0 : maxItems.longValue();
        if (limit < 0) {
            limit = 0;
        }
        long offset = skipCount == null ? 0 : skipCount.longValue();
        if (offset < 0) {
            offset = 0;
        }

        DocumentModelList children = coreSession.query(query, null, limit, offset, true);

        for (DocumentModel child : children) {
            NuxeoObjectData data = new NuxeoObjectData(this, child, filter, includeAllowableActions,
                    includeRelationships, renditionFilter, Boolean.FALSE, Boolean.FALSE, null);
            ObjectInFolderDataImpl oifd = new ObjectInFolderDataImpl();
            oifd.setObject(data);
            if (Boolean.TRUE.equals(includePathSegment)) {
                oifd.setPathSegment(child.getName());
            }
            list.add(oifd);
            collectObjectInfo(repositoryId, data.getId());
        }

        Boolean hasMoreItems;
        if (limit == 0) {
            hasMoreItems = Boolean.FALSE;
        } else {
            hasMoreItems = Boolean.valueOf(children.totalSize() > offset + limit);
        }
        result.setObjects(list);
        result.setHasMoreItems(hasMoreItems);
        result.setNumItems(BigInteger.valueOf(children.totalSize()));
        collectObjectInfo(repositoryId, folderId);
        return result;
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId, String folderId, BigInteger depth,
            String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        int levels = depth == null ? DEFAULT_FOLDER_LEVELS : depth.intValue();
        if (levels == 0) {
            throw new CmisInvalidArgumentException("Invalid depth: 0");
        }
        return getDescendantsInternal(repositoryId, folderId, filter, includeAllowableActions, includeRelationships,
                renditionFilter, includePathSegment, 0, levels, false);
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId, String folderId, BigInteger depth,
            String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        int levels = depth == null ? DEFAULT_FOLDER_LEVELS : depth.intValue();
        if (levels == 0) {
            throw new CmisInvalidArgumentException("Invalid depth: 0");
        }
        return getDescendantsInternal(repositoryId, folderId, filter, includeAllowableActions, includeRelationships,
                renditionFilter, includePathSegment, 0, levels, true);
    }

    protected List<ObjectInFolderContainer> getDescendantsInternal(String repositoryId, String folderId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegments, int level, int maxLevels, boolean folderOnly) {
        if (maxLevels != -1 && level >= maxLevels) {
            return null;
        }
        ObjectInFolderList children = getChildrenInternal(repositoryId, folderId, filter, null, includeAllowableActions,
                includeRelationships, renditionFilter, includePathSegments, null, null, folderOnly);
        if (children == null) {
            return Collections.emptyList();
        }
        List<ObjectInFolderContainer> res = new ArrayList<>(children.getObjects().size());
        for (ObjectInFolderData child : children.getObjects()) {
            ObjectInFolderContainerImpl oifc = new ObjectInFolderContainerImpl();
            oifc.setObject(child);
            // recurse
            List<ObjectInFolderContainer> subChildren = getDescendantsInternal(repositoryId, child.getObject().getId(),
                    filter, includeAllowableActions, includeRelationships, renditionFilter, includePathSegments,
                    level + 1, maxLevels, folderOnly);
            if (subChildren != null) {
                oifc.setChildren(subChildren);
            }
            res.add(oifc);
        }
        return res;
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId, String filter, ExtensionsData extension) {
        List<ObjectParentData> parents = getObjectParentsInternal(repositoryId, folderId, filter, null, null, null,
                Boolean.TRUE, true);
        return parents.isEmpty() ? null : parents.get(0).getObject();
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        return getObjectParentsInternal(repositoryId, objectId, filter, includeAllowableActions, includeRelationships,
                renditionFilter, includeRelativePathSegment, false);
    }

    protected List<ObjectParentData> getObjectParentsInternal(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, boolean folderOnly) {
        DocumentRef docRef = new IdRef(objectId);
        if (!coreSession.exists(docRef)) {
            throw new CmisObjectNotFoundException(objectId);
        }
        DocumentModel doc = coreSession.getDocument(docRef);
        if (isFilteredOut(doc)) {
            throw new CmisObjectNotFoundException(objectId);
        }
        if (folderOnly && !doc.isFolder()) {
            throw new CmisInvalidArgumentException("Not a folder: " + objectId);
        }
        String pathSegment = doc.getName();
        if (pathSegment == null) { // root
            return Collections.emptyList();
        }
        DocumentRef parentRef = doc.getParentRef();
        if (parentRef == null) { // placeless
            return Collections.emptyList();
        }
        if (!coreSession.exists(parentRef)) { // non-accessible
            return Collections.emptyList();
        }
        DocumentModel parent = coreSession.getDocument(parentRef);
        if (isFilteredOut(parent)) { // filtered out
            return Collections.emptyList();
        }
        String parentId = parent.getId();

        ObjectData od = getObject(repositoryId, parentId, filter, includeAllowableActions, includeRelationships,
                renditionFilter, Boolean.FALSE, Boolean.FALSE, null);
        ObjectParentDataImpl opd = new ObjectParentDataImpl(od);
        if (!Boolean.FALSE.equals(includeRelativePathSegment)) {
            opd.setRelativePathSegment(pathSegment);
        }
        return Collections.<ObjectParentData> singletonList(opd);
    }

    @Override
    public void applyPolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<ObjectData> getAppliedPolicies(String repositoryId, String objectId, String filter,
            ExtensionsData extension) {
        return Collections.emptyList();
    }

    @Override
    public void removePolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
        throw new CmisNotSupportedException();
    }

    @Override
    public ObjectList getObjectRelationships(String repositoryId, String objectId, Boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, String typeId, String filter, Boolean includeAllowableActions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        IncludeRelationships includeRelationships;
        if (relationshipDirection == null || relationshipDirection == RelationshipDirection.SOURCE) {
            includeRelationships = IncludeRelationships.SOURCE;
        } else if (relationshipDirection == RelationshipDirection.TARGET) {
            includeRelationships = IncludeRelationships.TARGET;
        } else { // RelationshipDirection.EITHER
            includeRelationships = IncludeRelationships.BOTH;
        }
        List<ObjectData> rels = NuxeoObjectData.getRelationships(objectId, includeRelationships, this);
        BatchedList<ObjectData> batch = ListUtils.getBatchedList(rels, maxItems, skipCount, DEFAULT_MAX_RELATIONSHIPS);
        ObjectListImpl res = new ObjectListImpl();
        res.setObjects(batch.getList());
        res.setNumItems(batch.getNumItems());
        res.setHasMoreItems(batch.getHasMoreItems());
        for (ObjectData data : res.getObjects()) {
            collectObjectInfo(repositoryId, data.getId());
        }
        return res;
    }

    @Override
    public void checkIn(String repositoryId, Holder<String> objectIdHolder, Boolean major, Properties properties,
            ContentStream contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {
        String objectId;
        if (objectIdHolder == null || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        VersioningOption option = Boolean.TRUE.equals(major) ? VersioningOption.MAJOR : VersioningOption.MINOR;

        DocumentModel doc = getDocumentModel(objectId);

        NuxeoObjectData object = new NuxeoObjectData(this, doc);
        updateProperties(object, null, properties, false);
        boolean setContentStream = contentStream != null;
        if (setContentStream) {
            try {
                NuxeoPropertyData.setContentStream(doc, contentStream, true);
            } catch (IOException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
        // comment for save event
        doc.putContextData("comment", checkinComment);
        doc = coreSession.saveDocument(doc);
        if (setContentStream) {
            NuxeoPropertyData.validateBlobDigest(doc, callContext);
        }
        DocumentRef ver;
        try {
            ver = doc.checkIn(option, checkinComment);
        } catch (VersionNotModifiableException e) {
            throw new CmisInvalidArgumentException("Cannot check in non-PWC: " + doc);
        }
        doc.removeLock();
        save();
        objectIdHolder.setValue(getIdFromDocumentRef(ver));
    }

    @Override
    public void checkOut(String repositoryId, Holder<String> objectIdHolder, ExtensionsData extension,
            Holder<Boolean> contentCopiedHolder) {
        String objectId;
        if (objectIdHolder == null || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        String pwcId = checkOut(objectId);
        objectIdHolder.setValue(pwcId);
        if (contentCopiedHolder != null) {
            contentCopiedHolder.setValue(Boolean.TRUE);
        }
    }

    public String checkOut(String objectId) {
        DocumentModel doc = getDocumentModel(objectId);
        try {
            // find pwc
            DocumentModel pwc;
            if (doc.isVersion()) {
                pwc = coreSession.getWorkingCopy(doc.getRef());
                if (pwc == null) {
                    // no live document available
                    // TODO do a restore somewhere
                    throw new CmisObjectNotFoundException(objectId);
                }
            } else {
                pwc = doc;
            }
            if (pwc.isCheckedOut()) {
                throw new CmisConstraintException("Already checked out: " + objectId);
            }
            if (pwc.isLocked()) {
                throw new CmisConstraintException("Cannot check out since currently locked: " + objectId);
            }
            pwc.setLock();
            pwc.checkOut();
            save();
            return pwc.getId();
        } catch (VersionNotModifiableException e) {
            throw new CmisInvalidArgumentException("Cannot check out non-version: " + objectId);
        } catch (NuxeoException e) { // TODO use a core LockException
            String message = e.getMessage();
            if (message != null && message.startsWith("Document already locked")) {
                throw new CmisConstraintException("Cannot check out since currently locked: " + objectId);
            }
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void cancelCheckOut(String repositoryId, String objectId, ExtensionsData extension) {
        cancelCheckOut(objectId);
    }

    public void cancelCheckOut(String objectId) {
        DocumentModel doc = getDocumentModel(objectId);
        if (!doc.isCheckedOut()) {
            throw new CmisInvalidArgumentException("Cannot cancel check out of non-PWC: " + doc);
        }
        DocumentRef docRef = doc.getRef();
        // find last version
        DocumentRef verRef = coreSession.getLastDocumentVersionRef(docRef);
        if (verRef == null) {
            // delete
            coreSession.removeDocument(docRef);
        } else {
            // restore and keep checked in
            coreSession.restoreToVersion(docRef, verRef, true, true);
            doc.removeLock();
        }
        save();
    }

    @Override
    public ObjectList getCheckedOutDocs(String repositoryId, String folderId, String filter, String orderBy,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        // columns from filter
        List<String> props;
        if (StringUtils.isBlank(filter)) {
            props = Arrays.asList(PropertyIds.OBJECT_ID, PropertyIds.OBJECT_TYPE_ID, PropertyIds.BASE_TYPE_ID);
        } else {
            props = NuxeoObjectData.getPropertyIdsFromFilter(filter);
            // same as query names
        }
        // clause from folderId
        List<String> clauses = new ArrayList<>(3);
        clauses.add(NuxeoTypeHelper.NX_ISVERSION + " = false");
        clauses.add(NuxeoTypeHelper.NX_ISCHECKEDIN + " = false");
        if (folderId != null) {
            String qid = "'" + folderId.replace("'", "''") + "'";
            clauses.add("IN_FOLDER(" + qid + ")");
        }
        // orderBy
        String order;
        if (StringUtils.isBlank(orderBy)) {
            order = "";
        } else {
            order = " ORDER BY " + orderBy;
        }
        String statement = "SELECT " + StringUtils.join(props, ", ") + " FROM " + BaseTypeId.CMIS_DOCUMENT.value()
                + " WHERE " + StringUtils.join(clauses, " AND ") + order;
        Boolean searchAllVersions = Boolean.TRUE;
        return query(repositoryId, statement, searchAllVersions, includeAllowableActions, includeRelationships,
                renditionFilter, maxItems, skipCount, extension);
    }

    @Override
    public List<ObjectData> getAllVersions(String repositoryId, String objectId, String versionSeriesId, String filter,
            Boolean includeAllowableActions, ExtensionsData extension) {
        DocumentModel doc;
        if (objectId != null) {
            // atompub passes object id
            doc = getDocumentModel(objectId);
        } else if (versionSeriesId != null) {
            // soap passes version series id
            // version series id is (for now) id of live document
            // TODO deal with removal of live doc
            doc = getDocumentModel(versionSeriesId);
        } else {
            throw new CmisInvalidArgumentException("Missing object ID or version series ID");
        }
        List<DocumentRef> versions = coreSession.getVersionsRefs(doc.getRef());
        List<ObjectData> list = new ArrayList<>(versions.size());
        for (DocumentRef verRef : versions) {
            // First check if we have enough permission on versions
            if (coreSession.hasPermission(verRef, SecurityConstants.READ)) {
                String verId = getIdFromDocumentRef(verRef);
                ObjectData od = getObject(repositoryId, verId, filter, includeAllowableActions,
                        IncludeRelationships.NONE, null, Boolean.FALSE, Boolean.FALSE, null);
                list.add(od);
            }
        }
        // PWC last
        DocumentModel pwc = doc.isVersion() ? coreSession.getWorkingCopy(doc.getRef()) : doc;
        if (pwc != null && (pwc.isCheckedOut() || list.isEmpty())) {
            NuxeoObjectData od = new NuxeoObjectData(this, pwc, filter, includeAllowableActions,
                    IncludeRelationships.NONE, null, Boolean.FALSE, Boolean.FALSE, extension);
            list.add(od);
        }
        // CoreSession returns them in creation order,
        // CMIS wants them last first
        Collections.reverse(list);
        return list;
    }

    @Override
    public NuxeoObjectData getObjectOfLatestVersion(String repositoryId, String objectId, String versionSeriesId,
            Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {
        DocumentModel doc;
        if (objectId != null) {
            // atompub passes object id
            doc = getDocumentModel(objectId);
        } else if (versionSeriesId != null) {
            // soap passes version series id
            // version series id is (for now) id of live document
            // TODO deal with removal of live doc
            doc = getDocumentModel(versionSeriesId);
        } else {
            throw new CmisInvalidArgumentException("Missing object ID or version series ID");
        }
        if (Boolean.TRUE.equals(major)) {
            // we must list all versions
            List<DocumentModel> versions = coreSession.getVersions(doc.getRef());
            Collections.reverse(versions);
            for (DocumentModel ver : versions) {
                if (ver.isMajorVersion()) {
                    return getObject(repositoryId, ver.getId(), filter, includeAllowableActions, includeRelationships,
                            renditionFilter, includePolicyIds, includeAcl, null);
                }
            }
            return null;
        } else {
            DocumentRef verRef = coreSession.getLastDocumentVersionRef(doc.getRef());
            String verId = getIdFromDocumentRef(verRef);
            return getObject(repositoryId, verId, filter, includeAllowableActions, includeRelationships,
                    renditionFilter, includePolicyIds, includeAcl, null);
        }
    }

    @Override
    public Properties getPropertiesOfLatestVersion(String repositoryId, String objectId, String versionSeriesId,
            Boolean major, String filter, ExtensionsData extension) {
        NuxeoObjectData od = getObjectOfLatestVersion(repositoryId, objectId, versionSeriesId, major, filter,
                Boolean.FALSE, IncludeRelationships.NONE, null, Boolean.FALSE, Boolean.FALSE, null);
        return od == null ? null : od.getProperties();
    }

    @Override
    public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        if (doc.isFolder()) {
            // check that there are no children left
            DocumentModelList docs = coreSession.getChildren(new IdRef(objectId), null, documentFilter, null);
            if (docs.size() > 0) {
                throw new CmisConstraintException("Cannot delete non-empty folder: " + objectId);
            }
        }
        coreSession.removeDocument(doc.getRef());
        save();
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId, Boolean allVersions,
            ExtensionsData extension) {
        DocumentModel doc = getDocumentModel(objectId);
        DocumentRef docRef = doc.getRef();
        // find last version
        DocumentRef verRef = coreSession.getLastDocumentVersionRef(docRef);
        // If doc has versions, is locked, and is checkedOut, then it was
        // likely
        // explicitly checkedOut so invoke cancelCheckOut not delete
        if (verRef != null && doc.isLocked() && doc.isCheckedOut()) {
            cancelCheckOut(repositoryId, objectId, extension);
        } else {
            deleteObject(repositoryId, objectId, allVersions, extension);
        }
    }

}
