/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.ScopeType.EVENT;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.*;

/**
 * Seam bean used for Orderable documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("orderableDocumentActions")
@Scope(ScopeType.CONVERSATION)
public class OrderableDocumentActions implements SelectDataModelListener,
        Serializable {

    private static final Log log = LogFactory.getLog(OrderableDocumentActions.class);

    public static final String CURRENT_DOC_ORDERED_CHILDREN_QM = "CURRENT_DOC_ORDERED_CHILDREN";

    public static final String CHILDREN_DOCUMENT_LIST = "CHILDREN_DOCUMENT_LIST";

    public static final String SECTION_TYPE = "Section";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(required = false, create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @Factory(value = "currentOrderedChildrenSelectModel", scope = EVENT)
    public SelectDataModel getCurrentOrderedChildrenSelectModel()
            throws ClientException {
        DocumentModelList documents = getCurrentDocumentChildrenPage();
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(CHILDREN_DOCUMENT_LIST,
                documents, selectedDocuments);
        model.addSelectModelListener(this);
        return model;
    }

    protected DocumentModelList getCurrentDocumentChildrenPage()
            throws ClientException {
        if (documentManager == null) {
            log.error("documentManager not initialized");
            return new DocumentModelListImpl();
        }

        PagedDocumentsProvider resultsProvider = getOrderedChildrenProvider();
        return resultsProvider.getCurrentPage();
    }

    protected PagedDocumentsProvider getOrderedChildrenProvider()
            throws ClientException {
        return resultsProvidersCache.get(CURRENT_DOC_ORDERED_CHILDREN_QM);
    }

    @Factory(value = "sectionOrderedChildrenSelectModel", scope = EVENT)
    public SelectDataModel getSectionOrderedChildrenSelectModel()
            throws ClientException {
        DocumentModelList documents = getCurrentDocumentChildrenPage();
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SECTION_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(CHILDREN_DOCUMENT_LIST,
                documents, selectedDocuments);
        model.addSelectModelListener(this);
        return model;
    }

    public void processSelectRowEvent(SelectDataModelRowEvent event) {
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            documentsListsManager.addToWorkingList(
                    CURRENT_DOCUMENT_SELECTION, data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    CURRENT_DOCUMENT_SELECTION, data);
        }
    }

    /*
     * -------- Web Actions --------
     */

    public boolean getCanMoveDown() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return getCanMoveDown(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveDown(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveDown(DocumentModel container,
            String documentsListName) throws ClientException {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentModel> children = getChildrenFor(container.getId());
        int selectedDocumentIndex = children.indexOf(selectedDocument);
        int nextIndex = selectedDocumentIndex + 1;
        if (nextIndex == children.size()) {
            // can't move down the last document
            return false;
        }
        return true;
    }

    protected DocumentModelList getChildrenFor(String containerId)
            throws ClientException {
        try {
            QueryModelService qmService = Framework.getService(QueryModelService.class);
            QueryModelDescriptor qmd = qmService.getQueryModelDescriptor(CURRENT_DOC_ORDERED_CHILDREN_QM);
            QueryModel qm = new QueryModel(qmd);
            return qm.getDocuments(documentManager, new Object[] { containerId });
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public String moveDown() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return moveDown(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveDown(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveDown(DocumentModel container, String documentsListName)
            throws ClientException {
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(
                documentsListName).get(0);

        List<DocumentModel> children = getChildrenFor(container.getId());
        int selectedDocumentIndex = children.indexOf(selectedDocument);
        int nextIndex = selectedDocumentIndex + 1;
        DocumentModel nextDocument = children.get(nextIndex);

        documentManager.orderBefore(container.getRef(), nextDocument.getName(),
                selectedDocument.getName());
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedDown");
        return null;
    }

    protected void notifyChildrenChanged(DocumentModel containerDocument)
            throws ClientException {
        if (containerDocument != null) {
            Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED,
                    containerDocument);
        }
    }

    public boolean getCanMoveUp() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return getCanMoveUp(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveUp(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveUp(DocumentModel container,
            String documentsListName) throws ClientException {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentModel> children = getChildrenFor(container.getId());
        int selectedDocumentIndex = children.indexOf(selectedDocument);
        int previousIndex = selectedDocumentIndex - 1;
        if (previousIndex < 0) {
            // can't move up the first document
            return false;
        }
        return true;
    }

    public String moveUp() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return moveUp(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveUp(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveUp(DocumentModel container, String documentsListName)
            throws ClientException {
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(
                documentsListName).get(0);

        List<DocumentModel> children = getChildrenFor(container.getId());
        int selectedDocumentIndex = children.indexOf(selectedDocument);
        int previousIndex = selectedDocumentIndex - 1;
        DocumentModel previousDocument = children.get(previousIndex);

        documentManager.orderBefore(container.getRef(),
                selectedDocument.getName(), previousDocument.getName());
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedUp");
        return null;
    }

    public boolean getCanMoveToTop() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return getCanMoveToTop(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveToTop(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveToTop(DocumentModel container,
            String documentsListName) throws ClientException {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentModel> children = getChildrenFor(container.getId());
        int selectedDocumentIndex = children.indexOf(selectedDocument);
        if (selectedDocumentIndex <= 0) {
            // can't move to top the first document
            return false;
        }
        return true;
    }

    public String moveToTop() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return moveToTop(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveToTop(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveToTop(DocumentModel container, String documentsListName)
            throws ClientException {
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(
                documentsListName).get(0);
        List<DocumentModel> children = getChildrenFor(container.getId());
        DocumentModel firstDocument = children.get(0);

        documentManager.orderBefore(container.getRef(),
                selectedDocument.getName(), firstDocument.getName());
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedToTop");
        return null;
    }

    public boolean getCanMoveToBottom() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return getCanMoveToBottom(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveToBottom(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveToBottom(DocumentModel container,
            String documentsListName) throws ClientException {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentModel> children = getChildrenFor(container.getId());
        int selectedDocumentIndex = children.indexOf(selectedDocument);
        if (selectedDocumentIndex >= children.size() - 1) {
            // can't move to bottom the last document
            return false;
        }
        return true;
    }

    public String moveToBottom() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (SECTION_TYPE.equals(currentDocument.getType())) {
            return moveToBottom(currentDocument,
                    CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveToBottom(currentDocument,
                    CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveToBottom(DocumentModel container,
            String documentsListName) throws ClientException {
        DocumentRef containerRef = container.getRef();
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(
                documentsListName).get(0);
        documentManager.orderBefore(containerRef, selectedDocument.getName(),
                null);
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedToBottom");
        return null;
    }

    protected boolean isOrderable(DocumentModel doc) {
        return doc.hasFacet(FacetNames.ORDERABLE);
    }

    protected void addFacesMessage(String messageLabel) {
        FacesMessage message = FacesMessages.createFacesMessage(
                FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(messageLabel));
        facesMessages.add(message);
    }

    @Observer(value = {DOCUMENT_SELECTION_CHANGED, DOMAIN_SELECTION_CHANGED,
            CONTENT_ROOT_SELECTION_CHANGED, DOCUMENT_CHILDREN_CHANGED}, create = false)
    public void invalidateProviderOnDocumentChanged() {
        resultsProvidersCache.invalidate(CURRENT_DOC_ORDERED_CHILDREN_QM);
    }

}
