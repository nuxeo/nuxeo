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
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.convert.Converter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
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
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LocalizableDocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.webapp.action.TypesTool;
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

    @In(create = true, required = false)
    protected TypesTool typesTool;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected EventManager eventManager;

    @In(required = true, create = false)
    protected NuxeoPrincipal currentUser;

    @RequestParameter("stepId")
    protected String stepId;

    protected String relatedRouteModelDocumentId;

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

    @Observer(value = { EventNames.DOCUMENT_CHANGED })
    public void resetRelatedRouteDocumentId() {
        relatedRouteModelDocumentId = null;
    }

    public String startRoute() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        currentDocument.setPropertyValue(
                DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                navigationContext.getChangeableDocument().getPropertyValue(
                        DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME));
        DocumentRoute currentRoute = currentDocument.getAdapter(DocumentRoute.class);
        getDocumentRoutingService().createNewInstance(currentRoute,
                currentRoute.getAttachedDocuments(), documentManager);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);
        return null;
    }

    public DocumentRoute getRelatedRoute() {
        // try to see if actually the current document is a route
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRoute relatedRoute = currentDocument.getAdapter(DocumentRoute.class);
        if (relatedRoute != null) {
            return relatedRoute;
        }
        // try to see if the current document is a routeElement
        DocumentRouteElement relatedRouteElement = currentDocument.getAdapter(DocumentRouteElement.class);
        if (relatedRouteElement != null) {
            return relatedRouteElement.getDocumentRoute(documentManager);
        }
        // else we must be in a document attached to a route
        String relatedRouteModelDocumentId;
        List<DocumentModel> relatedRoutes;
        try {
            relatedRoutes = findRelatedRouteDocument();
        } catch (ClientException e) {
            return null;
        }
        if (relatedRoutes.size() <= 0) {
            return null;
        }
        relatedRouteModelDocumentId = relatedRoutes.get(0).getId();
        DocumentModel docRoute;
        try {
            docRoute = documentManager.getDocument(new IdRef(
                    relatedRouteModelDocumentId));
        } catch (ClientException e) {
            return null;
        }
        return docRoute.getAdapter(DocumentRoute.class);
    }

    public String cancelRoute() throws ClientException {
        DocumentModel doc = findRelatedRouteDocument().get(0);
        DocumentRoute route = doc.getAdapter(DocumentRoute.class);
        route.cancel(documentManager);
        webActions.resetTabList();
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }
    public String saveRouteAsNewInstance() {
        getDocumentRoutingService().saveRouteAsNewModel(getRelatedRoute(), documentManager);
        return null;
    }

    public String validateRouteModel() throws ClientException {
        DocumentRoute currentRouteModel = getRelatedRoute();
        try {
            getDocumentRoutingService().lockDocumentRoute(currentRouteModel,
                    documentManager);
        } catch (DocumentRouteAlredayLockedException e) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
        }
        try {
            getDocumentRoutingService().validateRouteModel(currentRouteModel,
                    documentManager);
        } catch (DocumentRouteNotLockedException e) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.not.locked"));
            return null;
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentRouteModel.getDocument());
        getDocumentRoutingService().unlockDocumentRoute(currentRouteModel,
                documentManager);
        return null;
    }

    protected ArrayList<LocalizableDocumentRouteElement> computeRouteElements()
            throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRouteElement currentRouteModelElement = currentDocument.getAdapter(DocumentRouteElement.class);
        return getElements(currentRouteModelElement);
    }

    protected ArrayList<LocalizableDocumentRouteElement> computeRelatedRouteElements()
            throws ClientException {
        List<DocumentModel> routes = findRelatedRouteDocument();
        if(routes == null || routes.isEmpty())   {
            return new ArrayList<LocalizableDocumentRouteElement>();
        }
        DocumentModel relatedRouteDocumentModel = documentManager.getDocument(new IdRef(
                routes.get(0).getId()));
        DocumentRouteElement currentRouteModelElement = relatedRouteDocumentModel.getAdapter(DocumentRouteElement.class);
        return getElements(currentRouteModelElement);
    }

    protected ArrayList<LocalizableDocumentRouteElement> getElements(
            DocumentRouteElement currentRouteModelElement)
            throws ClientException {
        ArrayList<LocalizableDocumentRouteElement> routeElements = new ArrayList<LocalizableDocumentRouteElement>();
        getDocumentRoutingService().getRouteElements(currentRouteModelElement,
                documentManager, routeElements, 0);
        return routeElements;
    }

    @Factory(value = "routeElementsSelectModel", scope = EVENT)
    public SelectDataModel computeSelectDataModelRouteElements()
            throws ClientException {
        return new SelectDataModelImpl("dm_route_elements",
                computeRouteElements(), null);
    }

    @Factory(value = "relatedRouteElementsSelectModel", scope = EVENT)
    public SelectDataModel computeSelectDataModelRelatedRouteElements()
            throws ClientException {
        return new SelectDataModelImpl("related_route_elements",
                computeRelatedRouteElements(), null);
    }

    public List<DocumentModel> findRelatedRouteDocument()
            throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        List<DocumentRoute> relatedRoutes = getDocumentRoutingService().getDocumentRoutesForAttachedDocument(
                documentManager, navigationContext.getCurrentDocument().getId());
        for (DocumentRoute documentRoute : relatedRoutes) {
            docs.add(documentRoute.getDocument());
        }
        return docs;
    }

    /**
     * Check if the related route to this case is started (ready or running) or
     * no
     *
     * @param doc the mail to remove
     */
    public boolean hasRelatedRoute() throws ClientException {
        return !findRelatedRouteDocument().isEmpty();
    }

    public String startRouteRelatedToCurrentDocument() throws ClientException {
        DocumentRoute route = getRelatedRoute();
        // check relatedRoutedoc id
        if(relatedRouteModelDocumentId != null) {
            DocumentModel model = documentManager.getDocument(new IdRef(relatedRouteModelDocumentId));
            route = model.getAdapter(DocumentRoute.class);
        }
        if (route == null) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.no.valid.route"));
            return null;
        }

        List<String> documentIds = new ArrayList<String>();
        documentIds.add(navigationContext.getCurrentDocument().getId());
        route.setAttachedDocuments(documentIds);
        getDocumentRoutingService().createNewInstance(route,
                route.getAttachedDocuments(), documentManager);
        return null;
    }

    /**
     * returns true if the routeStarted on the current Document is editable (is
     * Ready )
     * */
    public boolean routeRelatedToCurrentDocumentIsRunning()
            throws ClientException {
        DocumentRoute route = getRelatedRoute();
        if (route == null) {
            return false;
        }
        return route.isRunning();
    }

    public String getTypeDescription(LocalizableDocumentRouteElement localizable) {
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

    public String removeStep() throws ClientException {
        DocumentRoute routeModel = getRelatedRoute();
        try {
            getDocumentRoutingService().lockDocumentRoute(routeModel,
                    documentManager);
        } catch (DocumentRouteAlredayLockedException e) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
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
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.not.locked"));
            return null;
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                routeModel.getDocument());
        getDocumentRoutingService().unlockDocumentRoute(routeModel,
                documentManager);
        return null;
    }

    /**
     * Returns true if the givenDoc is a step that can be edited
     * */
    public boolean isEditableStep(DocumentModel stepDoc) throws ClientException {
        DocumentRouteElement stepElement = stepDoc.getAdapter(DocumentRouteElement.class);
        // if fork, is not simple editable step
        if(stepDoc.hasFacet("Folderish")){
            return false;
        }
        return stepElement.isModifiable();
    }


    /**
     * Returns true if the givenDoc is an routeElement that can be edited
     * */
    public boolean isEditableRouteElement(DocumentModel stepDoc) throws ClientException {
        DocumentRouteElement stepElement = stepDoc.getAdapter(DocumentRouteElement.class);
        return stepElement.isModifiable();
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
        return navigationContext.navigateToDocument(
                documentManager.getDocument(stepRef), "edit");
    }

    public String updateRouteElement() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        DocumentRouteElement docRouteElement = changeableDocument.getAdapter(DocumentRouteElement.class);
        try {
            getDocumentRoutingService().lockDocumentRoute(
                    docRouteElement.getDocumentRoute(documentManager),
                    documentManager);
        } catch (DocumentRouteAlredayLockedException e) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
        }
        try {
            getDocumentRoutingService().updateRouteElement(docRouteElement,
                    documentManager);
        } catch (DocumentRouteNotLockedException e) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
        }
        navigationContext.invalidateCurrentDocument();
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("document_modified"),
                resourcesAccessor.getMessages().get(
                        changeableDocument.getType()));
        EventManager.raiseEventsOnDocumentChange(changeableDocument);
        getDocumentRoutingService().unlockDocumentRoute(
                docRouteElement.getDocumentRoute(documentManager),
                documentManager);
        return webActions.setCurrentTabAndNavigate(
                docRouteElement.getDocumentRoute(documentManager).getDocument(),
                "TAB_DOCUMENT_ROUTE_ELEMENTS");
    }

    public String goBackToRoute() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        DocumentRouteElement docRouteElement = changeableDocument.getAdapter(DocumentRouteElement.class);
        return webActions.setCurrentTabAndNavigate(
                docRouteElement.getDocumentRoute(documentManager).getDocument(),
                "TAB_DOCUMENT_ROUTE_ELEMENTS");
    }

    public String createRouteElement(String typeName) throws ClientException {
        if (!getDocumentRoutingService().canUserCreateRoute(currentUser)) {
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "feedback.document.route.no.creation.rights"));
            return "";
        }
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

    public String saveRouteElement() throws ClientException {
        DocumentRoute routeModel = getRelatedRoute();
        try {
            getDocumentRoutingService().lockDocumentRoute(routeModel,
                    documentManager);
        } catch (DocumentRouteAlredayLockedException e) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.already.locked"));
            return null;
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
                        FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "feedback.casemanagement.document.route.not.locked"));
                return null;
            }
            DocumentModel routeDocument = documentManager.getDocument(routeDocRef);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_saved"),
                    resourcesAccessor.getMessages().get(newDocument.getType()));

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    routeDocument);
            getDocumentRoutingService().unlockDocumentRoute(routeModel,
                    documentManager);
            return navigationContext.navigateToDocument(routeDocument);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
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

    public void setRelatedRouteModelDocumentId(String relatedRouteModelDocumentId) {
        this.relatedRouteModelDocumentId = relatedRouteModelDocumentId;
    }
}
