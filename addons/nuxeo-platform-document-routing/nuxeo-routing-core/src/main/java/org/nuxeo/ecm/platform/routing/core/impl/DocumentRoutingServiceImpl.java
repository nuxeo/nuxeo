/*
 * (C) Copyright 2009-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.MAX_RESULTS_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.PAGE_SIZE_RESULTS_KEY;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS_PROVIDER_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOC_ROUTING_SEARCH_ROUTE_MODELS_WITH_TITLE_PROVIDER_NAME;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteTableElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingPersister;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LockableDocumentRoute;
import org.nuxeo.ecm.platform.routing.api.RouteFolderElement;
import org.nuxeo.ecm.platform.routing.api.RouteModelResourceType;
import org.nuxeo.ecm.platform.routing.api.RouteTable;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.audit.RoutingAuditHelper;
import org.nuxeo.ecm.platform.routing.core.listener.RouteModelsInitializator;
import org.nuxeo.ecm.platform.routing.core.registries.RouteTemplateResourceRegistry;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;
import org.nuxeo.ecm.platform.task.core.service.TaskEventNotificationHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * The implementation of the routing service.
 */
public class DocumentRoutingServiceImpl extends DefaultComponent implements DocumentRoutingService {

    private static Log log = LogFactory.getLog(DocumentRoutingServiceImpl.class);

    /** Routes in any state (model or not). */
    private static final String AVAILABLE_ROUTES_QUERY = String.format("SELECT * FROM %s",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    /** Routes Models. */
    private static final String AVAILABLE_ROUTES_MODEL_QUERY = String.format(
            "SELECT * FROM %s WHERE ecm:currentLifeCycleState = '%s'",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE,
            DocumentRoutingConstants.DOCUMENT_ROUTE_MODEL_LIFECYCLESTATE);

    /** Route models that have been validated. */
    private static final String ROUTE_MODEL_WITH_ID_QUERY = String.format(
            "SELECT * FROM %s WHERE ecm:name = %%s AND ecm:currentLifeCycleState = 'validated' AND ecm:isVersion = 0  AND ecm:isProxy = 0 ",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    /** Route models that have been validated. */
    private static final String ROUTE_MODEL_DOC_ID_WITH_ID_QUERY = String.format(
            "SELECT ecm:uuid FROM %s WHERE ecm:name = %%s AND ecm:currentLifeCycleState = 'validated' AND ecm:isVersion = 0  AND ecm:isProxy = 0 ",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    private static final String ORDERED_CHILDREN_QUERY = "SELECT * FROM Document WHERE"
            + " ecm:parentId = '%s' AND ecm:isVersion = 0 AND "
            + "ecm:isTrashed = 0 ORDER BY ecm:pos";

    public static final String CHAINS_TO_TYPE_XP = "chainsToType";

    public static final String PERSISTER_XP = "persister";

    /**
     * @since 7.10
     */
    public static final String ACTOR_ACE_CREATOR = "Workflow";

    // FIXME: use ContributionFragmentRegistry instances instead to handle hot
    // reload

    public static final String ROUTE_MODELS_IMPORTER_XP = "routeModelImporter";

    protected Map<String, String> typeToChain = new HashMap<>();

    protected Map<String, String> undoChainIdFromRunning = new HashMap<>();

    protected Map<String, String> undoChainIdFromDone = new HashMap<>();

    protected DocumentRoutingPersister persister;

    protected RouteTemplateResourceRegistry routeResourcesRegistry = new RouteTemplateResourceRegistry();

    protected RepositoryInitializationHandler repositoryInitializationHandler;

    private Cache<String, String> modelsChache;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CHAINS_TO_TYPE_XP.equals(extensionPoint)) {
            ChainToTypeMappingDescriptor desc = (ChainToTypeMappingDescriptor) contribution;
            typeToChain.put(desc.getDocumentType(), desc.getChainId());
            undoChainIdFromRunning.put(desc.getDocumentType(), desc.getUndoChainIdFromRunning());
            undoChainIdFromDone.put(desc.getDocumentType(), desc.getUndoChainIdFromDone());
        } else if (PERSISTER_XP.equals(extensionPoint)) {
            PersisterDescriptor des = (PersisterDescriptor) contribution;
            try {
                persister = des.getKlass().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else if (ROUTE_MODELS_IMPORTER_XP.equals(extensionPoint)) {
            RouteModelResourceType res = (RouteModelResourceType) contribution;
            registerRouteResource(res, contributor.getRuntimeContext());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof RouteModelResourceType) {
            routeResourcesRegistry.removeContribution((RouteModelResourceType) contribution);
        }
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    protected static void fireEvent(String eventName, Map<String, Serializable> eventProperties, DocumentRoute route,
            CoreSession session) {
        eventProperties.put(DocumentRoutingConstants.DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY, route);
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY);
        DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(),
                route.getDocument());
        envContext.setProperties(eventProperties);
        EventProducer eventProducer = Framework.getService(EventProducer.class);
        eventProducer.fireEvent(envContext.newEvent(eventName));
    }

    @Override
    public String createNewInstance(final String routeModelId, final List<String> docIds,
            final Map<String, Serializable> map, CoreSession session, final boolean startInstance) {
        final String initiator = session.getPrincipal().getName();
        final String res[] = new String[1];
        new UnrestrictedSessionRunner(session) {

            protected DocumentRoute route;

            @Override
            public void run() {
                String routeDocId = getRouteModelDocIdWithId(session, routeModelId);
                DocumentModel model = session.getDocument(new IdRef(routeDocId));
                DocumentModel instance = persister.createDocumentRouteInstanceFromDocumentRouteModel(model, session);
                route = instance.getAdapter(DocumentRoute.class);
                route.setAttachedDocuments(docIds);
                route.save(session);
                Map<String, Serializable> props = new HashMap<>();
                props.put(DocumentRoutingConstants.INITIATOR_EVENT_CONTEXT_KEY, initiator);
                fireEvent(DocumentRoutingConstants.Events.beforeRouteReady.name(), props);
                route.setReady(session);
                fireEvent(DocumentRoutingConstants.Events.afterRouteReady.name(), props);
                route.save(session);
                if (startInstance) {
                    fireEvent(DocumentRoutingConstants.Events.beforeRouteStart.name(), new HashMap<>());
                    DocumentRoutingEngineService routingEngine = Framework.getService(
                            DocumentRoutingEngineService.class);
                    routingEngine.start(route, map, session);
                    fireEventAfterWorkflowStarted(route, session);
                }
                res[0] = instance.getId();
            }

            protected void fireEvent(String eventName, Map<String, Serializable> eventProperties) {
                DocumentRoutingServiceImpl.fireEvent(eventName, eventProperties, route, session);
            }

        }.runUnrestricted();

        return res[0];
    }

    @Override
    public String createNewInstance(String routeModelId, List<String> docIds, CoreSession session,
            boolean startInstance) {
        return createNewInstance(routeModelId, docIds, null, session, startInstance);
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model, List<String> docIds, CoreSession session,
            boolean startInstance) {
        String id = createNewInstance(model.getDocument().getName(), docIds, session, startInstance);
        return session.getDocument(new IdRef(id)).getAdapter(DocumentRoute.class);
    }

    @Override
    @Deprecated
    public DocumentRoute createNewInstance(DocumentRoute model, String documentId, CoreSession session,
            boolean startInstance) {
        return createNewInstance(model, Collections.singletonList(documentId), session, startInstance);
    }

    @Override
    @Deprecated
    public DocumentRoute createNewInstance(DocumentRoute model, List<String> documentIds, CoreSession session) {
        return createNewInstance(model, documentIds, session, true);
    }

    @Override
    @Deprecated
    public DocumentRoute createNewInstance(DocumentRoute model, String documentId, CoreSession session) {
        return createNewInstance(model, Collections.singletonList(documentId), session, true);
    }

    @Override
    public void startInstance(final String routeInstanceId, final List<String> docIds,
            final Map<String, Serializable> map, CoreSession session) {
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel instance = session.getDocument(new IdRef(routeInstanceId));
                DocumentRoute route = instance.getAdapter(DocumentRoute.class);
                if (docIds != null) {
                    route.setAttachedDocuments(docIds);
                    route.save(session);
                }
                fireEvent(DocumentRoutingConstants.Events.beforeRouteStart.name(), new HashMap<>(), route, session);
                DocumentRoutingEngineService routingEngine = Framework.getService(DocumentRoutingEngineService.class);
                routingEngine.start(route, map, session);
                fireEventAfterWorkflowStarted(route, session);
            }

        }.runUnrestricted();
    }

