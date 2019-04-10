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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
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
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.listener.RouteModelsInitializator;
import org.nuxeo.ecm.platform.routing.core.registries.RouteTemplateResourceRegistry;
import org.nuxeo.ecm.platform.routing.core.runner.CreateNewRouteInstanceUnrestricted;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * The implementation of the routing service.
 */
public class DocumentRoutingServiceImpl extends DefaultComponent implements
        DocumentRoutingService {

    private static final Log log = LogFactory.getLog(DocumentRoutingServiceImpl.class);

    /** Routes in any state (model or not). */
    private static final String AVAILABLE_ROUTES_QUERY = String.format(
            "SELECT * FROM %s",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    /** Route models that have been validated. */
    private static final String ROUTE_MODEL_WITH_ID_QUERY = String.format(
            "SELECT * FROM %s WHERE ecm:name = %%s AND ecm:currentLifeCycleState = 'validated'",
            DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE);

    private static final String ORDERED_CHILDREN_QUERY = "SELECT * FROM Document WHERE"
            + " ecm:parentId = '%s' AND ecm:isCheckedInVersion  = 0 AND "
            + "ecm:mixinType != 'HiddenInNavigation' AND "
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

    protected DocumentRoutingEngineService getEngineService() {
        try {
            return Framework.getService(DocumentRoutingEngineService.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

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

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> docIds, CoreSession session, boolean startInstance) {
        CreateNewRouteInstanceUnrestricted runner = new CreateNewRouteInstanceUnrestricted(
                session, model, docIds, startInstance, persister);
        try {
            runner.runUnrestricted();
            DocumentRef routeRef = runner.getInstance().getDocument().getRef();
            return session.getDocument(routeRef).getAdapter(DocumentRoute.class);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session, boolean startInstance) {
        return createNewInstance(model, Collections.singletonList(documentId),
                session, startInstance);
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session) {
        return createNewInstance(model, documentIds, session, true);
    }

    @Override
    public DocumentRoute createNewInstance(DocumentRoute model,
            String documentId, CoreSession session) {
        return createNewInstance(model, Collections.singletonList(documentId),
                session, true);
    }

    @Override
    public void resumeInstance(DocumentRef routeRef, CoreSession session,
            String nodeId, Map<String, Object> data) {
        try {
            new ResumeRouteInstanceRunner(routeRef, session, nodeId, data).runUnrestricted();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public static class ResumeRouteInstanceRunner extends
            UnrestrictedSessionRunner {

        protected DocumentRef routeRef;

        protected String nodeId;

        protected Map<String, Object> data;

        public ResumeRouteInstanceRunner(DocumentRef routeRef,
                CoreSession session, String nodeId, Map<String, Object> data) {
            super(session);
            this.routeRef = routeRef;
            this.nodeId = nodeId;
            this.data = data;
        }

        @Override
        public void run() throws ClientException {
            DocumentRoutingEngineService engineService = Framework.getLocalService(DocumentRoutingEngineService.class);
            DocumentModel routeDoc = session.getDocument(routeRef);
            DocumentRoute routeInstance = routeDoc.getAdapter(DocumentRoute.class);
            engineService.resume(routeInstance, session, nodeId, data, null);
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
        String RELATED_TOUTES_QUERY = String.format(
                " SELECT * FROM DocumentRoute WHERE " + statesString.toString()
                        + " docri:participatingDocuments IN ('%s') ",
                attachedDocId);
        try {
            list = session.query(RELATED_TOUTES_QUERY);
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

    @Override
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

    @Override
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
            DocumentModel doc = getFileManager().createDocumentFromBlob(
                    session,
                    fb,
                    persister.getParentFolderForDocumentRouteModels(session).getPathAsString(),
                    true, modelToImport.getFile());
            if (doc == null) {
                throw new ClientException("Can not import document");
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
        RouteModelsInitializator routeInializator = new RouteModelsInitializator();
        routeInializator.install();
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
        PageProviderService pageProviderService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(MAX_RESULTS_PROPERTY, PAGE_SIZE_RESULTS_KEY);
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS_PROVIDER_NAME, null, null,
                0L, props, String.format("%s%%", searchString));
        return pageProvider.getCurrentPage();
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
        DocumentModelList list = null;
        String query = String.format(ROUTE_MODEL_WITH_ID_QUERY,
                NXQLQueryBuilder.prepareStringLiteral(id, true, true));
        try {
            list = session.query(query);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        if (list.size() != 1) {
            return null;
        }
        return list.get(0).getAdapter(DocumentRoute.class);
    }
}
