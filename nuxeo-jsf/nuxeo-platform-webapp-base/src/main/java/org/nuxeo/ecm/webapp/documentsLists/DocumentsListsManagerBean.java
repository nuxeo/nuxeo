/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documentsLists;

import static org.jboss.seam.ScopeType.SESSION;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remove;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.webapp.helpers.EventNames;

@Name("documentsListsManager")
@Scope(SESSION)
public class DocumentsListsManagerBean extends BaseDocumentsListsManager
        implements DocumentsListsManager {

    private static final long serialVersionUID = 2895324573454635971L;

    private Boolean initialized = false;

    @In(create = true)
    private ConversationDocumentsListsManager conversationDocumentsListsManager;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true)
    private transient Principal currentUser;

    @Override
    protected void notifyListUpdated(String listName) {
        Events.instance().raiseEvent(listName + "Updated");
    }

    private DocumentRef lastDocumentRef;

    @Remove
    @Destroy
    public void destroy() {
    }

    @Create
    public void initListManager() {
        if (!initialized) {
            super.setUserName(currentUser.getName());
            List<String> listContribNames = getService().getDocumentsListDescriptorsName();
            for (String listName : listContribNames) {
                DocumentsListDescriptor desc = getService().getDocumentsListDescriptor(
                        listName);

                if (desc.getIsSession()) {
                    super.createWorkingList(listName, desc, documentManager,
                            currentUser.getName());
                } else {
                    // just store the descriptor
                    documentsLists_descriptors.put(listName, desc);
                }
            }
            initialized = true;
        }
    }

    // Forward API
    @Override
    public void createWorkingList(String listName,
            DocumentsListDescriptor descriptor) {
        if (descriptor.getIsSession()) {
            super.createWorkingList(listName, descriptor);
        } else {
            conversationDocumentsListsManager.createWorkingList(listName,
                    descriptor);
            documentsLists_descriptors.put(listName, descriptor);
        }
    }

    @Override
    public List<DocumentModel> getWorkingList(String listName) {
        if (isSessionOrIsNull(listName)) {
            return super.getWorkingList(listName);
        } else {
            return conversationDocumentsListsManager.getWorkingList(listName);
        }
    }

    @Override
    public List<String> getWorkingListTypes(String listName) {
        if (isSessionOrIsNull(listName)) {
            return super.getWorkingListTypes(listName);
        } else {
            return conversationDocumentsListsManager.getWorkingListTypes(listName);
        }
    }

    @Override
    public void setWorkingList(String listName, List<DocumentModel> docList) {
        if (isSessionOrIsNull(listName)) {
            super.setWorkingList(listName, docList);
        } else {
            conversationDocumentsListsManager.setWorkingList(listName, docList);
        }
    }

    @Override
    public List<DocumentModel> addToWorkingList(String listName,
            DocumentModel doc) {
        if (isSessionOrIsNull(listName)) {
            return super.addToWorkingList(listName, doc);
        } else {
            return conversationDocumentsListsManager.addToWorkingList(listName,
                    doc);
        }
    }

    @Override
    public List<DocumentModel> addToWorkingList(String listName,
            List<DocumentModel> docList) {
        if (isSessionOrIsNull(listName)) {
            return super.addToWorkingList(listName, docList);
        } else {
            return conversationDocumentsListsManager.addToWorkingList(listName,
                    docList);
        }
    }

    @Override
    public List<DocumentModel> addToWorkingList(String listName,
            List<DocumentModel> docList, Boolean forceAppend) {
        if (isSessionOrIsNull(listName)) {
            return super.addToWorkingList(listName, docList, forceAppend);
        } else {
            return conversationDocumentsListsManager.addToWorkingList(listName,
                    docList, forceAppend);
        }
    }

    @Override
    public List<DocumentModel> removeFromWorkingList(String listName,
            DocumentModel doc) {
        if (isSessionOrIsNull(listName)) {
            return super.removeFromWorkingList(listName, doc);
        } else {
            return conversationDocumentsListsManager.removeFromWorkingList(
                    listName, doc);
        }
    }

    @Override
    public List<DocumentModel> removeFromWorkingList(String listName,
            List<DocumentModel> lst) {
        if (isSessionOrIsNull(listName)) {
            return super.removeFromWorkingList(listName, lst);
        } else {
            return conversationDocumentsListsManager.removeFromWorkingList(
                    listName, lst);
        }
    }

    @Override
    public List<DocumentModel> resetWorkingList(String listName) {
        if (isSessionOrIsNull(listName)) {
            return super.resetWorkingList(listName);
        } else {
            return conversationDocumentsListsManager.resetWorkingList(listName);
        }
    }

    @Override
    public List<DocumentModel> resetWorkingList(String listName,
            List<DocumentModel> newDocList) {
        if (isSessionOrIsNull(listName)) {
            return super.resetWorkingList(listName, newDocList);
        } else {
            return conversationDocumentsListsManager.resetWorkingList(listName,
                    newDocList);
        }
    }

    @Override
    public boolean isWorkingListEmpty(String listName) {
        if (isSessionOrIsNull(listName)) {
            return super.isWorkingListEmpty(listName);
        } else {
            return conversationDocumentsListsManager.isWorkingListEmpty(listName);
        }
    }

    @Override
    public void removeFromAllLists(List<DocumentModel> documentsToRemove) {
        super.removeFromAllLists(documentsToRemove);
        conversationDocumentsListsManager.removeFromAllLists(documentsToRemove);
    }

    @Override
    public List<String> getWorkingListNamesForCategory(String categoryName) {
        List<String> result = new ArrayList<String>();

        result.addAll(super.getWorkingListNamesForCategory(categoryName));
        result.addAll(conversationDocumentsListsManager.getWorkingListNamesForCategory(categoryName));
        return result;
    }

    @Override
    public DocumentsListDescriptor getWorkingListDescriptor(String listName) {
        // Session level contains all the descriptors
        return super.getWorkingListDescriptor(listName);
    }

    // Shortcut API
    public List<DocumentModel> getWorkingList() {
        return getWorkingList(DEFAULT_WORKING_LIST);
    }

    public DocumentsListDescriptor getWorkingListDescriptor() {
        return getWorkingListDescriptor(DEFAULT_WORKING_LIST);
    }

    public List<String> getWorkingListTypes() {
        return getWorkingListTypes(DEFAULT_WORKING_LIST);
    }

    public void setWorkingList(List<DocumentModel> docList) {
        setWorkingList(DEFAULT_WORKING_LIST, docList);
    }

    public List<DocumentModel> addToWorkingList(DocumentModel doc) {
        return addToWorkingList(DEFAULT_WORKING_LIST, doc);
    }

    public List<DocumentModel> addToWorkingList(List<DocumentModel> docList) {
        return addToWorkingList(DEFAULT_WORKING_LIST, docList, false);
    }

    public List<DocumentModel> removeFromWorkingList(DocumentModel doc) {
        return removeFromWorkingList(DEFAULT_WORKING_LIST, doc);
    }

    public List<DocumentModel> resetWorkingList() {
        return resetWorkingList(DEFAULT_WORKING_LIST);
    }

    public List<DocumentModel> resetWorkingList(List<DocumentModel> newDocList) {
        resetWorkingList();
        return addToWorkingList(newDocList);
    }

    public boolean isWorkingListEmpty() {
        return isWorkingListEmpty(DEFAULT_WORKING_LIST);
    }

    // Event listener
    @Observer(value = { EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED }, create = false)
    public void refreshLists(DocumentModel currentDocument) {

        if (lastDocumentRef != null
                && lastDocumentRef.equals(currentDocument.getRef())) {
            return;
        }

        if (!documentsLists_events.containsKey(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED)) {
            return;
        }

        for (String listName : documentsLists_events.get(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED)) {
            if (getWorkingList(listName) != null) {
                documentsLists.get(listName).clear();
                notifyListUpdated(listName);
            }
        }

        lastDocumentRef = currentDocument.getRef();
    }

    /**
     * Refresh lists when a search is performed
     */
    @Observer(value = { EventNames.SEARCH_PERFORMED }, create = false)
    public void refreshListsOnSearch() {
        if (!documentsLists_events.containsKey(EventNames.SEARCH_PERFORMED)) {
            return;
        }
        for (String listName : documentsLists_events.get(EventNames.SEARCH_PERFORMED)) {
            List<DocumentModel> docList = documentsLists.get(listName);
            if (!docList.isEmpty()) {
                docList.clear();
                notifyListUpdated(listName);
            }
        }
    }

    private boolean isSessionOrIsNull(String listName) {
        DocumentsListDescriptor desc = documentsLists_descriptors.get(listName);
        return desc == null || desc.getIsSession();
    }

    private boolean isPersistent(String listName) {
        DocumentsListDescriptor desc = documentsLists_descriptors.get(listName);
        if (desc == null) {
            return false;
        } else {
            return desc.getPersistent();
        }
    }

}
