/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Thomas Roger
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHILDREN_CHANGED;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Seam bean used for Orderable documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("orderableDocumentActions")
@Scope(ScopeType.CONVERSATION)
public class OrderableDocumentActions implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(OrderableDocumentActions.class);

    public static final String SECTION_TYPE = "Section";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    /*
     * -------- Web Actions --------
     */

    public boolean getCanMoveDown() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (isSectionType(currentDocument)) {
            return getCanMoveDown(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveDown(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveDown(DocumentModel container, String documentsListName) {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        int selectedDocumentIndex = children.indexOf(new IdRef(selectedDocument.getId()));
        int nextIndex = selectedDocumentIndex + 1;
        if (nextIndex == children.size()) {
            // can't move down the last document
            return false;
        }
        return true;
    }

    public String moveDown() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (isSectionType(currentDocument)) {
            return moveDown(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveDown(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveDown(DocumentModel container, String documentsListName) {
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(documentsListName).get(0);

        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        int selectedDocumentIndex = children.indexOf(new IdRef(selectedDocument.getId()));
        int nextIndex = selectedDocumentIndex + 1;
        DocumentRef nextDocumentRef = children.get(nextIndex);

        documentManager.orderBefore(container.getRef(), documentManager.getDocument(nextDocumentRef).getName(),
                selectedDocument.getName());
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedDown");
        return null;
    }

    protected void notifyChildrenChanged(DocumentModel containerDocument) {
        if (containerDocument != null) {
            Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED, containerDocument);
        }
    }

    public boolean getCanMoveUp() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (isSectionType(currentDocument)) {
            return getCanMoveUp(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveUp(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveUp(DocumentModel container, String documentsListName) {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        int selectedDocumentIndex = children.indexOf(new IdRef(selectedDocument.getId()));
        int previousIndex = selectedDocumentIndex - 1;
        if (previousIndex < 0) {
            // can't move up the first document
            return false;
        }
        return true;
    }

    public String moveUp() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (isSectionType(currentDocument)) {
            return moveUp(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveUp(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveUp(DocumentModel container, String documentsListName) {
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(documentsListName).get(0);

        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        int selectedDocumentIndex = children.indexOf(new IdRef(selectedDocument.getId()));
        int previousIndex = selectedDocumentIndex - 1;
        DocumentRef previousDocumentRef = children.get(previousIndex);

        documentManager.orderBefore(container.getRef(), selectedDocument.getName(),
                documentManager.getDocument(previousDocumentRef).getName());
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedUp");
        return null;
    }

    public boolean getCanMoveToTop() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (isSectionType(currentDocument)) {
            return getCanMoveToTop(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveToTop(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveToTop(DocumentModel container, String documentsListName) {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        int selectedDocumentIndex = children.indexOf(new IdRef(selectedDocument.getId()));
        if (selectedDocumentIndex <= 0) {
            // can't move to top the first document
            return false;
        }
        return true;
    }

    public String moveToTop() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (isSectionType(currentDocument)) {
            return moveToTop(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveToTop(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveToTop(DocumentModel container, String documentsListName) {
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(documentsListName).get(0);
        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        DocumentRef firstDocumentRef = children.get(0);

        documentManager.orderBefore(container.getRef(), selectedDocument.getName(),
                documentManager.getDocument(firstDocumentRef).getName());
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedToTop");
        return null;
    }

    public boolean getCanMoveToBottom() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!isOrderable(currentDocument)) {
            return false;
        }
        if (isSectionType(currentDocument)) {
            return getCanMoveToBottom(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return getCanMoveToBottom(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected boolean getCanMoveToBottom(DocumentModel container, String documentsListName) {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(documentsListName);
        if (docs.isEmpty() || docs.size() > 1) {
            return false;
        }

        DocumentModel selectedDocument = docs.get(0);
        List<DocumentRef> children = documentManager.getChildrenRefs(container.getRef(), null);
        int selectedDocumentIndex = children.indexOf(new IdRef(selectedDocument.getId()));
        if (selectedDocumentIndex >= children.size() - 1) {
            // can't move to bottom the last document
            return false;
        }
        return true;
    }

    public String moveToBottom() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        if (isSectionType(currentDocument)) {
            return moveToBottom(currentDocument, CURRENT_DOCUMENT_SECTION_SELECTION);
        } else {
            return moveToBottom(currentDocument, CURRENT_DOCUMENT_SELECTION);
        }
    }

    protected String moveToBottom(DocumentModel container, String documentsListName) {
        DocumentRef containerRef = container.getRef();
        DocumentModel selectedDocument = documentsListsManager.getWorkingList(documentsListName).get(0);
        documentManager.orderBefore(containerRef, selectedDocument.getName(), null);
        documentManager.save();

        notifyChildrenChanged(container);
        addFacesMessage("feedback.order.movedToBottom");
        return null;
    }

    protected boolean isOrderable(DocumentModel doc) {
        return doc.hasFacet(FacetNames.ORDERABLE);
    }

    protected boolean isSectionType(DocumentModel doc) {
        return doc.hasFacet(FacetNames.PUBLISH_SPACE);
    }

    protected void addFacesMessage(String messageLabel) {
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(messageLabel));
    }

}
