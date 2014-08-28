/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.MAX_RESULTS_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.PAGE_SIZE_RESULTS_KEY;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS_PROVIDER_NAME;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
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
import org.nuxeo.ecm.platform.routing.core.listener.RouteModelsInitializator;
import org.nuxeo.ecm.platform.routing.core.registries.RouteTemplateResourceRegistry;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
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
public class DocumentRoutingServiceImpl extends DefaultComponent implements
        DocumentRoutingService {

    private static Log log = LogFactory.getLog(DocumentRoutingServiceImpl.class);

    /** Routes in any state (model or not). */
    private static final String AVAILABLE_ROUTES_QUERY = String.format(
            "SELECT * FROM %s",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    /** Route models that have been validated. */
    private static final String ROUTE_MODEL_WITH_ID_QUERY = String.format(
            "SELECT * FROM %s WHERE ecm:name = %%s AND ecm:currentLifeCycleState = 'validated' AND ecm:isCheckedInVersion  = 0  AND ecm:isProxy = 0 ",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    /** Route models that have been validated. */
    private static final String ROUTE_MODEL_DOC_ID_WITH_ID_QUERY = String.format(
            "SELECT ecm:uuid FROM %s WHERE ecm:name = %%s AND ecm:currentLifeCycleState = 'validated' AND ecm:isCheckedInVersion  = 0  AND ecm:isProxy = 0 ",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    private static final String ORDERED_CHILDREN_QUERY = "SELECT * FROM Document WHERE"
            + " ecm:parentId = '%s' AND ecm:isCheckedInVersion  = 0 AND "
            + "ecm:currentLifeCycleState != 'deleted' ORDER BY ecm:pos";

    public static final String CHAINS_TO_TYPE_XP = "chainsToType";

    public static final String PERSISTER_XP = "persister";

    // FIXME: use ContributionFragmentRegistry instances instead to handle hot
    // reload

    public static final String ROUTE_MODELS_IMPORTER_XP = "routeModelImporter";

    protected Map<String, String> typeToChain = new HashMap<String, String>();

    protected Map<String, String> undoChainIdFromRunning = new HashMap<String, String>();

    protected Map<String, String> undoChainIdFromDone = new HashMap<String, String>();

    protected DocumentRoutingPersister persister;

    protected RouteTemplateResourceRegistry routeResourcesRegistry = new RouteTemplateResourceRegistry();

    protected RepositoryInitializationHandler repositoryInitializationHandler;

    private Cache<String, String> modelsChache;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CHAINS_TO_TYPE_XP.equals(extensionPoint)) {
            ChainToTypeMappingDescriptor desc = (ChainToTypeMappingDescriptor) contribution;
            typeToChain.put(desc.getDocumentType(), desc.getChainId());
            undoChainIdFromRunning.put(desc.getDocumentType(),
                    desc.getUndoChainIdFromRunning());
            undoChainIdFromDone.put(desc.getDocumentType(),
                    desc.getUndoChainIdFromDone());
        } else if (PERSISTER_XP.equals(extensionPoint)) {
            PersisterDescriptor des = (PersisterDescriptor) contribution;
            persister = des.getKlass().newInstance();
        } else if (ROUTE_MODELS_IMPORTER_XP.equals(extensionPoint)) {
            RouteModelResourceType res = (RouteModelResourceType) contribution;
            registerRouteResource(res, contributor.getRuntimeContext());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof RouteModelResourceType) {
            routeResourcesRegistry.removeContribution((RouteModelResourceType) contribution);
        }
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    protected static void fireEvent(String eventName,
            Map<String, Serializable> eventProperties, DocumentRoute route,
            CoreSession session) {
        eventProperties.put(
                DocumentRoutingConstants.DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY,
                route);
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                DocumentRoutingConstants.ROUTING_CATEGORY);
        DocumentEventContext envContext = new DocumentEventContext(session,
                session.getPrincipal(), route.getDocument());
        envContext.setProperties(eventProperties);
        EventProducer eventProducer = Framework.getLocalService(EventProducer.class);
        try {
            eventProducer.fireEvent(envContext.newEvent(eventName));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public String createNewInstance(final String routeModelId,
            final List<String> docIds, final Map<String, Serializable> map,
            CoreSession session, final boolean startInstance) {
        try {
            final String initiator = session.getPrincipal().getName();
            final String res[] = new String[1];
            new UnrestrictedSessionRunner(session) {

                protected DocumentRoute route;

                @Override
                public void run() throws ClientException {
                    String routeDocId = getRouteModelDocIdWithId(session,
                            routeModelId);
                    DocumentModel model = session.getDocument(new IdRef(
                            routeDocId));
                    DocumentModel instance = persister.createDocumentRouteInstanceFromDocumentRouteModel(
                            model, session);
                    route = instance.getAdapter(DocumentRoute.class);
                    route.setAttachedDocuments(docIds);
                    route.save(session);
                    Map<String, Serializable> props = new HashMap<String, Serializable>();
                    props.put(
                            DocumentRoutingConstants.INITIATOR_EVENT_CONTEXT_KEY,
                            initiator);
                    fireEvent(
                            DocumentRoutingConstants.Events.beforeRouteReady.name(),
                            props);
                    route.setReady(session);
                    fireEvent(
                            DocumentRoutingConstants.Events.afterRouteReady.name(),
                            props);
                    route.save(session);
                    if (startInstance) {
                        fireEvent(
                                DocumentRoutingConstants.Events.beforeRouteStart.name(),
                                new HashMap<String, Serializable>());
                        DocumentRoutingEngineService routingEngine = Framework.getLocalService(DocumentRoutingEngineService.class);
                        routingEngine.start(route, map, session);
                    }
                    res[0] = instance.getId();
                }

                protected void fireEvent(String eventName,
                        Map<String, Serializable> eventProperties) {
                    DocumentRoutingServiceImpl.fireEvent(eventName,
                            eventProperties, route, session);
                }

            }.runUnrestricted();

            return res[0];
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String createNewInstance(String routeModelId, List<String> docIds,
            CoreSession session, boolean startInstance) {
        return createNewInstance(routeModelId, docIds, null, session,
                startInstance);
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> docIds, CoreSession session, boolean startInstance) {
        String id = createNewInstance(model.getDocument().getName(), docIds,
                session, startInstance);
        try {
            return session.getDocument(new IdRef(id)).getAdapter(
                    DocumentRoute.class);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    @Deprecated
    public DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session, boolean startInstance) {
        return createNewInstance(model, Collections.singletonList(documentId),
                session, startInstance);
    }

    @Override
    @Deprecated
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session) {
        return createNewInstance(model, documentIds, session, true);
    }

    @Override
    @Deprecated
    public DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session) {
        return createNewInstance(model, Collections.singletonList(documentId),
                session, true);
    }

    @Override
    public void startInstance(final String routeInstanceId,
            final List<String> docIds, final Map<String, Serializable> map,
            CoreSession session) {
        try {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    DocumentModel instance = session.getDocument(new IdRef(
                            routeInstanceId));
                    DocumentRoute route = instance.getAdapter(DocumentRoute.class);
                    if (docIds != null) {
                        route.setAttachedDocuments(docIds);
                        route.save(session);
                    }
                    fireEvent(
                            DocumentRoutingConstants.Events.beforeRouteStart.name(),
                            new HashMap<String, Serializable>(), route, session);
                    DocumentRoutingEngineService routingEngine = Framework.getLocalService(DocumentRoutingEngineService.class);
                    routingEngine.start(route, map, session);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resumeInstance(String routeId, String nodeId,
            Map<String, Object> data, String status, CoreSession session) {
        completeTask(routeId, nodeId, null, data, status, session);
    }

    @Override
    public void completeTask(String routeId, String taskId,
            Map<String, Object> data, String status, CoreSession session) {
        completeTask(routeId, null, taskId, data, status, session);
    }

    protected void completeTask(final String routeId, final String nodeId,
            final String taskId, final Map<String, Object> data,
            final String status, CoreSession session) {
        try {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    DocumentRoutingEngineService routingEngine = Framework.getLocalService(DocumentRoutingEngineService.class);
                    DocumentModel routeDoc = session.getDocument(new IdRef(
                            routeId));
                    DocumentRoute routeInstance = routeDoc.getAdapter(DocumentRoute.class);
                    routingEngine.resume(routeInstance, nodeId, taskId, data,
                            status, session);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public List<DocumentRoute> getAvailableDocumentRouteModel(
            CoreSession session) {
        DocumentModelList list = null;
        try {
            list = session.query(AVAILABLE_ROUTES_QUERY);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        List<DocumentRoute> routes = new ArrayList<DocumentRoute>();
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
    public DocumentRoute unlockDocumentRouteUnrestrictedSession(
            final DocumentRoute routeModel, CoreSession userSession)
            throws ClientException {
        new UnrestrictedSessionRunner(userSession) {
            @Override
            public void run() throws ClientException {
                DocumentRoute route = session.getDocument(
                        routeModel.getDocument().getRef()).getAdapter(
                        DocumentRoute.class);
                LockableDocumentRoute lockableRoute = route.getDocument().getAdapter(
                        LockableDocumentRoute.class);
                lockableRoute.unlockDocument(session);
            }
        }.runUnrestricted();
        return userSession.getDocument(routeModel.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
    }

    @Override
    public DocumentRoute validateRouteModel(final DocumentRoute routeModel,
            CoreSession userSession) throws DocumentRouteNotLockedException,
            ClientException {
        if (!routeModel.getDocument().isLocked()) {
            throw new DocumentRouteNotLockedException();
        }
        new UnrestrictedSessionRunner(userSession) {
            @Override
            public void run() throws ClientException {
                DocumentRoute route = session.getDocument(
                        routeModel.getDocument().getRef()).getAdapter(
                        DocumentRoute.class);
                route.validate(session);
            }
        }.runUnrestricted();
        return userSession.getDocument(routeModel.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    @Override
    public List<DocumentRouteTableElement> getRouteElements(
            DocumentRoute route, CoreSession session) {
        RouteTable table = new RouteTable(route);
        List<DocumentRouteTableElement> elements = new ArrayList<DocumentRouteTableElement>();
        processElementsInFolder(route.getDocument(), elements, table, session,
                0, null);
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
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    protected void processElementsInFolder(DocumentModel doc,
            List<DocumentRouteTableElement> elements, RouteTable table,
            CoreSession session, int depth, RouteFolderElement folder) {
        try {
            DocumentModelList children = session.getChildren(doc.getRef());
            boolean first = true;
            for (DocumentModel child : children) {
                if (child.isFolder()
                        && !session.getChildren(child.getRef()).isEmpty()) {
                    RouteFolderElement thisFolder = new RouteFolderElement(
                            child.getAdapter(DocumentRouteElement.class),
                            table, first, folder, depth);
                    processElementsInFolder(child, elements, table, session,
                            depth + 1, thisFolder);
                } else {
                    if (folder != null) {
                        folder.increaseTotalChildCount();
                    } else {
                        table.increaseTotalChildCount();
                    }
                    elements.add(new DocumentRouteTableElement(
                            child.getAdapter(DocumentRouteElement.class),
                            table, depth, folder, first));
                }
                first = false;
            }
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    protected List<DocumentRouteTableElement> getRouteElements(
            DocumentRouteElement routeElementDocument, CoreSession session,
            List<DocumentRouteTableElement> routeElements, int depth) {
        return null;
    }

    @Override
    public List<DocumentRoute> getDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId) {
        List<DocumentRouteElement.ElementLifeCycleState> states = new ArrayList<DocumentRouteElement.ElementLifeCycleState>();
        states.add(DocumentRouteElement.ElementLifeCycleState.ready);
        states.add(DocumentRouteElement.ElementLifeCycleState.running);
        return getDocumentRoutesForAttachedDocument(session, attachedDocId,
                states);
    }

    @Override
    public List<DocumentRoute> getDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId,
            List<DocumentRouteElement.ElementLifeCycleState> states) {
        DocumentModelList list = null;
        StringBuilder statesString = new StringBuilder();
        if (states != null && !states.isEmpty()) {
            statesString.append(" ecm:currentLifeCycleState IN (");
            for (DocumentRouteElement.ElementLifeCycleState state : states) {
                statesString.append("'" + state.name() + "',");
            }
            statesString.deleteCharAt(statesString.length() - 1);
            statesString.append(") AND");
        }
        String query = String.format("SELECT * FROM DocumentRoute WHERE "
                + statesString.toString()
                + " docri:participatingDocuments = '%s'"
                // ordering by dc:created makes sure that
                // a sub-workflow is listed under its parent
                + " ORDER BY dc:created", attachedDocId);
        try {
            UnrestrictedQueryRunner queryRunner = new UnrestrictedQueryRunner(
                    session, query);
            list = queryRunner.runQuery();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        List<DocumentRoute> routes = new ArrayList<DocumentRoute>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
        return routes;
    }

    @Override
    public boolean canUserValidateRoute(NuxeoPrincipal currentUser) {
        return currentUser.getGroups().contains(
                DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME);
    }

    @Override
    public boolean canValidateRoute(DocumentModel documentRoute,
            CoreSession coreSession) throws ClientException {
        if (!coreSession.hasChildren(documentRoute.getRef())) {
            // Cannot validate an empty route
            return false;
        }
        return coreSession.hasPermission(documentRoute.getRef(),
                SecurityConstants.EVERYTHING);
    }

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Override
    @Deprecated
    public void addRouteElementToRoute(DocumentRef parentDocumentRef, int idx,
            DocumentRouteElement routeElement, CoreSession session)
            throws DocumentRouteNotLockedException, ClientException {
        DocumentRoute route = getParentRouteModel(parentDocumentRef, session);
        if (!isLockedByCurrentUser(route, session)) {
            throw new DocumentRouteNotLockedException();
        }
        DocumentModelList children = session.query(String.format(
                ORDERED_CHILDREN_QUERY,
                session.getDocument(parentDocumentRef).getId()));
        DocumentModel sourceDoc;
        try {
            sourceDoc = children.get(idx);
            addRouteElementToRoute(parentDocumentRef, sourceDoc.getName(),
                    routeElement, session);
        } catch (IndexOutOfBoundsException e) {
            addRouteElementToRoute(parentDocumentRef, null, routeElement,
                    session);
        }
    }

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Override
    @Deprecated
    public void addRouteElementToRoute(DocumentRef parentDocumentRef,
            String sourceName, DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException {
        DocumentRoute parentRoute = getParentRouteModel(parentDocumentRef,
                session);
        if (!isLockedByCurrentUser(parentRoute, session)) {
            throw new DocumentRouteNotLockedException();
        }
        PathSegmentService pss;
        try {
            pss = Framework.getService(PathSegmentService.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
        DocumentModel docRouteElement = routeElement.getDocument();
        DocumentModel parentDocument = session.getDocument(parentDocumentRef);
        docRouteElement.setPathInfo(parentDocument.getPathAsString(),
                pss.generatePathSegment(docRouteElement));
        String lifecycleState = parentDocument.getCurrentLifeCycleState().equals(
                DocumentRouteElement.ElementLifeCycleState.draft.name()) ? DocumentRouteElement.ElementLifeCycleState.draft.name()
                : DocumentRouteElement.ElementLifeCycleState.ready.name();
        docRouteElement.putContextData(
                LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME,
                lifecycleState);
        docRouteElement = session.createDocument(docRouteElement);
        session.orderBefore(parentDocumentRef, docRouteElement.getName(),
                sourceName);
        session.save();// the new document will be queried later on
    }

    @Override
    public void removeRouteElement(DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException {
        DocumentRoute parentRoute = routeElement.getDocumentRoute(session);
        if (!isLockedByCurrentUser(parentRoute, session)) {
            throw new DocumentRouteNotLockedException();
        }
        session.removeDocument(routeElement.getDocument().getRef());
        session.save();// the document will be queried later on
    }

    @Override
    public DocumentModelList getOrderedRouteElement(String routeElementId,
            CoreSession session) throws ClientException {
        String query = String.format(ORDERED_CHILDREN_QUERY, routeElementId);
        DocumentModelList orderedChildren = session.query(query);
        return orderedChildren;
    }

    @Override
    public void lockDocumentRoute(DocumentRoute routeModel, CoreSession session)
            throws DocumentRouteAlredayLockedException, ClientException {
        LockableDocumentRoute lockableRoute = routeModel.getDocument().getAdapter(
                LockableDocumentRoute.class);
        boolean lockedByCurrent = isLockedByCurrentUser(routeModel, session);
        if (lockableRoute.isLocked(session) && !lockedByCurrent) {
            throw new DocumentRouteAlredayLockedException();
        }
        if (!lockedByCurrent) {
            lockableRoute.lockDocument(session);
        }
    }

    @Override
    public void unlockDocumentRoute(DocumentRoute routeModel,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException {
        LockableDocumentRoute lockableRoute = routeModel.getDocument().getAdapter(
                LockableDocumentRoute.class);
        if (!lockableRoute.isLockedByCurrentUser(session)) {
            throw new DocumentRouteNotLockedException();
        }
        lockableRoute.unlockDocument(session);
    }

    @Override
    public boolean isLockedByCurrentUser(DocumentRoute routeModel,
            CoreSession session) throws ClientException {
        LockableDocumentRoute lockableRoute = routeModel.getDocument().getAdapter(
                LockableDocumentRoute.class);
        return lockableRoute.isLockedByCurrentUser(session);
    }

    @Override
    public void updateRouteElement(DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException {
        if (!isLockedByCurrentUser(routeElement.getDocumentRoute(session),
                session)) {
            throw new DocumentRouteNotLockedException();
        }
        routeElement.save(session);
    }

    private DocumentRoute getParentRouteModel(DocumentRef documentRef,
            CoreSession session) throws ClientException {
        DocumentModel parentDoc = session.getDocument(documentRef);
        if (parentDoc.hasFacet(DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_FACET)) {
            return parentDoc.getAdapter(DocumentRoute.class);
        }
        DocumentRouteElement rElement = parentDoc.getAdapter(DocumentRouteElement.class);
        return rElement.getDocumentRoute(session);

    }

    @Override
    public DocumentRoute saveRouteAsNewModel(DocumentRoute instance,
            CoreSession session) {
        DocumentModel instanceModel = instance.getDocument();
        DocumentModel parent = persister.getParentFolderForNewModel(session,
                instanceModel);
        String newName = persister.getNewModelName(instanceModel);
        try {
            DocumentModel newmodel = persister.saveDocumentRouteInstanceAsNewModel(
                    instanceModel, parent, newName, session);
            DocumentRoute newRoute = newmodel.getAdapter(DocumentRoute.class);
            if (!newRoute.isDraft()) {
                newRoute.followTransition(
                        DocumentRouteElement.ElementLifeCycleTransistion.toDraft,
                        session, false);
            }
            newRoute.getDocument().setPropertyValue("dc:title", newName);
            newRoute.setAttachedDocuments(new ArrayList<String>());
            newRoute.save(session);
            return newRoute;
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
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
    public DocumentRoute importRouteModel(URL modelToImport, boolean overwrite,
            CoreSession session) throws ClientException {
        if (modelToImport == null) {
            throw new ClientException(
                    ("No resource containing route templates found"));
        }
        StreamingBlob fb;
        try {
            fb = StreamingBlob.createFromStream(modelToImport.openStream());
        } catch (IOException e) {
            throw new ClientRuntimeException(e);
        }
        try {
            final String file = modelToImport.getFile();
            DocumentModel doc = getFileManager().createDocumentFromBlob(
                    session,
                    fb,
                    persister.getParentFolderForDocumentRouteModels(session).getPathAsString(),
                    true, file);
            if (doc == null) {
                throw new ClientException("Can not import document " + file);
            }
            // remove model from cache if any model with the same id existed
            if (modelsChache != null) {
                modelsChache.invalidate(doc.getName());
            }

            return doc.getAdapter(DocumentRoute.class);
        } catch (Exception e) {
            throw new ClientException(e);
        } finally {
            try {
                FileUtils.close(fb.getStream());
            } catch (IOException e) {
                throw new ClientException(e);
            }
        }
    }

    protected FileManager getFileManager() {
        try {
            return Framework.getService(FileManager.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        modelsChache = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(
                10, TimeUnit.MINUTES).build();
        repositoryInitializationHandler = new RouteModelsInitializator();
        repositoryInitializationHandler.install();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        if (repositoryInitializationHandler != null) {
            repositoryInitializationHandler.uninstall();
        }
    }

    @Override
    public List<URL> getRouteModelTemplateResources() throws ClientException {
        List<URL> urls = new ArrayList<URL>();
        for (URL url : routeResourcesRegistry.getRouteModelTemplateResources()) {
            urls.add(url); // test contrib parsing and deployment
        }
        return urls;
    }

    @Override
    public List<DocumentModel> searchRouteModels(CoreSession session,
            String searchString) throws ClientException {
        List<DocumentModel> allRouteModels = new ArrayList<DocumentModel>();
        PageProviderService pageProviderService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(MAX_RESULTS_PROPERTY, PAGE_SIZE_RESULTS_KEY);
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);
        @SuppressWarnings({ "unchecked", "boxing" })
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS_PROVIDER_NAME, null, null,
                0L, props, String.format("%s%%", searchString));
        allRouteModels.addAll(pageProvider.getCurrentPage());
        while (pageProvider.isNextPageAvailable()) {
            pageProvider.nextPage();
            allRouteModels.addAll(pageProvider.getCurrentPage());
        }
        return allRouteModels;
    }

    @Override
    public void registerRouteResource(RouteModelResourceType res,
            RuntimeContext context) {
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

    protected URL getUrlFromPath(RouteModelResourceType res,
            RuntimeContext extensionContext) {
        String path = res.getPath();
        if (path == null) {
            return null;
        }
        URL url = null;
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
    public DocumentRoute getRouteModelWithId(CoreSession session, String id)
            throws ClientException {
        String routeDocModelId = getRouteModelDocIdWithId(session, id);
        DocumentModel routeDoc = session.getDocument(new IdRef(routeDocModelId));
        return routeDoc.getAdapter(DocumentRoute.class);
    }

    @Override
    public String getRouteModelDocIdWithId(CoreSession session, String id)
            throws ClientException {
        if (modelsChache != null) {
            String routeDocId = modelsChache.getIfPresent(id);
            if (routeDocId != null) {
                return routeDocId;
            }
        }
        String query = String.format(ROUTE_MODEL_DOC_ID_WITH_ID_QUERY,
                NXQL.escapeString(id));
        IterableQueryResult results = session.queryAndFetch(query, "NXQL");
        if (results.size() == 0) {
            throw new ClientRuntimeException("No route found for id: " + id);
        }
        if (results.size() != 1) {
            throw new ClientRuntimeException(
                    "More than one route model found with id: " + id);
        }
        List<String> routeIds = new ArrayList<String>();
        for (Map<String, Serializable> map : results) {
            routeIds.add(map.get("ecm:uuid").toString());
        }
        results.close();
        String routeDocId = routeIds.get(0);
        if (modelsChache == null) {
            modelsChache = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(
                    10, TimeUnit.MINUTES).build();
        }
        modelsChache.put(id, routeDocId);
        return routeDocId;
    }

    @Override
    public void makeRoutingTasks(CoreSession coreSession, final List<Task> tasks)
            throws ClientException {
        new UnrestrictedSessionRunner(coreSession) {
            @Override
            public void run() throws ClientException {
                for (Task task : tasks) {
                    DocumentModel taskDoc = task.getDocument();
                    taskDoc.addFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME);
                    session.saveDocument(taskDoc);
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void endTask(CoreSession session, Task task,
            Map<String, Object> data, String status) throws ClientException {
        String comment = (String) data.get(GraphNode.NODE_VARIABLE_COMMENT);
        TaskService taskService = Framework.getLocalService(TaskService.class);
        taskService.endTask(session, (NuxeoPrincipal) session.getPrincipal(),
                task, comment, TaskEventNames.WORKFLOW_TASK_COMPLETED, false);

        Map<String, String> taskVariables = task.getVariables();
        String routeInstanceId = taskVariables.get(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (StringUtils.isEmpty(routeInstanceId)) {
            throw new DocumentRouteException(
                    "Can not resume workflow, no related route");
        }
        completeTask(routeInstanceId, null, task.getId(), data, status, session);
    }

    @Override
    public List<DocumentModel> getWorkflowInputDocuments(CoreSession session,
            Task task) throws ClientException {
        String routeInstanceId;
        try {
            routeInstanceId = task.getProcessId();
        } catch (ClientException e) {
            throw new DocumentRouteException(
                    "Can not get the related workflow instance");
        }
        if (StringUtils.isEmpty(routeInstanceId)) {
            throw new DocumentRouteException(
                    "Can not get the related workflow instance");
        }
        DocumentModel routeDoc;
        try {
            routeDoc = session.getDocument(new IdRef(routeInstanceId));
        } catch (ClientException e) {
            throw new DocumentRouteException("No workflow with the id:"
                    + routeInstanceId);
        }
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        return route.getAttachedDocuments(session);
    }

    @Override
    public void grantPermissionToTaskAssignees(CoreSession session,
            String permission, List<DocumentModel> docs, Task task)
            throws ClientException {
        setAclForActors(session, getRoutingACLName(task), permission, docs,
                task.getActors());
    }

    @Override
    public void grantPermissionToTaskDelegatedActors(CoreSession session,
            String permission, List<DocumentModel> docs, Task task)
            throws ClientException {
        setAclForActors(session, getDelegationACLName(task), permission, docs,
                task.getDelegatedActors());
    }

    @Override
    public void removePermissionFromTaskAssignees(CoreSession session,
            final List<DocumentModel> docs, Task task) throws ClientException {
        final String aclName = getRoutingACLName(task);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                for (DocumentModel doc : docs) {
                    ACP acp = doc.getACP();
                    acp.removeACL(aclName);
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }
            };
        }.runUnrestricted();
    }

    @Override
    public void removePermissionsForTaskActors(CoreSession session,
            final List<DocumentModel> docs, Task task) throws ClientException {
        final String aclRoutingName = getRoutingACLName(task);
        final String aclDelegationName = getDelegationACLName(task);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                for (DocumentModel doc : docs) {
                    ACP acp = doc.getACP();
                    acp.removeACL(aclRoutingName);
                    acp.removeACL(aclDelegationName);
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }
            };
        }.runUnrestricted();
    }

    /**
     * Finds an ACL name specific to the task (there may be several tasks
     * applying permissions to the same document).
     */
    protected static String getRoutingACLName(Task task) {
        return DocumentRoutingConstants.DOCUMENT_ROUTING_ACL + '/'
                + task.getId();
    }

    protected static String getDelegationACLName(Task task) {
        return DocumentRoutingConstants.DOCUMENT_ROUTING_DELEGATION_ACL + '/'
                + task.getId();
    }

    class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {

        String query;

        DocumentModelList docs;

        protected UnrestrictedQueryRunner(CoreSession session, String query) {
            super(session);
            this.query = query;
        }

        @Override
        public void run() throws ClientException {
            docs = session.query(query);
            for (DocumentModel documentModel : docs) {
                documentModel.detach(true);
            }
        }

        public DocumentModelList runQuery() throws ClientException {
            runUnrestricted();
            return docs;
        }
    }

    @Override
    public void finishTask(CoreSession session, DocumentRoute route, Task task,
            boolean delete) throws DocumentRouteException {
        DocumentModelList docs = route.getAttachedDocuments(session);
        try {
            removePermissionsForTaskActors(session, docs, task);
            // delete task
            if (delete) {
                session.removeDocument(new IdRef(task.getId()));
            }
        } catch (ClientException e) {
            throw new DocumentRouteException("Cannot finish task", e);
        }
    }

    @Override
    public void cancelTask(CoreSession session, final String taskId)
            throws DocumentRouteException {
        try {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    DocumentModel taskDoc = session.getDocument(new IdRef(
                            taskId));
                    Task task = taskDoc.getAdapter(Task.class);
                    if (task == null) {
                        throw new DocumentRouteException("Invalid taskId: "
                                + taskId);
                    }

                    if (!task.isOpened()) {
                        log.info("Can not cancel task " + taskId
                                + "as is not open");
                        return;
                    }
                    task.cancel(session);

                    // if the task was created by an workflow , update info
                    String routeId = task.getProcessId();
                    if (routeId != null) {
                        DocumentModel routeDoc = session.getDocument(new IdRef(
                                routeId));
                        GraphRoute routeInstance = routeDoc.getAdapter(GraphRoute.class);
                        if (routeInstance == null) {
                            throw new DocumentRouteException(
                                    "Invalid routeInstanceId: " + routeId);
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
        } catch (ClientException e) {
            throw new DocumentRouteException("Cannot cancel task", e);
        }
    }

    protected void updateTaskInfo(CoreSession session, GraphRoute graph,
            Task task, String status) throws ClientException {
        String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (StringUtils.isEmpty(nodeId)) {
            throw new DocumentRouteException("No nodeId found on task: "
                    + task.getId());
        }
        GraphNode node = graph.getNode(nodeId);

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        String actor = principal.getActingUser();
        node.updateTaskInfo(task.getId(), true, status, actor, null);
    }

    @Override
    public void reassignTask(CoreSession session, final String taskId,
            final List<String> actors, final String comment)
            throws DocumentRouteException {
        try {
            new UnrestrictedSessionRunner(session) {

                @Override
                public void run() throws ClientException {
                    DocumentModel taskDoc = session.getDocument(new IdRef(
                            taskId));
                    Task task = taskDoc.getAdapter(Task.class);
                    if (task == null) {
                        throw new DocumentRouteException("Invalid taskId: "
                                + taskId);
                    }
                    if (!task.isOpened()) {
                        throw new DocumentRouteException("Task  " + taskId
                                + " is not opened, can not reassign it");
                    }
                    String routeId = task.getProcessId();
                    if (routeId != null) {
                        DocumentModel routeDoc = session.getDocument(new IdRef(
                                routeId));
                        GraphRoute routeInstance = routeDoc.getAdapter(GraphRoute.class);
                        if (routeInstance == null) {
                            throw new DocumentRouteException(
                                    "Invalid routeInstanceId: " + routeId
                                            + " referenced by the task "
                                            + taskId);
                        }
                        GraphNode node = routeInstance.getNode(task.getType());
                        if (node == null) {
                            throw new DocumentRouteException("Invalid node "
                                    + routeId + " referenced by the task "
                                    + taskId);
                        }
                        if (!node.allowTaskReassignment()) {
                            throw new DocumentRouteException("Task " + taskId
                                    + " can not be reassigned. Node "
                                    + node.getId()
                                    + " doesn't allow reassignment.");
                        }
                        DocumentModelList docs = routeInstance.getAttachedDocumentModels();
                        // remove permissions on the document following the
                        // workflow for the current assignees
                        removePermissionFromTaskAssignees(session, docs, task);
                        Framework.getLocalService(TaskService.class).reassignTask(
                                session, taskId, actors, comment);
                        // refresh task
                        task.getDocument().refresh();
                        // grant permission to the new assignees
                        grantPermissionToTaskAssignees(session,
                                node.getTaskAssigneesPermission(), docs, task);
                    }
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new DocumentRouteException("Can not reassign task " + taskId,
                    e);
        }
    }

    @Override
    public void delegateTask(CoreSession session, final String taskId,
            final List<String> delegatedActors, final String comment)
            throws DocumentRouteException {
        try {
            new UnrestrictedSessionRunner(session) {

                @Override
                public void run() throws ClientException {
                    DocumentModel taskDoc = session.getDocument(new IdRef(
                            taskId));
                    Task task = taskDoc.getAdapter(Task.class);
                    if (task == null) {
                        throw new DocumentRouteException("Invalid taskId: "
                                + taskId);
                    }
                    String routeId = task.getProcessId();
                    if (routeId != null) {
                        DocumentModel routeDoc = session.getDocument(new IdRef(
                                routeId));
                        GraphRoute routeInstance = routeDoc.getAdapter(GraphRoute.class);
                        if (routeInstance == null) {
                            throw new DocumentRouteException(
                                    "Invalid routeInstanceId: " + routeId
                                            + " referenced by the task "
                                            + taskId);
                        }
                        GraphNode node = routeInstance.getNode(task.getType());
                        if (node == null) {
                            throw new DocumentRouteException("Invalid node "
                                    + routeId + " referenced by the task "
                                    + taskId);
                        }
                        DocumentModelList docs = routeInstance.getAttachedDocumentModels();
                        Framework.getLocalService(TaskService.class).delegateTask(
                                session, taskId, delegatedActors, comment);
                        // refresh task
                        task.getDocument().refresh();
                        // grant permission to the new assignees
                        grantPermissionToTaskDelegatedActors(session,
                                node.getTaskAssigneesPermission(), docs, task);
                    }
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new DocumentRouteException("Can not delegate task " + taskId,
                    e);
        }
    }

    protected void setAclForActors(CoreSession session, final String aclName,
            final String permission, final List<DocumentModel> docs,
            List<String> actors) throws ClientException {
        final List<String> actorIds = new ArrayList<String>();
        for (String actor : actors) {
            if (actor.contains(":")) {
                actorIds.add(actor.split(":")[1]);
            } else {
                actorIds.add(actor);
            }
        }
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                for (DocumentModel doc : docs) {
                    ACP acp = doc.getACP();
                    acp.removeACL(aclName);
                    ACL acl = new ACLImpl(aclName);
                    for (String actorId : actorIds) {
                        acl.add(new ACE(actorId, permission, true));
                    }
                    acp.addACL(0, acl); // add first to get before blocks
                    doc.setACP(acp, true);
                    session.saveDocument(doc);
                }
            }

        }.runUnrestricted();
    }

    @Override
    public void cleanupDoneAndCanceledRouteInstances(String reprositoryName,
            final int limit) throws ClientException {
        new UnrestrictedSessionRunner(reprositoryName) {

            @Override
            public void run() throws ClientException {
                List<String> routeIds = new ArrayList<String>();
                String query = "SELECT ecm:uuid FROM DocumentRoute WHERE (ecm:currentLifeCycleState = 'done' "
                        + "OR ecm:currentLifeCycleState = 'canceled') ORDER BY dc:created";
                IterableQueryResult results = session.queryAndFetch(query,
                        "NXQL");
                int i = 0;
                for (Map<String, Serializable> result : results) {
                    routeIds.add(result.get("ecm:uuid").toString());
                    i++;
                    // stop when the limit is reached and close the resultSet
                    if (i == limit) {
                        break;
                    }
                }
                results.close();
                for (String routeDocId : routeIds) {
                    session.removeDocument(new IdRef(routeDocId));
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void invalidateRouteModelsCache() {
        modelsChache.invalidateAll();
    }
}