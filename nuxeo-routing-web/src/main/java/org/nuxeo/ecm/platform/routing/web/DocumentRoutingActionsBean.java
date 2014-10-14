/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.convert.Converter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteTableElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LockableDocumentRoute;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.edit.lock.LockActions;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Actions for current document route
 *
 * @author Mariana Cedica
 */
@Scope(CONVERSATION)
@Name("routingActions")
@Install(precedence = Install.FRAMEWORK)
public class DocumentRoutingActionsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentRoutingActionsBean.class);

    public static final String SOURCE_DOC_NAME = "source_doc_name";

    public static final String ROUTE_DOCUMENT_REF = "route_doc_ref";

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected WebActions webActions;

    @In(create = true)
    protected LockActions lockActions;

    @In(create = true, required = false)
    protected TypesTool typesTool;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected EventManager eventManager;

    @In(required = true, create = true)
    protected NuxeoPrincipal currentUser;

    @In(create = true)
    protected List<DocumentModel> relatedRoutes;

    @In(create = true)
    protected RelatedRouteActionBean relatedRouteAction;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    @RequestParameter("stepId")
    protected String stepId;

    protected String relatedRouteModelDocumentId;

    protected String docWithAttachedRouteId;

    protected String hiddenSourceDocId;

    protected String hiddenDocOrder;

    enum StepOrder {
        before, in, after
    }

    public DocumentRoutingService getDocumentRoutingService() {
        try {
            return Framework.getService(DocumentRoutingService.class);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Observer(value = { EventNames.DOCUMENT_CHANGED,
            EventNames.DOCUMENT_SELECTION_CHANGED })
    public void resetRelatedRouteDocumentId() {
        relatedRouteModelDocumentId = null;
    }

    public boolean isRoutable() {
        return getDocumentRoutingService().isRoutable(
                navigationContext.getCurrentDocument());
    }

    public String startRoute() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRoute currentRoute = currentDocument.getAdapter(DocumentRoute.class);
        if (currentRoute == null) {
            log.warn("Current document is not a workflow model");
            facesMessages.add(
                    StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.document.routing.no.workflow"));
            return null;
        }
        getDocumentRoutingService().createNewInstance(
                currentDocument.getName(), currentRoute.getAttachedDocuments(),
                documentManager, true);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_NEW_STARTED);
        webActions.resetTabList();
        return null;
    }

    /**
     * Gets the first related route.
     * <p>
     * When called on an actual route or route element, the route is returned.
     * <p>
     * When called on a regular document, the routing service is queried to get
     * the routes which have the current document attached.
     * <p>
     * When dealing with a regular document, this is DEPRECATED as several
     * graph routes may be related to the current document (for instance in the
     * case of sub-workflows). Use {@link #getRelatedRoutes} instead.
     */
    public DocumentRoute getRelatedRoute() {
        // try to see if actually the current document is a route
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (currentDocument.hasFacet(DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_FACET)) {
            docWithAttachedRouteId = null;
            return currentDocument.getAdapter(DocumentRoute.class);
        }
        // try to see if the current document is a routeElement
        DocumentRouteElement relatedRouteElement = currentDocument.getAdapter(DocumentRouteElement.class);
        if (relatedRouteElement != null) {
            docWithAttachedRouteId = null;
            return relatedRouteElement.getDocumentRoute(documentManager);
        }
        // else we must be in a document attached to a route
        List<DocumentRoute> routes = getRelatedRoutes();
        if (routes.isEmpty()) {
            return null;
        }
        docWithAttachedRouteId = currentDocument.getId();
        return routes.get(0);
    }

    /**
     * Gets the list of routes related to the current document, by querying the
     * routing service.
     *
     * @return the list of routes (may be empty)
     * @since 5.7.2
     */
    public List<DocumentRoute> getRelatedRoutes() {
        queryForRelatedRoutes();
        List<DocumentRoute> routes = new ArrayList<DocumentRoute>(
                relatedRoutes.size());
        for (DocumentModel doc : relatedRoutes) {
            routes.add(doc.getAdapter(DocumentRoute.class));
        }
        return routes;
    }

    protected void queryForRelatedRoutes() {
        if (relatedRoutes == null) {
            try {
                relatedRoutes = relatedRouteAction.findRelatedRoute();
            } catch (ClientException e) {
                log.error("Cannot get related routes", e);
                relatedRoutes = Collections.emptyList();
            }
        }
    }

    /**
     * Cancels the first workflow found on the current document
     */
    public String cancelRoute() throws ClientException {
        List<DocumentRoute> routes = getRelatedRoutes();
        if (routes.size() == 0) {
            log.error("No workflow to cancel");
            return null;
        }
        DocumentRoute route = routes.get(0);
        Framework.getLocalService(DocumentRoutingEngineService.class).cancel(
                route, documentManager);
        // force computing of tabs
        webActions.resetTabList();
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_CANCELED);
        Contexts.removeFromAllContexts("relatedRoutes");
        documentManager.save();
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

    public void saveRouteAsNewInstance() {
        getDocumentRoutingService().saveRouteAsNewModel(getRelatedRoute(),
                documentManager);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED);
        facesMessages.add(
                StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get(
                        "feedback.casemanagement.document.route.route_duplicated"));
    }

    public void saveSelectedRouteAsNewInstance() {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        if (!docs.isEmpty()) {
            DocumentRoute route;
            for (DocumentModel doc : docs) {
                route = doc.getAdapter(DocumentRoute.class);
                if (route != null) {
                    getDocumentRoutingService().saveRouteAsNewModel(route,
                            documentManager);
                }
            }
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED);
        facesMessages.add(
                StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get(
                        "feedback.casemanagement.document.route.selected_route_duplicated"));
    }

    public boolean getCanDuplicateRouteInstance() {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        if (docs.isEmpty()) {
            return false;
        }
        for (DocumentModel doc : docs) {
            if (!doc.hasFacet(DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_FACET)) {
                return false;
            }
        }
        return true;
    }

    public String validateRouteModel() throws ClientException {
        DocumentRoute currentRouteModel = getRelatedRoute();
        try {
            getDocumentRoutingService().validateRouteModel(currentRouteModel,
                    documentManager);
        } catch (DocumentRouteNotLockedException e) {
            facesMessages.add(
                    StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.not.locked"));
            return null;
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentRouteModel.getDocument());
        getDocumentRoutingService().unlockDocumentRouteUnrestrictedSession(
                currentRouteModel, documentManager);
        return null;
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    protected List<DocumentRouteTableElement> computeRouteElements()
            throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRoute currentRoute = currentDocument.getAdapter(DocumentRoute.class);
        return getElements(currentRoute);
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    protected List<DocumentRouteTableElement> computeRelatedRouteElements()
            throws ClientException {
        if (relatedRoutes.isEmpty()) {
            return new ArrayList<DocumentRouteTableElement>();
        }
        DocumentModel relatedRouteDocumentModel = documentManager.getDocument(new IdRef(
                relatedRoutes.get(0).getId()));
        DocumentRoute currentRoute = relatedRouteDocumentModel.getAdapter(DocumentRoute.class);
        return getElements(currentRoute);
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    protected List<DocumentRouteTableElement> getElements(
            DocumentRoute currentRoute) {
        return getDocumentRoutingService().getRouteElements(currentRoute,
                documentManager);
    }

    /**
     * Check if the related route to this case is started (ready or running) or
     * no
     */
    public boolean hasRelatedRoute() {
        queryForRelatedRoutes();
        return !relatedRoutes.isEmpty();
    }

    public String startRouteRelatedToCurrentDocument() throws ClientException {
        DocumentRoute route = getRelatedRoute();
        // check relatedRoutedoc id
        if (!StringUtils.isEmpty(relatedRouteModelDocumentId)) {
            DocumentModel model = documentManager.getDocument(new IdRef(
                    relatedRouteModelDocumentId));
            route = model.getAdapter(DocumentRoute.class);
        }
        if (route == null) {
            facesMessages.add(
                    StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.no.valid.route"));
            return null;
        }

        List<String> documentIds = new ArrayList<String>();
        documentIds.add(navigationContext.getCurrentDocument().getId());
        route.setAttachedDocuments(documentIds);
        getDocumentRoutingService().createNewInstance(
                route.getDocument().getName(), documentIds, documentManager,
                true);
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_NEW_STARTED);
        webActions.resetTabList();
        return null;
    }

    /**
     * returns true if the routeStarted on the current Document is editable (is
     * Ready )
     */
    public boolean routeRelatedToCurrentDocumentIsRunning()
            throws ClientException {
        DocumentRoute route = getRelatedRoute();
        if (route == null) {
            return false;
        }
        return route.isRunning();
    }

    public String getTypeDescription(DocumentRouteTableElement localizable) {
        return depthFormatter(localizable.getDepth(),
                localizable.getElement().getDocument().getType());
    }

    private String depthFormatter(int depth, String type) {
        StringBuilder depthFormatter = new StringBuilder();
        for (int i = 0; i < depth - 1; i++) {
            depthFormatter.append("__");
        }
        depthFormatter.append(type);
        return depthFormatter.toString();
    }

    public Converter getDocumentModelConverter() {
        return new DocumentModelConvertor(documentManager);
    }

    public boolean isStep(DocumentModel doc) {
        return (doc.hasFacet(DocumentRoutingConstants.ROUTE_STEP_FACET));
    }

    public boolean currentRouteModelIsDraft() {
        DocumentModel relatedRouteModel = navigationContext.getCurrentDocument();
        DocumentRoute routeModel = relatedRouteModel.getAdapter(DocumentRoute.class);
        if (routeModel == null) {
            return false;
        }
        return routeModel.isDraft();
    }

    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public String removeStep() throws ClientException {
        boolean alreadyLockedByCurrentUser = false;
        DocumentRoute routeModel = getRelatedRoute();
        if (getDocumentRoutingService().isLockedByCurrentUser(routeModel,
                documentManager)) {
            alreadyLockedByCurrentUser = true;
        } else {
            if (lockRoute(routeModel) == null) {
                return null;
            }
        }
        if (StringUtils.isEmpty(stepId)) {
            return null;
        }
        DocumentRef docRef = new IdRef(stepId);
        DocumentModel stepToDelete = documentManager.getDocument(docRef);
        try {
            getDocumentRoutingService().removeRouteElement(
                    stepToDelete.getAdapter(DocumentRouteElement.class),
                    documentManager);
        } catch (DocumentRouteNotLockedException e) {
            facesMessages.add(
                    StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.not.locked"));
            return null;
        }
        Contexts.removeFromAllContexts("relatedRoutes");
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                routeModel.getDocument());
        // Release the lock only when currentUser had locked it before
        // entering this method.
        if (!alreadyLockedByCurrentUser) {
            getDocumentRoutingService().unlockDocumentRoute(routeModel,
                    documentManager);
        }
        return null;
    }

    /**
     * Returns true if the givenDoc is a step that can be edited
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    public boolean isEditableStep(DocumentModel stepDoc) throws ClientException {
        DocumentRouteElement stepElement = stepDoc.getAdapter(DocumentRouteElement.class);
        // if fork, is not simple editable step
        if (stepDoc.hasFacet("Folderish")) {
            return false;
        }
        return stepElement.isModifiable();
    }

    /**
     * Returns true if the givenDoc is an routeElement that can be edited
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    public boolean isEditableRouteElement(DocumentModel stepDoc)
            throws ClientException {
        DocumentRouteElement stepElement = stepDoc.getAdapter(DocumentRouteElement.class);
        return stepElement.isModifiable();
    }

    @Factory(value = "currentRouteLockedByCurrentUser", scope = ScopeType.EVENT)
    public boolean isCurrentRouteLockedByCurrentUser() throws ClientException {
        return getDocumentRoutingService().isLockedByCurrentUser(
                getRelatedRoute(), documentManager);
    }

    public boolean isCurrentRouteLocked() throws ClientException {
        LockableDocumentRoute lockableRoute = getRelatedRoute().getDocument().getAdapter(
                LockableDocumentRoute.class);
        return lockableRoute.isLocked(documentManager);
    }

    public boolean canUnlockRoute() throws ClientException {
        return Boolean.TRUE.equals(lockActions.getCanUnlockDoc(getRelatedRoute().getDocument()));
    }

    public boolean canLockRoute() throws ClientException {
        return Boolean.TRUE.equals(lockActions.getCanLockDoc(getRelatedRoute().getDocument()));
    }

    public Map<String, Serializable> getCurrentRouteLockDetails()
            throws ClientException {
        return lockActions.getLockDetails(getRelatedRoute().getDocument());
    }

    public String lockCurrentRoute() throws ClientException {
        DocumentRoute docRouteElement = getRelatedRoute();
        return lockRoute(docRouteElement);
    }

    protected String lockRoute(DocumentRoute docRouteElement)
            throws ClientException {
        try {
            getDocumentRoutingService().lockDocumentRoute(
                    docRouteElement.getDocumentRoute(documentManager),
                    documentManager);
        } catch (DocumentRouteAlredayLockedException e) {
            facesMessages.add(
                    StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
        }
        return null;
    }

    public String unlockCurrentRoute() throws ClientException {
        DocumentRoute route = getRelatedRoute();
        getDocumentRoutingService().unlockDocumentRoute(route, documentManager);
        return null;
    }

    public boolean isEmptyFork(DocumentModel forkDoc) throws ClientException {
        return forkDoc.hasFacet("Folderish")
                && !documentManager.hasChildren(forkDoc.getRef());
    }

    public String editStep() throws ClientException {
        if (StringUtils.isEmpty(stepId)) {
            return null;
        }
        DocumentRef stepRef = new IdRef(stepId);
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc.getAdapter(DocumentRoute.class) == null) {
            setDocWithAttachedRouteId(currentDoc.getId());
        }
        return navigationContext.navigateToDocument(
                documentManager.getDocument(stepRef), "edit");
    }

    public String updateRouteElement() throws ClientException {
        boolean alreadyLockedByCurrentUser = false;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRouteElement docRouteElement = currentDocument.getAdapter(DocumentRouteElement.class);
        DocumentRoute route = docRouteElement.getDocumentRoute(documentManager);
        if (getDocumentRoutingService().isLockedByCurrentUser(route,
                documentManager)) {
            alreadyLockedByCurrentUser = true;
        } else {
            if (lockRoute(route) == null) {
                return null;
            }
        }
        try {
            getDocumentRoutingService().updateRouteElement(docRouteElement,
                    documentManager);
        } catch (DocumentRouteNotLockedException e) {
            facesMessages.add(
                    StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
        }
        navigationContext.invalidateCurrentDocument();
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("document_modified"),
                resourcesAccessor.getMessages().get(currentDocument.getType()));
        EventManager.raiseEventsOnDocumentChange(currentDocument);
        // Release the lock only when currentUser had locked it before
        // entering this method.
        if (!alreadyLockedByCurrentUser) {
            getDocumentRoutingService().unlockDocumentRoute(route,
                    documentManager);
        }
        if (docWithAttachedRouteId == null) {
            return webActions.setCurrentTabAndNavigate(
                    docRouteElement.getDocumentRoute(documentManager).getDocument(),
                    "TAB_DOCUMENT_ROUTE_ELEMENTS");
        }

        setRelatedRouteWhenNavigateBackToCase();
        return webActions.setCurrentTabAndNavigate(
                documentManager.getDocument(new IdRef(docWithAttachedRouteId)),
                "TAB_CASE_MANAGEMENT_VIEW_RELATED_ROUTE");
    }

    public String goBackToRoute() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRouteElement docRouteElement = currentDocument.getAdapter(DocumentRouteElement.class);
        return webActions.setCurrentTabAndNavigate(
                docRouteElement.getDocumentRoute(documentManager).getDocument(),
                "TAB_DOCUMENT_ROUTE_ELEMENTS");
    }

    /**
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    public String createRouteElement(String typeName) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRef routeRef = currentDocument.getRef();
        DocumentRef sourceDocRef = new IdRef(hiddenSourceDocId);
        DocumentModel sourceDoc = documentManager.getDocument(sourceDocRef);
        String sourceDocName = null;
        String parentPath = null;
        if (StepOrder.in.toString().equals(hiddenDocOrder)) {
            parentPath = sourceDoc.getPathAsString();
        } else {
            DocumentModel parentDoc = documentManager.getParentDocument(sourceDocRef);
            parentPath = parentDoc.getPathAsString();
            if (StepOrder.before.toString().equals(hiddenDocOrder)) {
                sourceDocName = sourceDoc.getName();
            } else {
                DocumentModelList orderedChilds = getDocumentRoutingService().getOrderedRouteElement(
                        parentDoc.getId(), documentManager);
                int selectedDocumentIndex = orderedChilds.indexOf(sourceDoc);
                int nextIndex = selectedDocumentIndex + 1;
                if (nextIndex >= orderedChilds.size()) {
                    sourceDocName = null;
                } else {
                    sourceDocName = orderedChilds.get(nextIndex).getName();
                }
            }
        }
        org.nuxeo.ecm.platform.types.Type docType = typeManager.getType(typeName);
        // we cannot use typesTool as intermediary since the DataModel callback
        // will alter whatever type we set
        typesTool.setSelectedType(docType);
        try {
            DocumentModel changeableDocument = documentManager.createDocumentModel(typeName);
            ScopedMap context = changeableDocument.getContextData();
            context.put(CoreEventConstants.PARENT_PATH, parentPath);
            context.put(SOURCE_DOC_NAME, sourceDocName);
            context.put(ROUTE_DOCUMENT_REF, routeRef);
            navigationContext.setChangeableDocument(changeableDocument);
            return "create_route_element";
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Moves the step in the parent container in the specified direction. If
     * the step is in a parallel container, it can't be moved. A step can't be
     * moved before a step already done or running. Assumed that the route is
     * already locked to have this action available, so no check is done.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    public String moveRouteElement(String direction) throws ClientException {
        if (StringUtils.isEmpty(stepId)) {
            return null;
        }
        DocumentModel routeElementDocToMove = documentManager.getDocument(new IdRef(
                stepId));
        DocumentModel parentDoc = documentManager.getDocument(routeElementDocToMove.getParentRef());
        ExecutionTypeValues executionType = ExecutionTypeValues.valueOf((String) parentDoc.getPropertyValue(DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME));
        if (!DocumentRoutingConstants.ExecutionTypeValues.serial.equals(executionType)) {
            facesMessages.add(
                    StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.cant.move.steps.in.parallel.container"));
            return null;
        }
        DocumentModelList orderedChilds = getDocumentRoutingService().getOrderedRouteElement(
                parentDoc.getId(), documentManager);
        int selectedDocumentIndex = orderedChilds.indexOf(routeElementDocToMove);
        if (DocumentRoutingWebConstants.MOVE_STEP_UP.equals(direction)) {
            if (selectedDocumentIndex == 0) {
                facesMessages.add(
                        StatusMessage.Severity.WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.route.already.first.step.in.container"));
                return null;
            }
            routeElementDocToMove.getAdapter(DocumentRouteElement.class);
            DocumentModel stepMoveBefore = orderedChilds.get(selectedDocumentIndex - 1);
            DocumentRouteElement stepElementMoveBefore = stepMoveBefore.getAdapter(DocumentRouteElement.class);
            if (stepElementMoveBefore.isRunning()) {
                facesMessages.add(
                        StatusMessage.Severity.WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.route.cant.move.step.before.already.running.step"));
                return null;
            }
            if (!stepElementMoveBefore.isModifiable()) {
                facesMessages.add(
                        StatusMessage.Severity.WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.route.cant.move.step.after.no.modifiable.step"));
                return null;
            }
            documentManager.orderBefore(parentDoc.getRef(),
                    routeElementDocToMove.getName(), stepMoveBefore.getName());
        }
        if (DocumentRoutingWebConstants.MOVE_STEP_DOWN.equals(direction)) {
            if (selectedDocumentIndex == orderedChilds.size() - 1) {
                facesMessages.add(
                        StatusMessage.Severity.WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.already.last.step.in.container"));
                return null;
            }
            routeElementDocToMove.getAdapter(DocumentRouteElement.class);
            DocumentModel stepMoveAfter = orderedChilds.get(selectedDocumentIndex + 1);
            DocumentRouteElement stepElementMoveAfter = stepMoveAfter.getAdapter(DocumentRouteElement.class);
            if (stepElementMoveAfter.isRunning()) {
                facesMessages.add(
                        StatusMessage.Severity.WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.route.cant.move.step.after.already.running.step"));
                return null;
            }
            documentManager.orderBefore(parentDoc.getRef(),
                    orderedChilds.get(selectedDocumentIndex + 1).getName(),
                    routeElementDocToMove.getName());
        }
        if (docWithAttachedRouteId == null) {
            return webActions.setCurrentTabAndNavigate(
                    getRelatedRoute().getDocument(),
                    "TAB_DOCUMENT_ROUTE_ELEMENTS");
        }

        setRelatedRouteWhenNavigateBackToCase();
        return webActions.setCurrentTabAndNavigate(
                documentManager.getDocument(new IdRef(docWithAttachedRouteId)),
                "TAB_CASE_MANAGEMENT_VIEW_RELATED_ROUTE");
    }

    public String saveRouteElement() throws ClientException {
        boolean alreadyLockedByCurrentUser = false;
        DocumentRoute routeModel = getRelatedRoute();
        if (getDocumentRoutingService().isLockedByCurrentUser(routeModel,
                documentManager)) {
            alreadyLockedByCurrentUser = true;
        } else {
            lockRoute(routeModel);
        }

        DocumentModel newDocument = navigationContext.getChangeableDocument();
        // Document has already been created if it has an id.
        // This will avoid creation of many documents if user hit create button
        // too many times.
        if (newDocument.getId() != null) {
            log.debug("Document " + newDocument.getName() + " already created");
            return navigationContext.navigateToDocument(newDocument,
                    "after-create");
        }
        try {
            String parentDocumentPath = (String) newDocument.getContextData().get(
                    CoreEventConstants.PARENT_PATH);
            String sourceDocumentName = (String) newDocument.getContextData().get(
                    SOURCE_DOC_NAME);
            DocumentRef routeDocRef = (DocumentRef) newDocument.getContextData().get(
                    ROUTE_DOCUMENT_REF);
            try {
                getDocumentRoutingService().addRouteElementToRoute(
                        new PathRef(parentDocumentPath), sourceDocumentName,
                        newDocument.getAdapter(DocumentRouteElement.class),
                        documentManager);
            } catch (DocumentRouteNotLockedException e) {
                facesMessages.add(
                        StatusMessage.Severity.WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.route.not.locked"));
                return null;
            }
            DocumentModel routeDocument = documentManager.getDocument(routeDocRef);
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("document_saved"),
                    resourcesAccessor.getMessages().get(newDocument.getType()));

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    routeDocument);
            // Release the lock only when currentUser had locked it before
            // entering this method.
            if (!alreadyLockedByCurrentUser) {
                getDocumentRoutingService().unlockDocumentRoute(routeModel,
                        documentManager);
            }
            return navigationContext.navigateToDocument(routeDocument);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public List<DocumentModel> getOrderedChildren(String docRouteElementId,
            String type) throws ClientException {
        // xxx move me in serice with query
        DocumentModelList orderedChildren = getDocumentRoutingService().getOrderedRouteElement(
                docRouteElementId, documentManager);
        List<DocumentModel> filteredChildren = new ArrayList<DocumentModel>();
        for (DocumentModel documentModel : orderedChildren) {
            if (type.equals(documentModel.getType())) {
                filteredChildren.add(documentModel);
            }
        }
        return filteredChildren;
    }

    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public DocumentModel getChildWithPosition(DocumentModel docRouteElement,
            String pos) throws ClientException {
        DocumentModelList orderedChildren = getDocumentRoutingService().getOrderedRouteElement(
                docRouteElement.getId(), documentManager);
        return orderedChildren.get(Integer.parseInt(pos));
    }

    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public String getPositionForChild(DocumentModel docRouteElement,
            DocumentModel docChild) throws ClientException {
        DocumentModelList orderedChildren = getDocumentRoutingService().getOrderedRouteElement(
                docRouteElement.getId(), documentManager);
        return String.valueOf(orderedChildren.indexOf(docChild));
    }

    public String getHiddenSourceDocId() {
        return hiddenSourceDocId;
    }

    public void setHiddenSourceDocId(String hiddenSourceDocId) {
        this.hiddenSourceDocId = hiddenSourceDocId;
    }

    public String getHiddenDocOrder() {
        return hiddenDocOrder;
    }

    public void setHiddenDocOrder(String hiddenDocOrder) {
        this.hiddenDocOrder = hiddenDocOrder;
    }

    public String getRelatedRouteModelDocumentId() {
        return relatedRouteModelDocumentId;
    }

    public void setRelatedRouteModelDocumentId(
            String relatedRouteModelDocumentId) {
        this.relatedRouteModelDocumentId = relatedRouteModelDocumentId;
    }

    public String getDocWithAttachedRouteId() {
        return docWithAttachedRouteId;
    }

    public void setDocWithAttachedRouteId(String docWithAttachedRouteId) {
        this.docWithAttachedRouteId = docWithAttachedRouteId;
    }

    private void setRelatedRouteWhenNavigateBackToCase() throws ClientException {
        // recompute factory
        webActions.resetTabList();
        navigationContext.setCurrentDocument(documentManager.getDocument(new IdRef(
                docWithAttachedRouteId)));
        relatedRoutes = relatedRouteAction.findRelatedRoute();
    }

    @Observer(value = { TaskEventNames.WORKFLOW_ENDED,
            TaskEventNames.WORKFLOW_CANCELED }, create = false)
    public void resetCache() {
        webActions.resetTabList();
    }

    /**
     * @since 5.6
     */
    public DocumentModel getRouteModel(String routeId) throws ClientException {
        return documentManager.getDocument(new IdRef(routeId));
    }

    /**
     * @since 5.6
     */
    public DocumentModel getRouteInstanceFor(Task task) throws ClientException {
        final String routeDocId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (routeDocId == null) {
            return null;
        }
        final DocumentModel[] res = new DocumentModel[1];
        new UnrestrictedSessionRunner(documentManager) {
            @Override
            public void run() throws ClientException {
                DocumentModel doc = session.getDocument(new IdRef(routeDocId));
                doc.detach(true);
                res[0] = doc;
            }
        }.runUnrestricted();
        return res[0];
    }

    /**
     * @since 5.6
     */
    public List<DocumentModel> getFilteredRouteModels() throws ClientException {
        DocumentRoutingService documentRoutingService = Framework.getLocalService(DocumentRoutingService.class);
        List<DocumentModel> routeModels = documentRoutingService.searchRouteModels(
                documentManager, "");
        for (Iterator<DocumentModel> it = routeModels.iterator(); it.hasNext();) {
            DocumentModel route = it.next();
            Object graphRouteObj = route.getAdapter(GraphRoute.class);
            if (graphRouteObj instanceof GraphRoute) {
                String filter = ((GraphRoute) graphRouteObj).getAvailabilityFilter();
                if (!StringUtils.isBlank(filter)) {
                    if (!webActions.checkFilter(filter)) {
                        it.remove();
                    }
                }
            } else {
                // old workflow document => ignore
                it.remove();
            }
        }
        return routeModels;
    }

    /**
     * @since 5.6
     */
    public List<Task> getCurrentRouteAllTasks() throws ClientException {
        TaskService taskService = Framework.getLocalService(TaskService.class);
        DocumentRoute currentRoute = getRelatedRoute();
        if (currentRoute != null) {
            return taskService.getAllTaskInstances(
                    currentRoute.getDocument().getId(), documentManager);
        }
        return null;
    }

    /**
     * @since 5.6
     */
    public List<Task> getCurrentRouteCurrentUserTasks() throws ClientException {
        TaskService taskService = Framework.getLocalService(TaskService.class);
        DocumentRoute currentRoute = getRelatedRoute();
        if (currentRoute != null) {
            return taskService.getAllTaskInstances(
                    currentRoute.getDocument().getId(),
                    (NuxeoPrincipal) documentManager.getPrincipal(),
                    documentManager);
        }
        return null;
    }

    /**
     * @since 5.6
     */
    public String getCurrentWorkflowInitiator() throws ClientException {
        DocumentRoute currentRoute = getRelatedRoute();
        if (currentRoute != null) {
            return (String) currentRoute.getDocument().getPropertyValue(
                    DocumentRoutingConstants.INITIATOR);
        }
        return "";
    }

    /**
     * since 5.7
     */
    public boolean isCurrentRouteGraph() throws ClientException {
        return isRouteGraph(getRelatedRoute());
    }

    /**
     * Checks if a given route is a Graph.
     *
     * @since 5.7.2
     */
    public boolean isRouteGraph(DocumentRoute route) throws ClientException {
        return route != null
                && ExecutionTypeValues.graph.toString().equals(
                        route.getDocument().getPropertyValue(
                                DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME));
    }

}
