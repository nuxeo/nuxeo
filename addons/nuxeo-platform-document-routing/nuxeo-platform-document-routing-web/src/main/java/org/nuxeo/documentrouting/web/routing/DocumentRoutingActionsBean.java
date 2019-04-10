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

package org.nuxeo.documentrouting.web.routing;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.LocalizableDocumentRouteElement;
import org.nuxeo.ecm.platform.routing.core.impl.DocumentRouteStepImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.jboss.seam.core.Events;

/**
 * Actions for current document route
 *
 * @author Mariana Cedica
 */
@Scope(CONVERSATION)
@Name("routingActions")
public class DocumentRoutingActionsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    private DocumentRoutingService documentRoutingService;

    public DocumentRoutingService getDocumentRoutingService() {
        try {
            if (documentRoutingService == null) {
                documentRoutingService = Framework.getService(DocumentRoutingService.class);
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
        return documentRoutingService;
    }

    public String startRoute() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        setPropertisOnDocumentBeforeExecution(currentDocument);
        DocumentRoute currentRoute = currentDocument.getAdapter(DocumentRoute.class);
        DocumentRoute routeInstance = getDocumentRoutingService().createNewInstance(
                currentRoute, currentRoute.getAttachedDocuments(),
                documentManager);
        return navigationContext.navigateToDocument(routeInstance.getDocument());
    }

    public String validateRouteModel() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRoute currentRouteModel = currentDocument.getAdapter(DocumentRoute.class);
        getDocumentRoutingService().validateRouteModel(currentRouteModel,
                documentManager);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
        return navigationContext.navigateToDocument(currentDocument);
    }

    private void setPropertisOnDocumentBeforeExecution(DocumentModel doc)
            throws PropertyException, ClientException {
        doc.setPropertyValue(
                DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                navigationContext.getChangeableDocument().getPropertyValue(
                        DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME));
        documentManager.saveDocument(doc);
        documentManager.save();
    }

    protected ArrayList<LocalizableDocumentRouteElement> computeRouteElements()
            throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRouteElement currentRouteModelElement = currentDocument.getAdapter(DocumentRouteElement.class);
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

    public String getTypeDescription(LocalizableDocumentRouteElement localizable) {
        return depthFormatter(localizable.getDepth() ,localizable.getElement().getTypeDescription());
    }

    private String depthFormatter(int depth, String type) {
        StringBuilder depthFormatter = new StringBuilder();
        for (int i = 0; i < depth - 1; i++) {
            depthFormatter.append("__");
        }
        depthFormatter.append(type);
        depthFormatter.append(" (");
        depthFormatter.append(depth -1);
        depthFormatter.append(")");
        return depthFormatter.toString();
    }

    public boolean isStep(DocumentModel doc) {
        if (doc.hasFacet(DocumentRoutingConstants.ROUTE_STEP_FACET)) {
            return true;
        }
        return false;
    }
}