/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.io.IOException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocsQueryProviderDef;
import org.nuxeo.ecm.core.api.impl.DocumentModelIteratorImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.ProgressMonitor;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.lifecycle.LifeCycleEventTypes;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.DocumentVersionProxy;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.PathComparator;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.FilterableQuery;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.NXSchema;
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
        SecurityConstants, Serializable {

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

    protected ProgressMonitor monitor = DefaultProgressMonitor.INSTANCE;

    /**
     * Used to check permissions.
     */
    private transient SecurityService __securityService;

    protected SecurityService getSecurityService() {
        if (__securityService == null) {
            __securityService = NXCore.getSecurityService();
        }
        return __securityService;
    }

    /**
     * Used to resolve core documents based on session.
     */
    protected final DocumentResolver documentResolver = new DocumentResolver();

    private String sessionId;

    /**
     * Gets the current session based on the client session id.
     *
     * @return the repository session
     * @throws ClientException
     */
    protected abstract Session getSession() throws ClientException;

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
        // register this session locally -> this way document models can retirve
        // their session
        // on the server side
        CoreInstance.getInstance().registerSession(sessionId, this);

        // <------------ begin repository initialization
        // we need to intialize the repository if this is the first time it is
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
                                "system"));
                        handler.initializeRepository(this);
                        try {
                            session.save();
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
     * <li> A is the repository name (uniquely identify the repository in the
     * system)
     * <li>B is the time of the session creation in milliseconds
     * </ul>
     *
     * @return
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
     * @throws ClientException
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
        if (!hasPermission(doc, permission)) {
            throw new DocumentSecurityException("Privilege '" + permission
                    + "' is not granted to '" + getPrincipal().getName() + "'");
        }
    }

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Object> options, String category, String comment,
            boolean withLifeCycle) {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (options == null) {
            options = new HashMap<String, Object>();
        }

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, repositoryName);

        // Document life cycle
        if (source != null && withLifeCycle) {
            String currentLifeCycleState = null;
            try {
                currentLifeCycleState = source.getCurrentLifeCycleState();
            } catch (ClientException err) {
                // FIXME no lifecycle -- this shouldn't generated an
                // exception (and ClientException logs the spurious error)
            }
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
        }
        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID, sessionId);

        CoreEvent coreEvent = new CoreEventImpl(eventId, source, options,
                getPrincipal(), category, comment);

        CoreEventListenerService service = NXCore.getCoreEventListenerService();

        if (service != null) {
            service.notifyEventListeners(coreEvent);
        } else {
            log.debug("No CoreEventListenerService, cannot notify event "
                    + eventId);
        }
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
     * @throws ClientException
     */
    protected DocumentModel readModel(Document doc, String[] schemas)
            throws ClientException {
        try {
            return DocumentModelFactory.createDocumentModel(doc, schemas);
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document model", e);
        }
    }

    /**
     * Writes the model as modified by the used in the corresponding document.
     *
     * @param doc the document where to write the changes
     * @param docModel the model containing the changes
     * @returns the newly read document model.
     * @throws DocumentException
     */
    protected DocumentModel writeModel_OLD(Document doc, DocumentModel docModel)
            throws DocumentException, ClientException {
        // get loaded data models
        boolean changed = false;
        Collection<DataModel> dataModels = docModel.getDataModelsCollection();
        for (DataModel dataModel : dataModels) {
            if (dataModel.isDirty()) {
                Collection<String> fields = dataModel.getDirtyFields();
                for (String field : fields) {
                    Object data = dataModel.getData(field);
                    doc.setPropertyValue(field, data);
                    changed = true;
                }
            }
        }

        if (changed) {
            doc.setDirty(true);
        }
        DocumentModel newModel = readModel(doc, null);
        newModel.copyContextData(docModel);
        return newModel;
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
                    null, null, true);

            Document doc = getSession().copy(srcDoc, dstDoc, name);

            Map<String, Object> options = new HashMap<String, Object>();

            // notify document created by copy
            DocumentModel docModel = readModel(doc, null);
            options.put(CoreEventConstants.DOCUMENT, doc);
            String comment = srcDoc.getRepository().getName() + ':'
                    + src.toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel,
                    options, null, comment, true);
            docModel = writeModel(doc, docModel);

            // notify document copied
            comment = doc.getRepository().getName() + ':'
                    + docModel.getRef().toString();
            options.put(CoreEventConstants.DOCUMENT, srcDoc);
            notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel,
                    options, null, comment, true);

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
                    null, null, true);
            docModel = createDocument(docModel);
            Document doc = resolveReference(docModel.getRef());

            Map<String, Object> options = new HashMap<String, Object>();
            // notify document created by copy
            options.put(CoreEventConstants.DOCUMENT, doc);
            String comment = srcDoc.getRepository().getName() + ':'
                    + src.toString();
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY, docModel,
                    options, null, comment, true);

            // notify document copied
            comment = doc.getRepository().getName() + ':'
                    + docModel.getRef().toString();
            options.put(CoreEventConstants.DOCUMENT, srcDoc);
            notifyEvent(DocumentEventTypes.DOCUMENT_DUPLICATED, srcDocModel,
                    options, null, comment, true);

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
                    null, null, true);

            String comment = srcDoc.getRepository().getName() + ':'
                    + srcDoc.getParent().getUUID();

            if (name == null) {
                name = srcDoc.getName();
            }
            name = generateDocumentName(dstDoc, name);
            Document doc = getSession().move(srcDoc, dstDoc, name);

            // notify document moved
            DocumentModel docModel = readModel(doc, null);
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(CoreEventConstants.DOCUMENT, doc);
            options.put(CoreEventConstants.PARENT_PATH,
                    srcDocModel.getParentRef());
            notifyEvent(DocumentEventTypes.DOCUMENT_MOVED, docModel, options,
                    null, comment, true);

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

            Map<String, Object> options = new HashMap<String, Object>();
            options.put(CoreEventConstants.DOCUMENT, doc);
            options.put(CoreEventConstants.OLD_ACP, docModel.getACP().clone());
            options.put(CoreEventConstants.NEW_ACP, newAcp.clone());

            notifyEvent(DocumentEventTypes.BEFORE_DOC_SECU_UPDATE, docModel,
                    options, null, null, true);
            getSession().getSecurityManager().setACP(doc, newAcp, overwrite);
            docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.DOCUMENT_SECURITY_UPDATED, docModel,
                    options, null, null, true);
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
            Map<String, Object> options) throws ClientException {
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
                options = new HashMap<String, Object>();
            }
            // do not forward this event on the JMS Bus
            options.put("BLOCK_JMS_PRODUCING", true);
            notifyEvent(DocumentEventTypes.EMPTY_DOCUMENTMODEL_CREATED,
                    docModel, options, null, null, false);
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document model", e);
        }
    }

    public DocumentModel createDocumentModel(String typeName)
            throws ClientException {
        Map<String, Object> options = new HashMap<String, Object>();
        return createDocumentModelFromTypeName(typeName, options);
    }

    public DocumentModel createDocumentModel(String parentPath, String id,
            String typeName) throws ClientException {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(CoreEventConstants.PARENT_PATH, parentPath);
        options.put(CoreEventConstants.DOCUMENT_MODEL_ID, id);
        DocumentModel model = createDocumentModelFromTypeName(typeName, options);
        model.setPathInfo(parentPath, id);
        return model;
    }

    public DocumentModel createDocumentModel(String typeName,
            Map<String, Object> options) throws ClientException {
        return createDocumentModelFromTypeName(typeName, options);
    }

    public DocumentModel createDocument(DocumentModel docModel)
            throws ClientException {
        String name = docModel.getName();
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
            notifyEvent(DocumentEventTypes.ABOUT_TO_CREATE, docModel, null,
                    null, null, false); // no lifecycle yet
            name = generateDocumentName(folder, name);
            Document doc = folder.addChild(name, typeName);

            // init document life cycle
            LifeCycleService service = NXCore.getLifeCycleService();
            if (service != null) {
                try {
                    service.initialize(doc);
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
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(CoreEventConstants.DOCUMENT, doc);
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, docModel, options,
                    null, null, true);
            docModel = writeModel(doc, docModel);

            // final Map<String, Object> ancestorOptions = new HashMap<String,
            // Object>();
            // ancestorOptions.put("newDocument", docModel);
            // notifyAncestors(doc,
            // DocumentEventTypes.CONTENT_SUBDOCUMENT_CREATED,
            // ancestorOptions);

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to create document " + name, e);
        }
    }

    /**
     * Generate a non-null unique name within given parent's children.
     * <p>
     * If name is null, a name is generated. If name is already used, a random
     * suffix is appended to it.
     *
     * @param parent
     * @param name
     * @return a unique name within given parent's children
     */
    public String generateDocumentName(Document parent, String name)
            throws DocumentException {
        if (name == null || name.length() == 0) {
            name = IdUtils.generateStringId();
        }
        if (parent.hasChild(name)) {
            name = name + '.' + String.valueOf(System.currentTimeMillis());
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
        DocumentModelList list = new DocumentModelListImpl(docs);
        return list;
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

    public DocumentModelList query(String query, Filter filter, long limit,
            long offset, boolean countTotal) throws ClientException {
        SecurityService securityService = getSecurityService();
        Principal principal = getPrincipal();
        try {
            Query compiledQuery = getSession().createQuery(query,
                    Query.Type.NXQL);
            if (limit != 0) {
                compiledQuery.setLimit(limit);
                compiledQuery.setOffset(offset);
            }
            QueryResult results;
            boolean postFilterPermission;
            boolean postFilterFilter;
            boolean postFilterPolicies;
            String permission = BROWSE;
            if (compiledQuery instanceof FilterableQuery) {
                postFilterPermission = false;
                postFilterPolicies = !securityService.arePoliciesExpressibleInQuery();
                postFilterFilter = filter != null
                        && !(filter instanceof FacetFilter);
                String[] principals;
                if (principal.getName().equals("system")) {
                    principals = null; // means: no security check needed
                } else {
                    principals = SecurityService.getPrincipalsToCheck(principal);
                }
                String[] permissions = securityService.getPermissionsToCheck(permission);
                QueryFilter queryFilter = new QueryFilter(principals,
                        permissions,
                        filter instanceof FacetFilter ? (FacetFilter) filter
                                : null,
                        securityService.getPoliciesQueryTransformers());
                results = ((FilterableQuery) compiledQuery).execute(
                        queryFilter, countTotal);
            } else {
                postFilterPermission = true;
                postFilterPolicies = securityService.arePoliciesRestrictingPermission(permission);
                postFilterFilter = filter != null;
                results = compiledQuery.execute(countTotal);
            }
            if (!postFilterPermission && !postFilterFilter
                    && !postFilterPolicies) {
                // the backend has done all the needed filtering
                return results.getDocumentModels();
            }
            // post-filter the results if the query couldn't do it
            final DocumentModelList docs = new DocumentModelListImpl(
                    Collections.<DocumentModel> emptyList(),
                    results.getTotalSize());
            int nbr = 0;
            for (DocumentModel model : results.getDocumentModels()) {
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
                docs.add(model);
                nbr++;
                if (limit != 0 && nbr >= limit) {
                    break;
                }
            }
            return docs;
        } catch (Exception e) {
            log.error("failed to execute query", e);
            final String causeErrMsg = tryToExtractMeaningfulErrMsg(e);
            throw new ClientException(
                    "Failed to get the root document. Cause: " + causeErrMsg, e);
        }
    }

    @Deprecated
    public DocumentModelIterator queryIt(String query, Filter filter, int max)
            throws ClientException {
        DocsQueryProviderDef def = new DocsQueryProviderDef(
                DocsQueryProviderDef.DefType.TYPE_QUERY);
        def.setQuery(query);
        return new DocumentModelIteratorImpl(this, 15, def, null, BROWSE,
                filter);
    }

    /**
     * @param t
     * @return
     */
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
            throw new ClientException("Failed to fetch document " + docRef +
                    " before removal", e);
        }
    }

    protected void removeDocument(Document doc) throws ClientException {
        try {
            if (!canRemoveDocument(doc)) {
                throw new DocumentSecurityException("Permission denied: cannot remove document " + doc.getUUID());
            }
            removeNotifyOneDoc(doc);
        } catch (DocumentException e) {
            try {
                throw new ClientException("Failed to remove document " +
                        doc.getUUID(), e);
            } catch (DocumentException e2) {
                log.error("Failed to remove doc", e);
                throw new ClientException("Failed to remove and " +
                        "even to get UUID " + doc.toString());
            }
        }
    }

    protected void removeNotifyOneDoc(Document doc) throws ClientException,
            DocumentException {
        // XXX notify with options if needed
        DocumentModel docModel = readModel(doc, null);
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(CoreEventConstants.DOCUMENT, doc);
        if (docModel != null) {
            options.put("docTitle", docModel.getTitle());
        }
        // for now we don't notify for versions themselves, as they have the
        // same path as the working document
        if (!doc.isVersion()) {
            notifyEvent(DocumentEventTypes.ABOUT_TO_REMOVE, docModel, options,
                    null, null, true);
            CoreService coreService = Framework.getLocalService(CoreService.class);
            coreService.getVersionRemovalPolicy().removeVersions(getSession(),
                    doc, this);
        }
        doc.remove();
        if (!doc.isVersion()) {
            options.remove(CoreEventConstants.DOCUMENT);
            notifyEvent(DocumentEventTypes.DOCUMENT_REMOVED, docModel, options,
                    null, null, false);
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
                if (i == 0 || !paths[i].startsWith(latestRemoved)) {
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
            final Map<String, Object> options = new HashMap<String, Object>();
            getSession().save();
            notifyEvent(DocumentEventTypes.SESSION_SAVED, null, options, null,
                    null, true);
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

            // add Document context data to core event
            final ScopedMap ctxData = docModel.getContextData();
            final Map<String, Object> options = new HashMap<String, Object>();
            options.putAll(ctxData.getDefaultScopeValues());
            options.putAll(ctxData.getScopeValues(ScopeType.REQUEST));

            // TODO make this configurable or put in other place
            Boolean createSnapshot = (Boolean) ctxData.getScopedValue(
                    ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY);
            DocumentModel oldDoc = null;
            if (createSnapshot != null && createSnapshot) {
                // FIXME: remove this - pass the flag as an arg or create
                // anotehr method!!!
                save(); // creating versions failes if the documents involved
                // are nmot saved
                oldDoc = createDocumentSnapshot(docModel);
                // ok, now it is consumed
                ctxData.putScopedValue(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, null);
            }

            options.put(CoreEventConstants.DOCUMENT, doc);
            notifyEvent(DocumentEventTypes.BEFORE_DOC_UPDATE, docModel,
                    options, null, null, true);

            // modify document - DO NOT write doc twice -> since internal
            // document listeners was removed
            // docModel = writeModel(doc, docModel);

            docModel = writeModel(doc, docModel);

            notifyEvent(DocumentEventTypes.DOCUMENT_UPDATED, docModel, options,
                    null, null, true);

            // Send events for parents that their content got modified
            // final Map<String, Object> ancestorOptions = new HashMap<String,
            // Object>();
            // ancestorOptions.put("newDocument", docModel);
            // notifyAncestors(doc,
            // DocumentEventTypes.CONTENT_SUBDOCUMENT_UPDATED,
            // ancestorOptions);

            // NXGED-395: v. change notification to be sent after docModel
            // changes
            if (oldDoc != null) {
                VersioningChangeNotifier.notifyVersionChange(oldDoc, docModel,
                        options);
            }

            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to save document " + docModel, e);
        }
    }

    /*
     * Recursive method that sends a given eventName for all parents of the
     * document that was modified.
     *
     * @param doc @throws DocumentException
     */
    /*
     * private void notifyAncestors(Document doc, String eventName, Map<String,
     * Object> options) throws DocumentException, ClientException { if (doc !=
     * null) { log.debug("Document notified : " + doc.getName()); Document
     * parent = doc.getParent(); if (parent != null) { DocumentModel parentModel =
     * readModel(parent, null); if (options == null) { options = new HashMap<String,
     * Object>(); } options.put(CoreEventConstants.DOCUMENT, parent);
     * notifyEvent(eventName, parentModel, options, null, null, true);
     * notifyAncestors(parent, eventName, options); } } }
     */
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
     * @deprecated instead use saveDocument with
     *             VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY in contextData
     */
    @Deprecated
    public DocumentModel saveDocumentAsNewVersion(DocumentModel doc)
            throws ClientException {
        createDocumentSnapshot(doc);

        // save the changed data to the current working version
        final DocumentModel savedDoc = saveDocument(doc);
        // FIXME BS: why save is done explicitely here?
        save();

        return savedDoc;
    }

    /**
     * Creates a snapshot (version) for the given DocumentModel.
     *
     * @param doc
     * @return the last version (that was just created)
     * @throws ClientException
     */
    private DocumentModel createDocumentSnapshot(DocumentModel doc)
            throws ClientException {
        if (!isDirty(doc.getRef())) {
            log.debug("Document not dirty -> avoid creating a new version");
            return null;
        }

        // Do a checkin / checkout of the edited version
        DocumentRef docRef = doc.getRef();
        VersionModel newVersion = new VersionModelImpl();
        String vlabel = generateVersionLabelFor(docRef);
        newVersion.setLabel(vlabel);

        // if session is not saved (i.e. there are pending changes), checkin
        // might fail
        // moved the decision to save in JCRDocument.checkin()
        // save();

        checkIn(docRef, newVersion);
        log.debug("doc checked in " + doc.getTitle());
        checkOut(docRef);
        log.debug("doc checked out " + doc.getTitle());

        // send notifications about new version
        // TODO we need to make it clearer the fact that this event is
        // about new version snapshot and not about versioning fields change
        // (i.e. major, minor version increment)
        DocumentModel oldDoc = getDocumentWithVersion(docRef, newVersion);
        // VersioningChangeNotifier.notifyVersionChange(oldDoc, doc);

        return oldDoc;
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
            checkPermission(doc, VERSION);
            Document headDocument = doc.getSourceDocument();
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
            checkPermission(doc, VERSION);

            DocumentVersion version = doc.getLastVersion();
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
            checkPermission(doc, VERSION);

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
            checkPermission(doc, VERSION);
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
            checkPermission(doc, VERSION);
            List<DocumentModel> versions = new ArrayList<DocumentModel>();
            DocumentVersionIterator versionIterator = doc.getVersions();

            while (versionIterator.hasNext()) {
                DocumentVersion docVersion = versionIterator.nextDocumentVersion();
                if (docVersion.getLabel() == null) {
                    // discard root default version
                    continue;
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
            checkPermission(doc, VERSION);

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
        assert docRef != null;
        assert version != null;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, WRITE_PROPERTIES);
            checkPermission(doc, VERSION);

            DocumentModel docModel = readModel(doc, null);

            // we're about to overwrite the document, make sure it's archived
            createDocumentSnapshot(docModel);

            final Map<String, Object> options = new HashMap<String, Object>();

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
            options.put(CoreEventConstants.DOCUMENT, doc);
            notifyEvent(DocumentEventTypes.BEFORE_DOC_RESTORE, docModel,
                    options, null, null, true);
            writeModel(doc, docModel);

            doc.restore(version.getLabel());
            // restore gives us a checked in document, so do a checkout
            doc.checkOut();

            // re-read doc model after restoration
            docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.DOCUMENT_RESTORED, docModel,
                    options, null, null, true);
            docModel = writeModel(doc, docModel);

            log.debug("Document restored to version:" + version.getLabel());
            return docModel;
        } catch (DocumentException e) {
            throw new ClientException("Failed to restore document " + docRef, e);
        }

    }

    public void checkIn(DocumentRef docRef, VersionModel version)
            throws ClientException {
        assert docRef != null;
        assert version != null;

        try {
            Document doc = resolveReference(docRef);
            // TODO: add a new permission names CHECKIN and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);

            DocumentModel docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKIN, docModel, null,
                    null, null, true);
            writeModel(doc, docModel);

            String description = version.getDescription();
            if (description != null) {
                doc.checkIn(version.getLabel(), description);
            } else {
                doc.checkIn(version.getLabel());
            }

            Document versionDocument = doc.getVersion(version.getLabel());
            DocumentModel versionModel = readModel(versionDocument, null);
            notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDIN, versionModel,
                    null, null, null, true);
            writeModel(versionDocument, versionModel);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check in document " + docRef,
                    e);
        }
    }

    public void checkOut(DocumentRef docRef) throws ClientException {
        assert docRef != null;
        try {
            Document doc = resolveReference(docRef);
            // TODO: add a new permission names CHECKOUT and use it instead of
            // WRITE_PROPERTIES
            checkPermission(doc, WRITE_PROPERTIES);
            DocumentModel docModel = readModel(doc, null);
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(CoreEventConstants.DOCUMENT, doc);
            notifyEvent(DocumentEventTypes.ABOUT_TO_CHECKOUT, docModel,
                    options, null, null, true);

            // checkout
            doc.checkOut();
            // doc was just checked out -> reset the dirty flag
            doc.setDirty(false);

            // re-read doc model
            docModel = readModel(doc, null);
            notifyEvent(DocumentEventTypes.DOCUMENT_CHECKEDOUT, docModel,
                    options, null, null, true);
        } catch (DocumentException e) {
            throw new ClientException("Failed to check out document " + docRef,
                    e);
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

    public DocumentModel getDocumentWithVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        assert docRef != null;
        assert version != null;

        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            doc = doc.getVersion(version.getLabel());
            log.debug("Retrieved the version " + version.getLabel()
                    + " of the document " + doc.getPath());
            return readModel(doc, null);
        } catch (DocumentException e) {
            throw new ClientException("Failed to get version for " + docRef, e);
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

            if (overwriteExistingProxy) {
                removeExistingProxies(doc, section);
            }

            // create the new proxy
            String vlabel = version.getLabel();
            Document proxy = getSession().createProxyForVersion(section, doc,
                    vlabel);
            log.debug("Created proxy for version " + vlabel
                    + " of the document " + doc.getPath());

            Map<String, Object> options = new HashMap<String, Object>();

            // No need - this notification is sent from the ActionBean
            // // save in history
            // DocumentModel docModel = readModel(doc, null);
            // options.put(CoreEventConstants.DOCUMENT, doc);
            // notifyEvent(DocumentEventTypes.DOCUMENT_PUBLISHED, docModel,
            // options, null, null, true);

            // notify for reindexation
            DocumentModel proxyModel = readModel(proxy, null);
            options.put(CoreEventConstants.DOCUMENT, proxy);
            notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                    proxyModel, options, null, null, true);

            // notify for document creation (proxy)
            notifyEvent(DocumentEventTypes.DOCUMENT_CREATED, proxyModel,
                    options, null, null, true);

            DocumentModel sectionModel = readModel(section, null);
            options.put(CoreEventConstants.DOCUMENT, section);
            notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                    sectionModel, options, null, null, true);

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
    protected void removeExistingProxies(Document doc, Document folder)
            throws DocumentException, ClientException {
        Collection<Document> otherProxies = getSession().getProxies(doc, folder);
        for (Document otherProxy : otherProxies) {
            removeNotifyOneDoc(otherProxy);
        }
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
                    versions.add(((DocumentVersionProxy) child).getTargetVersion().getLabel());
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

    public byte[] getContentData(DocumentRef docRef, String path)
            throws ClientException {
        try {
            Document doc = resolveReference(docRef);
            checkPermission(doc, READ);
            // TODO: add in Document support for retrieving properties by their
            // path
            Path pathObj = new Path(path);
            int len = pathObj.segmentCount();
            if (len == 0) {
                throw new ClientException("Failed to get content for " + docRef
                        + ". invalid path: " + path);
            }
            Property prop = doc.getProperty(pathObj.segment(0));
            if (prop == null) {
                throw new ClientException("Failed to get content for " + docRef
                        + ". invalid path " + path);
            }
            for (int i = 1; i < len; i++) {
                String seg = pathObj.segment(i);
                prop = prop.getProperty(seg);
                if (prop == null) {
                    throw new ClientException("Failed to get content for "
                            + docRef + ". no such property " + seg);
                }
            }
            if (!prop.getType().getName().equals("content")) {
                throw new DocumentException(
                        "Invalid content node. Only nodes with type \"content\" are content nodes");
            }
            Blob blob = (Blob) prop.getValue();
            if (blob != null) {
                return blob.getByteArray();
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        } catch (IOException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return null;
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
            try {
                checkPermission(doc, READ_LIFE_CYCLE);
                currentLifeCycleState = doc.getCurrentLifeCycleState();
            } catch (LifeCycleException e) {
                ClientException ce = new ClientException(
                        "Failed to get life cycle " + docRef, e);
                ce.fillInStackTrace();
                throw ce;
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return currentLifeCycleState;
    }

    public String getLifeCyclePolicy(DocumentRef docRef) throws ClientException {
        String lifecyclePolicy;
        try {
            Document doc = resolveReference(docRef);
            try {
                checkPermission(doc, READ_LIFE_CYCLE);
                lifecyclePolicy = doc.getLifeCyclePolicy();
            } catch (LifeCycleException e) {
                ClientException ce = new ClientException(
                        "Failed to get life cycle policy" + docRef, e);
                ce.fillInStackTrace();
                throw ce;
            }
        } catch (DocumentException e) {
            throw new ClientException("Failed to get content data " + docRef, e);
        }
        return lifecyclePolicy;
    }

    public boolean followTransition(DocumentRef docRef, String transition)
            throws ClientException {
        boolean operationResult = false;
        try {
            Document doc = resolveReference(docRef);
            try {
                checkPermission(doc, WRITE_LIFE_CYCLE);
                String formerStateName = doc.getCurrentLifeCycleState();
                operationResult = doc.followTransition(transition);

                if (operationResult) {
                    // Construct a map holding meta information about the event.
                    Map<String, Object> options = new HashMap<String, Object>();
                    options.put(LifeCycleEventTypes.OPTION_NAME_FROM,
                            formerStateName);
                    options.put(LifeCycleEventTypes.OPTION_NAME_TO,
                            doc.getCurrentLifeCycleState());
                    options.put(LifeCycleEventTypes.OPTION_NAME_TRANSITION,
                            transition);
                    options.put(CoreEventConstants.DOCUMENT, doc);
                    DocumentModel docModel = readModel(doc, null);
                    notifyEvent(LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT,
                            docModel, options,
                            DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY,
                            null, true);
                }
            } catch (LifeCycleException e) {
                ClientException ce = new ClientException(
                        "Unable to follow transition <" + transition +
                                "> for document : " + docRef, e);
                ce.fillInStackTrace();
                throw ce;
            }
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
            try {
                checkPermission(doc, READ_LIFE_CYCLE);
                allowedStateTransitions = doc.getAllowedStateTransitions();
            } catch (LifeCycleException e) {
                ClientException ce = new ClientException(
                        "Unable to get allowed state transitions for document : " +
                                docRef, e);
                ce.fillInStackTrace();
                throw ce;
            }
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
                // docRef construction as in DocumentModelFactory
                final DocumentRef parentDocRef = new IdRef(parentDoc.getUUID());
                docRefs.add(parentDocRef);

                // we don't actually read the parent doc data so not need to
                // :: checkPermission(parentDoc, READ);

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
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(CoreEventConstants.DOCUMENT, doc);
            notifyEvent(DocumentEventTypes.DOCUMENT_LOCKED, docModel, options,
                    null, null, true);
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

            // TODO: isAdministrator() should be replaced by a check on the
            // UNLOCK permission to be given to the "administrators" group in
            // the default ACLs
            if (isAdministrator() || lockDetails[0].equals(username)) {
                String lockKey = doc.unlock();
                DocumentModel docModel = readModel(doc, null);
                Map<String, Object> options = new HashMap<String, Object>();
                options.put(CoreEventConstants.DOCUMENT, doc);
                notifyEvent(DocumentEventTypes.DOCUMENT_UNLOCKED, docModel,
                        options, null, null, true);
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
        if (SecurityConstants.ADMINISTRATOR.equals(principal.getName())) {
            return true;
        }
        if (principal instanceof NuxeoPrincipal) {
            return ((NuxeoPrincipal) principal).getGroups().contains(
                    SecurityConstants.ADMINISTRATORS);
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

                if (overwriteExistingProxy) {
                    removeExistingProxies(doc, sec);
                }

                // publishing a proxy is just a copy
                // TODO copy also copies security. just recreate a proxy
                DocumentModel newDocument = copy(docRef, sectionRef,
                        docToPublish.getName());

                Map<String, Object> options = new HashMap<String, Object>();
                options.put(CoreEventConstants.DOCUMENT, newDocument);
                notifyEvent(DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED,
                        newDocument, options, null, null, true);

                options.put(CoreEventConstants.DOCUMENT, section);
                notifyEvent(DocumentEventTypes.SECTION_CONTENT_PUBLISHED,
                        section, options, null, null, true);

                return newDocument;
            } catch (DocumentException e) {
                throw new ClientException(e);
            }
        } else {
            // prepare document for creating snapshot
            docToPublish.putContextData(
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
            // snapshot the document
            createDocumentSnapshot(docToPublish);
            VersionModel version = getLastVersion(docRef);
            DocumentModel newProxy = createProxy(sectionRef, docRef, version,
                    overwriteExistingProxy);
            return newProxy;
        }

        /*
         * -- this cannot resolve unwanted transition 'approved -> project'
         * LifeCycleService lifeCycleService = NXCore.getLifeCycleService();
         *
         * try { String currentLifeCycle =
         * lifeCycleService.getCurrentLifeCycleState(resolveReference(docToPublish.getRef()));
         * lifeCycleService.setCurrentLifeCycleState(
         * resolveReference(newProxy.getRef()), currentLifeCycle); } catch
         * (LifeCycleException e) { throw new ClientException(e); } catch
         * (DocumentException e) { throw new ClientException(e); }
         */
        // XXX OG: why this method does commit? The client should be able to
        // decide when she wants to do the commit
        // save();
    }

    public VersionModel isPublished(DocumentModel document,
            DocumentModel section) {
        // TODO Auto-generated method stub
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
                return getRootDocument(); // return Root instead of null
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
        // FIXME BS: why this save() is done explicitely here?
        save();
    }

    public void orderBefore(DocumentRef parent, String src, String dest)
            throws ClientException {
        try {
            if ((src == null) || (src.equals(dest))) {
                return;
            }
            Document doc = resolveReference(parent);
            doc.orderBefore(src, dest);
            Map<String, Object> options = new HashMap<String, Object>();

            // send event on container passing the reordered child as parameter
            DocumentModel docModel = readModel(doc, null);
            String comment = src;
            options.put(CoreEventConstants.DOCUMENT, doc);
            options.put(CoreEventConstants.REORDERED_CHILD, src);
            notifyEvent(DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED,
                    docModel, options, null, comment, true);

        } catch (DocumentException e) {
            throw new ClientException("Failed to resolve documents: " + src
                    + ", " + dest, e);
        }
        // save();
    }

    public <T> T run(Operation<T> op) throws ClientException {
        try {
            return op.run(this, monitor, (Object[]) null);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

}
