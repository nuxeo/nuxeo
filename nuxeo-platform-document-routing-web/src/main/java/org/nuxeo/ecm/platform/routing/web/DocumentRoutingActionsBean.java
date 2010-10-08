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
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LocalizableDocumentRouteElement;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
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

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected String relatedRouteModelDocumentId;

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

    public String getRelatedRouteModelDocument() {
        if (StringUtils.isEmpty(relatedRouteModelDocumentId)) {
            List<DocumentModel> relatedRoute;
            try {
                relatedRoute = findRelatedRouteDocument();
            } catch (ClientException e) {
                return "";
            }
            if (relatedRoute.size() > 0) {
                relatedRouteModelDocumentId = relatedRoute.get(0).getId();
            }
        }
        return relatedRouteModelDocumentId;
    }

    public String validateRouteModel() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRoute currentRouteModel = currentDocument.getAdapter(DocumentRoute.class);
        getDocumentRoutingService().validateRouteModel(currentRouteModel,
                documentManager);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);
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
        DocumentModel relatedRouteDocumentModel = documentManager.getDocument(new IdRef(
                findRelatedRouteDocument().get(0).getId()));
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

    public void setRelatedRouteModelDocument(String relatedRouteModelDocumentId) {
        this.relatedRouteModelDocumentId = relatedRouteModelDocumentId;
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
        // if no relatedRouteModelDocumentId
        if (relatedRouteModelDocumentId != null
                && relatedRouteModelDocumentId.isEmpty()) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "feedback.casemanagement.document.route.no.valid.route"));
            return null;
        }
        DocumentModel relatedRouteModel = documentManager.getDocument(new IdRef(
                relatedRouteModelDocumentId));
        // set currentDocumentId to participatingDocuments on the route
        DocumentRoute route = relatedRouteModel.getAdapter(DocumentRoute.class);
        List<String> documentIds = new ArrayList<String>();
        documentIds.add(navigationContext.getCurrentDocument().getId());
        route.setAttachedDocuments(documentIds);
        getDocumentRoutingService().createNewInstance(route,
                route.getAttachedDocuments(), documentManager);
        relatedRouteModelDocumentId = null;
        return null;
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
        if (doc.hasFacet(DocumentRoutingConstants.ROUTE_STEP_FACET)) {
            return true;
        }
        return false;
    }
}
