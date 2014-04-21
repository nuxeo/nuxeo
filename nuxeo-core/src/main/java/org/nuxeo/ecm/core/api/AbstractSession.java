/*
 * Copyright (c) 2006-2011, 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.api;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_LIFE_CYCLE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_PROPERTIES;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_SECURITY;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_VERSION;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNLOCK;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_LIFE_CYCLE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_SECURITY;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_VERSION;

import java.io.InputStream;
import java.io.Serializable;
import java.security.Principal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentModel.DocumentModelRefresh;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelChildrenIterator;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentProxy;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.PathComparator;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.FilterableQuery;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Abstract implementation of the client interface.
 * <p>
 * This handles all the aspects that are independent on the final implementation
 * (like running inside a J2EE platform or not).
 * <p>
 * The only aspect not implemented is the session management that should be
 * handled by subclasses.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public abstract class AbstractSession implements CoreSession, Serializable {

    public static final NuxeoPrincipal ANONYMOUS = new UserPrincipal(
            "anonymous", new ArrayList<String>(), true, false);

    private static final Log log = LogFactory.getLog(CoreSession.class);

    private static final long serialVersionUID = 1L;

    private static final Comparator<? super Document> pathComparator = new PathComparator();

    public static final String DEFAULT_MAX_RESULTS = "1000";

    public static final String MAX_RESULTS_PROPERTY = "org.nuxeo.ecm.core.max.results";

    public static final String LIMIT_RESULTS_PROPERTY = "org.nuxeo.ecm.core.limit.results";

    public static final String BINARY_TEXT_SYS_PROP = "binaryText";

    private Boolean limitedResults;

    private Long maxResults;

    // @since 5.7.2
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected Counter createDocumentCount;

    protected Counter deleteDocumentCount;

    protected Counter updateDocumentCount;

    protected void createMetrics() {
        createDocumentCount = registry.counter(MetricRegistry.name(
                "nuxeo.repositories", getRepositoryName(), "documents",
                "create"));
        deleteDocumentCount = registry.counter(MetricRegistry.name(
                "nuxeo.repositories", getRepositoryName(), "documents",
                "delete"));
        updateDocumentCount = registry.counter(MetricRegistry.name(
                "nuxeo.repositories", getRepositoryName(), "documents",
                "update"));
    }

    /**
     * Used to check permissions.
     */
    private transient SecurityService securityService;

    protected SecurityService getSecurityService() {
        if (securityService == null) {
            securityService = NXCore.getSecurityService();
        }
        return securityService;
    }

    private transient VersioningService versioningService;

    protected VersioningService getVersioningService() {
        if (versioningService == null) {
            try {
                versioningService = Framework.getService(VersioningService.class);
            } catch (Exception e) {
                throw new RuntimeException("VersioningService not found", e);
            }
        }
        return versioningService;
    }

    /**
     * Internal method: Gets the current session based on the client session id.
     *
     * @return the repository session
     */
    public abstract Session getSession() throws ClientException;

    @Override
    public DocumentType getDocumentType(String type) {
        return Framework.getLocalService(SchemaManager.class).getDocumentType(
                type);
    }

    protected final void checkPermission(Document doc, String permission)
            throws DocumentSecurityException, DocumentException {
        if (isAdministrator()) {
            return;
        }
        if (!hasPermission(doc, permission)) {
            log.error("Permission '" + permission + "' is not granted to '"
                    + getPrincipal().getName() + "' on document "
                    + doc.getPath() + " (" + doc.getUUID() + " - "
                    + doc.getType().getName() + ")");
            throw new DocumentSecurityException("Privilege '" + permission
                    + "' is not granted to '" + getPrincipal().getName() + "'");
        }
    }

    protected Map<String, Serializable> getContextMapEventInfo(DocumentModel doc) {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        if (doc != null) {
            ScopedMap ctxData = doc.getContextData();
            if (ctxData != null) {
                options.putAll(ctxData.getDefaultScopeValues());
                options.putAll(ctxData.getScopeValues(ScopeType.REQUEST));
            }
        }
        return options;
    }

    public DocumentEventContext newEventContext(DocumentModel source) {
        DocumentEventContext ctx = new DocumentEventContext(this,
                getPrincipal(), source);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, getSessionId());
        return ctx;
    }

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Serializable> options, String category, String comment,
            boolean withLifeCycle, boolean inline) throws ClientException {

        DocumentEventContext ctx = new DocumentEventContext(this,
                getPrincipal(), source);

        // compatibility with old code (< 5.2.M4) - import info from old event
        // model
        if (options != null) {
            ctx.setProperties(options);
        }
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, getSessionId());
        // Document life cycle
        if (source != null && withLifeCycle) {
            String currentLifeCycleState = null;
            try {
                currentLifeCycleState = source.getCurrentLifeCycleState();
            } catch (ClientException err) {
                // FIXME no lifecycle -- this shouldn't generated an
                // exception (and ClientException logs the spurious error)
            }
            ctx.setProperty(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
        }
        if (comment != null) {
            ctx.setProperty("comment", comment);
        }
        ctx.setProperty(
                "category",
                category == null ? DocumentEventCategories.EVENT_DOCUMENT_CATEGORY
                        : category);
        // compat code: mark SAVE event as a commit event
        Event event = ctx.newEvent(eventId);
        if (DocumentEventTypes.SESSION_SAVED.equals(eventId)) {
            event.setIsCommitEvent(true);
        }
        if (inline) {
            event.setInline(true);
        }
        // compat code: set isLocal on event if JMS is blocked
        if (source != null) {
            Boolean blockJms = (Boolean) source.getContextData("BLOCK_JMS_PRODUCING");
            if (blockJms != null && blockJms) {
                event.setLocal(true);
                event.setInline(true);
            }
        }
        Framework.getLocalService(EventService.class).fireEvent(event);
    }

    /**
     * Copied from obsolete VersionChangeNotifier.
     * <p>
     * Sends change notifications to core event listeners. The event contains
     * info with older document (before version change) and newer doc (current
     * document).
     *
     * @param oldDocument
     * @param newDocument
     * @param options additional info to pass to the event
     */
    protected void notifyVersionChange(DocumentModel oldDocument,
            DocumentModel newDocument, Map<String, Serializable> options)
            throws ClientException {
        final Map<String, Serializable> info = new HashMap<String, Serializable>();
        if (options != null) {
            info.putAll(options);
        }
        info.put(VersioningChangeNotifier.EVT_INFO_NEW_DOC_KEY, newDocument);
        info.put(VersioningChangeNotifier.EVT_INFO_OLD_DOC_KEY, oldDocument);
        notifyEvent(VersioningChangeNotifier.CORE_EVENT_ID_VERSIONING_CHANGE,
                newDocument, info,
                DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY, null,
                false, false);
    }

    @Override
    public boolean hasPermission(Principal principal, DocumentRef docRef,
            String permission) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            return hasPermission(principal, doc, permission);
        } catch (DocumentException e) {
            throw new ClientException("Failed to resolve document ref: "
                    + docRef.toString(), e);
        }
    }

    protected final boolean hasPermission(Principal principal, Document doc,
            String permission) throws DocumentException {
        return getSecurityService().checkPermission(doc, principal, permission);
    }

    @Override
    public boolean hasPermission(DocumentRef docRef, String permission)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            return hasPermission(doc, permission);
        } catch (DocumentException e) {
            throw new ClientException("Failed to resolve document ref: "
                    + docRef.toString(), e);
        }
    }

    protected final boolean hasPermission(Document doc, String permission)
            throws DocumentException {
        // TODO: optimize this - usually ACP is already available when calling
        // this method.
        // -> cache ACP at securitymanager level or try to reuse the ACP when
        // it is known
        return getSecurityService().checkPermission(doc, getPrincipal(),
                permission);
        // return doc.getSession().getSecurityManager().checkPermission(doc,
        // getPrincipal().getName(), permission);
    }

    protected Document resolveReference(DocumentRef docRef)
            throws DocumentException, ClientException {
        if (docRef == null) {
            throw new DocumentException("Invalid reference (null)");
        }
        Object ref = docRef.reference();
        if (ref == null) {
            throw new DocumentException("Invalid reference (null)");
        }
        int type = docRef.type();
        switch (type) {
        case DocumentRef.ID:
            return getSession().getDocumentByUUID((String) ref);
        case DocumentRef.PATH:
            return getSession().resolvePath((String) ref);
        default:
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    /**
     * Gets the document model for the given core document.
     *
     * @param doc the document
     * @return the document model
     */
    protected DocumentModel readModel(Document doc) throws ClientException {
        try {
            return DocumentModelFactory.createDocumentModel(doc, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document model", e);
        }
    }

    /**
     * Gets the document model for the given core document, preserving the
     * contextData.
     *
     * @param doc the document
     * @return the document model
     */
    protected DocumentModel readModel(Document doc, DocumentModel docModel)
            throws ClientException {
        DocumentModel newModel = readModel(doc);
        newModel.copyContextData(docModel);
        return newModel;
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    protected DocumentModel readModel(Document doc, String[] schemas)
            throws ClientException {
        try {
            return DocumentModelFactory.createDocumentModel(doc, schemas);
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document model", e);
        }
    }

    protected DocumentModel writeModel(Document doc, DocumentModel docModel)
            throws DocumentException, ClientException {
        return DocumentModelFactory.writeDocumentModel(docModel, doc);
    }

    @Override
    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name,
            boolean resetLifeCycle) throws ClientException {
        try {
            Document dstDoc = resolveReference(dst);
            checkPermission(dstDoc, ADD_CHILDREN);

            Document srcDoc = resolveReference(src);
            if (name == null) {
                name = srcDoc.getName();
            }

            Map<String, Serializable> options = new HashMap<String, Serializable>();

            // add the destination name, destination, resetLifeCycle flag and
            // source references in
            // the options of the event
            options.put(CoreEventConstants.SOURCE_REF, src);
            options.put(CoreEventConstants.DESTINATION_REF, dst);
            options.put(CoreEventConstants.DESTINATION_PATH, dstDoc.getPath());
            options.put(CoreEventConstants.DESTINATION_NAME, name);
            options.put(CoreEventConstants.DESTINATION_EXISTS,
                    dstDoc.hasChild(name));
            options.put(CoreEventConstants.RESET_LIFECYCLE, resetLifeCycle);
            DocumentModel srcDocModel = readModel(srcDoc);
            notifyEvent(DocumentEventTypes.ABOUT_TO_COPY, srcDocModel, options,
                    null, null, true, true);

            name = (String) options.get(CoreEventConstants.DESTINATION_NAME);
            Document doc = getSession().copy(srcDoc, dstDoc, name);
            // no need to clear lock, locks table is not copied

            // notify document created by copy
            DocumentModel docModel = readModel(doc);

            String comment = srcDoc.getRepositoryName() + ':' + src.toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel,
                    options, null, comment, true, false);
            docModel = writeModel(doc, docModel);

            // notify document copied
            comment = doc.getRepositoryName() + ':'
                    + docModel.getRef().toString();

            notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel,
                    options, null, comment, true, false);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to copy document: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        return copy(src, dst, name, false);
    }

    @Override
    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst,
            boolean resetLifeCycle) throws ClientException {
        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        for (DocumentRef ref : src) {
            newDocuments.add(copy(ref, dst, null, resetLifeCycle));
        }

        return newDocuments;
    }

    @Override
    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        return copy(src, dst, false);
    }

    @Override
    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst,
            String name, boolean resetLifeCycle) throws ClientException {
        try {
            Document srcDoc = resolveReference(src);
            if (!srcDoc.isProxy()) {
                return copy(src, dst, name);
            }
            Document dstDoc = resolveReference(dst);
            checkPermission(dstDoc, WRITE);

            // create a new document using the expanded proxy
            DocumentModel srcDocModel = readModel(srcDoc);
            String docName = (name != null) ? name : srcDocModel.getName();
            DocumentModel docModel = createDocumentModel(dstDoc.getPath(),
                    docName, srcDocModel.getType());
            docModel.copyContent(srcDocModel);
            notifyEvent(DocumentEventTypes.ABOUT_TO_COPY, srcDocModel, null,
                    null, null, true, true);
            docModel = createDocument(docModel);
            Document doc = resolveReference(docModel.getRef());

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            // add resetLifeCycle flag to the event
            options.put(CoreEventConstants.RESET_LIFECYCLE, resetLifeCycle);
            // notify document created by copy
            String comment = srcDoc.getRepositoryName() + ':' + src.toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel,
                    options, null, comment, true, false);

            // notify document copied
            comment = doc.getRepositoryName() + ':'
                    + docModel.getRef().toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel,
                    options, null, comment, true, false);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to copy document: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst,
            String name) throws ClientException {
        return copyProxyAsDocument(src, dst, name, false);
    }

    @Override
    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src,
            DocumentRef dst, boolean resetLifeCycle) throws ClientException {
        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        for (DocumentRef ref : src) {
            newDocuments.add(copyProxyAsDocument(ref, dst, null, resetLifeCycle));
        }

        return newDocuments;
    }

    @Override
    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src,
            DocumentRef dst) throws ClientException {
        return copyProxyAsDocument(src, dst, false);
    }

    @Override
    public DocumentModel move(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        try {
            Document srcDoc = resolveReference(src);
            Document dstDoc;
            if (dst == null) {
                // rename
                dstDoc = srcDoc.getParent();
                checkPermission(dstDoc, WRITE_PROPERTIES);
            } else {
                dstDoc = resolveReference(dst);
                checkPermission(dstDoc, ADD_CHILDREN);
                checkPermission(srcDoc.getParent(), REMOVE_CHILDREN);
                checkPermission(srcDoc, REMOVE);
            }

            DocumentModel srcDocModel = readModel(srcDoc);
            if (name == null) {
                name = srcDocModel.getName();
            }
            Map<String, Serializable> options = getContextMapEventInfo(srcDocModel);
            // add the destination name, destination and source references in
            // the options of the event
            options.put(CoreEventConstants.SOURCE_REF, src);
            options.put(CoreEventConstants.DESTINATION_REF, dst);
            options.put(CoreEventConstants.DESTINATION_PATH, dstDoc.getPath());
            options.put(CoreEventConstants.DESTINATION_NAME, name);
            options.put(CoreEventConstants.DESTINATION_EXISTS,
                    dstDoc.hasChild(name));

            notifyEvent(DocumentEventTypes.ABOUT_TO_MOVE, srcDocModel, options,
                    null, null, true, true);

            name = (String) options.get(CoreEventConstants.DESTINATION_NAME);

            String comment = srcDoc.getRepositoryName() + ':'
                    + srcDoc.getParent().getUUID();

            Document doc = getSession().move(srcDoc, dstDoc, name);

            // notify document moved
            DocumentModel docModel = readModel(doc);
            options.put(CoreEventConstants.PARENT_PATH,
                    srcDocModel.getParentRef());
            notifyEvent(DocumentEventTypes.DOCUMENT_MOVED, docModel, options,
                    null, comment, true, false);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to move document: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public void move(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        for (DocumentRef ref : src) {
            move(ref, dst, null);
        }
    }

    @Override
    public ACP getACP(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_SECURITY);
            return getSession().getMergedACP(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get acp", e);
        }
    }

    @Override
    public void setACP(DocumentRef docRef, ACP newAcp, boolean overwrite)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_SECURITY);
            DocumentModel docModel = readModel(doc);

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            options.put(CoreEventConstants.OLD_ACP,
                    (Serializable) docModel.getACP().clone());
            options.put(CoreEventConstants.NEW_ACP,
                    (Serializable) newAcp.clone());

            notifyEvent(DocumentEventTypes.BEFORE_DOC_SECU_UPDATE, docModel,
                    options, null, null, true, true);
            getSession().setACP(doc, newAcp, overwrite);
            docModel = readModel(doc);
            notifyEvent(DocumentEventTypes.DOCUMENT_SECURITY_UPDATED, docModel,
                    options, null, null, true, false);
        } catch (DocumentException e) {
            throw new ClientException("Failed to set acp", e);
        }
    }

    @Override
    public void cancel() {
        // nothing
    }

    private DocumentModel createDocumentModelFromTypeName(String typeName,
            Map<String, Serializable> options) throws ClientException {
        try {
            SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
            DocumentType docType = schemaManager.getDocumentType(typeName);
            if (docType == null) {
                throw new ClientException(typeName
                        + " is not a registered core type");
            }
            DocumentModel docModel = DocumentModelFactory.createDocumentModel(
                    getSessionId(), docType);
            if (options == null) {
                options = new HashMap<String, Serializable>();
            }
            // do not forward this event on the JMS Bus
            options.put("BLOCK_JMS_PRODUCING", true);
            notifyEvent(DocumentEventTypes.EMPTY_DOCUMENTMODEL_CREATED,
                    docModel, options, null, null, false, true);
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document model", e);
        }
    }

    @Override
    public DocumentModel createDocumentModel(String typeName)
            throws ClientException {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        return createDocumentModelFromTypeName(typeName, options);
    }

    @Override
    public DocumentModel createDocumentModel(String parentPath, String id,
            String typeName) throws ClientException {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(CoreEventConstants.PARENT_PATH, parentPath);
        options.put(CoreEventConstants.DOCUMENT_MODEL_ID, id);
        DocumentModel model = createDocumentModelFromTypeName(typeName, options);
        model.setPathInfo(parentPath, id);
        return model;
    }

    @Override
    public DocumentModel createDocumentModel(String typeName,
            Map<String, Object> options) throws ClientException {

        Map<String, Serializable> serializableOptions = new HashMap<String, Serializable>();

        for (Entry<String, Object> entry : options.entrySet()) {
            serializableOptions.put(entry.getKey(),
                    (Serializable) entry.getValue());
        }
        return createDocumentModelFromTypeName(typeName, serializableOptions);
    }

    @Override
    public DocumentModel createDocument(DocumentModel docModel)
            throws ClientException {
        String typeName = docModel.getType();
        DocumentRef parentRef = docModel.getParentRef();
        if (typeName == null) {
            throw new ClientException(String.format(
                    "cannot create document '%s' with undefined type name",
                    docModel.getTitle()));
        }
        if (parentRef == null && !isAdministrator()) {
            throw new ClientException(
                    "Only Administrators can create placeless documents");
        }
        String childName = docModel.getName();
        try {
            Map<String, Serializable> options = getContextMapEventInfo(docModel);
            Document folder = fillCreateOptions(parentRef, childName, options);

            // get initial life cycle state info
            String initialLifecycleState = null;
            Object lifecycleStateInfo = docModel.getContextData(LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME);
            if (lifecycleStateInfo instanceof String) {
                initialLifecycleState = (String) lifecycleStateInfo;
            }
            notifyEvent(DocumentEventTypes.ABOUT_TO_CREATE, docModel, options,
                    null, null, false, true); // no lifecycle yet
            childName = (String) options.get(CoreEventConstants.DESTINATION_NAME);
            Document doc = folder.addChild(childName, typeName);

            // update facets too since some of them may be dynamic
            for (String facetName : docModel.getFacets()) {
                if (!doc.getAllFacets().contains(facetName)
                        && !FacetNames.IMMUTABLE.equals(facetName)) {
                    doc.addFacet(facetName);
                }
            }

            // init document life cycle
            LifeCycleService service = NXCore.getLifeCycleService();
            if (service != null) {
                try {
                    service.initialize(doc, initialLifecycleState);
                } catch (Exception e) {
                    throw new ClientException(
                            "Failed to initialize document lifecycle", e);
                }
            } else {
                log.debug("No lifecycle service registered");
            }

            // init document with data from doc model
            docModel = writeModel(doc, docModel);

            if (!Boolean.TRUE.equals(docModel.getContextData(ScopeType.REQUEST,
                    VersioningService.SKIP_VERSIONING))) {
                // during remote publishing we want to skip versioning
                // to avoid overwriting the version number
                getVersioningService().doPostCreate(doc, options);
                docModel = readModel(doc, docModel);
            }

            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, docModel, options,
                    null, null, true, false);
            docModel = writeModel(doc, docModel);

            createDocumentCount.inc();
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException(
                    "Failed to create document: " + childName, e);
        }
    }

    protected Document fillCreateOptions(DocumentRef parentRef,
            String childName, Map<String, Serializable> options)
            throws DocumentException, ClientException,
            DocumentSecurityException {
        Document folder;
        if (parentRef == null || EMPTY_PATH.equals(parentRef)) {
            folder = getSession().getNullDocument();
            options.put(CoreEventConstants.DESTINATION_REF, null);
            options.put(CoreEventConstants.DESTINATION_PATH, null);
            options.put(CoreEventConstants.DESTINATION_NAME, childName);
            options.put(CoreEventConstants.DESTINATION_EXISTS, false);
        } else {
            folder = resolveReference(parentRef);
            checkPermission(folder, ADD_CHILDREN);
            options.put(CoreEventConstants.DESTINATION_REF, parentRef);
            options.put(CoreEventConstants.DESTINATION_PATH, folder.getPath());
            options.put(CoreEventConstants.DESTINATION_NAME, childName);
            options.put(CoreEventConstants.DESTINATION_EXISTS,
                    folder.hasChild(childName));
        }
        return folder;
    }

    @Override
    public void importDocuments(List<DocumentModel> docModels)
            throws ClientException {
        try {
            for (DocumentModel docModel : docModels) {
                importDocument(docModel);
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to import documents", e);
        }
    }

    protected static final PathRef EMPTY_PATH = new PathRef("");


    protected void importDocument(DocumentModel docModel)
            throws DocumentException, ClientException {
        if (!isAdministrator()) {
            throw new DocumentSecurityException("Only Administrator can import");
        }
        String name = docModel.getName();
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid empty name");
        }
        String typeName = docModel.getType();
        if (typeName == null || typeName.length() == 0) {
            throw new IllegalArgumentException("Invalid empty type");
        }
        String id = docModel.getId();
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Invalid empty id");
        }
        DocumentRef parentRef = docModel.getParentRef();
        Map<String, Serializable> props = getContextMapEventInfo(docModel);
        if (parentRef != null && EMPTY_PATH.equals(parentRef)) {
            parentRef = null;
        }
        Document parent = fillCreateOptions(parentRef, name, props);
        notifyEvent(DocumentEventTypes.ABOUT_TO_IMPORT, docModel, props, null,
                null, false, true);
        name = (String) props.get(CoreEventConstants.DESTINATION_NAME);

        // create the document
        Document doc = getSession().importDocument(id,
                parentRef == null ? null : parent, name, typeName, props);

        if (typeName.equals(CoreSession.IMPORT_PROXY_TYPE)) {
            // just reread the final document
            docModel = readModel(doc);
        } else {
            // init document with data from doc model
            docModel = writeModel(doc, docModel);
        }

        // send an event about the import
        notifyEvent(DocumentEventTypes.DOCUMENT_IMPORTED, docModel, null, null,
                null, true, false);
    }

    @Override
    public DocumentModel[] createDocument(DocumentModel[] docModels)
            throws ClientException {
        DocumentModel[] models = new DocumentModel[docModels.length];
        int i = 0;
        // TODO: optimize this (do not call at each iteration createDocument())
        for (DocumentModel docModel : docModels) {
            models[i++] = createDocument(docModel);
        }
        return models;
    }

    @Override
    public boolean exists(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            return hasPermission(doc, BROWSE);
        } catch (NoSuchDocumentException e) {
            return false;
        } catch (DocumentException e) {
            throw new ClientException("Failed to check existence of " + docRef,
                    e);
        }
    }

    @Override
    public DocumentModel getChild(DocumentRef parent, String name)
            throws ClientException {
        try {
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Document child = doc.getChild(name);
            checkPermission(child, READ);
            return readModel(child);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get child " + name, e);
        }
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent)
            throws ClientException {
        return getChildren(parent, null, READ, null, null);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type)
            throws ClientException {
        return getChildren(parent, type, READ, null, null);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type,
            String perm) throws ClientException {
        return getChildren(parent, type, perm, null, null);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type,
            Filter filter, Sorter sorter) throws ClientException {
        return getChildren(parent, type, null, filter, sorter);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type,
            String perm, Filter filter, Sorter sorter) throws ClientException {
        try {
            if (perm == null) {
                perm = READ;
            }
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Iterator<Document> children = doc.getChildren();
            DocumentModelList docs = new DocumentModelListImpl();
            while (children.hasNext()) {
                Document child = children.next();
                if (hasPermission(child, perm)) {
                    if (child.getType() != null
                            && (type == null || type.equals(child.getType().getName()))) {
                        DocumentModel childModel = readModel(child);
                        if (filter == null || filter.accept(childModel)) {
                            docs.add(childModel);
                        }
                    }
                }
            }
            if (sorter != null) {
                Collections.sort(docs, sorter);
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get children for "
                    + parent.toString(), e);
        }
    }

    @Override
    public List<DocumentRef> getChildrenRefs(DocumentRef parentRef, String perm)
            throws ClientException {
        if (perm != null) {
            // XXX TODO
            throw new ClientException("perm != null not implemented");
        }
        try {
            Document parent = resolveReference(parentRef);
            checkPermission(parent, READ_CHILDREN);
            List<String> ids = parent.getChildrenIds();
            List<DocumentRef> refs = new ArrayList<DocumentRef>(ids.size());
            for (String id : ids) {
                refs.add(new IdRef(id));
            }
            return refs;
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public DocumentModelIterator getChildrenIterator(DocumentRef parent)
            throws ClientException {
        return getChildrenIterator(parent, null, null, null);
    }

    @Override
    public DocumentModelIterator getChildrenIterator(DocumentRef parent,
            String type) throws ClientException {
        return getChildrenIterator(parent, type, null, null);
    }

    @Override
    public DocumentModelIterator getChildrenIterator(DocumentRef parent,
            String type, String perm, Filter filter) throws ClientException {
        // perm unused, kept for API compat
        return new DocumentModelChildrenIterator(this, parent, type, filter);
    }

    @Override
    public DocumentModel getDocument(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return readModel(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get document "
                    + docRef.toString(), e);
        }
    }

    @Override
    public DocumentModelList getDocuments(DocumentRef[] docRefs)
            throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>(docRefs.length);
        for (DocumentRef docRef : docRefs) {
            Document doc;
            try {
                doc = resolveReference(docRef);
                checkPermission(doc, READ);
            } catch (DocumentException e) {
                // no permission, or other low-level error
                continue;
            }
            docs.add(readModel(doc));
        }
        return new DocumentModelListImpl(docs);
    }

    @Override
    public DocumentModelList getFiles(DocumentRef parent)
            throws ClientException {
        try {
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Iterator<Document> children = doc.getChildren();
            DocumentModelList docs = new DocumentModelListImpl();
            while (children.hasNext()) {
                Document child = children.next();
                if (!child.isFolder() && hasPermission(child, READ)) {
                    docs.add(readModel(child));
                }
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get leaf children for "
                    + parent.toString(), e);
        }
    }

    @Override
    public DocumentModelList getFiles(DocumentRef parent, Filter filter,
            Sorter sorter) throws ClientException {
        try {
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Iterator<Document> children = doc.getChildren();
            DocumentModelList docs = new DocumentModelListImpl();
            while (children.hasNext()) {
                Document child = children.next();
                if (!child.isFolder() && hasPermission(child, READ)) {
                    DocumentModel docModel = readModel(doc);
                    if (filter == null || filter.accept(docModel)) {
                        docs.add(readModel(child));
                    }
                }
            }
            if (sorter != null) {
                Collections.sort(docs, sorter);
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get files for "
                    + parent.toString(), e);
        }
    }

    @Override
    public DocumentModelList getFolders(DocumentRef parent)
            throws ClientException {
        try {
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Iterator<Document> children = doc.getChildren();
            DocumentModelList docs = new DocumentModelListImpl();
            while (children.hasNext()) {
                Document child = children.next();
                if (child.isFolder() && hasPermission(child, READ)) {
                    docs.add(readModel(child));
                }
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get folders " + parent, e);
        }
    }

    @Override
    public DocumentModelList getFolders(DocumentRef parent, Filter filter,
            Sorter sorter) throws ClientException {
        try {
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Iterator<Document> children = doc.getChildren();
            DocumentModelList docs = new DocumentModelListImpl();
            while (children.hasNext()) {
                Document child = children.next();
                if (child.isFolder() && hasPermission(child, READ)) {
                    DocumentModel childModel = readModel(child);
                    if (filter == null || filter.accept(childModel)) {
                        docs.add(childModel);
                    }
                }
            }
            if (sorter != null) {
                Collections.sort(docs, sorter);
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get folders "
                    + parent.toString(), e);
        }
    }

    @Override
    public DocumentRef getParentDocumentRef(DocumentRef docRef)
            throws ClientException {
        try {
            final Document doc = resolveReference(docRef);
            Document parentDoc = doc.getParent();
            return parentDoc != null ? new IdRef(parentDoc.getUUID()) : null;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get parent document ref for: "
                    + docRef, e);
        }
    }

    @Override
    public DocumentModel getParentDocument(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document parentDoc = doc.getParent();
            if (parentDoc == null) {
                return null;
            }
            if (!hasPermission(parentDoc, READ)) {
                throw new DocumentSecurityException(
                        "Privilege READ is not granted to "
                                + getPrincipal().getName());
            }
            return readModel(parentDoc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get parent document of "
                    + docRef, e);
        }
    }

    @Override
    public List<DocumentModel> getParentDocuments(final DocumentRef docRef)
            throws ClientException {

        if (null == docRef) {
            throw new IllegalArgumentException("null docRef");
        }

        final List<DocumentModel> docsList = new ArrayList<DocumentModel>();
        try {
            Document doc = resolveReference(docRef);
            while (doc != null && !"/".equals(doc.getPath())) {
                // XXX OG: shouldn't we check BROWSE and READ_PROPERTIES
                // instead?
                if (!hasPermission(doc, READ)) {
                    break;
                }
                docsList.add(readModel(doc));
                doc = doc.getParent();
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get parent documents: "
                    + docRef, e);
        }
        Collections.reverse(docsList);

        return docsList;
    }

    @Override
    public DocumentModel getRootDocument() throws ClientException {
        try {
            return readModel(getSession().getRootDocument());
        } catch (DocumentException e) {
            throw new ClientException("Failed to get the root document", e);
        }
    }

    @Override
    public boolean hasChildren(DocumentRef docRef) throws ClientException {
        try {
            // TODO: validate permission check with td
            Document doc = resolveReference(docRef);
            checkPermission(doc, BROWSE);
            return doc.hasChildren();
        } catch (DocumentException e) {
            throw new ClientException("Failed to check for children for "
                    + docRef, e);
        }
    }

    @Override
    public DocumentModelList query(String query) throws ClientException {
        return query(query, null, 0, 0, false);
    }

    @Override
    public DocumentModelList query(String query, int max)
            throws ClientException {
        return query(query, null, max, 0, false);
    }

    @Override
    public DocumentModelList query(String query, Filter filter)
            throws ClientException {
        return query(query, filter, 0, 0, false);
    }

    @Override
    public DocumentModelList query(String query, Filter filter, int max)
            throws ClientException {
        return query(query, filter, max, 0, false);
    }

    @Override
    public DocumentModelList query(String query, Filter filter, long limit,
            long offset, boolean countTotal) throws ClientException {
        return query(query, NXQL.NXQL, filter, limit, offset, countTotal);
    }

    @Override
    public DocumentModelList query(String query, String queryType,
            Filter filter, long limit, long offset, boolean countTotal)
            throws ClientException {
        long countUpTo;
        if (!countTotal) {
            countUpTo = 0;
        } else {
            if (isLimitedResults()) {
                countUpTo = getMaxResults();
            } else {
                countUpTo = -1;
            }
        }
        return query(query, queryType, filter, limit, offset, countUpTo);
    }

    protected long getMaxResults() {
        if (maxResults == null) {
            maxResults = Long.parseLong(Framework.getProperty(
                    MAX_RESULTS_PROPERTY, DEFAULT_MAX_RESULTS));
        }
        return maxResults;
    }

    protected boolean isLimitedResults() {
        if (limitedResults == null) {
            limitedResults = Boolean.parseBoolean(Framework.getProperty(LIMIT_RESULTS_PROPERTY));
        }
        return limitedResults;
    }

    protected void setMaxResults(long maxResults) {
        this.maxResults = maxResults;
    }

    protected void setLimitedResults(boolean limitedResults) {
        this.limitedResults = limitedResults;
    }

    @Override
    public DocumentModelList query(String query, Filter filter, long limit,
            long offset, long countUpTo) throws ClientException {
        return query(query, NXQL.NXQL, filter, limit, offset, countUpTo);
    }

    @Override
    public DocumentModelList query(String query, String queryType,
            Filter filter, long limit, long offset, long countUpTo)
            throws ClientException {
        SecurityService securityService = getSecurityService();
        Principal principal = getPrincipal();
        try {
            Query compiledQuery = getSession().createQuery(query, queryType);
            QueryResult results;
            boolean postFilterPermission;
            boolean postFilterFilter;
            boolean postFilterPolicies;
            boolean postFilter;
            String permission = BROWSE;
            if (compiledQuery instanceof FilterableQuery) {
                postFilterPermission = false;
                String repoName = getRepositoryName();
                postFilterPolicies = !securityService.arePoliciesExpressibleInQuery(repoName);
                postFilterFilter = filter != null
                        && !(filter instanceof FacetFilter);
                postFilter = postFilterPolicies || postFilterFilter;
                String[] principals;
                if (isAdministrator()) {
                    principals = null; // means: no security check needed
                } else {
                    principals = SecurityService.getPrincipalsToCheck(principal);
                }
                String[] permissions = securityService.getPermissionsToCheck(permission);
                QueryFilter queryFilter = new QueryFilter(principal,
                        principals, permissions,
                        filter instanceof FacetFilter ? (FacetFilter) filter
                                : null,
                        securityService.getPoliciesQueryTransformers(repoName),
                        postFilter ? 0 : limit, postFilter ? 0 : offset);
                if (postFilter) {
                    results = ((FilterableQuery) compiledQuery).execute(
                            queryFilter, -1);
                } else {
                    results = ((FilterableQuery) compiledQuery).execute(
                            queryFilter, countUpTo);
                }
            } else {
                postFilterPermission = true;
                postFilterPolicies = securityService.arePoliciesRestrictingPermission(permission);
                postFilterFilter = filter != null;
                postFilter = true;
                results = compiledQuery.execute();
            }

            DocumentModelList dms = results.getDocumentModels();

            if (!postFilter) {
                // the backend has done all the needed filtering
                return dms;
            }

            // post-filter the results "by hand", the backend couldn't do it
            long start = limit == 0 || offset < 0 ? 0 : offset;
            long stop = start + (limit == 0 ? dms.size() : limit);
            int n = 0;
            DocumentModelListImpl docs = new DocumentModelListImpl();
            for (DocumentModel model : dms) {
                if (postFilterPermission || postFilterPolicies) {
                    if (!hasPermission(model.getRef(), permission)) {
                        continue;
                    }
                }
                if (postFilterFilter) {
                    if (!filter.accept(model)) {
                        continue;
                    }
                }
                if (n < start) {
                    n++;
                    continue;
                }
                if (n >= stop) {
                    if (countUpTo == 0) {
                        // can break early
                        break;
                    }
                    n++;
                    continue;
                }
                n++;
                docs.add(model);
            }
            if (countUpTo != 0) {
                docs.setTotalSize(n);
            }
            return docs;
        } catch (Exception e) {
            throw new ClientException("Failed to execute query: "
                    + tryToExtractMeaningfulErrMsg(e), e);
        }
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            Object... params) throws ClientException {
        try {
            SecurityService securityService = getSecurityService();
            Principal principal = getPrincipal();
            String[] principals;
            if (isAdministrator()) {
                principals = null; // means: no security check needed
            } else {
                principals = SecurityService.getPrincipalsToCheck(principal);
            }
            String permission = BROWSE;
            String[] permissions = securityService.getPermissionsToCheck(permission);
            Collection<Transformer> transformers;
            if (NXQL.NXQL.equals(queryType)) {
                String repoName = getRepositoryName();
                transformers = securityService.getPoliciesQueryTransformers(repoName);
            } else {
                transformers = Collections.emptyList();
            }
            QueryFilter queryFilter = new QueryFilter(principal, principals,
                    permissions, null, transformers, 0, 0);
            IterableQueryResult result = getSession().queryAndFetch(query,
                    queryType, queryFilter, params);
            return result;
        } catch (Exception e) {
            throw new ClientException("Failed to execute query: " + queryType
                    + ": " + query + ": " + tryToExtractMeaningfulErrMsg(e), e);
        }
    }

    private String tryToExtractMeaningfulErrMsg(Throwable t) {
        if (t instanceof QueryParseException) {
            return t.getMessage();
        }
        if (t.getCause() != null) {
            return tryToExtractMeaningfulErrMsg(t.getCause());
        }
        return t.getMessage();
    }

    @Override
    public void removeChildren(DocumentRef docRef) throws ClientException {
        // TODO: check req permissions with td
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, REMOVE_CHILDREN);
            Iterator<Document> children = doc.getChildren();
            while (children.hasNext()) {
                // iterator remove method is not supported by jcr
                Document child = children.next();
                if (hasPermission(child, REMOVE)) {
                    removeNotifyOneDoc(child);
                }
            }
        } catch (DocumentException e) {
            throw new ClientException(
                    "Failed to remove children for " + docRef, e);
        }
    }

    @Override
    public boolean canRemoveDocument(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            return canRemoveDocument(doc);
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    protected boolean canRemoveDocument(Document doc) throws ClientException,
            DocumentException {
        if (isAdministrator()) {
            return true;
        }
        if (doc.isVersion()) {
            // TODO a hasProxies method would be more efficient
            Collection<Document> proxies = getSession().getProxies(doc, null);
            if (!proxies.isEmpty()) {
                return false;
            }
            // find a working document to check security
            Document working;
            try {
                working = doc.getSourceDocument();
            } catch (DocumentException e) {
                working = null;
            }
            if (working != null) {
                return hasPermission(working, WRITE_VERSION);
            } else {
                // no working document, only admins can remove
                return false;
            }
        } else {
            if (!hasPermission(doc, REMOVE)) {
                return false;
            }
            Document parent = doc.getParent();
            return parent == null || hasPermission(parent, REMOVE_CHILDREN);
        }
    }

    @Override
    public void removeDocument(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            removeDocument(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to fetch document " + docRef
                    + " before removal", e);
        }
    }

    protected void removeDocument(Document doc) throws ClientException {
        try {
            if (!canRemoveDocument(doc)) {
                throw new DocumentSecurityException(
                        "Permission denied: cannot remove document "
                                + doc.getUUID());
            }
            removeNotifyOneDoc(doc);

        } catch (ConcurrentUpdateDocumentException e) {
            throw new ConcurrentUpdateException("Failed to remove document "
                    + doc.getUUID(), e);
        } catch (DocumentException e) {
            throw new ClientException("Failed to remove document "
                    + doc.getUUID(), e);
        }
        deleteDocumentCount.inc();
    }

    protected void removeNotifyOneDoc(Document doc) throws ClientException,
            DocumentException {
        // XXX notify with options if needed
        DocumentModel docModel = readModel(doc);
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        if (docModel != null) {
            options.put("docTitle", docModel.getTitle());
        }
        String versionLabel = "";
        Document sourceDoc = null;
        // notify different events depending on wether the document is a
        // version or not
        if (!doc.isVersion()) {
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE, docModel, options,
                    null, null, true, true);
            CoreService coreService = Framework.getLocalService(CoreService.class);
            coreService.getVersionRemovalPolicy().removeVersions(getSession(),
                    doc, this);
        } else {
            versionLabel = docModel.getVersionLabel();
            try {
                sourceDoc = doc.getSourceDocument();
            } catch (DocumentException e) {
                sourceDoc = null;
            }
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE_VERSION, docModel,
                    options, null, null, true, true);

        }
        doc.remove();
        if (doc.isVersion()) {
            if (sourceDoc != null) {
                DocumentModel sourceDocModel = readModel(sourceDoc);
                if (sourceDocModel != null) {
                    options.put("comment", versionLabel); // to be used by
                                                          // audit
                    // service
                    notifyEvent(DocumentEventTypes.VERSION_REMOVED,
                            sourceDocModel, options, null, null, false, false);
                    options.remove("comment");
                }
                options.put("docSource", sourceDoc.getUUID());
            }
        }
        notifyEvent(DocumentEventTypes.DOCUMENT_REMOVED, docModel, options,
                null, null, false, false);
    }

    /**
     * Implementation uses the fact that the lexicographic ordering of paths is
     * a refinement of the "contains" partial ordering.
     */
    @Override
    public void removeDocuments(DocumentRef[] docRefs) throws ClientException {
        Document[] docs = new Document[docRefs.length];

        for (int i = 0; i < docs.length; i++) {
            try {
                docs[i] = resolveReference(docRefs[i]);
            } catch (DocumentException e) {
                throw new ClientException("Failed to resolve reference "
                        + docRefs[i], e);
            }
        }
        // TODO OPTIM: it's not guaranteed that getPath is cheap and
        // we call it a lot. Should use an object for pairs (document, path)
        // to call it just once per doc.
        Arrays.sort(docs, pathComparator);
        String[] paths = new String[docs.length];
        try {
            for (int i = 0; i < docs.length; i++) {
                paths[i] = docs[i].getPath();
            }
            String latestRemoved = null;
            for (int i = 0; i < docs.length; i++) {
                if (i == 0 || !paths[i].startsWith(latestRemoved + "/")) {
                    removeDocument(docs[i]);
                    latestRemoved = paths[i];
                }
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get path of document", e);
        }
    }

    @Override
    public void save() throws ClientException {
        try {
            final Map<String, Serializable> options = new HashMap<String, Serializable>();
            getSession().save();
            notifyEvent(DocumentEventTypes.SESSION_SAVED, null, options, null,
                    null, true, false);
        } catch (ConcurrentUpdateDocumentException e) {
            throw new ConcurrentUpdateException("Failed to save session", e);
        } catch (DocumentException e) {
            throw new ClientException("Failed to save session", e);
        }
    }

    @Override
    public DocumentModel saveDocument(DocumentModel docModel)
            throws ClientException {
        try {
            if (docModel.getRef() == null) {
                throw new ClientException(String.format(
                        "cannot save document '%s' with null reference: "
                                + "document has probably not yet been created "
                                + "in the repository with "
                                + "'CoreSession.createDocument(docModel)'",
                        docModel.getTitle()));
            }
            Document doc = resolveReference(docModel.getRef());
            checkPermission(doc, WRITE_PROPERTIES);

            Map<String, Serializable> options = getContextMapEventInfo(docModel);
            options.put(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL,
                    readModel(doc));
            // regular event, last chance to modify docModel
            options.put(CoreEventConstants.DESTINATION_NAME, docModel.getName());
            notifyEvent(DocumentEventTypes.BEFORE_DOC_UPDATE, docModel,
                    options, null, null, true, true);
            String name = (String) options.get(CoreEventConstants.DESTINATION_NAME);
            // did the event change the name? not applicable to Root whose
            // name is null/empty
            if (name != null && !name.equals(docModel.getName())) {
                doc = getSession().move(doc, doc.getParent(), name);
            }

            VersioningOption versioningOption = (VersioningOption) docModel.getContextData(VersioningService.VERSIONING_OPTION);
            docModel.putContextData(VersioningService.VERSIONING_OPTION, null);
            String checkinComment = (String) docModel.getContextData(VersioningService.CHECKIN_COMMENT);
            docModel.putContextData(VersioningService.CHECKIN_COMMENT, null);
            Boolean disableAutoCheckOut = (Boolean) docModel.getContextData(VersioningService.DISABLE_AUTO_CHECKOUT);
            docModel.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT,
                    null);
            options.put(VersioningService.DISABLE_AUTO_CHECKOUT,
                    disableAutoCheckOut);
            // compat
            boolean snapshot = Boolean.TRUE.equals(docModel.getContextData(
                    ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY));
            docModel.putContextData(ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, null);
            boolean dirty = docModel.isDirty();
            if (versioningOption == null && snapshot && dirty) {
                String key = String.valueOf(docModel.getContextData(
                        ScopeType.REQUEST,
                        VersioningDocument.KEY_FOR_INC_OPTION));
                docModel.putContextData(ScopeType.REQUEST,
                        VersioningDocument.KEY_FOR_INC_OPTION, null);
                versioningOption = "inc_major".equals(key) ? VersioningOption.MAJOR
                        : VersioningOption.MINOR;
            }

            if (!docModel.isImmutable()) {
                // pre-save versioning
                boolean checkout = getVersioningService().isPreSaveDoingCheckOut(
                        doc, dirty, versioningOption, options);
                if (checkout) {
                    notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel,
                            options, null, null, true, true);
                }
                versioningOption = getVersioningService().doPreSave(doc, dirty,
                        versioningOption, checkinComment, options);
                if (checkout) {
                    DocumentModel checkedOutDoc = readModel(doc);
                    notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT,
                            checkedOutDoc, options, null, null, true, false);
                }
            }

            boolean allowVersionWrite = Boolean.TRUE.equals(docModel.getContextData(ALLOW_VERSION_WRITE));
            docModel.putContextData(ALLOW_VERSION_WRITE, null);
            boolean setReadWrite = allowVersionWrite && doc.isVersion()
                    && doc.isReadOnly();

            // actual save
            if (setReadWrite) {
                doc.setReadOnly(false);
            }
            docModel = writeModel(doc, docModel);
            if (setReadWrite) {
                doc.setReadOnly(true);
            }

            Document checkedInDoc = null;
            if (!docModel.isImmutable()) {
                // post-save versioning
                boolean checkin = getVersioningService().isPostSaveDoingCheckIn(
                        doc, versioningOption, options);
                if (checkin) {
                    notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel,
                            options, null, null, true, true);
                }
                checkedInDoc = getVersioningService().doPostSave(doc,
                        versioningOption, checkinComment, options);
            }

            // post-save events
            docModel = readModel(doc);
            if (checkedInDoc != null) {
                DocumentRef checkedInVersionRef = new IdRef(
                        checkedInDoc.getUUID());
                notifyCheckedInVersion(docModel, checkedInVersionRef, options,
                        checkinComment);
            }
            notifyEvent(DocumentEventTypes.DOCUMENT_UPDATED, docModel, options,
                    null, null, true, false);
            updateDocumentCount.inc();
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to save document " + docModel, e);
        }
    }

    @Override
    @Deprecated
    public boolean isDirty(DocumentRef docRef) throws ClientException {
        try {
            return resolveReference(docRef).isCheckedOut();
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public void saveDocuments(DocumentModel[] docModels) throws ClientException {
        // TODO: optimize this - avoid calling at each iteration saveDoc...
        for (DocumentModel docModel : docModels) {
            saveDocument(docModel);
        }
    }

    @Override
    public DocumentModel getSourceDocument(DocumentRef docRef)
            throws ClientException {
        assert null != docRef;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            Document headDocument = doc.getSourceDocument();
            if (headDocument == null) {
                throw new DocumentException("Source document has been deleted");
            }
            return readModel(headDocument);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get head document for "
                    + docRef, e);
        }
    }

    protected VersionModel getVersionModel(Document version)
            throws DocumentException {
        VersionModel versionModel = new VersionModelImpl();
        versionModel.setId(version.getUUID());
        versionModel.setCreated(version.getVersionCreationDate());
        versionModel.setDescription(version.getCheckinComment());
        versionModel.setLabel(version.getVersionLabel());
        return versionModel;
    }

    @Override
    public VersionModel getLastVersion(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            Document version = doc.getLastVersion();
            return version == null ? null : getVersionModel(version);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    @Override
    public DocumentModel getLastDocumentVersion(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            Document version = doc.getLastVersion();
            return version == null ? null : readModel(version);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    @Override
    public DocumentRef getLastDocumentVersionRef(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            Document version = doc.getLastVersion();
            return version == null ? null : new IdRef(version.getUUID());
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    @Override
    public List<DocumentRef> getVersionsRefs(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            List<String> ids = doc.getVersionsIds();
            List<DocumentRef> refs = new ArrayList<DocumentRef>(ids.size());
            for (String id : ids) {
                refs.add(new IdRef(id));
            }
            return refs;
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public List<DocumentModel> getVersions(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            List<Document> docVersions = doc.getVersions();
            List<DocumentModel> versions = new ArrayList<DocumentModel>(
                    docVersions.size());
            for (Document version : docVersions) {
                versions.add(readModel(version));
            }
            return versions;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    @Override
    public List<VersionModel> getVersionsForDocument(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            List<Document> docVersions = doc.getVersions();
            List<VersionModel> versions = new ArrayList<VersionModel>(
                    docVersions.size());
            for (Document version : docVersions) {
                versions.add(getVersionModel(version));
            }
            return versions;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }

    }

    @Override
    public DocumentModel restoreToVersion(DocumentRef docRef,
            DocumentRef versionRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document ver = resolveReference(versionRef);
            return restoreToVersion(doc, ver, false, true);
        } catch (DocumentException e) {
            throw new ClientException("Failed to restore document", e);
        }
    }

    @Override
    @Deprecated
    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        return restoreToVersion(docRef, version, false);
    }

    @Override
    @Deprecated
    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version, boolean skipSnapshotCreation)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document ver = doc.getVersion(version.getLabel());
            return restoreToVersion(doc, ver, skipSnapshotCreation, false);
        } catch (DocumentException e) {
            throw new ClientException("Failed to restore document", e);
        }
    }

    @Override
    public DocumentModel restoreToVersion(DocumentRef docRef,
            DocumentRef versionRef, boolean skipSnapshotCreation,
            boolean skipCheckout) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document ver = resolveReference(versionRef);
            return restoreToVersion(doc, ver, skipSnapshotCreation,
                    skipCheckout);
        } catch (DocumentException e) {
            throw new ClientException("Failed to restore document", e);
        }
    }

    protected DocumentModel restoreToVersion(Document doc, Document version,
            boolean skipSnapshotCreation, boolean skipCheckout)
            throws ClientException {
        try {
            checkPermission(doc, WRITE_VERSION);

            DocumentModel docModel = readModel(doc);

            Map<String, Serializable> options = new HashMap<String, Serializable>();

            // we're about to overwrite the document, make sure it's archived
            if (!skipSnapshotCreation && doc.isCheckedOut()) {
                String checkinComment = (String) docModel.getContextData(VersioningService.CHECKIN_COMMENT);
                docModel.putContextData(VersioningService.CHECKIN_COMMENT, null);
                notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel,
                        options, null, null, true, true);
                Document ver = getVersioningService().doCheckIn(doc, null,
                        checkinComment);
                docModel.refresh(DocumentModel.REFRESH_STATE, null);
                notifyCheckedInVersion(docModel, new IdRef(ver.getUUID()),
                        null, checkinComment);
            }

            // FIXME: the fields are hardcoded. should be moved in versioning
            // component
            // HOW?
            final Long majorVer = (Long) doc.getPropertyValue("major_version");
            final Long minorVer = (Long) doc.getPropertyValue("minor_version");
            if (majorVer != null || minorVer != null) {
                options.put(
                        VersioningDocument.CURRENT_DOCUMENT_MAJOR_VERSION_KEY,
                        majorVer);
                options.put(
                        VersioningDocument.CURRENT_DOCUMENT_MINOR_VERSION_KEY,
                        minorVer);
            }
            // add the uuid of the version being restored
            String versionUUID = version.getUUID();
            options.put(VersioningDocument.RESTORED_VERSION_UUID_KEY,
                    versionUUID);

            notifyEvent(DocumentEventTypes.BEFORE_DOC_RESTORE, docModel,
                    options, null, null, true, true);
            writeModel(doc, docModel);

            doc.restore(version);
            // re-read doc model after restoration
            docModel = readModel(doc);
            notifyEvent(DocumentEventTypes.DOCUMENT_RESTORED, docModel,
                    options, null, docModel.getVersionLabel(), true, false);
            docModel = writeModel(doc, docModel);

            if (!skipCheckout) {
                // restore gives us a checked in document, so do a checkout
                notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel,
                        options, null, null, true, true);
                getVersioningService().doCheckOut(doc);
                docModel = readModel(doc);
                notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, docModel,
                        options, null, null, true, false);
            }

            log.debug("Document restored to version:" + version.getUUID());
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to restore document " + doc, e);
        }
    }

    @Override
    public DocumentRef getBaseVersion(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            Document ver = doc.getBaseVersion();
            if (ver == null) {
                return null;
            }
            checkPermission(ver, READ);
            return new IdRef(ver.getUUID());
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    @Deprecated
    public DocumentModel checkIn(DocumentRef docRef, VersionModel ver)
            throws ClientException {
        try {
            DocumentRef verRef = checkIn(docRef, VersioningOption.MINOR,
                    ver == null ? null : ver.getDescription());
            return readModel(resolveReference(verRef));
        } catch (DocumentException e) {
            throw new ClientException("Failed to check in document " + docRef,
                    e);
        }
    }

    @Override
    public DocumentRef checkIn(DocumentRef docRef, VersioningOption option,
            String checkinComment) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_PROPERTIES);
            DocumentModel docModel = readModel(doc);

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, options,
                    null, null, true, true);
            writeModel(doc, docModel);

            Document version = getVersioningService().doCheckIn(doc, option,
                    checkinComment);

            docModel = readModel(doc);
            DocumentRef checkedInVersionRef = new IdRef(version.getUUID());
            notifyCheckedInVersion(docModel, checkedInVersionRef, options,
                    checkinComment);
            writeModel(doc, docModel);

            return checkedInVersionRef;
        } catch (DocumentException e) {
            throw new ClientException("Failed to check in document " + docRef,
                    e);
        }
    }

    /**
     * Send a core event for the creation of a new check in version. The source
     * document is the live document model used as the source for the checkin,
     * not the archived version it-self.
     *
     * @param docModel work document that has been checked-in as a version
     * @param checkedInVersionRef document ref of the new checked-in version
     * @param options initial option map, or null
     * @param checkinComment
     * @throws ClientException
     */
    protected void notifyCheckedInVersion(DocumentModel docModel,
            DocumentRef checkedInVersionRef, Map<String, Serializable> options,
            String checkinComment) throws ClientException {
        String label = getVersioningService().getVersionLabel(docModel);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        if (options != null) {
            props.putAll(options);
        }
        props.put("versionLabel", label);
        props.put("checkInComment", checkinComment);
        props.put("checkedInVersionRef", checkedInVersionRef);
        if (checkinComment == null && options != null) {
            // check if there's a comment already in options
            Object optionsComment = options.get("comment");
            if (optionsComment instanceof String) {
                checkinComment = (String) optionsComment;
            }
        }
        String comment = checkinComment == null ? label : label + ' '
                + checkinComment;
        props.put("comment", comment); // compat, used in audit
        // notify checkin on live document
        notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDIN, docModel, props,
                null, null, true, false);
        // notify creation on version document
        notifyEvent(DocumentEventTypes.DOCUMENT_CREATED,
                getDocument(checkedInVersionRef), props, null, null, true,
                false);

    }

    @Override
    public void checkOut(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            // TODO: add a new permission names CHECKOUT and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);
            DocumentModel docModel = readModel(doc);
            Map<String, Serializable> options = new HashMap<String, Serializable>();

            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel,
                    options, null, null, true, true);

            getVersioningService().doCheckOut(doc);
            docModel = readModel(doc);

            notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, docModel,
                    options, null, null, true, false);
            writeModel(doc, docModel);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document " + docRef,
                    e);
        }
    }

    public void internalCheckOut(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document " + docRef,
                    e);
        }
    }

    @Override
    public boolean isCheckedOut(DocumentRef docRef) throws ClientException {
        assert null != docRef;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, BROWSE);
            return doc.isCheckedOut();
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document " + docRef,
                    e);
        }
    }

    @Override
    public String getVersionSeriesId(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return doc.getVersionSeriesId();
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public DocumentModel getWorkingCopy(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            Document pwc = doc.getWorkingCopy();
            checkPermission(pwc, READ);
            return pwc == null ? null : readModel(pwc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    @Override
    public DocumentModel getVersion(String versionableId,
            VersionModel versionModel) throws ClientException {
        String id = versionModel.getId();
        if (id != null) {
            return getDocument(new IdRef(id));
        }
        try {
            Document doc = getSession().getVersion(versionableId, versionModel);
            if (doc == null) {
                return null;
            }
            checkPermission(doc, READ_PROPERTIES);
            checkPermission(doc, READ_VERSION);
            return readModel(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get version "
                    + versionModel.getLabel() + " for " + versionableId, e);
        }
    }

    @Override
    public String getVersionLabel(DocumentModel docModel)
            throws ClientException {
        return getVersioningService().getVersionLabel(docModel);
    }

    @Override
    public DocumentModel getDocumentWithVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        String id = version.getId();
        if (id != null) {
            return getDocument(new IdRef(id));
        }
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_PROPERTIES);
            checkPermission(doc, READ_VERSION);
            String docPath = doc.getPath();
            doc = doc.getVersion(version.getLabel());
            if (doc == null) {
                // SQL Storage uses to return null if version not found
                log.debug("Version " + version.getLabel()
                        + " does not exist for " + docPath);
                return null;
            }
            log.debug("Retrieved the version " + version.getLabel()
                    + " of the document " + docPath);
            return readModel(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get version for " + docRef, e);
        }
    }

    @Override
    public DocumentModel createProxy(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document fold = resolveReference(folderRef);
            checkPermission(doc, READ);
            checkPermission(fold, ADD_CHILDREN);
            return createProxyInternal(doc, fold,
                    new HashMap<String, Serializable>());
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    protected DocumentModel createProxyInternal(Document doc, Document folder,
            Map<String, Serializable> options) throws ClientException {
        try {
            // create the new proxy
            Document proxy = getSession().createProxy(doc, folder);
            DocumentModel proxyModel = readModel(proxy);

            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, proxyModel,
                    options, null, null, true, false);
            notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                    proxyModel, options, null, null, true, false);
            DocumentModel folderModel = readModel(folder);
            notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                    folderModel, options, null, null, true, false);
            return proxyModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to create proxy for doc: " + doc,
                    e);
        }
    }

    /**
     * Remove proxies for the same base document in the folder. doc may be a
     * normal document or a proxy.
     */
    protected List<String> removeExistingProxies(Document doc, Document folder)
            throws DocumentException, ClientException {
        Collection<Document> otherProxies = getSession().getProxies(doc, folder);
        List<String> removedProxyIds = new ArrayList<String>(
                otherProxies.size());
        for (Document otherProxy : otherProxies) {
            removedProxyIds.add(otherProxy.getUUID());
            removeNotifyOneDoc(otherProxy);
        }
        return removedProxyIds;
    }

    /**
     * Update the proxy for doc in the given section to point to the new target.
     * Do nothing if there are several proxies.
     *
     * @return the proxy if it was updated, or {@code null} if none or several
     *         were found
     */
    protected DocumentModel updateExistingProxies(Document doc,
            Document folder, Document target) throws DocumentException,
            ClientException {
        Collection<Document> proxies = getSession().getProxies(doc, folder);
        try {
            if (proxies.size() == 1) {
                for (Document proxy : proxies) {
                    if (proxy instanceof DocumentProxy) {
                        ((DocumentProxy) proxy).setTargetDocument(target);
                        return readModel(proxy);
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            log.error("Cannot update proxy, try to remove");
        }
        return null;
    }

    @Override
    public DocumentModelList getProxies(DocumentRef docRef,
            DocumentRef folderRef) throws ClientException {
        try {
            Document folder = null;
            if (folderRef != null) {
                folder = resolveReference(folderRef);
                checkPermission(folder, READ_CHILDREN);
            }
            Document doc = resolveReference(docRef);
            Collection<Document> children = getSession().getProxies(doc, folder);
            DocumentModelList docs = new DocumentModelListImpl();
            for (Document child : children) {
                if (hasPermission(child, READ)) {
                    docs.add(readModel(child));
                }
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException(
                    "Failed to get children for " + folderRef, e);
        }
    }

    @Override
    public String[] getProxyVersions(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException {
        try {
            Document folder = resolveReference(folderRef);
            Document doc = resolveReference(docRef);
            checkPermission(folder, READ_CHILDREN);
            Collection<Document> children = getSession().getProxies(doc, folder);
            if (children.isEmpty()) {
                return null;
            }
            List<String> versions = new ArrayList<String>();
            for (Document child : children) {
                if (hasPermission(child, READ)) {
                    Document target = ((DocumentProxy) child).getTargetDocument();
                    if (target.isVersion()) {
                        versions.add(target.getVersionLabel());
                    } else {
                        // live proxy
                        versions.add("");
                    }
                }
            }
            return versions.toArray(new String[versions.size()]);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get children for "
                    + folderRef.toString(), e);
        }
    }

    @Override
    public List<String> getAvailableSecurityPermissions()
            throws ClientException {
        // XXX: add security check?
        return Arrays.asList(getSecurityService().getPermissionProvider().getPermissions());
    }

    @Override
    public DataModel getDataModel(DocumentRef docRef, Schema schema)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return DocumentModelFactory.createDataModel(doc, schema);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get data model for " + docRef
                    + ':' + schema, e);
        }
    }

    protected Object getDataModelField(DocumentRef docRef, String schema,
            String field) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            if (doc != null) {
                checkPermission(doc, READ);
                Schema docSchema = doc.getType().getSchema(schema);
                if (docSchema != null) {
                    String prefix = docSchema.getNamespace().prefix;
                    if (prefix != null && prefix.length() > 0) {
                        field = prefix + ':' + field;
                    }
                    return doc.getPropertyValue(field);
                } else {
                    log.warn("Cannot find schema with name=" + schema);
                }
            } else {
                log.warn("Cannot resolve docRef=" + docRef);
            }
            return null;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get data model field "
                    + schema + ':' + field, e);
        }
    }

    @Override
    public SerializableInputStream getContentData(String key)
            throws ClientException {
        try {
            InputStream in = getSession().getDataStream(key);
            return new SerializableInputStream(in);
        } catch (Exception e) {
            throw new ClientException("Failed to get data stream for " + key, e);
        }
    }

    @Override
    public String getCurrentLifeCycleState(DocumentRef docRef)
            throws ClientException {
        String lifeCycleState;
        try {
            Document doc = resolveReference(docRef);

            checkPermission(doc, READ_LIFE_CYCLE);
            lifeCycleState = doc.getLifeCycleState();
        } catch (LifeCycleException e) {
            ClientException ce = new ClientException(
                    "Failed to get life cycle " + docRef, e);
            ce.fillInStackTrace();
            throw ce;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return lifeCycleState;
    }

    @Override
    public String getLifeCyclePolicy(DocumentRef docRef) throws ClientException {
        String lifecyclePolicy;
        try {
            Document doc = resolveReference(docRef);

            checkPermission(doc, READ_LIFE_CYCLE);
            lifecyclePolicy = doc.getLifeCyclePolicy();
        } catch (LifeCycleException e) {
            ClientException ce = new ClientException(
                    "Failed to get life cycle policy" + docRef, e);
            ce.fillInStackTrace();
            throw ce;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return lifecyclePolicy;
    }

    /**
     * Make a document follow a transition.
     * @param docRef a {@link DocumentRef}
     * @param transition the transition to follow
     * @param options an option map than can be used by callers to pass additional params
     * @return
     * @throws ClientException
     *
     * @since 5.9.3
     */
    private boolean followTransition(DocumentRef docRef, String transition,
            ScopedMap options) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_LIFE_CYCLE);

            if (!doc.isVersion() && !doc.isProxy() && !doc.isCheckedOut()) {
                checkOut(docRef);
                doc = resolveReference(docRef);
            }
            String formerStateName = doc.getLifeCycleState();
            doc.followTransition(transition);

            // Construct a map holding meta information about the event.
            Map<String, Serializable> eventOptions = new HashMap<String, Serializable>();
            eventOptions.put(
                    org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_FROM,
                    formerStateName);
            eventOptions.put(
                    org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TO,
                    doc.getLifeCycleState());
            eventOptions.put(
                    org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION,
                    transition);
            String comment = (String) options.getScopedValue("comment");
            DocumentModel docModel = readModel(doc);
            notifyEvent(
                    org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT,
                    docModel, eventOptions,
                    DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY, comment,
                    true, false);
            if (!docModel.isImmutable()) {
                writeModel(doc, docModel);
            }

        } catch (LifeCycleException e) {
            ClientException ce = new ClientException(
                    "Unable to follow transition <" + transition
                            + "> for document : " + docRef, e);
            ce.fillInStackTrace();
            throw ce;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return true; // throws if error
    }

    @Override
    public boolean followTransition(DocumentModel docModel, String transition)
            throws ClientException {
        return followTransition(docModel.getRef(), transition,
                docModel.getContextData());
    }

    @Override
    public boolean followTransition(DocumentRef docRef, String transition)
            throws ClientException {
        return followTransition(docRef, transition,
                new ScopedMap());
    }

    @Override
    public Collection<String> getAllowedStateTransitions(DocumentRef docRef)
            throws ClientException {
        Collection<String> allowedStateTransitions;
        try {
            Document doc = resolveReference(docRef);

            checkPermission(doc, READ_LIFE_CYCLE);
            allowedStateTransitions = doc.getAllowedStateTransitions();
        } catch (LifeCycleException e) {
            ClientException ce = new ClientException(
                    "Unable to get allowed state transitions for document : "
                            + docRef, e);
            ce.fillInStackTrace();
            throw ce;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return allowedStateTransitions;
    }

    @Override
    public void reinitLifeCycleState(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_LIFE_CYCLE);
            LifeCycleService service = NXCore.getLifeCycleService();
            service.reinitLifeCycle(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        } catch (LifeCycleException e) {
            throw new ClientException("Failed to reinit life cycle " + docRef,
                    e);
        }
    }

    @Override
    public Object[] getDataModelsField(DocumentRef[] docRefs, String schema,
            String field) throws ClientException {

        assert docRefs != null;
        assert schema != null;
        assert field != null;

        final Object[] values = new Object[docRefs.length];
        int i = 0;
        for (DocumentRef docRef : docRefs) {
            final Object value = getDataModelField(docRef, schema, field);
            values[i++] = value;
        }

        return values;
    }

    @Override
    public DocumentRef[] getParentDocumentRefs(DocumentRef docRef)
            throws ClientException {

        final List<DocumentRef> docRefs = new ArrayList<DocumentRef>();
        try {
            final Document doc = resolveReference(docRef);

            Document parentDoc = doc.getParent();
            while (parentDoc != null) {
                final DocumentRef parentDocRef = new IdRef(parentDoc.getUUID());
                docRefs.add(parentDocRef);
                parentDoc = parentDoc.getParent();
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get all parent documents: "
                    + docRef, e);
        }

        DocumentRef[] refs = new DocumentRef[docRefs.size()];

        return docRefs.toArray(refs);
    }

    @Override
    public Object[] getDataModelsFieldUp(DocumentRef docRef, String schema,
            String field) throws ClientException {

        final DocumentRef[] parentRefs = getParentDocumentRefs(docRef);
        final DocumentRef[] allRefs = new DocumentRef[parentRefs.length + 1];
        allRefs[0] = docRef;
        System.arraycopy(parentRefs, 0, allRefs, 1, parentRefs.length);

        return getDataModelsField(allRefs, schema, field);
    }

    protected String oldLockKey(Lock lock) {
        if (lock == null) {
            return null;
        }
        // return deprecated format, like "someuser:Nov 29, 2010"
        String lockCreationDate = (lock.getCreated() == null) ? null
                : DateFormat.getDateInstance(DateFormat.MEDIUM).format(
                        new Date(lock.getCreated().getTimeInMillis()));
        return lock.getOwner() + ':' + lockCreationDate;
    }

    @Override
    @Deprecated
    public String getLock(DocumentRef docRef) throws ClientException {
        Lock lock = getLockInfo(docRef);
        return oldLockKey(lock);
    }

    @Override
    @Deprecated
    public void setLock(DocumentRef docRef, String key) throws ClientException {
        setLock(docRef);
    }

    @Override
    @Deprecated
    public String unlock(DocumentRef docRef) throws ClientException {
        Lock lock = removeLock(docRef);
        return oldLockKey(lock);
    }

    @Override
    public Lock setLock(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            // TODO: add a new permission named LOCK and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);
            Lock lock = new Lock(getPrincipal().getName(),
                    new GregorianCalendar());
            Lock oldLock = doc.setLock(lock);
            if (oldLock != null) {
                throw new ClientException("Document already locked by "
                        + oldLock.getOwner() + ": " + docRef);
            }
            DocumentModel docModel = readModel(doc);
            Map<String, Serializable> options = new HashMap<String, Serializable>();
            options.put("lock", lock);
            notifyEvent(DocumentEventTypes.DOCUMENT_LOCKED, docModel, options,
                    null, null, true, false);
            return lock;
        } catch (DocumentException e) {
            throw new ClientException("Failed to set lock on " + docRef, e);
        }
    }

    @Override
    public Lock getLockInfo(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return doc.getLock();
        } catch (DocumentException e) {
            throw new ClientException("Failed to get lock info on " + docRef, e);
        }
    }

    @Override
    public Lock removeLock(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            String owner;
            if (hasPermission(docRef, UNLOCK)) {
                // always unlock
                owner = null;
            } else {
                owner = getPrincipal().getName();
            }
            Lock lock = doc.removeLock(owner);
            if (lock == null) {
                // there was no lock, we're done
                return null;
            }
            if (lock.getFailed()) {
                // lock removal failed due to owner check
                throw new ClientException("Document already locked by "
                        + lock.getOwner() + ": " + docRef);
            }
            DocumentModel docModel = readModel(doc);
            Map<String, Serializable> options = new HashMap<String, Serializable>();
            options.put("lock", lock);
            notifyEvent(DocumentEventTypes.DOCUMENT_UNLOCKED, docModel,
                    options, null, null, true, false);
            return lock;
        } catch (DocumentException e) {
            throw new ClientException("Failed to set lock on " + docRef, e);
        }
    }

    protected boolean isAdministrator() {
        Principal principal = getPrincipal();
        // FIXME: this is inconsistent with NuxeoPrincipal#isAdministrator
        // method because it allows hardcoded Administrator user
        if (Framework.isTestModeSet()) {
            if (SecurityConstants.ADMINISTRATOR.equals(principal.getName())) {
                return true;
            }
        }
        if (SYSTEM_USERNAME.equals(principal.getName())) {
            return true;
        }
        if (principal instanceof NuxeoPrincipal) {
            return ((NuxeoPrincipal) principal).isAdministrator();
        }
        return false;
    }

    @Override
    public void applyDefaultPermissions(String userOrGroupName)
            throws ClientException {
        if (null == userOrGroupName) {
            throw new ClientException("Null parameters received.");
        }
        if (!isAdministrator()) {
            throw new DocumentSecurityException(
                    "You need to be an Administrator to do this.");
        }
        DocumentModel rootDocument = getRootDocument();
        ACP acp = new ACPImpl();

        UserEntry userEntry = new UserEntryImpl(userOrGroupName);
        userEntry.addPrivilege(READ, true, false);

        acp.setRules(new UserEntry[] { userEntry });

        setACP(rootDocument.getRef(), acp, false);
    }

    @Override
    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException {
        return publishDocument(docToPublish, section, true);
    }

    @Override
    public DocumentModel publishDocument(DocumentModel docModel,
            DocumentModel section, boolean overwriteExistingProxy)
            throws ClientException {
        try {
            Document doc = resolveReference(docModel.getRef());
            Document sec = resolveReference(section.getRef());
            checkPermission(doc, READ);
            checkPermission(sec, ADD_CHILDREN);

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            DocumentModel proxy = null;
            Document target;
            if (docModel.isProxy() || docModel.isVersion()) {
                if (overwriteExistingProxy) {
                    // remove previous
                    List<String> removedProxyIds = removeExistingProxies(doc,
                            sec);
                    options.put(CoreEventConstants.REPLACED_PROXY_IDS,
                            (Serializable) removedProxyIds);
                }
                target = doc;
            } else {
                String checkinComment = (String) docModel.getContextData(VersioningService.CHECKIN_COMMENT);
                docModel.putContextData(VersioningService.CHECKIN_COMMENT, null);
                if (doc.isCheckedOut() || doc.getLastVersion() == null) {
                    if (!doc.isCheckedOut()) {
                        // last version was deleted while leaving a checked in
                        // doc. recreate a version
                        notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT,
                                docModel, options, null, null, true, true);
                        getVersioningService().doCheckOut(doc);
                        docModel = readModel(doc);
                        notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT,
                                docModel, options, null, null, true, false);
                    }
                    notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel,
                            options, null, null, true, true);
                    Document version = getVersioningService().doCheckIn(doc,
                            null, checkinComment);
                    docModel.refresh(DocumentModel.REFRESH_STATE
                            | DocumentModel.REFRESH_CONTENT_LAZY, null);
                    notifyCheckedInVersion(docModel,
                            new IdRef(version.getUUID()), null, checkinComment);
                }
                // NXP-12921: use base version because we could need to publish
                // a previous version (after restoring for example)
                target = doc.getBaseVersion();
                if (overwriteExistingProxy) {
                    proxy = updateExistingProxies(doc, sec, target);
                    if (proxy == null) {
                        // no or several proxies, remove them
                        List<String> removedProxyIds = removeExistingProxies(
                                doc, sec);
                        options.put(CoreEventConstants.REPLACED_PROXY_IDS,
                                (Serializable) removedProxyIds);
                    } else {
                        // notify proxy updates
                        notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_UPDATED,
                                proxy, options, null, null, true, false);
                        notifyEvent(
                                DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                                proxy, options, null, null, true, false);
                        notifyEvent(
                                DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                                section, options, null, null, true, false);
                    }
                }
            }
            if (proxy == null) {
                proxy = createProxyInternal(target, sec, options);
            }
            return proxy;
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public String getSuperParentType(DocumentModel doc) throws ClientException {
        DocumentModel superSpace = getSuperSpace(doc);
        if (superSpace == null) {
            return null;
        } else {
            return superSpace.getType();
        }
    }

    @Override
    public DocumentModel getSuperSpace(DocumentModel doc)
            throws ClientException {
        if (doc == null) {
            throw new ClientException("getSuperSpace: document is null");
        }
        if (doc.hasFacet(FacetNames.SUPER_SPACE)) {
            return doc;
        } else {

            DocumentModel parent = getDirectAccessibleParent(doc.getRef());
            if (parent == null || "/".equals(parent.getPathAsString())) {
                // return Root instead of null
                return getRootDocument();
            } else {
                return getSuperSpace(parent);
            }
        }
    }

    // walk the tree up until a accessible doc is found
    private DocumentModel getDirectAccessibleParent(DocumentRef docRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document parentDoc = doc.getParent();
            if (parentDoc == null) {
                return readModel(doc);
            }
            if (!hasPermission(parentDoc, READ)) {
                String parentPath = parentDoc.getPath();
                if ("/".equals(parentPath)) {
                    return getRootDocument();
                } else {
                    // try on parent
                    return getDirectAccessibleParent(new PathRef(
                            parentDoc.getPath()));
                }
            }
            return readModel(parentDoc);
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public <T extends Serializable> T getDocumentSystemProp(DocumentRef ref,
            String systemProperty, Class<T> type) throws ClientException,
            DocumentException {

        Document doc;
        try {
            doc = resolveReference(ref);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get document " + ref, e);
        }

        return doc.getSystemProp(systemProperty, type);
    }

    @Override
    public <T extends Serializable> void setDocumentSystemProp(DocumentRef ref,
            String systemProperty, T value) throws ClientException,
            DocumentException {
        Document doc;
        try {
            doc = resolveReference(ref);
            if (systemProperty != null
                    && systemProperty.startsWith(BINARY_TEXT_SYS_PROP)) {
                DocumentModel docModel = readModel(doc);
                Map<String, Serializable> options = new HashMap<String, Serializable>();
                options.put(systemProperty, value != null);
                notifyEvent(DocumentEventTypes.BINARYTEXT_UPDATED,
                        docModel, options, null, null, false, true);
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get document " + ref, e);
        }
        doc.setSystemProp(systemProperty, value);
    }

    @Override
    public void orderBefore(DocumentRef parent, String src, String dest)
            throws ClientException {
        try {
            if ((src == null) || (src.equals(dest))) {
                return;
            }
            Document doc = resolveReference(parent);
            doc.orderBefore(src, dest);
            Map<String, Serializable> options = new HashMap<String, Serializable>();

            // send event on container passing the reordered child as parameter
            DocumentModel docModel = readModel(doc);
            String comment = src;
            options.put(CoreEventConstants.REORDERED_CHILD, src);
            notifyEvent(DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED,
                    docModel, options, null, comment, true, false);

        } catch (DocumentException e) {
            throw new ClientException("Failed to resolve documents: " + src
                    + ", " + dest, e);
        }
    }

    @Override
    public DocumentModelRefresh refreshDocument(DocumentRef ref,
            int refreshFlags, String[] schemas) throws ClientException {
        try {
            Document doc = resolveReference(ref);
            if (doc == null) {
                throw new ClientException("No Such Document: " + ref);
            }

            // permission checks
            if ((refreshFlags & (DocumentModel.REFRESH_PREFETCH
                    | DocumentModel.REFRESH_STATE | DocumentModel.REFRESH_CONTENT)) != 0) {
                checkPermission(doc, READ);
            }
            if ((refreshFlags & DocumentModel.REFRESH_ACP) != 0) {
                checkPermission(doc, READ_SECURITY);
            }

            DocumentModelRefresh refresh = DocumentModelFactory.refreshDocumentModel(
                    doc, refreshFlags, schemas);

            // ACPs need the session, so aren't done in the factory method
            if ((refreshFlags & DocumentModel.REFRESH_ACP) != 0) {
                refresh.acp = getSession().getMergedACP(doc);
            }

            return refresh;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("Failed to get refresh data", e);
        }
    }

    @Override
    public String[] getPermissionsToCheck(String permission) {
        return getSecurityService().getPermissionsToCheck(permission);
    }

    @Override
    public <T extends DetachedAdapter> T adaptFirstMatchingDocumentWithFacet(
            DocumentRef docRef, String facet, Class<T> adapterClass)
            throws ClientException {
        Document doc = getFirstParentDocumentWithFacet(docRef, facet);
        if (doc != null) {
            DocumentModel docModel = readModel(doc);
            loadDataModelsForFacet(docModel, doc, facet);
            docModel.detach(false);
            return docModel.getAdapter(adapterClass);
        }
        return null;
    }

    protected void loadDataModelsForFacet(DocumentModel docModel, Document doc,
            String facetName) throws ClientException {
        // Load all the data related to facet's schemas
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        CompositeType facet = schemaManager.getFacet(facetName);
        if (facet == null) {
            return;
        }

        String[] facetSchemas = facet.getSchemaNames();
        for (String schema : facetSchemas) {
            try {
                DataModel dm = DocumentModelFactory.createDataModel(doc,
                        schemaManager.getSchema(schema));
                docModel.getDataModels().put(schema, dm);
            } catch (DocumentException e) {
                throw new ClientException(e);
            }
        }
    }

    /**
     * Returns the first {@code Document} with the given {@code facet},
     * recursively going up the parent hierarchy. Returns {@code null} if there
     * is no more parent.
     * <p>
     * This method does not check security rights.
     */
    protected Document getFirstParentDocumentWithFacet(DocumentRef docRef,
            String facet) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            while (doc != null && !doc.hasFacet(facet)) {
                doc = doc.getParent();
            }
            return doc;
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    @Override
    public Map<String, String> getBinaryFulltext(DocumentRef ref) throws ClientException {
        try {
            Document doc = resolveReference(ref);
            checkPermission(doc, READ);
            return getSession().getBinaryFulltext(ref.toString());
        } catch (DocumentException e) {
            throw new ClientException("Failed to get fulltext info  on " + ref, e);
        }
    }

}