    protected void fireEventAfterWorkflowStarted(DocumentRoute route, CoreSession session) {
        Map<String, Serializable> eventProperties = new HashMap<>();
        eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, route.getInitiator());
        eventProperties.put("modelId", route.getModelId());
        eventProperties.put("modelName", route.getModelName());
        if (route instanceof GraphRoute) {
            eventProperties.put(RoutingAuditHelper.WORKFLOW_VARIABLES,
                    (Serializable) ((GraphRoute) route).getVariables());
        }
        fireEvent(DocumentRoutingConstants.Events.afterWorkflowStarted.name(), eventProperties, route, session);
    }

    @Override
    public void resumeInstance(String routeId, String nodeId, Map<String, Object> data, String status,
            CoreSession session) {
        AttachedDocumentsChecker adc = new AttachedDocumentsChecker(session, routeId);
        adc.runUnrestricted();
        if (!adc.isWorkflowCanceled) {
            completeTask(routeId, nodeId, null, data, status, session);
        }
    }

    @Override
    public void completeTask(String routeId, String taskId, Map<String, Object> data, String status,
            CoreSession session) {
        DocumentModel task = session.getDocument(new IdRef(taskId));
        completeTask(routeId, null, task != null ? task.getAdapter(Task.class) : null, data, status, session);
    }

    protected void completeTask(final String routeId, final String nodeId, final Task task,
            final Map<String, Object> data, final String status, CoreSession session) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Completing task %s associated to node %s for workflow instance %s",
                    task != null ? task.getId() : null, nodeId, routeId));
        }
        CompleteTaskRunner runner = new CompleteTaskRunner(routeId, nodeId, task, data, status, session);
        runner.runUnrestricted();
    }

    /**
     * @since 7.4
     */
    private class CompleteTaskRunner extends UnrestrictedSessionRunner {

        String routeId;

        String nodeId;

        Task task;

        Map<String, Object> data;

        String status;

        protected CompleteTaskRunner(final String routeId, final String nodeId, final Task task,
                final Map<String, Object> data, final String status, CoreSession session) {
            super(session);
            this.routeId = routeId;
            this.nodeId = nodeId;
            this.task = task;
            this.data = data;
            this.status = status;
        }

        @Override
        public void run() {
            DocumentRoutingEngineService routingEngine = Framework.getService(DocumentRoutingEngineService.class);
            DocumentModel routeDoc = session.getDocument(new IdRef(routeId));
            DocumentRoute routeInstance = routeDoc.getAdapter(DocumentRoute.class);
            routingEngine.resume(routeInstance, nodeId, task != null ? task.getId() : null, data, status, session);

            // If task is null, it means we are resuming the workflow and about to cancel pending tasks.
            // Do not notify
            if (task != null) {
                String comment = data != null ? (String) data.get(GraphNode.NODE_VARIABLE_COMMENT) : null;
                final Map<String, Serializable> extraEventProperties = new HashMap<>();
                extraEventProperties.put(DocumentRoutingConstants.WORKFLOW_TASK_COMPLETION_ACTION_KEY, status);
                TaskEventNotificationHelper.notifyTaskEnded(session, session.getPrincipal(), task, comment,
                        TaskEventNames.WORKFLOW_TASK_COMPLETED, extraEventProperties);
            }
        }

    }

    @Override
    public List<DocumentRoute> getAvailableDocumentRouteModel(CoreSession session) {
        DocumentModelList list = session.query(AVAILABLE_ROUTES_MODEL_QUERY);
        List<DocumentRoute> routes = new ArrayList<>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
        return routes;
    }

    @Override
    public List<DocumentRoute> getAvailableDocumentRoute(CoreSession session) {
        DocumentModelList list = session.query(AVAILABLE_ROUTES_QUERY);
        List<DocumentRoute> routes = new ArrayList<>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
        return routes;
    }

    @Override
    public String getOperationChainId(String documentType) {
        return typeToChain.get(documentType);
    }

    @Override
    public String getUndoFromRunningOperationChainId(String documentType) {
        return undoChainIdFromRunning.get(documentType);
    }

    @Override
    public String getUndoFromDoneOperationChainId(String documentType) {
        return undoChainIdFromDone.get(documentType);
    }

    @Override
    public DocumentRoute unlockDocumentRouteUnrestrictedSession(final DocumentRoute routeModel,
            CoreSession userSession) {
        new UnrestrictedSessionRunner(userSession) {
            @Override
            public void run() {
                DocumentRoute route = session.getDocument(routeModel.getDocument().getRef())
                                             .getAdapter(DocumentRoute.class);
                LockableDocumentRoute lockableRoute = route.getDocument().getAdapter(LockableDocumentRoute.class);
                lockableRoute.unlockDocument(session);
            }
        }.runUnrestricted();
        return userSession.getDocument(routeModel.getDocument().getRef()).getAdapter(DocumentRoute.class);
    }

    @Override
    public DocumentRoute validateRouteModel(final DocumentRoute routeModel, CoreSession userSession)
            throws DocumentRouteNotLockedException {
        if (!routeModel.getDocument().isLocked()) {
            throw new DocumentRouteNotLockedException();
        }
        new UnrestrictedSessionRunner(userSession) {
            @Override
            public void run() {
                DocumentRoute route = session.getDocument(routeModel.getDocument().getRef())
                                             .getAdapter(DocumentRoute.class);
                route.validate(session);
            }
        }.runUnrestricted();
        return userSession.getDocument(routeModel.getDocument().getRef()).getAdapter(DocumentRoute.class);
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    @Override
    public List<DocumentRouteTableElement> getRouteElements(DocumentRoute route, CoreSession session) {
        RouteTable table = new RouteTable(route);
        List<DocumentRouteTableElement> elements = new ArrayList<>();
        processElementsInFolder(route.getDocument(), elements, table, session, 0, null);
        int maxDepth = 0;
        for (DocumentRouteTableElement element : elements) {
            int d = element.getDepth();
            maxDepth = d > maxDepth ? d : maxDepth;
        }
        table.setMaxDepth(maxDepth);
        for (DocumentRouteTableElement element : elements) {
            element.computeFirstChildList();
        }
        return elements;
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    protected void processElementsInFolder(DocumentModel doc, List<DocumentRouteTableElement> elements,
            RouteTable table, CoreSession session, int depth, RouteFolderElement folder) {
        DocumentModelList children = session.getChildren(doc.getRef());
        boolean first = true;
        for (DocumentModel child : children) {
            if (child.isFolder() && !session.getChildren(child.getRef()).isEmpty()) {
                RouteFolderElement thisFolder = new RouteFolderElement(child.getAdapter(DocumentRouteElement.class),
                        table, first, folder, depth);
                processElementsInFolder(child, elements, table, session, depth + 1, thisFolder);
            } else {
                if (folder != null) {
                    folder.increaseTotalChildCount();
                } else {
                    table.increaseTotalChildCount();
                }
                elements.add(new DocumentRouteTableElement(child.getAdapter(DocumentRouteElement.class), table, depth,
                        folder, first));
            }
            first = false;
        }
    }

    @Deprecated
    protected List<DocumentRouteTableElement> getRouteElements(DocumentRouteElement routeElementDocument,
            CoreSession session, List<DocumentRouteTableElement> routeElements, int depth) {
        return null;
    }

    @Override
    public List<DocumentRoute> getDocumentRoutesForAttachedDocument(CoreSession session, String attachedDocId) {
        List<DocumentRouteElement.ElementLifeCycleState> states = new ArrayList<>();
        states.add(DocumentRouteElement.ElementLifeCycleState.ready);
        states.add(DocumentRouteElement.ElementLifeCycleState.running);
        return getDocumentRoutesForAttachedDocument(session, attachedDocId, states);
    }

    @Override
    public List<DocumentRoute> getDocumentRoutesForAttachedDocument(CoreSession session, String attachedDocId,
            List<DocumentRouteElement.ElementLifeCycleState> states) {
        DocumentModelList list;
        StringBuilder statesString = new StringBuilder();
        if (states != null && !states.isEmpty()) {
            statesString.append(" ecm:currentLifeCycleState IN (");
            for (DocumentRouteElement.ElementLifeCycleState state : states) {
                statesString.append("'" + state.name() + "',");
            }
            statesString.deleteCharAt(statesString.length() - 1);
            statesString.append(") AND");
        }
        String query = String.format("SELECT * FROM DocumentRoute WHERE " + statesString.toString()
                + " docri:participatingDocuments/* = '%s'"
                // ordering by dc:created makes sure that
                // a sub-workflow is listed under its parent
                + " ORDER BY dc:created", attachedDocId);
        UnrestrictedQueryRunner queryRunner = new UnrestrictedQueryRunner(session, query);
        list = queryRunner.runQuery();
        List<DocumentRoute> routes = new ArrayList<>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
        return routes;
    }

    @Override
    public boolean canUserValidateRoute(NuxeoPrincipal currentUser) {
        return currentUser.getGroups().contains(DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME);
    }

    @Override
    public boolean canValidateRoute(DocumentModel documentRoute, CoreSession coreSession) {
        if (!coreSession.hasChildren(documentRoute.getRef())) {
            // Cannot validate an empty route
            return false;
        }
        return coreSession.hasPermission(documentRoute.getRef(), SecurityConstants.EVERYTHING);
    }

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Override
    @Deprecated
    public void addRouteElementToRoute(DocumentRef parentDocumentRef, int idx, DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException {
        DocumentRoute route = getParentRouteModel(parentDocumentRef, session);
        if (!isLockedByCurrentUser(route, session)) {
            throw new DocumentRouteNotLockedException();
        }
        DocumentModelList children = session.query(
                String.format(ORDERED_CHILDREN_QUERY, session.getDocument(parentDocumentRef).getId()));
        DocumentModel sourceDoc;
        try {
            sourceDoc = children.get(idx);
            addRouteElementToRoute(parentDocumentRef, sourceDoc.getName(), routeElement, session);
        } catch (IndexOutOfBoundsException e) {
            addRouteElementToRoute(parentDocumentRef, null, routeElement, session);
        }
    }

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Override
    @Deprecated
    public void addRouteElementToRoute(DocumentRef parentDocumentRef, String sourceName,
            DocumentRouteElement routeElement, CoreSession session) throws DocumentRouteNotLockedException {
        DocumentRoute parentRoute = getParentRouteModel(parentDocumentRef, session);
        if (!isLockedByCurrentUser(parentRoute, session)) {
            throw new DocumentRouteNotLockedException();
        }
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel docRouteElement = routeElement.getDocument();
        DocumentModel parentDocument = session.getDocument(parentDocumentRef);
        docRouteElement.setPathInfo(parentDocument.getPathAsString(), pss.generatePathSegment(docRouteElement));
        String lifecycleState = parentDocument.getCurrentLifeCycleState()
                                              .equals(DocumentRouteElement.ElementLifeCycleState.draft.name())
                                                      ? DocumentRouteElement.ElementLifeCycleState.draft.name()
                                                      : DocumentRouteElement.ElementLifeCycleState.ready.name();
        docRouteElement.putContextData(LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME, lifecycleState);
        docRouteElement = session.createDocument(docRouteElement);
        session.orderBefore(parentDocumentRef, docRouteElement.getName(), sourceName);
        session.save();// the new document will be queried later on
    }

    @Override
    public void removeRouteElement(DocumentRouteElement routeElement, CoreSession session)
            throws DocumentRouteNotLockedException {
        DocumentRoute parentRoute = routeElement.getDocumentRoute(session);
        if (!isLockedByCurrentUser(parentRoute, session)) {
            throw new DocumentRouteNotLockedException();
        }
        session.removeDocument(routeElement.getDocument().getRef());
        session.save();// the document will be queried later on
    }

    @Override
    public DocumentModelList getOrderedRouteElement(String routeElementId, CoreSession session) {
        String query = String.format(ORDERED_CHILDREN_QUERY, routeElementId);
        DocumentModelList orderedChildren = session.query(query);
        return orderedChildren;
    }

    @Override
    public void lockDocumentRoute(DocumentRoute routeModel, CoreSession session)
            throws DocumentRouteAlredayLockedException {
        LockableDocumentRoute lockableRoute = routeModel.getDocument().getAdapter(LockableDocumentRoute.class);
        boolean lockedByCurrent = isLockedByCurrentUser(routeModel, session);
        if (lockableRoute.isLocked(session) && !lockedByCurrent) {
            throw new DocumentRouteAlredayLockedException();
        }
        if (!lockedByCurrent) {
            lockableRoute.lockDocument(session);
        }
    }

    @Override
    public void unlockDocumentRoute(DocumentRoute routeModel, CoreSession session)
            throws DocumentRouteNotLockedException {
        LockableDocumentRoute lockableRoute = routeModel.getDocument().getAdapter(LockableDocumentRoute.class);
        if (!lockableRoute.isLockedByCurrentUser(session)) {
            throw new DocumentRouteNotLockedException();
        }
        lockableRoute.unlockDocument(session);
    }

    @Override
    public boolean isLockedByCurrentUser(DocumentRoute routeModel, CoreSession session) {
        LockableDocumentRoute lockableRoute = routeModel.getDocument().getAdapter(LockableDocumentRoute.class);
        return lockableRoute.isLockedByCurrentUser(session);
    }

    @Override
    public void updateRouteElement(DocumentRouteElement routeElement, CoreSession session)
            throws DocumentRouteNotLockedException {
        if (!isLockedByCurrentUser(routeElement.getDocumentRoute(session), session)) {
            throw new DocumentRouteNotLockedException();
        }
        routeElement.save(session);
    }

    private DocumentRoute getParentRouteModel(DocumentRef documentRef, CoreSession session) {
        DocumentModel parentDoc = session.getDocument(documentRef);
        if (parentDoc.hasFacet(DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_FACET)) {
            return parentDoc.getAdapter(DocumentRoute.class);
        }
        DocumentRouteElement rElement = parentDoc.getAdapter(DocumentRouteElement.class);
        return rElement.getDocumentRoute(session);

    }

    @Override
    public DocumentRoute saveRouteAsNewModel(DocumentRoute instance, CoreSession session) {
        DocumentModel instanceModel = instance.getDocument();
        DocumentModel parent = persister.getParentFolderForNewModel(session, instanceModel);
        String newName = persister.getNewModelName(instanceModel);
        DocumentModel newmodel = persister.saveDocumentRouteInstanceAsNewModel(instanceModel, parent, newName, session);
        DocumentRoute newRoute = newmodel.getAdapter(DocumentRoute.class);
        if (!newRoute.isDraft()) {
            newRoute.followTransition(DocumentRouteElement.ElementLifeCycleTransistion.toDraft, session, false);
        }
        newRoute.getDocument().setPropertyValue("dc:title", newName);
        newRoute.setAttachedDocuments(new ArrayList<>());
        newRoute.save(session);
        return newRoute;
    }

    @Override
    public boolean isRoutable(DocumentModel doc) {
        if (doc == null) {
            return false;
        }
        String type = doc.getType();
        // TODO make configurable
        return type.equals("File") || type.equals("Note");
    }

    @Override
    public void importAllRouteModels(CoreSession session) {
        for (URL url : getRouteModelTemplateResources()) {
            importRouteModel(url, true, session);
        }
    }

    @Override
    public DocumentRoute importRouteModel(URL modelToImport, boolean overwrite, CoreSession session) {
        if (modelToImport == null) {
            throw new NuxeoException(("No resource containing route templates found"));
        }
        Blob blob = new URLBlob(modelToImport);
        final String file = modelToImport.getFile();
        DocumentModel doc;
        try {
            doc = getFileManager().createDocumentFromBlob(session, blob,
                    persister.getParentFolderForDocumentRouteModels(session).getPathAsString(), true, file);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        if (doc == null) {
            throw new NuxeoException("Can not import document " + file);
        }
        // remove model from cache if any model with the same id existed
        if (modelsChache != null) {
            modelsChache.invalidate(doc.getName());
        }

        return doc.getAdapter(DocumentRoute.class);
    }

    protected FileManager getFileManager() {
        return Framework.getService(FileManager.class);
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        modelsChache = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();
        repositoryInitializationHandler = new RouteModelsInitializator();
        repositoryInitializationHandler.install();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        if (repositoryInitializationHandler != null) {
            repositoryInitializationHandler.uninstall();
        }
    }

    @Override
    public List<URL> getRouteModelTemplateResources() {
        List<URL> urls = new ArrayList<>();
        for (URL url : routeResourcesRegistry.getRouteModelTemplateResources()) {
            urls.add(url); // test contrib parsing and deployment
        }
        return urls;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DocumentModel> searchRouteModels(CoreSession session, String searchString) {
        List<DocumentModel> allRouteModels = new ArrayList<>();
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<>();
        props.put(MAX_RESULTS_PROPERTY, PAGE_SIZE_RESULTS_KEY);
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<DocumentModel> pageProvider;
        if (StringUtils.isEmpty(searchString)) {
            pageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                    DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS_PROVIDER_NAME, null, null, 0L, props);
        } else {
            pageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                    DOC_ROUTING_SEARCH_ROUTE_MODELS_WITH_TITLE_PROVIDER_NAME, null, null, 0L, props,
                    searchString + '%');
        }
        allRouteModels.addAll(pageProvider.getCurrentPage());
        while (pageProvider.isNextPageAvailable()) {
            pageProvider.nextPage();
            allRouteModels.addAll(pageProvider.getCurrentPage());
        }
        return allRouteModels;
    }

    @Override
    public void registerRouteResource(RouteModelResourceType res, RuntimeContext context) {
        if (res.getPath() != null && res.getId() != null) {
            if (routeResourcesRegistry.getResource(res.getId()) != null) {
                routeResourcesRegistry.removeContribution(res);
            }
            if (res.getUrl() == null) {
                res.setUrl(getUrlFromPath(res, context));
            }
            routeResourcesRegistry.addContribution(res);
        }
    }

    protected URL getUrlFromPath(RouteModelResourceType res, RuntimeContext extensionContext) {
        String path = res.getPath();
        if (path == null) {
            return null;
        }
        URL url;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            url = extensionContext.getLocalResource(path);
            if (url == null) {
                url = extensionContext.getResource(path);
            }
            if (url == null) {
                url = res.getClass().getResource(path);
            }
        }
        return url;
    }

    @Override
    public DocumentRoute getRouteModelWithId(CoreSession session, String id) {
        String routeDocModelId = getRouteModelDocIdWithId(session, id);
        DocumentModel routeDoc = session.getDocument(new IdRef(routeDocModelId));
        return routeDoc.getAdapter(DocumentRoute.class);
    }

    @Override
    public String getRouteModelDocIdWithId(CoreSession session, String id) {
        if (modelsChache != null) {
            String routeDocId = modelsChache.getIfPresent(id);
            if (routeDocId != null) {
                return routeDocId;
            }
        }
        String query = String.format(ROUTE_MODEL_DOC_ID_WITH_ID_QUERY, NXQL.escapeString(id));
        List<String> routeIds = new ArrayList<>();
        try (IterableQueryResult results = session.queryAndFetch(query, "NXQL")) {
            if (results.size() == 0) {
                throw new NuxeoException("No route found for id: " + id);
            }
            if (results.size() != 1) {
                throw new NuxeoException("More than one route model found with id: " + id);
            }
            for (Map<String, Serializable> map : results) {
                routeIds.add(map.get("ecm:uuid").toString());
            }
        }
        String routeDocId = routeIds.get(0);
        if (modelsChache == null) {
            modelsChache = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();
        }
        modelsChache.put(id, routeDocId);
        return routeDocId;
    }

    @Override
    @Deprecated
    public void makeRoutingTasks(CoreSession coreSession, final List<Task> tasks) {
        new UnrestrictedSessionRunner(coreSession) {
            @Override
            public void run() {
                for (Task task : tasks) {
                    DocumentModel taskDoc = task.getDocument();
                    taskDoc.addFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME);
                    session.saveDocument(taskDoc);
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void endTask(CoreSession session, Task task, Map<String, Object> data, String status) {
        String comment = (String) data.get(GraphNode.NODE_VARIABLE_COMMENT);
        TaskService taskService = Framework.getService(TaskService.class);
        taskService.endTask(session, session.getPrincipal(), task, comment, null, false);

        Map<String, String> taskVariables = task.getVariables();
        String routeInstanceId = taskVariables.get(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (StringUtils.isEmpty(routeInstanceId)) {
            throw new DocumentRouteException("Can not resume workflow, no related route");
        }
        completeTask(routeInstanceId, null, task, data, status, session);
    }

    @Override
    public List<DocumentModel> getWorkflowInputDocuments(CoreSession session, Task task) {
        String routeInstanceId;
        try {
            routeInstanceId = task.getProcessId();
        } catch (PropertyException e) {
            throw new DocumentRouteException("Can not get the related workflow instance");
        }
        if (StringUtils.isEmpty(routeInstanceId)) {
            throw new DocumentRouteException("Can not get the related workflow instance");
        }
        DocumentModel routeDoc;
        try {
            routeDoc = session.getDocument(new IdRef(routeInstanceId));
        } catch (DocumentNotFoundException e) {
            throw new DocumentRouteException("No workflow with the id:" + routeInstanceId);
        }
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        return route.getAttachedDocuments(session);
    }

    @Override
    public void grantPermissionToTaskAssignees(CoreSession session, String permission, List<DocumentModel> docs,
            Task task) {
        setAclForActors(session, getRoutingACLName(task), permission, docs, task.getActors());
    }

    @Override
    public void grantPermissionToTaskDelegatedActors(CoreSession session, String permission, List<DocumentModel> docs,
            Task task) {
        setAclForActors(session, getDelegationACLName(task), permission, docs, task.getDelegatedActors());
    }

    @Override
    public void removePermissionFromTaskAssignees(CoreSession session, final List<DocumentModel> docs, Task task) {
        final String aclName = getRoutingACLName(task);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                for (DocumentModel doc : docs) {
                    ACP acp = doc.getACP();
                    acp.removeACL(aclName);
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }
            }
        }.runUnrestricted();
    }

    /**
     * @since 7.4
     */
    @Override
    public void removePermissionsForTaskActors(CoreSession session, final List<DocumentModel> docs, String taskId) {
        final String aclRoutingName = getRoutingACLName(taskId);
        final String aclDelegationName = getDelegationACLName(taskId);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                for (DocumentModel doc : docs) {
                    ACP acp = doc.getACP();
                    acp.removeACL(aclRoutingName);
                    acp.removeACL(aclDelegationName);
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void removePermissionsForTaskActors(CoreSession session, final List<DocumentModel> docs, Task task) {
        removePermissionsForTaskActors(session, docs, task.getId());
    }

    /**
     * Finds an ACL name specific to the task (there may be several tasks applying permissions to the same document).
     */
    protected static String getRoutingACLName(Task task) {
        return getRoutingACLName(task.getId());
    }

    /**
     * @since 7.4
     */
    protected static String getRoutingACLName(String taskId) {
        return DocumentRoutingConstants.DOCUMENT_ROUTING_ACL + '/' + taskId;
    }

    protected static String getDelegationACLName(Task task) {
        return getDelegationACLName(task.getId());
    }

    /**
     * @since 7.4
     */
    protected static String getDelegationACLName(String taskId) {
        return DocumentRoutingConstants.DOCUMENT_ROUTING_DELEGATION_ACL + '/' + taskId;
    }

    /**
     * @since 7.1
     */
    private final class WfCleaner extends UnrestrictedSessionRunner {

        private static final String WORKFLOWS_QUERY = "SELECT ecm:uuid FROM DocumentRoute WHERE ecm:currentLifeCycleState IN ('done', 'canceled')";

        private static final String TASKS_QUERY = "SELECT ecm:uuid FROM Document WHERE ecm:mixinType = 'Task' AND nt:processId = '%s'";

        private final int limit;

        private int numberOfCleanedUpWorkflows = 0;

        private WfCleaner(String repositoryName, int limit) {
            super(repositoryName);
            this.limit = limit;
        }

        @Override
        public void run() {
            PartialList<Map<String, Serializable>> workflows = session.queryProjection(WORKFLOWS_QUERY, limit, 0);
            numberOfCleanedUpWorkflows = workflows.size();

            for (Map<String, Serializable> workflow : workflows) {
                String routeDocId = workflow.get(ECM_UUID).toString();
                final String associatedTaskQuery = String.format(TASKS_QUERY, routeDocId);
                session.queryProjection(associatedTaskQuery, 0, 0)
                       .stream()
                       .map(task -> new IdRef(task.get(ECM_UUID).toString()))
                       .forEach(session::removeDocument);
                session.removeDocument(new IdRef(routeDocId));
            }
        }

        public int getNumberOfCleanedUpWf() {
            return numberOfCleanedUpWorkflows;
        }
    }

    class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {

        String query;

        DocumentModelList docs;

        protected UnrestrictedQueryRunner(CoreSession session, String query) {
            super(session);
            this.query = query;
        }

        @Override
        public void run() {
            docs = session.query(query);
            for (DocumentModel documentModel : docs) {
                documentModel.detach(true);
            }
        }

        public DocumentModelList runQuery() {
            runUnrestricted();
            return docs;
        }
    }

    /**
     * Cancel the workflow instance if all its attached document don't exist anymore. If the workflow is cancelled then
     * the isWowkflowCanceled is set to true.
     *
     * @since 8.4
     */
    public static class AttachedDocumentsChecker extends UnrestrictedSessionRunner {

        String workflowInstanceId;

        boolean isWorkflowCanceled;

        protected AttachedDocumentsChecker(CoreSession session, String workflowInstanceId) {
            super(session);
            this.workflowInstanceId = workflowInstanceId;
        }

        @Override
        public void run() {
            DocumentModel routeDoc = session.getDocument(new IdRef(workflowInstanceId));
            DocumentRoute routeInstance = routeDoc.getAdapter(DocumentRoute.class);
            List<String> attachedDocumentIds = routeInstance.getAttachedDocuments();
            if (attachedDocumentIds.isEmpty()) {
                return;
            }
            for (String attachedDocumentId : attachedDocumentIds) {
                if (session.exists(new IdRef(attachedDocumentId))) {
                    return;
                }
            }
            DocumentRoutingEngineService routingEngine = Framework.getService(DocumentRoutingEngineService.class);
            routingEngine.cancel(routeInstance, session);
            isWorkflowCanceled = true;
        }
    }

    @Override
    public void finishTask(CoreSession session, DocumentRoute route, Task task, boolean delete)
            throws DocumentRouteException {
        DocumentModelList docs = route.getAttachedDocuments(session);
        try {
            removePermissionsForTaskActors(session, docs, task);
            // delete task
            if (delete) {
                session.removeDocument(new IdRef(task.getId()));
            }
        } catch (DocumentNotFoundException e) {
            throw new DocumentRouteException("Cannot finish task", e);
        }
    }

    @Override
    public void cancelTask(CoreSession session, final String taskId) throws DocumentRouteException {
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
                Task task = taskDoc.getAdapter(Task.class);
                if (task == null) {
                    throw new DocumentRouteException("Invalid taskId: " + taskId);
                }

                if (!task.isOpened()) {
                    log.info("Can not cancel task " + taskId + "as is not open");
                    return;
                }
                task.cancel(session);

                // if the task was created by an workflow , update info
                String routeId = task.getProcessId();
                if (routeId != null) {
                    DocumentModel routeDoc = session.getDocument(new IdRef(routeId));
                    GraphRoute routeInstance = routeDoc.getAdapter(GraphRoute.class);
                    if (routeInstance == null) {
                        throw new DocumentRouteException("Invalid routeInstanceId: " + routeId);
                    }

                    DocumentModelList docs = routeInstance.getAttachedDocumentModels();
                    removePermissionsForTaskActors(session, docs, task);
                    // task is considered processed with the status "null"
                    // when
                    // is
                    // canceled
                    updateTaskInfo(session, routeInstance, task, null);
                }
                session.saveDocument(task.getDocument());

            }
        }.runUnrestricted();
    }

    protected void updateTaskInfo(CoreSession session, GraphRoute graph, Task task, String status) {
        String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (StringUtils.isEmpty(nodeId)) {
            throw new DocumentRouteException("No nodeId found on task: " + task.getId());
        }
        GraphNode node = graph.getNode(nodeId);

        NuxeoPrincipal principal = session.getPrincipal();
        String actor = principal.getActingUser();
        node.updateTaskInfo(task.getId(), true, status, actor, null);
    }

    @Override
    public void reassignTask(CoreSession session, final String taskId, final List<String> actors, final String comment)
            throws DocumentRouteException {
        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
                Task task = taskDoc.getAdapter(Task.class);
                if (task == null) {
                    throw new DocumentRouteException("Invalid taskId: " + taskId);
                }
                if (!task.isOpened()) {
                    throw new DocumentRouteException("Task  " + taskId + " is not opened, can not reassign it");
                }
                String routeId = task.getProcessId();
                if (routeId != null) {
                    DocumentModel routeDoc = session.getDocument(new IdRef(routeId));
                    GraphRoute routeInstance = routeDoc.getAdapter(GraphRoute.class);
                    if (routeInstance == null) {
                        throw new DocumentRouteException(
                                "Invalid routeInstanceId: " + routeId + " referenced by the task " + taskId);
                    }
                    GraphNode node = routeInstance.getNode(task.getType());
                    if (node == null) {
                        throw new DocumentRouteException(
                                "Invalid node " + routeId + " referenced by the task " + taskId);
                    }
                    if (!node.allowTaskReassignment()) {
                        throw new DocumentRouteException("Task " + taskId + " can not be reassigned. Node "
                                + node.getId() + " doesn't allow reassignment.");
                    }
                    DocumentModelList docs = routeInstance.getAttachedDocumentModels();
                    // remove permissions on the document following the
                    // workflow for the current assignees
                    removePermissionFromTaskAssignees(session, docs, task);
                    Framework.getService(TaskService.class).reassignTask(session, taskId, actors, comment);
                    // refresh task
                    task.getDocument().refresh();
                    // grant permission to the new assignees
                    grantPermissionToTaskAssignees(session, node.getTaskAssigneesPermission(), docs, task);

                    // Audit task reassignment
                    Map<String, Serializable> eventProperties = new HashMap<>();
                    eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                            DocumentRoutingConstants.ROUTING_CATEGORY);
                    eventProperties.put("taskName", task.getName());
                    eventProperties.put("actors", (Serializable) actors);
                    eventProperties.put("modelId", routeInstance.getModelId());
                    eventProperties.put("modelName", routeInstance.getModelName());
                    eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, routeInstance.getInitiator());
                    eventProperties.put(RoutingAuditHelper.TASK_ACTOR, session.getPrincipal().getActingUser());
                    eventProperties.put("comment", comment);
                    // compute duration since workflow started
                    long timeSinceWfStarted = RoutingAuditHelper.computeDurationSinceWfStarted(task.getProcessId());
                    if (timeSinceWfStarted >= 0) {
                        eventProperties.put(RoutingAuditHelper.TIME_SINCE_WF_STARTED, timeSinceWfStarted);
                    }
                    // compute duration since task started
                    long timeSinceTaskStarted = RoutingAuditHelper.computeDurationSinceTaskStarted(task.getId());
                    if (timeSinceWfStarted >= 0) {
                        eventProperties.put(RoutingAuditHelper.TIME_SINCE_TASK_STARTED, timeSinceTaskStarted);
                    }
                    DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(),
                            task.getDocument());
                    envContext.setProperties(eventProperties);
                    EventProducer eventProducer = Framework.getService(EventProducer.class);
                    eventProducer.fireEvent(
                            envContext.newEvent(DocumentRoutingConstants.Events.afterWorkflowTaskReassigned.name()));
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void delegateTask(CoreSession session, final String taskId, final List<String> delegatedActors,
            final String comment) throws DocumentRouteException {
        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
                Task task = taskDoc.getAdapter(Task.class);
                if (task == null) {
                    throw new DocumentRouteException("Invalid taskId: " + taskId);
                }
                String routeId = task.getProcessId();
                if (routeId != null) {
                    DocumentModel routeDoc = session.getDocument(new IdRef(routeId));
                    GraphRoute routeInstance = routeDoc.getAdapter(GraphRoute.class);
                    if (routeInstance == null) {
                        throw new DocumentRouteException(
                                "Invalid routeInstanceId: " + routeId + " referenced by the task " + taskId);
                    }
                    GraphNode node = routeInstance.getNode(task.getType());
                    if (node == null) {
                        throw new DocumentRouteException(
                                "Invalid node " + routeId + " referenced by the task " + taskId);
                    }
                    DocumentModelList docs = routeInstance.getAttachedDocumentModels();
                    Framework.getService(TaskService.class).delegateTask(session, taskId, delegatedActors, comment);
                    // refresh task
                    task.getDocument().refresh();
                    // grant permission to the new assignees
                    grantPermissionToTaskDelegatedActors(session, node.getTaskAssigneesPermission(), docs, task);

                    // Audit task delegation
                    Map<String, Serializable> eventProperties = new HashMap<>();
                    eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                            DocumentRoutingConstants.ROUTING_CATEGORY);
                    eventProperties.put("taskName", task.getName());
                    eventProperties.put("delegatedActors", (Serializable) delegatedActors);
                    eventProperties.put("modelId", routeInstance.getModelId());
                    eventProperties.put("modelName", routeInstance.getModelName());
                    eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, routeInstance.getInitiator());
                    eventProperties.put(RoutingAuditHelper.TASK_ACTOR, session.getPrincipal().getActingUser());
                    eventProperties.put("comment", comment);

                    // compute duration since workflow started
                    long timeSinceWfStarted = RoutingAuditHelper.computeDurationSinceWfStarted(task.getProcessId());
                    if (timeSinceWfStarted >= 0) {
                        eventProperties.put(RoutingAuditHelper.TIME_SINCE_WF_STARTED, timeSinceWfStarted);
                    }
                    // compute duration since task started
                    long timeSinceTaskStarted = RoutingAuditHelper.computeDurationSinceTaskStarted(task.getId());
                    if (timeSinceWfStarted >= 0) {
                        eventProperties.put(RoutingAuditHelper.TIME_SINCE_TASK_STARTED, timeSinceTaskStarted);
                    }

                    DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(),
                            task.getDocument());
                    envContext.setProperties(eventProperties);
                    EventProducer eventProducer = Framework.getService(EventProducer.class);
                    eventProducer.fireEvent(
                            envContext.newEvent(DocumentRoutingConstants.Events.afterWorkflowTaskDelegated.name()));
                }
            }
        }.runUnrestricted();
    }

    protected void setAclForActors(CoreSession session, final String aclName, final String permission,
            final List<DocumentModel> docs, List<String> actors) {
        final List<String> actorIds = new ArrayList<>();
        for (String actor : actors) {
            if (actor.startsWith(NuxeoPrincipal.PREFIX)) {
                actorIds.add(actor.substring(NuxeoPrincipal.PREFIX.length()));
            } else if (actor.startsWith(NuxeoGroup.PREFIX)) {
                actorIds.add(actor.substring(NuxeoGroup.PREFIX.length()));
            } else {
                actorIds.add(actor);
            }
        }
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                for (DocumentModel doc : docs) {
                    ACP acp = doc.getACP();
                    acp.removeACL(aclName);
                    ACL acl = new ACLImpl(aclName);
                    for (String actorId : actorIds) {
                        acl.add(ACE.builder(actorId, permission).creator(ACTOR_ACE_CREATOR).build());
                    }
                    acp.addACL(0, acl); // add first to get before blocks
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }
            }

        }.runUnrestricted();
    }

    @Override
    public void cleanupDoneAndCanceledRouteInstances(final String reprositoryName, final int limit) {
        doCleanupDoneAndCanceledRouteInstances(reprositoryName, limit);
    }

    @Override
    public int doCleanupDoneAndCanceledRouteInstances(final String reprositoryName, final int limit) {
        WfCleaner unrestrictedSessionRunner = new WfCleaner(reprositoryName, limit);
        unrestrictedSessionRunner.runUnrestricted();
        return unrestrictedSessionRunner.getNumberOfCleanedUpWf();
    }

    @Override
    public void invalidateRouteModelsCache() {
        modelsChache.invalidateAll();
    }

    /**
     * @since 7.2
     */
    @Override
    public List<Task> getTasks(final DocumentModel document, String actorId, String workflowInstanceId,
            final String worflowModelName, CoreSession session) {
        StringBuilder query = new StringBuilder(
                String.format("SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s'",
                        TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE));
        if (StringUtils.isNotBlank(actorId)) {
            List<String> actors = new ArrayList<String>();
            UserManager userManager = Framework.getService(UserManager.class);
            NuxeoPrincipal principal = userManager.getPrincipal(actorId);
            if (principal != null) {
                for (String actor : TaskActorsHelper.getTaskActors(principal)) {
                    actors.add(NXQL.escapeString(actor));
                }
            } else {
                actors.add(NXQL.escapeString(actorId));
            }
            String actorsParam = StringUtils.join(actors, ", ");
            query.append(String.format(" AND (nt:actors/* IN (%s) OR nt:delegatedActors/* IN (%s))", actorsParam,
                    actorsParam));
        }
        if (StringUtils.isNotBlank(workflowInstanceId)) {
            query.append(String.format(" AND nt:processId = %s", NXQL.escapeString(workflowInstanceId)));
        }
        if (document != null) {
            query.append(String.format(" AND nt:targetDocumentsIds = '%s'", document.getId()));
        }
        query.append(String.format(" ORDER BY %s ASC", TaskConstants.TASK_DUE_DATE_PROPERTY_NAME));
        final DocumentModelList documentModelList = session.query(query.toString());
        final List<Task> result = new ArrayList<>();

        // User does not necessary have READ on the workflow instance
        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                for (DocumentModel documentModel : documentModelList) {
                    final Task task = documentModel.getAdapter(Task.class);
                    if (StringUtils.isNotBlank(worflowModelName)) {

                        final String processId = task.getProcessId();
                        if (processId != null && session.exists(new IdRef(processId))) {
                            final DocumentRoute routeInstance = session.getDocument(new IdRef(processId))
                                                                       .getAdapter(DocumentRoute.class);
                            if (routeInstance != null) {
                                final String routeInstanceName = routeInstance.getName();
                                if (routeInstanceName != null && (routeInstanceName.equals(worflowModelName)
                                        || routeInstanceName.matches("^(" + worflowModelName + ")\\.\\d+"))) {
                                    result.add(task);
                                }
                            }
                        }
                    } else {
                        result.add(task);
                    }
                }
            }
        }.runUnrestricted();

        return result;
    }

    /**
     * @since 7.2
     */
    @Override
    public List<DocumentRoute> getDocumentRelatedWorkflows(DocumentModel document, CoreSession session) {
        final String query = String.format(
                "SELECT * FROM %s WHERE docri:participatingDocuments/* = '%s' AND ecm:currentLifeCycleState = '%s'",
                DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE, document.getId(),
                DocumentRouteElement.ElementLifeCycleState.running);
        DocumentModelList documentModelList = session.query(query);
        List<DocumentRoute> result = new ArrayList<>();
        for (DocumentModel documentModel : documentModelList) {
            result.add(documentModel.getAdapter(GraphRoute.class));
        }
        return result;
    }

    /**
     * @since 7.2
     */
    @Override
    public List<DocumentRoute> getRunningWorkflowInstancesLaunchedByCurrentUser(CoreSession session) {
        return getRunningWorkflowInstancesLaunchedByCurrentUser(session, null);
    }

    /**
     * @since 7.2
     */
    @Override
    public List<DocumentRoute> getRunningWorkflowInstancesLaunchedByCurrentUser(CoreSession session,
            String worflowModelName) {
        final String query = String.format(
                "SELECT * FROM %s WHERE docri:initiator = '%s' AND ecm:currentLifeCycleState = '%s'",
                DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE, session.getPrincipal().getName(),
                DocumentRouteElement.ElementLifeCycleState.running);
        DocumentModelList documentModelList = session.query(query);
        List<DocumentRoute> result = new ArrayList<>();
        for (DocumentModel documentModel : documentModelList) {
            final GraphRoute graphRoute = documentModel.getAdapter(GraphRoute.class);
            if (StringUtils.isNotBlank(worflowModelName)) {
                final String modelId = graphRoute.getModelId();
                if (StringUtils.isNotBlank(modelId)) {
                    DocumentRoute model = session.getDocument(new IdRef(modelId)).getAdapter(DocumentRoute.class);
                    if (worflowModelName.equals(model.getName())) {
                        result.add(graphRoute);
                    }
                }
            } else {
                result.add(graphRoute);
            }
        }
        return result;
    }

    /**
     * Returns true id the document route is a model, false if it is just an instance i.e. a running workflow.
     *
     * @since 7.2
     */
    @Override
    public boolean isWorkflowModel(final DocumentRoute documentRoute) {
        return documentRoute.isValidated();
    }
}
