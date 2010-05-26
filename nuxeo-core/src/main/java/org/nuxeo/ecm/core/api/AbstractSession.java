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

import java.io.InputStream;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Null;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocsQueryProviderDef;
import org.nuxeo.ecm.core.api.impl.DocumentModelIteratorImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.OperationHandler;
import org.nuxeo.ecm.core.api.operation.ProgressMonitor;
import org.nuxeo.ecm.core.api.operation.Status;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.lifecycle.LifeCycleConstants;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.DocumentProxy;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.PathComparator;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.FilterableQuery;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.utils.SIDGenerator;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.InputStreamSource;
import org.nuxeo.runtime.services.streaming.StreamManager;

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
public abstract class AbstractSession implements CoreSession,
        SecurityConstants, OperationHandler, Serializable {

    public static final NuxeoPrincipal ANONYMOUS = new UserPrincipal(
            "anonymous");

    public static final NuxeoPrincipal ADMINISTRATOR = new UserPrincipal(
            SecurityConstants.ADMINISTRATOR);

    private static final Log log = LogFactory.getLog(CoreSession.class);

    private static final long serialVersionUID = 6585443198474361876L;

    private static final Comparator<? super Document> pathComparator = new PathComparator();

    // the repository name
    protected String repositoryName;

    protected Map<String, Serializable> sessionContext;

    /**
     * Private access to protected it again direct access since this field is
     * lazy loaded. You must use {@link #getEventService()} to get the service
     */
    private transient EventService eventService;

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

    /**
     * Used to resolve core documents based on session.
     */
    protected final DocumentResolver documentResolver = new DocumentResolver();

    private String sessionId;

    /**
     * Internal method: Gets the current session based on the client session id.
     *
     * @return the repository session
     */
    public abstract Session getSession() throws ClientException;

    public String connect(String repositoryName,
            Map<String, Serializable> context) throws ClientException {
        if (null == context) {
            context = new HashMap<String, Serializable>();
        }

        if (sessionId != null) {
            throw new AlreadyConnectedException();
        }
        this.repositoryName = repositoryName;
        sessionContext = context;
        // this session is valid until the disconnect method is called
        sessionId = createSessionId();
        sessionContext.put("SESSION_ID", sessionId);
        // register this session locally -> this way document models can
        // retrieve
        // their session on the server side
        CoreInstance.getInstance().registerSession(sessionId, this);

        // <------------ begin repository initialization
        // we need to initialize the repository if this is the first time it is
        // accessed in this JVM session
        // for this we get the session and test if the "REPOSITORY_FIRST_ACCESS"
        // is set after the session is created
        // we need to synchronize the call to be sure we initialize only once.
        synchronized (AbstractSession.class) {
            Session session = getSession(); // force the creation of the
            // underlying session
            if (sessionContext.remove("REPOSITORY_FIRST_ACCESS") != null) {
                // this is the first time we access the repository in this JVM
                // notify the InitializationHandler if any.
                RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
                if (handler != null) {
                    // change principal to give all rights
                    Principal ctxPrincipal = (Principal) sessionContext.get("principal");
                    try {
                        // change current principal to give all right to the
                        // handler
                        // FIXME : this should be fixed by using SystemPrincipal
                        // -> we must synchronize this with SecurityService
                        // check
                        sessionContext.put("principal", new SimplePrincipal(
                                SecurityConstants.SYSTEM_USERNAME));
                        try {
                            handler.initializeRepository(this);
                            session.save();
                        } catch (ClientException e) {
                            // shouldn't remove the root? ... to restart with an
                            // empty repository
                            log.error(
                                    "Failed to initialize repository content",
                                    e);
                        } catch (DocumentException e) {
                            log.error("Unable to save session after repository init : "
                                    + e.getMessage());
                        }
                    } finally {
                        sessionContext.remove("principal");
                        if (ctxPrincipal != null) { // restore principal
                            sessionContext.put("principal",
                                    (Serializable) ctxPrincipal);
                        }
                    }
                }
            }
        }
        // <------------- end repository intialization

        return sessionId;
    }

    /**
     * Default implementation for session ID generation.
     * <p>
     * The ID has the following format:
     * &lt;repository-name&gt;-&lt;JVM-Unique-ID&gt; where the JVM-Unique-ID is
     * an unique ID on a running JVM and repository-name is a used to avoid name
     * clashes with sessions on different machines (the repository name should
     * be unique in the system)
     *
     * <ul>
     * <li>A is the repository name (which uniquely identifies the repository in
     * the system)
     * <li>B is the time of the session creation in milliseconds
     * </ul>
     */
    protected String createSessionId() {
        return repositoryName + '-' + SIDGenerator.next();
    }

    public DocumentType getDocumentType(String type) {
        return NXSchema.getSchemaManager().getDocumentType(type);
    }

    /**
     * Utility method to generate VersionModel labels.
     *
     * @return the String representation of an auto-incremented counter that not
     *         used in any label of docRef
     */
    public String generateVersionLabelFor(DocumentRef docRef)
            throws ClientException {
        // find the biggest label that is castable to an int
        int max = 0;
        for (VersionModel version : getVersionsForDocument(docRef)) {
            try {
                int current = Integer.parseInt(version.getLabel());
                max = current > max ? current : max;
            } catch (NumberFormatException e) {
                // ignore labels that are not parsable as int
            }
        }
        return Integer.toString(max + 1);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Principal getPrincipal() {
        return ANONYMOUS;
    }

    protected final void checkPermission(Document doc, String permission)
            throws DocumentSecurityException, DocumentException {
        if (isAdministrator()) {
            return;
        }
        if (!hasPermission(doc, permission)) {
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
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, repositoryName);
        ctx.setProperty(CoreEventConstants.SESSION_ID, sessionId);
        return ctx;
    }

    public EventService getEventService() {
        if (eventService == null) {
            try {
                eventService = Framework.getLocalService(EventService.class);
            } catch (Exception e) {
                throw new Error("Core Event Service not found");
            }
        }
        return eventService;
    }

    public void afterBegin() {
        if (log.isTraceEnabled()) {
            log.trace("Transaction started");
        }
        try {
            getEventService().transactionStarted();
        } catch (Exception e) {
            log.error("Error while notifying transaction start", e);
        }
    }

    public void beforeCompletion() {
    }

    public void afterCompletion(boolean committed) {
        if (log.isTraceEnabled()) {
            log.trace("Transaction "
                    + (committed ? "committed" : "rolled back"));
        }
        try {
            if (committed) {
                getEventService().transactionCommitted();
            } else {
                getEventService().transactionRolledback();
            }
        } catch (Exception e) {
            log.error("Error while notifying transaction completion", e);
        }
    }

    public void fireEvent(Event event) throws ClientException {
        getEventService().fireEvent(event);
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
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, repositoryName);
        ctx.setProperty(CoreEventConstants.SESSION_ID, sessionId);
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
        fireEvent(event);
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

    public boolean hasPermission(DocumentRef docRef, String permission)
            throws ClientException {
        try {
            Session session = getSession();
            Document doc = DocumentResolver.resolveReference(session, docRef);
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
        // -> cache ACP at securitymanager level or try to reuse the ACP when it
        // is known
        return getSecurityService().checkPermission(doc, getPrincipal(),
                permission);
        // return doc.getSession().getSecurityManager().checkPermission(doc,
        // getPrincipal().getName(), permission);
    }

    protected final Document resolveReference(DocumentRef docRef)
            throws DocumentException, ClientException {
        return DocumentResolver.resolveReference(getSession(), docRef);
    }

    /**
     * Gets the document model for the given core document.
     * <p>
     * If no schemas are specified (schemas are null) use the default schemas as
     * configured in the document type manager.
     *
     * @param doc the document
     * @param schemas the schemas if any, null otherwise
     * @return the document model
     */
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
        // get loaded data models
        boolean changed = false;
        DocumentPart[] parts = docModel.getParts();
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

        if (changed) {
            doc.setDirty(true);
        }
        // TODO: here we can optimize document part doesn't need to be read
        DocumentModel newModel = readModel(doc, null);
        newModel.copyContextData(docModel);
        return newModel;
    }

    protected void writeDocumentPart(DocumentPart part) {
        part.iterator();
    }

    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        try {
            Document srcDoc = resolveReference(src);
            Document dstDoc = resolveReference(dst);
            checkPermission(dstDoc, ADD_CHILDREN);

            DocumentModel srcDocModel = readModel(srcDoc, null);
            notifyEvent(DocumentEventTypes.ABOUT_TO_COPY, srcDocModel, null,
                    null, null, true, true);

            Document doc = getSession().copy(srcDoc, dstDoc, name);
            if (doc.isLocked()) { // if we copy a locked document - the new
                // document will be locked too! - fixing
                // this
                doc.unlock();
            }

            Map<String, Serializable> options = new HashMap<String, Serializable>();

            // notify document created by copy
            DocumentModel docModel = readModel(doc, null);

            String comment = srcDoc.getRepository().getName() + ':'
                    + src.toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel,
                    options, null, comment, true, false);
            docModel = writeModel(doc, docModel);

            // notify document copied
            comment = doc.getRepository().getName() + ':'
                    + docModel.getRef().toString();

            notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel,
                    options, null, comment, true, false);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to copy document: "
                    + e.getMessage(), e);
        }
    }

    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        for (DocumentRef ref : src) {
            newDocuments.add(copy(ref, dst, null));
        }

        return newDocuments;
    }

    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst,
            String name) throws ClientException {
        try {
            Document srcDoc = resolveReference(src);
            if (!srcDoc.isProxy()) {
                return copy(src, dst, name);
            }
            Document dstDoc = resolveReference(dst);
            checkPermission(dstDoc, WRITE);

            // create a new document using the expanded proxy
            DocumentModel srcDocModel = readModel(srcDoc, null);
            String docName = (name != null) ? name : srcDocModel.getName();
            DocumentModel docModel = createDocumentModel(dstDoc.getPath(),
                    docName, srcDocModel.getType());
            docModel.copyContent(srcDocModel);
            notifyEvent(DocumentEventTypes.ABOUT_TO_COPY, srcDocModel, null,
                    null, null, true, true);
            docModel = createDocument(docModel);
            Document doc = resolveReference(docModel.getRef());

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            // notify document created by copy
            String comment = srcDoc.getRepository().getName() + ':'
                    + src.toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel,
                    options, null, comment, true, false);

            // notify document copied
            comment = doc.getRepository().getName() + ':'
                    + docModel.getRef().toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel,
                    options, null, comment, true, false);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to copy document: "
                    + e.getMessage(), e);
        }
    }

    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src,
            DocumentRef dst) throws ClientException {
        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        for (DocumentRef ref : src) {
            newDocuments.add(copyProxyAsDocument(ref, dst, null));
        }

        return newDocuments;
    }

    public DocumentModel move(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        try {
            Document srcDoc = resolveReference(src);
            Document dstDoc = resolveReference(dst);
            checkPermission(dstDoc, ADD_CHILDREN);
            checkPermission(srcDoc.getParent(), REMOVE_CHILDREN);
            checkPermission(srcDoc, REMOVE);

            DocumentModel srcDocModel = readModel(srcDoc, null);
            notifyEvent(DocumentEventTypes.ABOUT_TO_MOVE, srcDocModel, null,
                    null, null, true, true);

            String comment = srcDoc.getRepository().getName() + ':'
                    + srcDoc.getParent().getUUID();

            if (name == null) {
                name = srcDoc.getName();
            }
            name = generateDocumentName(dstDoc, name);
            Document doc = getSession().move(srcDoc, dstDoc, name);

            // notify document moved
            DocumentModel docModel = readModel(doc, null);
            Map<String, Serializable> options = new HashMap<String, Serializable>();
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

    public void move(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        for (DocumentRef ref : src) {
            move(ref, dst, null);
        }
    }

    public ACP getACP(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_SECURITY);
            return getSession().getSecurityManager().getMergedACP(doc);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get acp", e);
        }
    }

    public void setACP(DocumentRef docRef, ACP newAcp, boolean overwrite)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_SECURITY);
            DocumentModel docModel = readModel(doc, null);

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            options.put(CoreEventConstants.OLD_ACP,
                    (Serializable) docModel.getACP().clone());
            options.put(CoreEventConstants.NEW_ACP,
                    (Serializable) newAcp.clone());

            notifyEvent(DocumentEventTypes.BEFORE_DOC_SECU_UPDATE, docModel,
                    options, null, null, true, true);
            getSession().getSecurityManager().setACP(doc, newAcp, overwrite);
            docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.DOCUMENT_SECURITY_UPDATED, docModel,
                    options, null, null, true, false);
        } catch (DocumentException e) {
            throw new ClientException("Failed to set acp", e);
        }
    }

    public void cancel() throws ClientException {
        try {
            getSession().cancel();
        } catch (DocumentException e) {
            throw new ClientException("Failed to cancel session", e);
        }
    }

    private DocumentModel createDocumentModelFromTypeName(String typeName,
            Map<String, Serializable> options) throws ClientException {
        try {
            DocumentType docType = getSession().getTypeManager().getDocumentType(
                    typeName);
            if (docType == null) {
                throw new ClientException(typeName
                        + " is not a registered core type");
            }
            DocumentModel docModel = DocumentModelFactory.createDocumentModel(
                    sessionId, docType);
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

    public DocumentModel createDocumentModel(String typeName)
            throws ClientException {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        return createDocumentModelFromTypeName(typeName, options);
    }

    public DocumentModel createDocumentModel(String parentPath, String id,
            String typeName) throws ClientException {
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(CoreEventConstants.PARENT_PATH, parentPath);
        options.put(CoreEventConstants.DOCUMENT_MODEL_ID, id);
        DocumentModel model = createDocumentModelFromTypeName(typeName, options);
        model.setPathInfo(parentPath, id);
        return model;
    }

    public DocumentModel createDocumentModel(String typeName,
            Map<String, Object> options) throws ClientException {

        Map<String, Serializable> serializableOptions = new HashMap<String, Serializable>();

        for (Entry<String, Object> entry : options.entrySet()) {
            serializableOptions.put(entry.getKey(),
                    (Serializable) entry.getValue());
        }
        return createDocumentModelFromTypeName(typeName, serializableOptions);
    }

    public DocumentModel createDocument(DocumentModel docModel)
            throws ClientException {
        String typeName = docModel.getType();
        DocumentRef parentRef = docModel.getParentRef();
        if (typeName == null) {
            throw new ClientException(String.format(
                    "cannot create document '%s' with undefined type name",
                    docModel.getTitle()));
        }
        if (parentRef == null) {
            throw new ClientException(
                    String.format(
                            "cannot create document '%s' with undefined reference to parent document",
                            docModel.getTitle()));
        }
        try {
            Document folder = resolveReference(parentRef);
            checkPermission(folder, ADD_CHILDREN);

            // get initial life cycle state info
            String initialLifecycleState = null;
            Object lifecycleStateinfo = docModel.getContextData(LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME);
            if (lifecycleStateinfo instanceof String) {
                initialLifecycleState = (String) lifecycleStateinfo;
            }

            Map<String, Serializable> options = getContextMapEventInfo(docModel);
            notifyEvent(DocumentEventTypes.ABOUT_TO_CREATE, docModel, options,
                    null, null, false, true); // no lifecycle yet
            String name = docModel.getName();
            name = generateDocumentName(folder, name);
            Document doc = folder.addChild(name, typeName);

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

            // re-read docmodel
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, docModel, options,
                    null, null, true, false);
            docModel = writeModel(doc, docModel);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document: "
                    + docModel.getName(), e);
        }
    }

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
        Document parent = parentRef == null || EMPTY_PATH.equals(parentRef) ? null
                : resolveReference(parentRef);
        Map<String, Serializable> props = docModel.getContextData().getDefaultScopeValues();

        // create the document
        Document doc = getSession().importDocument(id, parent, name, typeName,
                props);

        if (typeName.equals(CoreSession.IMPORT_PROXY_TYPE)) {
            // just reread the final document
            docModel = readModel(doc, null);
        } else {
            // init document with data from doc model
            docModel = writeModel(doc, docModel);
        }

        // send an event about the import
        notifyEvent(DocumentEventTypes.DOCUMENT_IMPORTED, docModel, null, null,
                null, true, false);
    }

    /**
     * Generate a non-null unique name within given parent's children.
     * <p>
     * If name is null, a name is generated. If name is already used, a random
     * suffix is appended to it.
     *
     * @return a unique name within given parent's children
     */
    public String generateDocumentName(Document parent, String name)
            throws DocumentException {
        if (name == null || name.length() == 0) {
            name = IdUtils.generateStringId();
        }
        if (parent.hasChild(name)) {
            name += '.' + String.valueOf(System.currentTimeMillis());
        }
        return name;
    }

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

    public abstract boolean isSessionAlive();

    public void disconnect() throws ClientException {
        if (isSessionAlive()) {
            getSession().dispose();
        }
        if (sessionId != null) {
            CoreInstance.getInstance().unregisterSession(sessionId);
        }
        sessionContext = null;
        sessionId = null;
        repositoryName = null;
    }

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

    public DocumentModel getChild(DocumentRef parent, String name)
            throws ClientException {
        try {
            Document doc = resolveReference(parent);
            checkPermission(doc, READ_CHILDREN);
            Document child = doc.getChild(name);
            checkPermission(child, READ);
            return readModel(child, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get child " + name, e);
        }
    }

    public DocumentModelList getChildren(DocumentRef parent)
            throws ClientException {
        return getChildren(parent, null, READ, null, null);
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent)
            throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_CHILDREN);
        def.setParent(parent);
        return new DocumentModelIteratorImpl(this, 15, def, null, READ, null);
    }

    public DocumentModelList getChildren(DocumentRef parent, String type)
            throws ClientException {
        return getChildren(parent, type, READ, null, null);
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent,
            String type) throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_CHILDREN);
        def.setParent(parent);
        return new DocumentModelIteratorImpl(this, 15, def, type, null, null);
    }

    public DocumentModelList getChildren(DocumentRef parent, String type,
            String perm) throws ClientException {
        return getChildren(parent, type, perm, null, null);
    }

    public DocumentModelList getChildren(DocumentRef parent, String type,
            Filter filter, Sorter sorter) throws ClientException {
        return getChildren(parent, type, null, filter, sorter);
    }

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
                        DocumentModel childModel = readModel(child, null);
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

    /**
     * Method used internally to retrieve frames of a long result.
     */
    public DocumentModelsChunk getDocsResultChunk(DocsQueryProviderDef def,
            String type, String perm, Filter filter, final int start,
            final int max) throws ClientException {
        // convention: if count == 0 return all results to the end
        if (max < 0) {
            throw new IllegalArgumentException("invalid count=" + max);
        }
        int count = max;

        DocsQueryProviderFactory dqpFactory = new DocsQueryProviderFactory(this);
        try {
            if (perm == null) {
                perm = READ;
            }

            DocsQueryProvider dqp = dqpFactory.getDQLbyType(def);
            // Document doc = resolveReference(parent);
            // checkPermission(doc, READ_CHILDREN);
            // Iterator<Document> children = doc.getChildren(start);
            final DocumentIterator children = dqp.getDocs(start);
            DocumentModelList docs = new DocumentModelListImpl();
            int lastIndex = start;
            boolean hasMore = false;
            while (children.hasNext()) {
                lastIndex++;
                Document child = children.next();

                // 1st filter:
                if (!dqp.accept(child)) {
                    continue;
                }

                if (hasPermission(child, perm)) {
                    if (child.getType() != null
                            && (type == null || type.equals(child.getType().getName()))) {
                        DocumentModel childModel = readModel(child, null);
                        if (filter == null || filter.accept(childModel)) {
                            if (count == 0) {
                                // end of page
                                hasMore = true;
                                break;
                            }
                            count--;
                            docs.add(childModel);
                        }
                    }
                }
            }

            final long total = children.getSize();

            return new DocumentModelsChunk(docs, lastIndex - 1, hasMore, total);
        } catch (DocumentException e) {
            if (def.getParent() != null) {
                throw new ClientException("Failed to get children for "
                        + def.getParent().toString(), e);
            } else {
                throw new ClientException("Failed to get documents for query: "
                        + def.getQuery(), e);
            }
        }
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent,
            String type, String perm, Filter filter) throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_CHILDREN);
        def.setParent(parent);
        return new DocumentModelIteratorImpl(this, 15, def, type, perm, filter);
    }

    public DocumentModel getDocument(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return readModel(doc, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get document "
                    + docRef.toString(), e);
        }
    }

    public DocumentModel getDocument(DocumentRef docRef, String[] schemas)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return readModel(doc, schemas);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get document " + docRef, e);
        }
    }

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
            docs.add(readModel(doc, null));
        }
        return new DocumentModelListImpl(docs);
    }

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
                    docs.add(readModel(child, null));
                }
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get leaf children for "
                    + parent.toString(), e);
        }
    }

    public DocumentModelIterator getFilesIterator(DocumentRef parent)
            throws ClientException {

        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_CHILDREN_NON_FOLDER);
        def.setParent(parent);
        return new DocumentModelIteratorImpl(this, 15, def, null, null, null);
    }

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
                    DocumentModel docModel = readModel(doc, null);
                    if (filter == null || filter.accept(docModel)) {
                        docs.add(readModel(child, null));
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
                    docs.add(readModel(child, null));
                }
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get folders " + parent, e);
        }
    }

    public DocumentModelIterator getFoldersIterator(DocumentRef parent)
            throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_CHILDREN_FOLDERS);
        def.setParent(parent);
        return new DocumentModelIteratorImpl(this, 15, def, null, null, null);
    }

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
                    DocumentModel docModel = readModel(doc, null);
                    if (filter == null || filter.accept(docModel)) {
                        docs.add(readModel(child, null));
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
            return readModel(parentDoc, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get parent document of "
                    + docRef, e);
        }
    }

    public List<DocumentModel> getParentDocuments(final DocumentRef docRef)
            throws ClientException {

        if (null == docRef) {
            throw new IllegalArgumentException("null docRef");
        }

        final List<DocumentModel> docsList = new ArrayList<DocumentModel>();
        try {
            Document doc = resolveReference(docRef);
            String rootPath = getSession().getRootDocument().getPath();
            while (!doc.getPath().equals(rootPath)) {
                // XXX OG: shouldn't we check BROWSE and READ_PROPERTIES
                // instead?
                if (!hasPermission(doc, READ)) {
                    break;
                }
                docsList.add(readModel(doc, null));
                doc = doc.getParent();
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get parent documents: "
                    + docRef, e);
        }
        Collections.reverse(docsList);

        return docsList;
    }

    public DocumentModel getRootDocument() throws ClientException {
        try {
            return readModel(getSession().getRootDocument(), null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get the root document", e);
        }
    }

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

    public DocumentModelList query(String query) throws ClientException {
        return query(query, null, 0, 0, false);
    }

    public DocumentModelList query(String query, int max)
            throws ClientException {
        return query(query, null, max, 0, false);
    }

    public DocumentModelList query(String query, Filter filter)
            throws ClientException {
        return query(query, filter, 0, 0, false);
    }

    public DocumentModelList query(String query, Filter filter, int max)
            throws ClientException {
        return query(query, filter, max, 0, false);
    }

    @SuppressWarnings("null")
    public DocumentModelList query(String query, Filter filter, long limit,
            long offset, boolean countTotal) throws ClientException {
        SecurityService securityService = getSecurityService();
        Principal principal = getPrincipal();
        try {
            Query compiledQuery = getSession().createQuery(query,
                    Query.Type.NXQL);
            QueryResult results;
            boolean postFilterPermission;
            boolean postFilterFilter;
            boolean postFilterPolicies;
            boolean postFilter;
            String permission = BROWSE;
            if (compiledQuery instanceof FilterableQuery) {
                postFilterPermission = false;
                postFilterPolicies = !securityService.arePoliciesExpressibleInQuery();
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
                        securityService.getPoliciesQueryTransformers(),
                        postFilter ? 0 : limit, postFilter ? 0 : offset);
                results = ((FilterableQuery) compiledQuery).execute(
                        queryFilter, countTotal && !postFilter);
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
                    if (!countTotal) {
                        // can break early
                        break;
                    }
                    n++;
                    continue;
                }
                n++;
                docs.add(model);
            }
            if (countTotal) {
                docs.setTotalSize(n);
            }
            return docs;
        } catch (Exception e) {
            throw new ClientException("Failed to execute query: "
                    + tryToExtractMeaningfulErrMsg(e), e);
        }
    }

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
            if ("NXQL".equals(queryType)) {
                if (!securityService.arePoliciesExpressibleInQuery()) {
                    log.warn("Security policy cannot be expressed in query");
                }
                transformers = securityService.getPoliciesQueryTransformers();
            } else {
                transformers = Collections.emptyList();
            }
            QueryFilter queryFilter = new QueryFilter(principal, principals,
                    permissions, null, transformers, 0, 0);
            return getSession().queryAndFetch(query, queryType, queryFilter,
                    params);
        } catch (Exception e) {
            throw new ClientException("Failed to execute query: " + queryType
                    + ": " + query + ": " + tryToExtractMeaningfulErrMsg(e), e);
        }
    }

    public DocumentModelIterator queryIt(String query, Filter filter, int max)
            throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_QUERY);
        def.setQuery(query);
        return new DocumentModelIteratorImpl(this, 15, def, null, BROWSE,
                filter);
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

    @Deprecated
    public DocumentModelList querySimpleFts(String keywords)
            throws ClientException {
        return querySimpleFts(keywords, null);
    }

    @Deprecated
    public DocumentModelList querySimpleFts(String keywords, Filter filter)
            throws ClientException {
        try {
            // TODO this is hardcoded query : need to add support for CONTAINS
            // in NXQL
            // TODO check (repair) for keywords sanity to avoid xpath injection
            final String xpathQ = "//element(*, ecmnt:document)[jcr:contains(.,'*"
                    + keywords + "*')]";
            final Query compiledQuery = getSession().createQuery(xpathQ,
                    Query.Type.XPATH);
            final QueryResult qr = compiledQuery.execute();

            final DocumentModelList retrievedDocs = qr.getDocumentModels();

            final DocumentModelList docs = new DocumentModelListImpl();
            for (DocumentModel model : retrievedDocs) {
                if (hasPermission(model.getRef(), READ)) {
                    if (filter == null || filter.accept(model)) {
                        docs.add(model);
                    }
                }
            }

            return docs;

        } catch (Exception e) {
            log.error("failed to execute query", e);
            throw new ClientException("Failed to get the root document", e);
        }
    }

    @Deprecated
    public DocumentModelIterator querySimpleFtsIt(String query, Filter filter,
            int pageSize) throws ClientException {
        return querySimpleFtsIt(query, null, filter, pageSize);
    }

    @Deprecated
    public DocumentModelIterator querySimpleFtsIt(String query,
            String startingPath, Filter filter, int pageSize)
            throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_QUERY_FTS);
        def.setQuery(query);
        def.setStartingPath(startingPath);
        return new DocumentModelIteratorImpl(this, pageSize, def, null, BROWSE,
                filter);
    }

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
                return hasPermission(working, REMOVE);
            } else {
                // no working document, only admins can remove
                return isAdministrator();
            }
        } else {
            if (!hasPermission(doc, REMOVE)) {
                return false;
            }
            Document parent = doc.getParent();
            return parent == null || hasPermission(parent, REMOVE_CHILDREN);
        }
    }

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

        } catch (DocumentException e) {
            try {
                throw new ClientException("Failed to remove document "
                        + doc.getUUID(), e);
            } catch (DocumentException e2) {
                log.error("Failed to remove doc", e);
                throw new ClientException("Failed to remove and "
                        + "even to get UUID " + doc.toString());
            }
        }
    }

    protected void removeNotifyOneDoc(Document doc) throws ClientException,
            DocumentException {
        // XXX notify with options if needed
        DocumentModel docModel = readModel(doc, null);
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        if (docModel != null) {
            options.put("docTitle", docModel.getTitle());
        }

        // notify different events depending on wether the document is a version
        // or not
        if (!doc.isVersion()) {
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE, docModel, options,
                    null, null, true, true);
            CoreService coreService = Framework.getLocalService(CoreService.class);
            coreService.getVersionRemovalPolicy().removeVersions(getSession(),
                    doc, this);
        } else {
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE_VERSION, docModel,
                    options, null, null, true, true);
        }
        doc.remove();
        if (!doc.isVersion()) {
            notifyEvent(DocumentEventTypes.DOCUMENT_REMOVED, docModel, options,
                    null, null, false, false);
        } else {
            notifyEvent(DocumentEventTypes.VERSION_REMOVED, docModel, options,
                    null, null, false, false);
        }
    }

    /**
     * Implementation uses the fact that the lexicographic ordering of paths is
     * a refinement of the "contains" partial ordering.
     */
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

    public void save() throws ClientException {
        try {
            final Map<String, Serializable> options = new HashMap<String, Serializable>();
            getSession().save();
            notifyEvent(DocumentEventTypes.SESSION_SAVED, null, options, null,
                    null, true, false);
        } catch (DocumentException e) {
            throw new ClientException("Failed to save session", e);
        }
    }

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

            // add document context data to core event
            Map<String, Serializable> options = getContextMapEventInfo(docModel);

            // TODO make this configurable or put in other place
            final ScopedMap ctxData = docModel.getContextData();
            Boolean createSnapshot = (Boolean) ctxData.getScopedValue(
                    ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY);
            DocumentModel oldDoc = null;
            if (Boolean.TRUE.equals(createSnapshot)) {
                oldDoc = createDocumentSnapshot(docModel, doc, options, true);
                ctxData.putScopedValue(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, null);
                if (oldDoc != null) {
                    // pass that info to future events to avoid incrementing
                    // versions twice
                    options.put(VersioningDocument.DOCUMENT_WAS_SNAPSHOTTED,
                            Boolean.TRUE);
                }
            }

            if (!docModel.isImmutable()) {
                // version incrementing listeners mustn't dirty the doc,
                // so use an alternate DocumentModel for the event
                boolean dirty = doc.isDirty();
                DocumentModel tmpDocModel = readModel(doc, null);
                notifyEvent(DocumentEventTypes.INCREMENT_BEFORE_UPDATE,
                        tmpDocModel, options, null, null, true, true);
                // write potential number changes, reset old dirty flags
                writeModel(doc, tmpDocModel);
                doc.setDirty(dirty);

                // regular event, last chance to modify docModel
                notifyEvent(DocumentEventTypes.BEFORE_DOC_UPDATE, docModel,
                        options, null, null, true, true);
            }

            // actual save
            docModel = writeModel(doc, docModel);

            notifyEvent(DocumentEventTypes.DOCUMENT_UPDATED, docModel, options,
                    null, null, true, false);

            if (oldDoc != null) {
                notifyVersionChange(oldDoc, docModel, options);
            }

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to save document " + docModel, e);
        }
    }

    public boolean isDirty(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            return doc.isDirty();
        } catch (DocumentException e) {
            throw new ClientException("Failed to get dirty state on " + docRef,
                    e);
        }
    }

    /**
     * Creates a snapshot (version) for the given DocumentModel.
     * <p>
     * Passed options are propagated to the checked out event.
     *
     * @return the last version (that was just created)
     */
    private DocumentModel createDocumentSnapshot(DocumentModel docModel,
            Document doc, Map<String, Serializable> options, boolean pendingSave)
            throws ClientException {
        DocumentRef docRef = docModel.getRef();
        if (!isDirty(docRef) && getLastVersion(docRef) != null) {
            log.debug("Document not dirty and has a last version "
                    + "-> avoid creating a new version");
            return null;
        }

        // Do a checkin / checkout of the edited version
        VersionModel newVersion = new VersionModelImpl();
        String vlabel = generateVersionLabelFor(docRef);
        newVersion.setLabel(vlabel);

        checkIn(docRef, newVersion);
        log.debug("doc checked in " + docModel.getTitle());
        checkOut(docModel, doc, options, pendingSave);
        log.debug("doc checked out " + docModel.getTitle());

        return getDocumentWithVersion(docRef, newVersion);
    }

    public void saveDocuments(DocumentModel[] docModels) throws ClientException {
        // TODO: optimize this - avoid calling at each iteration saveDoc...
        for (DocumentModel docModel : docModels) {
            saveDocument(docModel);
        }
    }

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
            return readModel(headDocument, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get head document for "
                    + docRef, e);
        }
    }

    public VersionModel getLastVersion(DocumentRef docRef)
            throws ClientException {
        assert null != docRef;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);

            DocumentVersion version = doc.getLastVersion();
            if (version == null) {
                return null;
            }
            VersionModel versionModel = new VersionModelImpl();
            versionModel.setCreated(version.getCreated());
            versionModel.setDescription(version.getDescription());
            versionModel.setLabel(version.getLabel());
            return versionModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    public DocumentModel getLastDocumentVersion(DocumentRef docRef)
            throws ClientException {
        assert null != docRef;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);

            DocumentVersion version = doc.getLastVersion();
            if (version != null) {
                return readModel(version, null);
            }
            return null;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

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

    public List<DocumentModel> getVersions(DocumentRef docRef)
            throws ClientException {
        assert null != docRef;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);
            List<DocumentModel> versions = new ArrayList<DocumentModel>();
            DocumentVersionIterator versionIterator = doc.getVersions();

            while (versionIterator.hasNext()) {
                DocumentVersion docVersion = versionIterator.nextDocumentVersion();
                if (docVersion.getLabel() == null) {
                    // discard root default version
                    continue;
                }
                if (docVersion.getType() == null) {
                    throw new IllegalStateException(
                            "FAILED TO GET VERSIONS FOR" + docRef
                                    + " with path: " + doc.getPath());
                }
                DocumentModel versionModel = readModel(docVersion, null);
                versions.add(versionModel);
            }

            log.debug("Retrieved the versions of the document " + doc.getPath());

            return versions;

        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }
    }

    public List<VersionModel> getVersionsForDocument(DocumentRef docRef)
            throws ClientException {
        assert null != docRef;

        try {

            Document doc = resolveReference(docRef);
            checkPermission(doc, READ_VERSION);

            List<VersionModel> versions = new ArrayList<VersionModel>();
            DocumentVersionIterator versionIterator = doc.getVersions();

            while (versionIterator.hasNext()) {
                DocumentVersion docVersion = versionIterator.nextDocumentVersion();

                if (null == docVersion.getLabel()) {
                    // TODO: the root default version - discard
                    continue;
                }

                VersionModel versionModel = new VersionModelImpl();

                versionModel.setCreated(docVersion.getCreated());
                versionModel.setDescription(docVersion.getDescription());
                versionModel.setLabel(docVersion.getLabel());

                versions.add(versionModel);
            }

            log.debug("Retrieved the versions of the document " + doc.getPath());

            return versions;

        } catch (DocumentException e) {
            throw new ClientException("Failed to get versions for " + docRef, e);
        }

    }

    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        return restoreToVersion(docRef, version, false);
    }

    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version, boolean skipSnapshotCreation)
            throws ClientException {
        assert docRef != null;
        assert version != null;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_PROPERTIES);
            checkPermission(doc, WRITE_VERSION);

            DocumentModel docModel = readModel(doc, null);

            // we're about to overwrite the document, make sure it's archived
            if (!skipSnapshotCreation) {
                createDocumentSnapshot(docModel, doc, null, false);
            }

            final Map<String, Serializable> options = new HashMap<String, Serializable>();

            // FIXME: the fields are hardcoded. should be moved in versioning
            // component
            // HOW?
            final Long majorVer = doc.getLong("major_version");
            final Long minorVer = doc.getLong("minor_version");
            if (majorVer != null || minorVer != null) {
                options.put(
                        VersioningDocument.CURRENT_DOCUMENT_MAJOR_VERSION_KEY,
                        majorVer);
                options.put(
                        VersioningDocument.CURRENT_DOCUMENT_MINOR_VERSION_KEY,
                        minorVer);
            }
            // add the uuid of the version being restored
            Document docVersion = doc.getVersion(version.getLabel());
            String versionUUID = docVersion.getUUID();
            options.put(VersioningDocument.RESTORED_VERSION_UUID_KEY,
                    versionUUID);

            notifyEvent(DocumentEventTypes.BEFORE_DOC_RESTORE, docModel,
                    options, null, null, true, true);
            writeModel(doc, docModel);

            doc.restore(version.getLabel());
            // restore gives us a checked in document, so do a checkout
            doc.checkOut();

            // re-read doc model after restoration
            docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.DOCUMENT_RESTORED, docModel,
                    options, null, null, true, false);
            docModel = writeModel(doc, docModel);

            log.debug("Document restored to version:" + version.getLabel());
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to restore document " + docRef, e);
        }

    }

    public void checkIn(DocumentRef docRef, VersionModel version)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            // TODO: add a new permission names CHECKIN and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);
            checkPermission(doc, WRITE_VERSION);

            DocumentModel docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, null,
                    null, null, true, true);
            writeModel(doc, docModel);

            String description = version.getDescription();
            if (description != null) {
                doc.checkIn(version.getLabel(), description);
            } else {
                doc.checkIn(version.getLabel());
            }

            Document versionDocument = doc.getVersion(version.getLabel());
            DocumentModel versionModel = readModel(versionDocument, null);
            Map<String, Serializable> options = getContextMapEventInfo(docModel);

            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, versionModel,
                    options, null, null, true, false);

            // FIXME: the fields are hardcoded. should be moved in versioning
            // component
            if (doc.getType().hasSchema("uid")) {
                final Long majorVer = doc.getLong("major_version");
                final Long minorVer = doc.getLong("minor_version");
                String versionComment = majorVer + "." + minorVer;
                options.put("comment", versionComment);
            }

            notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDIN, docModel,
                    options, null, null, true, false);
            writeModel(versionDocument, versionModel);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check in document " + docRef,
                    e);
        }
    }

    public void checkOut(DocumentRef docRef) throws ClientException {
        Document doc;
        try {
            doc = resolveReference(docRef);
            // TODO: add a new permission names CHECKOUT and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document " + docRef,
                    e);
        }
        checkOut(readModel(doc, null), doc, null, false);
    }

    protected void checkOut(DocumentModel docModel, Document doc,
            Map<String, Serializable> options, boolean pendingSave)
            throws ClientException {
        try {
            if (doc == null) {
                doc = resolveReference(docModel.getRef());
            }
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel,
                    options, null, null, true, true);
            doc.checkOut();

            // notify listeners to increment the version number;
            DocumentModel eventDocModel;
            if (pendingSave) {
                // save is pending and will want an untouched DocumentModel. so
                // use a pristined DocumentModel to avoid interfering with it
                eventDocModel = readModel(doc, null);
            } else {
                eventDocModel = docModel;
            }
            notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, eventDocModel,
                    options, null, null, true, false);
            // write version number changes
            writeModel(doc, eventDocModel);
            // and reset the dirty flag
            doc.setDirty(false);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document "
                    + docModel.getRef(), e);
        }
    }

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

    public DocumentModel getVersion(String versionableId,
            VersionModel versionModel) throws ClientException {
        if (!isAdministrator()) {
            throw new DocumentSecurityException(
                    "Only Administrator can fetch versions directly");
        }
        String label = versionModel.getLabel();
        try {
            Document doc = getSession().getVersion(versionableId, versionModel);
            return doc == null ? null : readModel(doc, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get version " + label
                    + " for " + versionableId, e);
        }
    }

    public DocumentModel getDocumentWithVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        assert docRef != null;
        assert version != null;

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
            return readModel(doc, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get version for " + docRef, e);
        }
    }

    /**
     * Creates a generic proxy to a document.
     * <p>
     * The document may be a version, or a working copy (live document) in which
     * case the proxy will be a "shortcut".
     */
    public DocumentModel createProxy(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            Document folder = resolveReference(folderRef);
            checkPermission(doc, READ);
            checkPermission(folder, ADD_CHILDREN);

            // create the new proxy
            Document proxy = getSession().createProxy(doc, folder);
            DocumentModel proxyModel = readModel(proxy, null);

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, proxyModel,
                    options, null, null, true, false);
            notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                    proxyModel, options, null, null, true, false);
            DocumentModel folderModel = readModel(folder, null);
            notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                    folderModel, options, null, null, true, false);
            return proxyModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to create proxy for doc: "
                    + docRef, e);
        }
    }

    public DocumentModel createProxy(DocumentRef parentRef, DocumentRef docRef,
            VersionModel version, boolean overwriteExistingProxy)
            throws ClientException {
        assert null != parentRef;
        assert null != docRef;
        assert null != version;

        try {
            Document doc = resolveReference(docRef);
            Document section = resolveReference(parentRef);
            checkPermission(doc, READ);
            checkPermission(section, ADD_CHILDREN);

            DocumentModel proxyModel = null;
            Map<String, Serializable> options = new HashMap<String, Serializable>();
            String vlabel = version.getLabel();

            if (overwriteExistingProxy) {
                Document target = getSession().getVersion(doc.getUUID(),
                        version);
                if (target == null) {
                    throw new ClientException("Document " + docRef
                            + " has no version " + vlabel);
                }
                proxyModel = updateExistingProxies(doc, section, target);
                // proxyModel is null is update fails
                if (proxyModel != null) {
                    // notify for proxy updates
                    notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_UPDATED,
                            proxyModel, options, null, null, true, false);
                } else {
                    List<String> removedProxyIds = Collections.emptyList();
                    removedProxyIds = removeExistingProxies(doc, section);
                    options.put(CoreEventConstants.REPLACED_PROXY_IDS,
                            (Serializable) removedProxyIds);
                }
            }

            if (proxyModel == null) {
                // create the new proxy
                Document proxy = getSession().createProxyForVersion(section,
                        doc, vlabel);
                log.debug("Created proxy for version " + vlabel
                        + " of the document " + doc.getPath());
                // notify for reindexing
                proxyModel = readModel(proxy, null);

                // notify for document creation (proxy)
                notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, proxyModel,
                        options, null, null, true, false);
            }

            notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                    proxyModel, options, null, null, true, false);

            DocumentModel sectionModel = readModel(section, null);
            notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                    sectionModel, options, null, null, true, false);

            return proxyModel;

        } catch (DocumentException e) {
            throw new ClientException("Failed to create proxy for doc "
                    + docRef + " , version: " + version.getLabel(), e);
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
     * Update the proxy while republishing.
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
                        return readModel(proxy, null);
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            log.error("Cannot update proxy, try to remove");
        }
        return null;
    }

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
                    docs.add(readModel(child, null));
                }
            }
            return docs;
        } catch (DocumentException e) {
            throw new ClientException(
                    "Failed to get children for " + folderRef, e);
        }
    }

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
                    if (target instanceof DocumentVersion) {
                        versions.add(((DocumentVersion) target).getLabel());
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

    public List<String> getAvailableSecurityPermissions()
            throws ClientException {
        // XXX: add security check?
        return Arrays.asList(getSecurityService().getPermissionProvider().getPermissions());
    }

    public DataModel getDataModel(DocumentRef docRef, String schema)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            Schema docSchema = doc.getType().getSchema(schema);
            assert docSchema != null;
            return DocumentModelFactory.exportSchema(
                    doc.getSession().getUserSessionId(), docRef, doc, docSchema);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get data model for " + docRef
                    + ':' + schema, e);
        }
    }

    public Object getDataModelField(DocumentRef docRef, String schema,
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

    public Object[] getDataModelFields(DocumentRef docRef, String schema,
            String[] fields) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            Schema docSchema = doc.getType().getSchema(schema);
            String prefix = docSchema.getNamespace().prefix;
            if (prefix != null && prefix.length() > 0) {
                prefix += ':';
            }
            // prefix is not used for the moment
            // else {
            // prefix = null;
            // }
            Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                if (prefix != null) {
                    values[i] = doc.getPropertyValue(fields[i]);
                }
            }
            return values;
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document " + docRef,
                    e);
        }
    }

    public SerializableInputStream getContentData(String key)
            throws ClientException {
        try {
            InputStream in = getSession().getDataStream(key);
            return new SerializableInputStream(in);
        } catch (Exception e) {
            throw new ClientException("Failed to get data stream for " + key, e);
        }
    }

    public String getStreamURI(String blobPropertyId) throws ClientException {
        String uri;
        try {
            InputStream in = getContentData(blobPropertyId);
            StreamManager sm = Framework.getLocalService(StreamManager.class);
            if (sm == null) {
                throw new ClientException("No Streaming Service was registered");
            }
            uri = sm.addStream(new InputStreamSource(in));
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("Failed to register blob stream: "
                    + blobPropertyId, e);
        }
        return uri;
    }

    public String getCurrentLifeCycleState(DocumentRef docRef)
            throws ClientException {
        String currentLifeCycleState;
        try {
            Document doc = resolveReference(docRef);

            checkPermission(doc, READ_LIFE_CYCLE);
            currentLifeCycleState = doc.getCurrentLifeCycleState();
        } catch (LifeCycleException e) {
            ClientException ce = new ClientException(
                    "Failed to get life cycle " + docRef, e);
            ce.fillInStackTrace();
            throw ce;
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return currentLifeCycleState;
    }

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

    public boolean followTransition(DocumentRef docRef, String transition)
            throws ClientException {
        boolean operationResult;
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_LIFE_CYCLE);
            String formerStateName = doc.getCurrentLifeCycleState();
            operationResult = doc.followTransition(transition);

            if (operationResult) {
                // Construct a map holding meta information about the event.
                Map<String, Serializable> options = new HashMap<String, Serializable>();
                options.put(
                        org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_FROM,
                        formerStateName);
                options.put(
                        org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TO,
                        doc.getCurrentLifeCycleState());
                options.put(
                        org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION,
                        transition);
                DocumentModel docModel = readModel(doc, null);
                notifyEvent(
                        org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT,
                        docModel, options,
                        DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY,
                        null, true, false);
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
        return operationResult;
    }

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

    public Object[] getDataModelsFieldUp(DocumentRef docRef, String schema,
            String field) throws ClientException {

        final DocumentRef[] parentRefs = getParentDocumentRefs(docRef);
        final DocumentRef[] allRefs = new DocumentRef[parentRefs.length + 1];
        allRefs[0] = docRef;
        System.arraycopy(parentRefs, 0, allRefs, 1, parentRefs.length);

        return getDataModelsField(allRefs, schema, field);
    }

    public String getLock(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            return doc.getLock();
        } catch (DocumentException e) {
            throw new ClientException("Failed to get lock info on " + docRef, e);
        }
    }

    public void setLock(DocumentRef docRef, String key) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            // TODO: add a new permission named LOCK and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);
            doc.setLock(key);
            DocumentModel docModel = readModel(doc, null);
            Map<String, Serializable> options = new HashMap<String, Serializable>();
            notifyEvent(DocumentEventTypes.DOCUMENT_LOCKED, docModel, options,
                    null, null, true, false);
        } catch (DocumentException e) {
            throw new ClientException("Failed to set lock on " + docRef, e);
        }
    }

    public String unlock(DocumentRef docRef) throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            if (!doc.isLocked()) {
                return null;
            }
            String[] lockDetails = doc.getLock().split(":");
            if (lockDetails == null) {
                return null;
            }
            String username = getPrincipal().getName();

            if (hasPermission(docRef, UNLOCK)
                    || lockDetails[0].equals(username)) {
                String lockKey = doc.unlock();
                DocumentModel docModel = readModel(doc, null);
                Map<String, Serializable> options = new HashMap<String, Serializable>();
                notifyEvent(DocumentEventTypes.DOCUMENT_UNLOCKED, docModel,
                        options, null, null, true, false);
                return lockKey;
            }
            throw new ClientException(
                    "The caller has no privileges to unlock the document");
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
        if (SecurityConstants.SYSTEM_USERNAME.equals(principal.getName())) {
            return true;
        }
        if (principal instanceof NuxeoPrincipal) {
            return ((NuxeoPrincipal) principal).isAdministrator();
        }
        return false;
    }

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
        userEntry.addPrivilege(SecurityConstants.READ, true, false);

        acp.setRules(new UserEntry[] { userEntry });

        setACP(rootDocument.getRef(), acp, false);
    }

    public void destroy() {
        log.debug("Destroying core session ...");
        try {
            disconnect();
        } catch (Exception e) {
            log.error("Failed to destroy core session", e);
        }
    }

    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException {
        return publishDocument(docToPublish, section, true);
    }

    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section, boolean overwriteExistingProxy)
            throws ClientException {

        DocumentRef docRef = docToPublish.getRef();
        DocumentRef sectionRef = section.getRef();
        if (docToPublish.isProxy()) {
            try {
                Document doc = resolveReference(docRef);
                Document sec = resolveReference(sectionRef);
                checkPermission(doc, READ);
                checkPermission(sec, ADD_CHILDREN);

                List<String> removedProxyIds = Collections.emptyList();
                if (overwriteExistingProxy) {
                    removedProxyIds = removeExistingProxies(doc, sec);
                }

                // publishing a proxy is just a copy
                // TODO copy also copies security. just recreate a proxy
                DocumentModel newDocument = copy(docRef, sectionRef,
                        docToPublish.getName());

                Map<String, Serializable> options = new HashMap<String, Serializable>();
                options.put(CoreEventConstants.REPLACED_PROXY_IDS,
                        (Serializable) removedProxyIds);
                notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                        newDocument, options, null, null, true, false);

                notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                        section, options, null, null, true, false);

                return newDocument;
            } catch (DocumentException e) {
                throw new ClientException(e);
            }
        } else {
            // snapshot the document
            createDocumentSnapshot(docToPublish, null, null, false);
            VersionModel version = getLastVersion(docRef);
            if (version == null) {
                throw new ClientException("Cannot create proxy: "
                        + "there is no version to point to");
            }
            DocumentModel newProxy = createProxy(sectionRef, docRef, version,
                    overwriteExistingProxy);
            return newProxy;
        }
    }

    public VersionModel isPublished(DocumentModel document,
            DocumentModel section) {
        // FIXME: this is very useful API
        return null;
    }

    public String getSuperParentType(DocumentModel doc) throws ClientException {
        DocumentModel superSpace = getSuperSpace(doc);
        if (superSpace == null) {
            return null;
        } else {
            return superSpace.getType();
        }
    }

    public DocumentModel getSuperSpace(DocumentModel doc)
            throws ClientException {
        if (doc == null) {
            throw new ClientException("getSuperSpace: document is null");
        }
        if (doc.hasFacet("SuperSpace")) {
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
                return readModel(doc, null);
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
            return readModel(parentDoc, null);
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }

    public List<SecuritySummaryEntry> getSecuritySummary(
            DocumentModel docModel, Boolean includeParents)
            throws ClientException {
        if (docModel == null) {
            docModel = getRootDocument();
        }
        Document doc;
        try {
            doc = resolveReference(docModel.getRef());

        } catch (DocumentException e) {
            throw new ClientException("Failed to get document "
                    + docModel.getRef().toString(), e);
        }

        return getSecurityService().getSecuritySummary(doc, includeParents);
    }

    public String getRepositoryName() {
        return repositoryName;
    }

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

    public <T extends Serializable> void setDocumentSystemProp(DocumentRef ref,
            String systemProperty, T value) throws ClientException,
            DocumentException {
        Document doc;
        try {
            doc = resolveReference(ref);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get document " + ref, e);
        }
        doc.setSystemProp(systemProperty, value);
    }

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
            DocumentModel docModel = readModel(doc, null);
            String comment = src;
            options.put(CoreEventConstants.REORDERED_CHILD, src);
            notifyEvent(DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED,
                    docModel, options, null, comment, true, false);

        } catch (DocumentException e) {
            throw new ClientException("Failed to resolve documents: " + src
                    + ", " + dest, e);
        }
    }

    public <T> T run(Operation<T> op) throws ClientException {
        return run(op, null);
    }

    public <T> T run(Operation<T> op, ProgressMonitor monitor)
            throws ClientException {
        // double s = System.currentTimeMillis();
        T result = op.run(this, this, monitor);
        // System.out.println(">>>>> OPERATION "+op.getName()+" took: "+
        // ((System.currentTimeMillis()-s)/1000));
        Status status = op.getStatus();
        if (status.isOk()) {
            return result;
        } else {
            Throwable t = status.getException();
            if (t != null) {
                if (t instanceof ClientException) {
                    throw (ClientException) t;
                } else {
                    throw new ClientException(status.getMessage(), t);
                }
            } else {
                String msg = status.getMessage();
                if (msg == null) {
                    msg = "Unknown Error";
                }
                throw new ClientException(msg);
            }
        }
    }

    /**
     * This method is for compatibility reasons to notify an operation start.
     * Operations must be reworked to use the new event model. In order for
     * operation notification to work the event compatibility bundle must be
     * deployed.
     *
     * @see org.nuxeo.ecm.core.event.compat.CompatibilityListener in
     *      nuxeo-core-event-compat
     */
    public void startOperation(Operation<?> operation) {
        EventContextImpl ctx = new EventContextImpl(this, getPrincipal(),
                operation);
        Event event = ctx.newEvent("!OPERATION_START!");
        try {
            fireEvent(event);
        } catch (ClientException e) {
            log.error("Failed to notify operation start for: " + operation, e);
        }
        // old code was:
        // CoreEventListenerService service =
        // NXCore.getCoreEventListenerService();
        // service.fireOperationStarted(operation);
    }

    /**
     * This method is for compatibility reasons to notify an operation end.
     * Operations must be reworked to use the new event model. In order for
     * operation notification to work the event compatibility bundle must be
     * deployed.
     *
     * @see org.nuxeo.ecm.core.event.compat.CompatibilityListener in
     *      nuxeo-core-event-compat
     */
    public void endOperation(Operation<?> operation) {
        EventContextImpl ctx = new EventContextImpl(this, getPrincipal(),
                operation);
        Event event = ctx.newEvent("!OPERATION_END!");
        try {
            fireEvent(event);
        } catch (ClientException e) {
            log.error("Failed to notify operation end for: " + operation, e);
        }
        // old code was:
        // CoreEventListenerService service =
        // NXCore.getCoreEventListenerService();
        // service.fireOperationTerminated(operation);
    }

    public Object[] refreshDocument(DocumentRef ref, int refreshFlags,
            String[] schemas) throws ClientException {
        Object[] result = new Object[6];

        try {

            Document doc = resolveReference(ref);
            if (doc == null) {
                throw new ClientException("No Such Document: " + ref);
            }

            boolean readPermChecked = false;
            if ((refreshFlags & DocumentModel.REFRESH_PREFETCH) != 0) {
                if (!readPermChecked) {
                    checkPermission(doc, READ);
                    readPermChecked = true;
                }
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
                            prefetch.put(field.getDeclaringType().getName()
                                    + '.' + field.getName().getLocalName(),
                                    value == null ? Null.VALUE
                                            : (Serializable) value);
                        }
                        result[0] = prefetch;
                    }
                }
            }

            if ((refreshFlags & DocumentModel.REFRESH_LOCK) != 0) {
                if (!readPermChecked) {
                    checkPermission(doc, READ);
                    readPermChecked = true;
                }
                result[1] = doc.getLock();
            }

            if ((refreshFlags & DocumentModel.REFRESH_LIFE_CYCLE) != 0) {
                checkPermission(doc, READ_LIFE_CYCLE);
                result[2] = doc.getCurrentLifeCycleState();
                result[3] = doc.getLifeCyclePolicy();
            }

            if ((refreshFlags & DocumentModel.REFRESH_ACP) != 0) {
                checkPermission(doc, READ_SECURITY);
                result[4] = getSession().getSecurityManager().getMergedACP(doc);
            }

            if ((refreshFlags & (DocumentModel.REFRESH_CONTENT)) != 0) {
                if (!readPermChecked) {
                    checkPermission(doc, READ);
                    readPermChecked = true;
                }
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
                result[5] = parts;
            }

        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("Failed to get refresh data", e);
        }
        return result;

    }

    public String[] getPermissionsToCheck(String permission) {
        return getSecurityService().getPermissionsToCheck(permission);
    }

}
