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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.api;

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.CHANGED_ACL_NAME;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.NEW_ACE;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.OLD_ACE;
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

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationReport;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.PathComparator;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
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
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Abstract implementation of the client interface.
 * <p>
 * This handles all the aspects that are independent on the final implementation (like running inside a J2EE platform or
 * not).
 * <p>
 * The only aspect not implemented is the session management that should be handled by subclasses.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public abstract class AbstractSession implements CoreSession, Serializable {

    public static final NuxeoPrincipal ANONYMOUS = new UserPrincipal("anonymous", new ArrayList<>(), true, false);

    private static final Log log = LogFactory.getLog(CoreSession.class);

    private static final long serialVersionUID = 1L;

    private static final Comparator<? super Document> pathComparator = new PathComparator();

    public static final String DEFAULT_MAX_RESULTS = "1000";

    public static final String MAX_RESULTS_PROPERTY = "org.nuxeo.ecm.core.max.results";

    public static final String LIMIT_RESULTS_PROPERTY = "org.nuxeo.ecm.core.limit.results";

    public static final String TRASH_KEEP_CHECKED_IN_PROPERTY = "org.nuxeo.trash.keepCheckedIn";

    public static final String BINARY_TEXT_SYS_PROP = "fulltextBinary";

    private Boolean limitedResults;

    private Long maxResults;

    // @since 5.7.2
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected Counter createDocumentCount;

    protected Counter deleteDocumentCount;

    protected Counter updateDocumentCount;

    protected void createMetrics() {
        createDocumentCount = registry.counter(MetricRegistry.name("nuxeo.repositories", getRepositoryName(),
                "documents", "create"));
        deleteDocumentCount = registry.counter(MetricRegistry.name("nuxeo.repositories", getRepositoryName(),
                "documents", "delete"));
        updateDocumentCount = registry.counter(MetricRegistry.name("nuxeo.repositories", getRepositoryName(),
                "documents", "update"));
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
            versioningService = Framework.getService(VersioningService.class);
        }
        return versioningService;
    }

    private transient DocumentValidationService validationService;

    protected DocumentValidationService getValidationService() {
        if (validationService == null) {
            validationService = Framework.getService(DocumentValidationService.class);
        }
        return validationService;
    }

    /**
     * Internal method: Gets the current session based on the client session id.
     *
     * @return the repository session
     */
    public abstract Session getSession();

    @Override
    public DocumentType getDocumentType(String type) {
        return Framework.getLocalService(SchemaManager.class).getDocumentType(type);
    }

    protected final void checkPermission(Document doc, String permission) throws DocumentSecurityException {
        if (isAdministrator()) {
            return;
        }
        if (!hasPermission(doc, permission)) {
            log.debug("Permission '" + permission + "' is not granted to '" + getPrincipal().getName()
                    + "' on document " + doc.getPath() + " (" + doc.getUUID() + " - " + doc.getType().getName() + ")");
            throw new DocumentSecurityException("Privilege '" + permission + "' is not granted to '"
                    + getPrincipal().getName() + "'");
        }
    }

    protected Map<String, Serializable> getContextMapEventInfo(DocumentModel doc) {
        Map<String, Serializable> options = new HashMap<>();
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
        DocumentEventContext ctx = new DocumentEventContext(this, getPrincipal(), source);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, getSessionId());
        return ctx;
    }

    protected void notifyEvent(String eventId, DocumentModel source, Map<String, Serializable> options,
            String category, String comment, boolean withLifeCycle, boolean inline) {

        DocumentEventContext ctx = new DocumentEventContext(this, getPrincipal(), source);

        // compatibility with old code (< 5.2.M4) - import info from old event
        // model
        if (options != null) {
            ctx.setProperties(options);
        }
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, getSessionId());
        // Document life cycle
        if (source != null && withLifeCycle) {
            String currentLifeCycleState = source.getCurrentLifeCycleState();
            ctx.setProperty(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        }
        if (comment != null) {
            ctx.setProperty("comment", comment);
        }
        ctx.setProperty("category", category == null ? DocumentEventCategories.EVENT_DOCUMENT_CATEGORY : category);
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
            if (blockJms != null && blockJms.booleanValue()) {
                event.setLocal(true);
                event.setInline(true);
            }
        }
        Framework.getLocalService(EventService.class).fireEvent(event);
    }

    /**
     * Copied from obsolete VersionChangeNotifier.
     * <p>
     * Sends change notifications to core event listeners. The event contains info with older document (before version
     * change) and newer doc (current document).
     *
     * @param options additional info to pass to the event
     */
    protected void notifyVersionChange(DocumentModel oldDocument, DocumentModel newDocument,
            Map<String, Serializable> options) {
        final Map<String, Serializable> info = new HashMap<>();
        if (options != null) {
            info.putAll(options);
        }
        info.put(VersioningChangeNotifier.EVT_INFO_NEW_DOC_KEY, newDocument);
        info.put(VersioningChangeNotifier.EVT_INFO_OLD_DOC_KEY, oldDocument);
        notifyEvent(VersioningChangeNotifier.CORE_EVENT_ID_VERSIONING_CHANGE, newDocument, info,
                DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY, null, false, false);
    }

    @Override
    public boolean hasPermission(Principal principal, DocumentRef docRef, String permission) {
        Document doc = resolveReference(docRef);
        return hasPermission(principal, doc, permission);
    }

    protected final boolean hasPermission(Principal principal, Document doc, String permission) {
        return getSecurityService().checkPermission(doc, principal, permission);
    }

    @Override
    public boolean hasPermission(DocumentRef docRef, String permission) {
        Document doc = resolveReference(docRef);
        return hasPermission(doc, permission);
    }

    protected final boolean hasPermission(Document doc, String permission) {
        // TODO: optimize this - usually ACP is already available when calling
        // this method.
        // -> cache ACP at securitymanager level or try to reuse the ACP when
        // it is known
        return getSecurityService().checkPermission(doc, getPrincipal(), permission);
        // return doc.getSession().getSecurityManager().checkPermission(doc,
        // getPrincipal().getName(), permission);
    }

    protected Document resolveReference(DocumentRef docRef) {
        if (docRef == null) {
            throw new IllegalArgumentException("null docRref");
        }
        Object ref = docRef.reference();
        if (ref == null) {
            throw new IllegalArgumentException("null reference");
        }
        int type = docRef.type();
        switch (type) {
        case DocumentRef.ID:
            return getSession().getDocumentByUUID((String) ref);
        case DocumentRef.PATH:
            return getSession().resolvePath((String) ref);
        case DocumentRef.INSTANCE:
            return getSession().getDocumentByUUID(((DocumentModel) ref).getId());
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
    protected DocumentModel readModel(Document doc) {
        return DocumentModelFactory.createDocumentModel(doc, getSessionId(), null);
    }

    /**
     * Gets the document model for the given core document, preserving the contextData.
     *
     * @param doc the document
     * @return the document model
     */
    protected DocumentModel readModel(Document doc, DocumentModel docModel) {
        DocumentModel newModel = readModel(doc);
        newModel.copyContextData(docModel);
        return newModel;
    }

    protected DocumentModel writeModel(Document doc, DocumentModel docModel) {
        return DocumentModelFactory.writeDocumentModel(docModel, doc);
    }

    @Override
    @Deprecated
    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name, boolean resetLifeCycle) {
        if (resetLifeCycle) {
            return copy(src, dst, name, CopyOption.RESET_LIFE_CYCLE);
        }
        return copy(src, dst, name);
    }

    @Override
    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name, CopyOption... copyOptions) {
        Document dstDoc = resolveReference(dst);
        checkPermission(dstDoc, ADD_CHILDREN);

        Document srcDoc = resolveReference(src);
        if (name == null) {
            name = srcDoc.getName();
        } else {
            PathRef.checkName(name);
        }

        Map<String, Serializable> options = new HashMap<>();

        // add the destination name, destination, resetLifeCycle flag and
        // source references in
        // the options of the event
        options.put(CoreEventConstants.SOURCE_REF, src);
        options.put(CoreEventConstants.DESTINATION_REF, dst);
        options.put(CoreEventConstants.DESTINATION_PATH, dstDoc.getPath());
        options.put(CoreEventConstants.DESTINATION_NAME, name);
        options.put(CoreEventConstants.DESTINATION_EXISTS, dstDoc.hasChild(name));
        options.put(CoreEventConstants.RESET_LIFECYCLE, CopyOption.isResetLifeCycle(copyOptions));
        options.put(CoreEventConstants.RESET_CREATOR, CopyOption.isResetCreator(copyOptions));
        DocumentModel srcDocModel = readModel(srcDoc);
        notifyEvent(DocumentEventTypes.ABOUT_TO_COPY, srcDocModel, options, null, null, true, true);

        name = (String) options.get(CoreEventConstants.DESTINATION_NAME);
        Document doc = getSession().copy(srcDoc, dstDoc, name);
        // no need to clear lock, locks table is not copied

        // notify document created by copy
        DocumentModel docModel = readModel(doc);

        String comment = srcDoc.getRepositoryName() + ':' + src.toString();
        notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel, options, null, comment, true, false);
        docModel = writeModel(doc, docModel);

        // notify document copied
        comment = doc.getRepositoryName() + ':' + docModel.getRef().toString();

        notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel, options, null, comment, true, false);

        return docModel;
    }

    @Override
    @Deprecated
    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst, boolean resetLifeCycle) {
        if (resetLifeCycle) {
            return copy(src, dst, CopyOption.RESET_LIFE_CYCLE);
        }
        return copy(src, dst);
    }

    @Override
    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst, CopyOption... opts) {
        return src.stream().map(ref -> copy(ref, dst, null, opts)).collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst, String name, boolean resetLifeCycle) {
        if (resetLifeCycle) {
            return copyProxyAsDocument(src, dst, name, CopyOption.RESET_LIFE_CYCLE);
        }
        return copyProxyAsDocument(src, dst, name);
    }

    @Override
    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst, String name, CopyOption... copyOptions) {
        Document srcDoc = resolveReference(src);
        if (!srcDoc.isProxy()) {
            return copy(src, dst, name);
        }
        Document dstDoc = resolveReference(dst);
        checkPermission(dstDoc, WRITE);

        // create a new document using the expanded proxy
        DocumentModel srcDocModel = readModel(srcDoc);
        String docName = (name != null) ? name : srcDocModel.getName();
        DocumentModel docModel = createDocumentModel(dstDoc.getPath(), docName, srcDocModel.getType());
        docModel.copyContent(srcDocModel);
        notifyEvent(DocumentEventTypes.ABOUT_TO_COPY, srcDocModel, null, null, null, true, true);
        docModel = createDocument(docModel);
        Document doc = resolveReference(docModel.getRef());

        Map<String, Serializable> options = new HashMap<>();
        options.put(CoreEventConstants.RESET_LIFECYCLE, CopyOption.isResetLifeCycle(copyOptions));
        options.put(CoreEventConstants.RESET_CREATOR, CopyOption.isResetCreator(copyOptions));
        // notify document created by copy
        String comment = srcDoc.getRepositoryName() + ':' + src.toString();
        notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel, options, null, comment, true, false);

        // notify document copied
        comment = doc.getRepositoryName() + ':' + docModel.getRef().toString();
        notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel, options, null, comment, true, false);

        return docModel;
    }

    @Override
    @Deprecated
    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src, DocumentRef dst, boolean resetLifeCycle) {
        if (resetLifeCycle) {
            return copyProxyAsDocument(src, dst, CopyOption.RESET_LIFE_CYCLE);
        }
        return copyProxyAsDocument(src, dst);
    }

    @Override
    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src, DocumentRef dst, CopyOption... opts) {
        return src.stream().map(ref -> copyProxyAsDocument(ref, dst, null, opts)).collect(Collectors.toList());
    }

    @Override
    public DocumentModel move(DocumentRef src, DocumentRef dst, String name) {
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
        String originalName = srcDocModel.getName();
        if (name == null) {
            name = srcDocModel.getName();
        } else {
            PathRef.checkName(name);
        }
        Map<String, Serializable> options = getContextMapEventInfo(srcDocModel);
        // add the destination name, destination and source references in
        // the options of the event
        options.put(CoreEventConstants.SOURCE_REF, src);
        options.put(CoreEventConstants.DESTINATION_REF, dst);
        options.put(CoreEventConstants.DESTINATION_PATH, dstDoc.getPath());
        options.put(CoreEventConstants.DESTINATION_NAME, name);
        options.put(CoreEventConstants.DESTINATION_EXISTS, dstDoc.hasChild(name));

        notifyEvent(DocumentEventTypes.ABOUT_TO_MOVE, srcDocModel, options, null, null, true, true);

        name = (String) options.get(CoreEventConstants.DESTINATION_NAME);

        if (!originalName.equals(name)) {
            options.put(CoreEventConstants.ORIGINAL_NAME, originalName);
        }

        String comment = srcDoc.getRepositoryName() + ':' + srcDoc.getParent().getUUID();

        Document doc = getSession().move(srcDoc, dstDoc, name);

        // notify document moved
        DocumentModel docModel = readModel(doc);
        options.put(CoreEventConstants.PARENT_PATH, srcDocModel.getParentRef());
        notifyEvent(DocumentEventTypes.DOCUMENT_MOVED, docModel, options, null, comment, true, false);

        return docModel;
    }

    @Override
    public void move(List<DocumentRef> src, DocumentRef dst) {
        for (DocumentRef ref : src) {
            move(ref, dst, null);
        }
    }

    @Override
    public ACP getACP(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_SECURITY);
        return getSession().getMergedACP(doc);
    }

    @Override
    public void setACP(DocumentRef docRef, ACP newAcp, boolean overwrite) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, WRITE_SECURITY);

        setACP(doc, newAcp, overwrite, null);
    }

    protected void setACP(Document doc, ACP newAcp, boolean overwrite, Map<String, Serializable> options) {
        DocumentModel docModel = readModel(doc);
        if (options == null) {
            options = new HashMap<>();
        }
        options.put(CoreEventConstants.OLD_ACP, docModel.getACP().clone());
        options.put(CoreEventConstants.NEW_ACP, newAcp);

        notifyEvent(DocumentEventTypes.BEFORE_DOC_SECU_UPDATE, docModel, options, null, null, true, true);
        getSession().setACP(doc, newAcp, overwrite);
        docModel = readModel(doc);
        options.put(CoreEventConstants.NEW_ACP, newAcp.clone());
        notifyEvent(DocumentEventTypes.DOCUMENT_SECURITY_UPDATED, docModel, options, null, null, true, false);
    }

    @Override
    public void replaceACE(DocumentRef docRef, String aclName, ACE oldACE, ACE newACE) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, WRITE_SECURITY);

        ACP acp = getACP(docRef);
        if (acp.replaceACE(aclName, oldACE, newACE)) {
            Map<String, Serializable> options = new HashMap<>();
            options.put(OLD_ACE, oldACE);
            options.put(NEW_ACE, newACE);
            options.put(CHANGED_ACL_NAME, aclName);
            setACP(doc, acp, true, options);
        }
    }

    @Override
    public boolean isNegativeAclAllowed() {
        return getSession().isNegativeAclAllowed();
    }

    @Override
    public void cancel() {
        // nothing
    }

    private DocumentModel createDocumentModelFromTypeName(String typeName, Map<String, Serializable> options) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        DocumentType docType = schemaManager.getDocumentType(typeName);
        if (docType == null) {
            throw new IllegalArgumentException(typeName + " is not a registered core type");
        }
        DocumentModel docModel = DocumentModelFactory.createDocumentModel(getSessionId(), docType);
        if (options == null) {
            options = new HashMap<>();
        }
        // do not forward this event on the JMS Bus
        options.put("BLOCK_JMS_PRODUCING", true);
        notifyEvent(DocumentEventTypes.EMPTY_DOCUMENTMODEL_CREATED, docModel, options, null, null, false, true);
        return docModel;
    }

    @Override
    public DocumentModel createDocumentModel(String typeName) {
        Map<String, Serializable> options = new HashMap<>();
        return createDocumentModelFromTypeName(typeName, options);
    }

    @Override
    public DocumentModel createDocumentModel(String parentPath, String name, String typeName) {
        Map<String, Serializable> options = new HashMap<>();
        options.put(CoreEventConstants.PARENT_PATH, parentPath);
        options.put(CoreEventConstants.DOCUMENT_MODEL_ID, name);
        options.put(CoreEventConstants.DESTINATION_NAME, name);
        DocumentModel model = createDocumentModelFromTypeName(typeName, options);
        model.setPathInfo(parentPath, name);
        return model;
    }

    @Override
    public DocumentModel createDocumentModel(String typeName, Map<String, Object> options) {

        Map<String, Serializable> serializableOptions = new HashMap<>();

        for (Entry<String, Object> entry : options.entrySet()) {
            serializableOptions.put(entry.getKey(), (Serializable) entry.getValue());
        }
        return createDocumentModelFromTypeName(typeName, serializableOptions);
    }

    @Override
    public DocumentModel createDocument(DocumentModel docModel) {
        if (docModel.getSessionId() == null) {
            // docModel was created using constructor instead of CoreSession.createDocumentModel
            docModel.attach(getSessionId());
        }
        String typeName = docModel.getType();
        DocumentRef parentRef = docModel.getParentRef();
        if (typeName == null) {
            throw new NullPointerException("null typeName");
        }
        if (parentRef == null && !isAdministrator()) {
            throw new NuxeoException("Only Administrators can create placeless documents");
        }
        String childName = docModel.getName();
        Map<String, Serializable> options = getContextMapEventInfo(docModel);

        // document validation
        if (getValidationService().isActivated(DocumentValidationService.CTX_CREATEDOC, options)) {
            DocumentValidationReport report = getValidationService().validate(docModel, true);
            if (report.hasError()) {
                throw new DocumentValidationException(report);
            }
        }

        Document folder = fillCreateOptions(parentRef, childName, options);

        // get initial life cycle state info
        String initialLifecycleState = null;
        Object lifecycleStateInfo = docModel.getContextData(LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME);
        if (lifecycleStateInfo instanceof String) {
            initialLifecycleState = (String) lifecycleStateInfo;
        }
        notifyEvent(DocumentEventTypes.ABOUT_TO_CREATE, docModel, options, null, null, false, true); // no lifecycle
                                                                                                     // yet
        childName = (String) options.get(CoreEventConstants.DESTINATION_NAME);
        Document doc = folder.addChild(childName, typeName);

        // update facets too since some of them may be dynamic
        for (String facetName : docModel.getFacets()) {
            if (!doc.getAllFacets().contains(facetName) && !FacetNames.IMMUTABLE.equals(facetName)) {
                doc.addFacet(facetName);
            }
        }

        // init document life cycle
        NXCore.getLifeCycleService().initialize(doc, initialLifecycleState);

        // init document with data from doc model
        docModel = writeModel(doc, docModel);

        if (!Boolean.TRUE.equals(docModel.getContextData(ScopeType.REQUEST, VersioningService.SKIP_VERSIONING))) {
            // during remote publishing we want to skip versioning
            // to avoid overwriting the version number
            getVersioningService().doPostCreate(doc, options);
            docModel = readModel(doc, docModel);
        }

        notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, docModel, options, null, null, true, false);
        docModel = writeModel(doc, docModel);

        createDocumentCount.inc();
        return docModel;
    }

    protected Document fillCreateOptions(DocumentRef parentRef, String childName, Map<String, Serializable> options)
            throws DocumentSecurityException {
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
            options.put(CoreEventConstants.DESTINATION_EXISTS, folder.hasChild(childName));
        }
        return folder;
    }

    @Override
    public void importDocuments(List<DocumentModel> docModels) {
        docModels.forEach(this::importDocument);
    }

    protected static final PathRef EMPTY_PATH = new PathRef("");

    protected void importDocument(DocumentModel docModel) {
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

        // document validation
        if (getValidationService().isActivated(DocumentValidationService.CTX_IMPORTDOC, props)) {
            DocumentValidationReport report = getValidationService().validate(docModel, true);
            if (report.hasError()) {
                throw new DocumentValidationException(report);
            }
        }

        if (parentRef != null && EMPTY_PATH.equals(parentRef)) {
            parentRef = null;
        }
        Document parent = fillCreateOptions(parentRef, name, props);
        notifyEvent(DocumentEventTypes.ABOUT_TO_IMPORT, docModel, props, null, null, false, true);
        name = (String) props.get(CoreEventConstants.DESTINATION_NAME);

        // create the document
        Document doc = getSession().importDocument(id, parentRef == null ? null : parent, name, typeName, props);

        if (typeName.equals(CoreSession.IMPORT_PROXY_TYPE)) {
            // just reread the final document
            docModel = readModel(doc);
        } else {
            // init document with data from doc model
            docModel = writeModel(doc, docModel);
        }

        // send an event about the import
        notifyEvent(DocumentEventTypes.DOCUMENT_IMPORTED, docModel, null, null, null, true, false);
    }

    @Override
    public DocumentModel[] createDocument(DocumentModel[] docModels) {
        DocumentModel[] models = new DocumentModel[docModels.length];
        int i = 0;
        // TODO: optimize this (do not call at each iteration createDocument())
        for (DocumentModel docModel : docModels) {
            models[i++] = createDocument(docModel);
        }
        return models;
    }

    @Override
    public boolean exists(DocumentRef docRef) {
        try {
            Document doc = resolveReference(docRef);
            return hasPermission(doc, BROWSE);
        } catch (DocumentNotFoundException e) {
            return false;
        }
    }

    @Override
    public DocumentModel getChild(DocumentRef parent, String name) {
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        Document child = doc.getChild(name);
        checkPermission(child, READ);
        return readModel(child);
    }

    @Override
    public boolean hasChild(DocumentRef parent, String name) {
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        return doc.hasChild(name);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent) {
        return getChildren(parent, null, READ, null, null);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type) {
        return getChildren(parent, type, READ, null, null);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type, String perm) {
        return getChildren(parent, type, perm, null, null);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type, Filter filter, Sorter sorter) {
        return getChildren(parent, type, null, filter, sorter);
    }

    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type, String perm, Filter filter, Sorter sorter) {
        if (perm == null) {
            perm = READ;
        }
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        DocumentModelList docs = new DocumentModelListImpl();
        for (Document child : doc.getChildren()) {
            if (hasPermission(child, perm)) {
                if (child.getType() != null && (type == null || type.equals(child.getType().getName()))) {
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
    }

    @Override
    public List<DocumentRef> getChildrenRefs(DocumentRef parentRef, String perm) {
        if (perm != null) {
            // XXX TODO
            throw new NullPointerException("perm != null not implemented");
        }
        Document parent = resolveReference(parentRef);
        checkPermission(parent, READ_CHILDREN);
        List<String> ids = parent.getChildrenIds();
        List<DocumentRef> refs = new ArrayList<>(ids.size());
        for (String id : ids) {
            refs.add(new IdRef(id));
        }
        return refs;
    }

    @Override
    public DocumentModelIterator getChildrenIterator(DocumentRef parent) {
        return getChildrenIterator(parent, null, null, null);
    }

    @Override
    public DocumentModelIterator getChildrenIterator(DocumentRef parent, String type) {
        return getChildrenIterator(parent, type, null, null);
    }

    @Override
    public DocumentModelIterator getChildrenIterator(DocumentRef parent, String type, String perm, Filter filter) {
        // perm unused, kept for API compat
        return new DocumentModelChildrenIterator(this, parent, type, filter);
    }

    @Override
    public DocumentModel getDocument(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ);
        return readModel(doc);
    }

    @Override
    public DocumentModelList getDocuments(DocumentRef[] docRefs) {
        List<DocumentModel> docs = new ArrayList<>(docRefs.length);
        for (DocumentRef docRef : docRefs) {
            Document doc;
            try {
                doc = resolveReference(docRef);
                checkPermission(doc, READ);
            } catch (DocumentSecurityException e) {
                // no permission
                continue;
            }
            docs.add(readModel(doc));
        }
        return new DocumentModelListImpl(docs);
    }

    @Override
    public DocumentModelList getFiles(DocumentRef parent) {
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        DocumentModelList docs = new DocumentModelListImpl();
        for (Document child : doc.getChildren()) {
            if (!child.isFolder() && hasPermission(child, READ)) {
                docs.add(readModel(child));
            }
        }
        return docs;
    }

    @Override
    public DocumentModelList getFiles(DocumentRef parent, Filter filter, Sorter sorter) {
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        DocumentModelList docs = new DocumentModelListImpl();
        for (Document child : doc.getChildren()) {
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
    }

    @Override
    public DocumentModelList getFolders(DocumentRef parent) {
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        DocumentModelList docs = new DocumentModelListImpl();
        for (Document child : doc.getChildren()) {
            if (child.isFolder() && hasPermission(child, READ)) {
                docs.add(readModel(child));
            }
        }
        return docs;
    }

    @Override
    public DocumentModelList getFolders(DocumentRef parent, Filter filter, Sorter sorter) {
        Document doc = resolveReference(parent);
        checkPermission(doc, READ_CHILDREN);
        DocumentModelList docs = new DocumentModelListImpl();
        for (Document child : doc.getChildren()) {
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
    }

    @Override
    public DocumentRef getParentDocumentRef(DocumentRef docRef) {
        final Document doc = resolveReference(docRef);
        Document parentDoc = doc.getParent();
        return parentDoc != null ? new IdRef(parentDoc.getUUID()) : null;
    }

    @Override
    public DocumentModel getParentDocument(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        Document parentDoc = doc.getParent();
        if (parentDoc == null) {
            return null;
        }
        if (!hasPermission(parentDoc, READ)) {
            throw new DocumentSecurityException("Privilege READ is not granted to " + getPrincipal().getName());
        }
        return readModel(parentDoc);
    }

    @Override
    public List<DocumentModel> getParentDocuments(final DocumentRef docRef) {

        if (null == docRef) {
            throw new IllegalArgumentException("null docRef");
        }

        final List<DocumentModel> docsList = new ArrayList<>();
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
        Collections.reverse(docsList);
        return docsList;
    }

    @Override
    public DocumentModel getRootDocument() {
        return readModel(getSession().getRootDocument());
    }

    @Override
    public boolean hasChildren(DocumentRef docRef) {
        // TODO: validate permission check with td
        Document doc = resolveReference(docRef);
        checkPermission(doc, BROWSE);
        return doc.hasChildren();
    }

    @Override
    public DocumentModelList query(String query) {
        return query(query, null, 0, 0, false);
    }

    @Override
    public DocumentModelList query(String query, int max) {
        return query(query, null, max, 0, false);
    }

    @Override
    public DocumentModelList query(String query, Filter filter) {
        return query(query, filter, 0, 0, false);
    }

    @Override
    public DocumentModelList query(String query, Filter filter, int max) {
        return query(query, filter, max, 0, false);
    }

    @Override
    public DocumentModelList query(String query, Filter filter, long limit, long offset, boolean countTotal) {
        return query(query, NXQL.NXQL, filter, limit, offset, countTotal);
    }

    @Override
    public DocumentModelList query(String query, String queryType, Filter filter, long limit, long offset,
            boolean countTotal) {
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
            maxResults = Long.parseLong(Framework.getProperty(MAX_RESULTS_PROPERTY, DEFAULT_MAX_RESULTS));
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
    public DocumentModelList query(String query, Filter filter, long limit, long offset, long countUpTo) {
        return query(query, NXQL.NXQL, filter, limit, offset, countUpTo);
    }

    @Override
    public DocumentModelList query(String query, String queryType, Filter filter, long limit, long offset,
            long countUpTo) {
        SecurityService securityService = getSecurityService();
        Principal principal = getPrincipal();
        try {
            String permission = BROWSE;
            String repoName = getRepositoryName();
            boolean postFilterPolicies = !securityService.arePoliciesExpressibleInQuery(repoName);
            boolean postFilterFilter = filter != null && !(filter instanceof FacetFilter);
            boolean postFilter = postFilterPolicies || postFilterFilter;
            String[] principals;
            if (isAdministrator()) {
                principals = null; // means: no security check needed
            } else {
                principals = SecurityService.getPrincipalsToCheck(principal);
            }
            String[] permissions = securityService.getPermissionsToCheck(permission);
            QueryFilter queryFilter = new QueryFilter(principal, principals, permissions,
                    filter instanceof FacetFilter ? (FacetFilter) filter : null,
                    securityService.getPoliciesQueryTransformers(repoName), postFilter ? 0 : limit, postFilter ? 0
                            : offset);

            // get document list with total size
            PartialList<Document> pl = getSession().query(query, queryType, queryFilter, postFilter ? -1 : countUpTo);
            // convert to DocumentModelList
            DocumentModelListImpl dms = new DocumentModelListImpl(pl.list.size());
            dms.setTotalSize(pl.totalSize);
            for (Document doc : pl.list) {
                dms.add(readModel(doc));
            }

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
                if (postFilterPolicies) {
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
        } catch (QueryParseException e) {
            e.addInfo("Failed to execute query: " + query);
            throw e;
        }
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, Object... params) {
        return queryAndFetch(query, queryType, false, params);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, boolean distinctDocuments,
            Object... params) {
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
            QueryFilter queryFilter = new QueryFilter(principal, principals, permissions, null, transformers, 0, 0);
            IterableQueryResult result = getSession().queryAndFetch(query, queryType, queryFilter, distinctDocuments,
                    params);
            return result;
        } catch (QueryParseException e) {
            e.addInfo("Failed to execute query: " + queryType + ": " + query);
            throw e;
        }
    }

    @Override
    public ScrollResult scroll(String query, int batchSize, int keepAliveSeconds) {
        if (!isAdministrator()) {
            throw new NuxeoException("Only Administrators can scroll");
        }
        return getSession().scroll(query, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        if (!isAdministrator()) {
            throw new NuxeoException("Only Administrators can scroll");
        }
        return getSession().scroll(scrollId);
    }

    @Override
    public void removeChildren(DocumentRef docRef) {
        // TODO: check req permissions with td
        Document doc = resolveReference(docRef);
        checkPermission(doc, REMOVE_CHILDREN);
        List<Document> children = doc.getChildren();
        // remove proxies first, otherwise they could become dangling
        for (Document child : children) {
            if (child.isProxy()) {
                if (hasPermission(child, REMOVE)) {
                    removeNotifyOneDoc(child);
                }
            }
        }
        // then remove regular docs or versions, both of which could be proxies targets
        for (Document child : children) {
            if (!child.isProxy()) {
                if (hasPermission(child, REMOVE)) {
                    removeNotifyOneDoc(child);
                }
            }
        }
    }

    @Override
    public boolean canRemoveDocument(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        return canRemoveDocument(doc) == null;
    }

    /**
     * Checks if a document can be removed, and returns a failure reason if not.
     */
    protected String canRemoveDocument(Document doc) {
        // TODO must also check for proxies on live docs
        if (doc.isVersion()) {
            // TODO a hasProxies method would be more efficient
            Collection<Document> proxies = getSession().getProxies(doc, null);
            if (!proxies.isEmpty()) {
                return "Proxy " + proxies.iterator().next().getUUID() + " targets version " + doc.getUUID();
            }
            // find a working document to check security
            Document working = doc.getSourceDocument();
            if (working != null) {
                Document baseVersion = working.getBaseVersion();
                if (baseVersion != null && !baseVersion.isCheckedOut() && baseVersion.getUUID().equals(doc.getUUID())) {
                    return "Working copy " + working.getUUID() + " is checked in with base version " + doc.getUUID();
                }
                return hasPermission(working, WRITE_VERSION) ? null : "Missing permission '" + WRITE_VERSION
                        + "' on working copy " + working.getUUID();
            } else {
                // no working document, only admins can remove
                return isAdministrator() ? null : "No working copy and not an Administrator";
            }
        } else {
            if (isAdministrator()) {
                return null; // ok
            }
            if (!hasPermission(doc, REMOVE)) {
                return "Missing permission '" + REMOVE + "' on document " + doc.getUUID();
            }
            Document parent = doc.getParent();
            if (parent == null) {
                return null; // ok
            }
            return hasPermission(parent, REMOVE_CHILDREN) ? null : "Missing permission '" + REMOVE_CHILDREN
                    + "' on parent document " + parent.getUUID();
        }
    }

    @Override
    public void removeDocument(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        removeDocument(doc);
    }

    protected void removeDocument(Document doc) {
        try {
            String reason = canRemoveDocument(doc);
            if (reason != null) {
                throw new DocumentSecurityException("Permission denied: cannot remove document " + doc.getUUID() + ", "
                        + reason);
            }
            removeNotifyOneDoc(doc);

        } catch (ConcurrentUpdateException e) {
            e.addInfo("Failed to remove document " + doc.getUUID());
            throw e;
        }
        deleteDocumentCount.inc();
    }

    protected void removeNotifyOneDoc(Document doc) {
        // XXX notify with options if needed
        DocumentModel docModel = readModel(doc);
        Map<String, Serializable> options = new HashMap<>();
        if (docModel != null) {
            options.put("docTitle", docModel.getTitle());
        }
        String versionLabel = "";
        Document sourceDoc = null;
        // notify different events depending on wether the document is a
        // version or not
        if (!doc.isVersion()) {
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE, docModel, options, null, null, true, true);
            CoreService coreService = Framework.getLocalService(CoreService.class);
            coreService.getVersionRemovalPolicy().removeVersions(getSession(), doc, this);
        } else {
            versionLabel = docModel.getVersionLabel();
            sourceDoc = doc.getSourceDocument();
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE_VERSION, docModel, options, null, null, true, true);

        }
        doc.remove();
        if (doc.isVersion()) {
            if (sourceDoc != null) {
                DocumentModel sourceDocModel = readModel(sourceDoc);
                if (sourceDocModel != null) {
                    options.put("comment", versionLabel); // to be used by
                                                          // audit
                    // service
                    notifyEvent(DocumentEventTypes.VERSION_REMOVED, sourceDocModel, options, null, null, false, false);
                    options.remove("comment");
                }
                options.put("docSource", sourceDoc.getUUID());
            }
        }
        notifyEvent(DocumentEventTypes.DOCUMENT_REMOVED, docModel, options, null, null, false, false);
    }

    /**
     * Implementation uses the fact that the lexicographic ordering of paths is a refinement of the "contains" partial
     * ordering.
     */
    @Override
    public void removeDocuments(DocumentRef[] docRefs) {
        Document[] docs = new Document[docRefs.length];

        for (int i = 0; i < docs.length; i++) {
            docs[i] = resolveReference(docRefs[i]);
        }
        // TODO OPTIM: it's not guaranteed that getPath is cheap and
        // we call it a lot. Should use an object for pairs (document, path)
        // to call it just once per doc.
        Arrays.sort(docs, pathComparator); // nulls first
        String[] paths = new String[docs.length];
        for (int i = 0; i < docs.length; i++) {
            paths[i] = docs[i].getPath();
        }
        String lastRemovedWithSlash = "\u0000";
        for (int i = 0; i < docs.length; i++) {
            String path = paths[i];
            if (i == 0 || path == null || !path.startsWith(lastRemovedWithSlash)) {
                removeDocument(docs[i]);
                if (path != null) {
                    lastRemovedWithSlash = path + "/";
                }
            }
        }
    }

    @Override
    public void save() {
        try {
            final Map<String, Serializable> options = new HashMap<>();
            getSession().save();
            notifyEvent(DocumentEventTypes.SESSION_SAVED, null, options, null, null, true, false);
        } catch (ConcurrentUpdateException e) {
            e.addInfo("Failed to save session");
            throw e;
        }
    }

    @Override
    public DocumentModel saveDocument(DocumentModel docModel) {
        if (docModel.getRef() == null) {
            throw new IllegalArgumentException(String.format("cannot save document '%s' with null reference: "
                    + "document has probably not yet been created " + "in the repository with "
                    + "'CoreSession.createDocument(docModel)'", docModel.getTitle()));
        }
        Document doc = resolveReference(docModel.getRef());
        checkPermission(doc, WRITE_PROPERTIES);

        Map<String, Serializable> options = getContextMapEventInfo(docModel);

        boolean dirty = docModel.isDirty();

        // document validation
        if (dirty && getValidationService().isActivated(DocumentValidationService.CTX_SAVEDOC, options)) {
            DocumentValidationReport report = getValidationService().validate(docModel, true);
            if (report.hasError()) {
                throw new DocumentValidationException(report);
            }
        }

        options.put(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL, readModel(doc));
        // regular event, last chance to modify docModel
        options.put(CoreEventConstants.DESTINATION_NAME, docModel.getName());
        options.put(CoreEventConstants.DOCUMENT_DIRTY, dirty);
        notifyEvent(DocumentEventTypes.BEFORE_DOC_UPDATE, docModel, options, null, null, true, true);
        String name = (String) options.get(CoreEventConstants.DESTINATION_NAME);
        // did the event change the name? not applicable to Root whose
        // name is null/empty
        if (name != null && !name.equals(docModel.getName())) {
            doc = getSession().move(doc, doc.getParent(), name);
        }

        // recompute the dirty state
        dirty = docModel.isDirty();
        options.put(CoreEventConstants.DOCUMENT_DIRTY, dirty);

        VersioningOption versioningOption = (VersioningOption) docModel.getContextData(VersioningService.VERSIONING_OPTION);
        docModel.putContextData(VersioningService.VERSIONING_OPTION, null);
        String checkinComment = (String) docModel.getContextData(VersioningService.CHECKIN_COMMENT);
        docModel.putContextData(VersioningService.CHECKIN_COMMENT, null);
        Boolean disableAutoCheckOut = (Boolean) docModel.getContextData(VersioningService.DISABLE_AUTO_CHECKOUT);
        docModel.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, null);
        options.put(VersioningService.DISABLE_AUTO_CHECKOUT, disableAutoCheckOut);

        if (!docModel.isImmutable()) {
            // pre-save versioning
            boolean checkout = getVersioningService().isPreSaveDoingCheckOut(doc, dirty, versioningOption, options);
            if (checkout) {
                notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel, options, null, null, true, true);
            }
            versioningOption = getVersioningService().doPreSave(doc, dirty, versioningOption, checkinComment, options);
            if (checkout) {
                DocumentModel checkedOutDoc = readModel(doc);
                notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, checkedOutDoc, options, null, null, true, false);
            }
        }

        boolean allowVersionWrite = Boolean.TRUE.equals(docModel.getContextData(ALLOW_VERSION_WRITE));
        docModel.putContextData(ALLOW_VERSION_WRITE, null);
        boolean setReadWrite = allowVersionWrite && doc.isVersion() && doc.isReadOnly();

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
            boolean checkin = getVersioningService().isPostSaveDoingCheckIn(doc, versioningOption, options);
            if (checkin) {
                notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, options, null, null, true, true);
            }
            checkedInDoc = getVersioningService().doPostSave(doc, versioningOption, checkinComment, options);
        }

        // post-save events
        docModel = readModel(doc);
        if (checkedInDoc != null) {
            DocumentRef checkedInVersionRef = new IdRef(checkedInDoc.getUUID());
            notifyCheckedInVersion(docModel, checkedInVersionRef, options, checkinComment);
        }
        notifyEvent(DocumentEventTypes.DOCUMENT_UPDATED, docModel, options, null, null, true, false);
        updateDocumentCount.inc();
        return docModel;
    }

    @Override
    public void saveDocuments(DocumentModel[] docModels) {
        // TODO: optimize this - avoid calling at each iteration saveDoc...
        for (DocumentModel docModel : docModels) {
            saveDocument(docModel);
        }
    }

    @Override
    public DocumentModel getSourceDocument(DocumentRef docRef) {
        assert null != docRef;

        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        Document headDocument = doc.getSourceDocument();
        if (headDocument == null) {
            throw new DocumentNotFoundException("Source document has been deleted");
        }
        return readModel(headDocument);
    }

    protected VersionModel getVersionModel(Document version) {
        VersionModel versionModel = new VersionModelImpl();
        versionModel.setId(version.getUUID());
        versionModel.setCreated(version.getVersionCreationDate());
        versionModel.setDescription(version.getCheckinComment());
        versionModel.setLabel(version.getVersionLabel());
        return versionModel;
    }

    @Override
    public DocumentModel getLastDocumentVersion(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        Document version = doc.getLastVersion();
        return version == null ? null : readModel(version);
    }

    @Override
    public DocumentRef getLastDocumentVersionRef(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        Document version = doc.getLastVersion();
        return version == null ? null : new IdRef(version.getUUID());
    }

    @Override
    public List<DocumentRef> getVersionsRefs(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        List<String> ids = doc.getVersionsIds();
        List<DocumentRef> refs = new ArrayList<>(ids.size());
        for (String id : ids) {
            refs.add(new IdRef(id));
        }
        return refs;
    }

    @Override
    public List<DocumentModel> getVersions(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        List<Document> docVersions = doc.getVersions();
        List<DocumentModel> versions = new ArrayList<>(docVersions.size());
        for (Document version : docVersions) {
            versions.add(readModel(version));
        }
        return versions;
    }

    @Override
    public List<VersionModel> getVersionsForDocument(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        List<Document> docVersions = doc.getVersions();
        List<VersionModel> versions = new ArrayList<>(docVersions.size());
        for (Document version : docVersions) {
            versions.add(getVersionModel(version));
        }
        return versions;

    }

    @Override
    public DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef) {
        Document doc = resolveReference(docRef);
        Document ver = resolveReference(versionRef);
        return restoreToVersion(doc, ver, false, true);
    }

    @Override
    public DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef, boolean skipSnapshotCreation,
            boolean skipCheckout) {
        Document doc = resolveReference(docRef);
        Document ver = resolveReference(versionRef);
        return restoreToVersion(doc, ver, skipSnapshotCreation, skipCheckout);
    }

    protected DocumentModel restoreToVersion(Document doc, Document version, boolean skipSnapshotCreation,
            boolean skipCheckout) {
        checkPermission(doc, WRITE_VERSION);

        DocumentModel docModel = readModel(doc);

        Map<String, Serializable> options = new HashMap<>();

        // we're about to overwrite the document, make sure it's archived
        if (!skipSnapshotCreation && doc.isCheckedOut()) {
            String checkinComment = (String) docModel.getContextData(VersioningService.CHECKIN_COMMENT);
            docModel.putContextData(VersioningService.CHECKIN_COMMENT, null);
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, options, null, null, true, true);
            Document ver = getVersioningService().doCheckIn(doc, null, checkinComment);
            docModel.refresh(DocumentModel.REFRESH_STATE, null);
            notifyCheckedInVersion(docModel, new IdRef(ver.getUUID()), null, checkinComment);
        }

        // FIXME: the fields are hardcoded. should be moved in versioning
        // component
        // HOW?
        final Long majorVer = (Long) doc.getPropertyValue("major_version");
        final Long minorVer = (Long) doc.getPropertyValue("minor_version");
        if (majorVer != null || minorVer != null) {
            options.put(VersioningDocument.CURRENT_DOCUMENT_MAJOR_VERSION_KEY, majorVer);
            options.put(VersioningDocument.CURRENT_DOCUMENT_MINOR_VERSION_KEY, minorVer);
        }
        // add the uuid of the version being restored
        String versionUUID = version.getUUID();
        options.put(VersioningDocument.RESTORED_VERSION_UUID_KEY, versionUUID);

        notifyEvent(DocumentEventTypes.BEFORE_DOC_RESTORE, docModel, options, null, null, true, true);
        writeModel(doc, docModel);

        doc.restore(version);
        // re-read doc model after restoration
        docModel = readModel(doc);
        notifyEvent(DocumentEventTypes.DOCUMENT_RESTORED, docModel, options, null, docModel.getVersionLabel(), true,
                false);
        docModel = writeModel(doc, docModel);

        if (!skipCheckout) {
            // restore gives us a checked in document, so do a checkout
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel, options, null, null, true, true);
            getVersioningService().doCheckOut(doc);
            docModel = readModel(doc);
            notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, docModel, options, null, null, true, false);
        }

        log.debug("Document restored to version:" + version.getUUID());
        return docModel;
    }

    @Override
    public DocumentRef getBaseVersion(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ);
        Document ver = doc.getBaseVersion();
        if (ver == null) {
            return null;
        }
        checkPermission(ver, READ);
        return new IdRef(ver.getUUID());
    }

    @Override
    public DocumentRef checkIn(DocumentRef docRef, VersioningOption option, String checkinComment) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, WRITE_PROPERTIES);
        DocumentModel docModel = readModel(doc);

        Map<String, Serializable> options = new HashMap<>();
        notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, options, null, null, true, true);
        writeModel(doc, docModel);

        Document version = getVersioningService().doCheckIn(doc, option, checkinComment);

        docModel = readModel(doc);
        DocumentRef checkedInVersionRef = new IdRef(version.getUUID());
        notifyCheckedInVersion(docModel, checkedInVersionRef, options, checkinComment);
        writeModel(doc, docModel);

        return checkedInVersionRef;
    }

    /**
     * Send a core event for the creation of a new check in version. The source document is the live document model used
     * as the source for the checkin, not the archived version it-self.
     *
     * @param docModel work document that has been checked-in as a version
     * @param checkedInVersionRef document ref of the new checked-in version
     * @param options initial option map, or null
     */
    protected void notifyCheckedInVersion(DocumentModel docModel, DocumentRef checkedInVersionRef,
            Map<String, Serializable> options, String checkinComment) {
        String label = getVersioningService().getVersionLabel(docModel);
        Map<String, Serializable> props = new HashMap<>();
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
        String comment = checkinComment == null ? label : label + ' ' + checkinComment;
        props.put("comment", comment); // compat, used in audit
        // notify checkin on live document
        notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDIN, docModel, props, null, null, true, false);
        // notify creation on version document
        notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, getDocument(checkedInVersionRef), props, null, null, true,
                false);

    }

    @Override
    public void checkOut(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        // TODO: add a new permission names CHECKOUT and use it instead of
        // WRITE_PROPERTIES
        checkPermission(doc, WRITE_PROPERTIES);
        DocumentModel docModel = readModel(doc);
        Map<String, Serializable> options = new HashMap<>();

        notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel, options, null, null, true, true);

        getVersioningService().doCheckOut(doc);
        docModel = readModel(doc);

        notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, docModel, options, null, null, true, false);
        writeModel(doc, docModel);
    }

    @Override
    public boolean isCheckedOut(DocumentRef docRef) {
        assert null != docRef;
        Document doc = resolveReference(docRef);
        checkPermission(doc, BROWSE);
        return doc.isCheckedOut();
    }

    @Override
    public String getVersionSeriesId(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ);
        return doc.getVersionSeriesId();
    }

    @Override
    public DocumentModel getWorkingCopy(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_VERSION);
        Document pwc = doc.getWorkingCopy();
        checkPermission(pwc, READ);
        return pwc == null ? null : readModel(pwc);
    }

    @Override
    public DocumentModel getVersion(String versionableId, VersionModel versionModel) {
        String id = versionModel.getId();
        if (id != null) {
            return getDocument(new IdRef(id));
        }
        Document doc = getSession().getVersion(versionableId, versionModel);
        if (doc == null) {
            return null;
        }
        checkPermission(doc, READ_PROPERTIES);
        checkPermission(doc, READ_VERSION);
        return readModel(doc);
    }

    @Override
    public String getVersionLabel(DocumentModel docModel) {
        return getVersioningService().getVersionLabel(docModel);
    }

    @Override
    public DocumentModel getDocumentWithVersion(DocumentRef docRef, VersionModel version) {
        String id = version.getId();
        if (id != null) {
            return getDocument(new IdRef(id));
        }
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_PROPERTIES);
        checkPermission(doc, READ_VERSION);
        String docPath = doc.getPath();
        doc = doc.getVersion(version.getLabel());
        if (doc == null) {
            // SQL Storage uses to return null if version not found
            log.debug("Version " + version.getLabel() + " does not exist for " + docPath);
            return null;
        }
        log.debug("Retrieved the version " + version.getLabel() + " of the document " + docPath);
        return readModel(doc);
    }

    @Override
    public DocumentModel createProxy(DocumentRef docRef, DocumentRef folderRef) {
        Document doc = resolveReference(docRef);
        Document fold = resolveReference(folderRef);
        checkPermission(doc, READ);
        checkPermission(fold, ADD_CHILDREN);
        return createProxyInternal(doc, fold, new HashMap<>());
    }

    protected DocumentModel createProxyInternal(Document doc, Document folder, Map<String, Serializable> options) {
        // create the new proxy
        Document proxy = getSession().createProxy(doc, folder);
        DocumentModel proxyModel = readModel(proxy);

        notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, proxyModel, options, null, null, true, false);
        notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED, proxyModel, options, null, null, true, false);
        DocumentModel folderModel = readModel(folder);
        notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED, folderModel, options, null, null, true, false);
        return proxyModel;
    }

    /**
     * Remove proxies for the same base document in the folder. doc may be a normal document or a proxy.
     */
    protected List<String> removeExistingProxies(Document doc, Document folder) {
        Collection<Document> otherProxies = getSession().getProxies(doc, folder);
        List<String> removedProxyIds = new ArrayList<>(otherProxies.size());
        for (Document otherProxy : otherProxies) {
            removedProxyIds.add(otherProxy.getUUID());
            removeNotifyOneDoc(otherProxy);
        }
        return removedProxyIds;
    }

    /**
     * Update the proxy for doc in the given section to point to the new target. Do nothing if there are several
     * proxies.
     *
     * @return the proxy if it was updated, or {@code null} if none or several were found
     */
    protected DocumentModel updateExistingProxies(Document doc, Document folder, Document target) {
        Collection<Document> proxies = getSession().getProxies(doc, folder);
        try {
            if (proxies.size() == 1) {
                for (Document proxy : proxies) {
                    proxy.setTargetDocument(target);
                    return readModel(proxy);
                }
            }
        } catch (UnsupportedOperationException e) {
            log.error("Cannot update proxy, try to remove");
        }
        return null;
    }

    @Override
    public DocumentModelList getProxies(DocumentRef docRef, DocumentRef folderRef) {
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
    }

    @Override
    public List<String> getAvailableSecurityPermissions() {
        // XXX: add security check?
        return Arrays.asList(getSecurityService().getPermissionProvider().getPermissions());
    }

    @Override
    public DataModel getDataModel(DocumentRef docRef, Schema schema) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ);
        return DocumentModelFactory.createDataModel(doc, schema);
    }

    protected Object getDataModelField(DocumentRef docRef, String schema, String field) {
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
    }

    @Override
    public String getCurrentLifeCycleState(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_LIFE_CYCLE);
        return doc.getLifeCycleState();
    }

    @Override
    public String getLifeCyclePolicy(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_LIFE_CYCLE);
        return doc.getLifeCyclePolicy();
    }

    /**
     * Make a document follow a transition.
     *
     * @param docRef a {@link DocumentRef}
     * @param transition the transition to follow
     * @param options an option map than can be used by callers to pass additional params
     * @since 5.9.3
     */
    private boolean followTransition(DocumentRef docRef, String transition, ScopedMap options)
            throws LifeCycleException {
        Document doc = resolveReference(docRef);
        checkPermission(doc, WRITE_LIFE_CYCLE);

        if (!doc.isVersion() && !doc.isProxy() && !doc.isCheckedOut()) {
            boolean deleteOrUndelete = LifeCycleConstants.DELETE_TRANSITION.equals(transition)
                    || LifeCycleConstants.UNDELETE_TRANSITION.equals(transition);
            if (!deleteOrUndelete || Framework.getService(ConfigurationService.class).isBooleanPropertyFalse(
                    TRASH_KEEP_CHECKED_IN_PROPERTY)) {
                checkOut(docRef);
                doc = resolveReference(docRef);
            }
        }
        String formerStateName = doc.getLifeCycleState();
        doc.followTransition(transition);

        // Construct a map holding meta information about the event.
        Map<String, Serializable> eventOptions = new HashMap<>();
        eventOptions.put(org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_FROM, formerStateName);
        eventOptions.put(org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TO, doc.getLifeCycleState());
        eventOptions.put(org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION, transition);
        String comment = (String) options.getScopedValue("comment");
        DocumentModel docModel = readModel(doc);
        notifyEvent(org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT, docModel, eventOptions,
                DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY, comment, true, false);
        if (!docModel.isImmutable()) {
            writeModel(doc, docModel);
        }
        return true; // throws if error
    }

    @Override
    public boolean followTransition(DocumentModel docModel, String transition) throws LifeCycleException {
        return followTransition(docModel.getRef(), transition, docModel.getContextData());
    }

    @Override
    public boolean followTransition(DocumentRef docRef, String transition) throws LifeCycleException {
        return followTransition(docRef, transition, new ScopedMap());
    }

    @Override
    public Collection<String> getAllowedStateTransitions(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ_LIFE_CYCLE);
        return doc.getAllowedStateTransitions();
    }

    @Override
    public void reinitLifeCycleState(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, WRITE_LIFE_CYCLE);
        LifeCycleService service = NXCore.getLifeCycleService();
        service.reinitLifeCycle(doc);
    }

    @Override
    public Object[] getDataModelsField(DocumentRef[] docRefs, String schema, String field) {

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
    public DocumentRef[] getParentDocumentRefs(DocumentRef docRef) {
        final List<DocumentRef> docRefs = new ArrayList<>();
        final Document doc = resolveReference(docRef);
        Document parentDoc = doc.getParent();
        while (parentDoc != null) {
            final DocumentRef parentDocRef = new IdRef(parentDoc.getUUID());
            docRefs.add(parentDocRef);
            parentDoc = parentDoc.getParent();
        }
        DocumentRef[] refs = new DocumentRef[docRefs.size()];
        return docRefs.toArray(refs);
    }

    @Override
    public Object[] getDataModelsFieldUp(DocumentRef docRef, String schema, String field) {

        final DocumentRef[] parentRefs = getParentDocumentRefs(docRef);
        final DocumentRef[] allRefs = new DocumentRef[parentRefs.length + 1];
        allRefs[0] = docRef;
        System.arraycopy(parentRefs, 0, allRefs, 1, parentRefs.length);

        return getDataModelsField(allRefs, schema, field);
    }

    @Override
    public Lock setLock(DocumentRef docRef) throws LockException {
        Document doc = resolveReference(docRef);
        // TODO: add a new permission named LOCK and use it instead of
        // WRITE_PROPERTIES
        checkPermission(doc, WRITE_PROPERTIES);
        Lock lock = new Lock(getPrincipal().getName(), new GregorianCalendar());
        Lock oldLock = doc.setLock(lock);
        if (oldLock != null) {
            throw new LockException("Document already locked by " + oldLock.getOwner() + ": " + docRef);
        }
        DocumentModel docModel = readModel(doc);
        Map<String, Serializable> options = new HashMap<>();
        options.put("lock", lock);
        notifyEvent(DocumentEventTypes.DOCUMENT_LOCKED, docModel, options, null, null, true, false);
        return lock;
    }

    @Override
    public Lock getLockInfo(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        checkPermission(doc, READ);
        return doc.getLock();
    }

    @Override
    public Lock removeLock(DocumentRef docRef) throws LockException {
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
            throw new LockException("Document already locked by " + lock.getOwner() + ": " + docRef);
        }
        DocumentModel docModel = readModel(doc);
        Map<String, Serializable> options = new HashMap<>();
        options.put("lock", lock);
        notifyEvent(DocumentEventTypes.DOCUMENT_UNLOCKED, docModel, options, null, null, true, false);
        return lock;
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
    public void applyDefaultPermissions(String userOrGroupName) {
        if (userOrGroupName == null) {
            throw new NullPointerException("null userOrGroupName");
        }
        if (!isAdministrator()) {
            throw new DocumentSecurityException("You need to be an Administrator to do this.");
        }
        DocumentModel rootDocument = getRootDocument();
        ACP acp = new ACPImpl();

        UserEntry userEntry = new UserEntryImpl(userOrGroupName);
        userEntry.addPrivilege(READ);

        acp.setRules(new UserEntry[] { userEntry });

        setACP(rootDocument.getRef(), acp, false);
    }

    @Override
    public DocumentModel publishDocument(DocumentModel docToPublish, DocumentModel section) {
        return publishDocument(docToPublish, section, true);
    }

    @Override
    public DocumentModel publishDocument(DocumentModel docModel, DocumentModel section, boolean overwriteExistingProxy) {
        Document doc = resolveReference(docModel.getRef());
        Document sec = resolveReference(section.getRef());
        checkPermission(doc, READ);
        checkPermission(sec, ADD_CHILDREN);

        Map<String, Serializable> options = new HashMap<>();
        DocumentModel proxy = null;
        Document target;
        if (docModel.isProxy() || docModel.isVersion()) {
            target = doc;
            if (overwriteExistingProxy) {
                if (docModel.isVersion()) {
                    Document base = resolveReference(new IdRef(doc.getVersionSeriesId()));
                    proxy = updateExistingProxies(base, sec, target);
                }
                if (proxy == null) {
                    // remove previous
                    List<String> removedProxyIds = removeExistingProxies(doc, sec);
                    options.put(CoreEventConstants.REPLACED_PROXY_IDS, (Serializable) removedProxyIds);
                }
            }

        } else {
            String checkinComment = (String) docModel.getContextData(VersioningService.CHECKIN_COMMENT);
            docModel.putContextData(VersioningService.CHECKIN_COMMENT, null);
            if (doc.isCheckedOut() || doc.getLastVersion() == null) {
                if (!doc.isCheckedOut()) {
                    // last version was deleted while leaving a checked in
                    // doc. recreate a version
                    notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel, options, null, null, true, true);
                    getVersioningService().doCheckOut(doc);
                    docModel = readModel(doc);
                    notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, docModel, options, null, null, true, false);
                }
                notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, options, null, null, true, true);
                Document version = getVersioningService().doCheckIn(doc, null, checkinComment);
                docModel.refresh(DocumentModel.REFRESH_STATE | DocumentModel.REFRESH_CONTENT_LAZY, null);
                notifyCheckedInVersion(docModel, new IdRef(version.getUUID()), null, checkinComment);
            }
            // NXP-12921: use base version because we could need to publish
            // a previous version (after restoring for example)
            target = doc.getBaseVersion();
            if (overwriteExistingProxy) {
                proxy = updateExistingProxies(doc, sec, target);
                if (proxy == null) {
                    // no or several proxies, remove them
                    List<String> removedProxyIds = removeExistingProxies(doc, sec);
                    options.put(CoreEventConstants.REPLACED_PROXY_IDS, (Serializable) removedProxyIds);
                } else {
                    // notify proxy updates
                    notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_UPDATED, proxy, options, null, null, true, false);
                    notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED, proxy, options, null, null, true, false);
                    notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED, section, options, null, null, true, false);
                }
            }
        }
        if (proxy == null) {
            proxy = createProxyInternal(target, sec, options);
        }
        return proxy;
    }

    @Override
    public String getSuperParentType(DocumentModel doc) {
        DocumentModel superSpace = getSuperSpace(doc);
        if (superSpace == null) {
            return null;
        } else {
            return superSpace.getType();
        }
    }

    @Override
    public DocumentModel getSuperSpace(DocumentModel doc) {
        if (doc == null) {
            throw new IllegalArgumentException("null document");
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
    private DocumentModel getDirectAccessibleParent(DocumentRef docRef) {
        Document doc = resolveReference(docRef);
        Document parentDoc = doc.getParent();
        if (parentDoc == null) {
            // return null for placeless document
            return null;
        }
        if (!hasPermission(parentDoc, READ)) {
            String parentPath = parentDoc.getPath();
            if ("/".equals(parentPath)) {
                return getRootDocument();
            } else {
                // try on parent
                return getDirectAccessibleParent(new PathRef(parentDoc.getPath()));
            }
        }
        return readModel(parentDoc);
    }

    @Override
    public <T extends Serializable> T getDocumentSystemProp(DocumentRef ref, String systemProperty, Class<T> type) {
        Document doc = resolveReference(ref);
        return doc.getSystemProp(systemProperty, type);
    }

    @Override
    public <T extends Serializable> void setDocumentSystemProp(DocumentRef ref, String systemProperty, T value) {
        Document doc = resolveReference(ref);
        if (systemProperty != null && systemProperty.startsWith(BINARY_TEXT_SYS_PROP)) {
            DocumentModel docModel = readModel(doc);
            Map<String, Serializable> options = new HashMap<>();
            options.put(systemProperty, value != null);
            notifyEvent(DocumentEventTypes.BINARYTEXT_UPDATED, docModel, options, null, null, false, true);
        }
        doc.setSystemProp(systemProperty, value);
    }

    @Override
    public void orderBefore(DocumentRef parent, String src, String dest) {
        if ((src == null) || (src.equals(dest))) {
            return;
        }
        Document doc = resolveReference(parent);
        doc.orderBefore(src, dest);
        Map<String, Serializable> options = new HashMap<>();

        // send event on container passing the reordered child as parameter
        DocumentModel docModel = readModel(doc);
        options.put(CoreEventConstants.REORDERED_CHILD, src);
        notifyEvent(DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED, docModel, options, null, src, true, false);
    }

    @Override
    public DocumentModelRefresh refreshDocument(DocumentRef ref, int refreshFlags, String[] schemas) {
        Document doc = resolveReference(ref);

        // permission checks
        if ((refreshFlags & (DocumentModel.REFRESH_PREFETCH | DocumentModel.REFRESH_STATE | DocumentModel.REFRESH_CONTENT)) != 0) {
            checkPermission(doc, READ);
        }
        if ((refreshFlags & DocumentModel.REFRESH_ACP) != 0) {
            checkPermission(doc, READ_SECURITY);
        }

        DocumentModelRefresh refresh = DocumentModelFactory.refreshDocumentModel(doc, refreshFlags, schemas);

        // ACPs need the session, so aren't done in the factory method
        if ((refreshFlags & DocumentModel.REFRESH_ACP) != 0) {
            refresh.acp = getSession().getMergedACP(doc);
        }

        return refresh;
    }

    @Override
    public String[] getPermissionsToCheck(String permission) {
        return getSecurityService().getPermissionsToCheck(permission);
    }

    @Override
    public <T extends DetachedAdapter> T adaptFirstMatchingDocumentWithFacet(DocumentRef docRef, String facet,
            Class<T> adapterClass) {
        Document doc = getFirstParentDocumentWithFacet(docRef, facet);
        if (doc != null) {
            DocumentModel docModel = readModel(doc);
            loadDataModelsForFacet(docModel, doc, facet);
            docModel.detach(false);
            return docModel.getAdapter(adapterClass);
        }
        return null;
    }

    protected void loadDataModelsForFacet(DocumentModel docModel, Document doc, String facetName) {
        // Load all the data related to facet's schemas
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        CompositeType facet = schemaManager.getFacet(facetName);
        if (facet == null) {
            return;
        }

        String[] facetSchemas = facet.getSchemaNames();
        for (String schema : facetSchemas) {
            DataModel dm = DocumentModelFactory.createDataModel(doc, schemaManager.getSchema(schema));
            docModel.getDataModels().put(schema, dm);
        }
    }

    /**
     * Returns the first {@code Document} with the given {@code facet}, recursively going up the parent hierarchy.
     * Returns {@code null} if there is no more parent.
     * <p>
     * This method does not check security rights.
     */
    protected Document getFirstParentDocumentWithFacet(DocumentRef docRef, String facet) {
        Document doc = resolveReference(docRef);
        while (doc != null && !doc.hasFacet(facet)) {
            doc = doc.getParent();
        }
        return doc;
    }

    @Override
    public Map<String, String> getBinaryFulltext(DocumentRef ref) {
        Document doc = resolveReference(ref);
        checkPermission(doc, READ);
        return getSession().getBinaryFulltext(doc.getUUID());
    }

}
