/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documentsLists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.ui.web.util.DocumentsListsUtils;
import org.nuxeo.runtime.api.Framework;

public abstract class BaseDocumentsListsManager implements Serializable {

    private static final long serialVersionUID = 98757690654316L;

    private transient DocumentsListsPersistenceManager persistenceManager;

    // ListName => DocumentModel List
    protected final Map<String, List<DocumentModel>> documentsLists = new HashMap<>();

    protected final Map<String, List<DocumentModel>> documentsListsPerConversation = new HashMap<>();

    // EventName => ListName
    protected final Map<String, List<String>> documentsLists_events = new HashMap<>();

    // ListName => List Descriptor
    protected final Map<String, DocumentsListDescriptor> documentsLists_descriptors = new HashMap<>();

    protected DocumentsListsService getService() {
        return Framework.getService(DocumentsListsService.class);
    }

    protected String userName;

    protected String getUserName() {
        return userName;
    }

    protected void setUserName(String userName) {
        this.userName = userName;
    }

    protected abstract void notifyListUpdated(String listName);

    protected DocumentsListsPersistenceManager getPersistenceManager() {
        if (persistenceManager == null) {
            persistenceManager = new DocumentsListsPersistenceManager();
        }

        return persistenceManager;
    }

    public List<DocumentModel> resetWorkingList(String listName) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }
        List<DocumentModel> docList = getWorkingList(listName);
        DocumentsListDescriptor desc = getWorkingListDescriptor(listName);
        if (desc.getPersistent()) {
            if (getPersistenceManager().clearPersistentList(userName, listName)) {
                docList.clear();
            }
        } else {
            docList.clear();
        }
        notifyListUpdated(listName);
        return docList;
    }

    public boolean isWorkingListEmpty(String listName) {
        if (!documentsLists.containsKey(listName)) {
            return true;
        }
        List<DocumentModel> docList = getWorkingList(listName);

        return docList.isEmpty();
    }

    public void removeFromAllLists(List<DocumentModel> documentsToRemove) {
        for (String listName : documentsLists.keySet()) {
            removeFromWorkingList(listName, documentsToRemove);
            // DocumentsListsUtils.removeDocumentsFromList(
            // getWorkingList(listName), documentsToRemove);
            notifyListUpdated(listName);
        }
    }

    public void createWorkingList(String listName, DocumentsListDescriptor descriptor) {
        createWorkingList(listName, descriptor, null, null);
    }

    public void createWorkingList(String listName, DocumentsListDescriptor descriptor, CoreSession session,
            String userName) {

        if (documentsLists.containsKey(listName)) {
            return;
        }

        if (descriptor != null && descriptor.getPersistent() && session != null && userName != null) {
            // load persistent list
            documentsLists.put(listName,
                    getPersistenceManager().loadPersistentDocumentsLists(session, userName, listName));
        } else {
            // create empty list
            documentsLists.put(listName, new ArrayList<DocumentModel>());
        }

        // create the descriptor
        if (descriptor == null) {
            descriptor = new DocumentsListDescriptor(listName);
        }

        documentsLists_descriptors.put(listName, descriptor);

        // manage events subscriptions
        for (String eventName : descriptor.getEventsName()) {
            if (documentsLists_events.containsKey(eventName)) {
                documentsLists_events.get(eventName).add(listName);
            } else {
                List<String> suscribersList = new ArrayList<>();
                suscribersList.add(listName);
                documentsLists_events.put(eventName, suscribersList);
            }
        }
    }

    public List<String> getWorkingListNamesForCategory(String categoryName) {
        List<String> res = new ArrayList<>();

        for (String listName : documentsLists_descriptors.keySet()) {
            if (documentsLists_descriptors.get(listName).getCategory().equals(categoryName)) {
                // default list in category is returned at start of the list !
                if (documentsLists_descriptors.get(listName).getDefaultInCategory()) {
                    res.add(0, listName);
                } else {
                    res.add(listName);
                }
            }
        }
        return res;
    }

    public List<DocumentModel> resetWorkingList(String listName, List<DocumentModel> newDocList) {
        resetWorkingList(listName);
        return addToWorkingList(listName, newDocList);
    }

    public List<DocumentModel> removeFromWorkingList(String listName, List<DocumentModel> lst) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }
        List<DocumentModel> docList = getWorkingList(listName);
        DocumentsListDescriptor desc = getWorkingListDescriptor(listName);

        for (DocumentModel doc : lst) {

            if (desc.getPersistent()) {
                if (getPersistenceManager().removeDocumentFromPersistentList(userName, listName, doc)) {
                    docList.remove(doc);
                }
            } else
                docList.remove(doc);
        }
        notifyListUpdated(listName);
        return docList;
    }

    public List<DocumentModel> removeFromWorkingList(String listName, DocumentModel doc) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }
        List<DocumentModel> docList = getWorkingList(listName);
        DocumentsListDescriptor desc = getWorkingListDescriptor(listName);

        if (desc.getPersistent()) {
            if (getPersistenceManager().removeDocumentFromPersistentList(userName, listName, doc)) {
                docList.remove(doc);
            }
        } else
            docList.remove(doc);
        notifyListUpdated(listName);
        return docList;
    }

    public List<DocumentModel> addToWorkingList(String listName, List<DocumentModel> docList) {
        return addToWorkingList(listName, docList, false);
    }

    public List<DocumentModel> addToWorkingList(String listName, List<DocumentModel> docList, Boolean forceAppend) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }

        List<DocumentModel> currentDocList = getWorkingList(listName);
        DocumentsListDescriptor currentDescriptor = getWorkingListDescriptor(listName);
        Boolean currentListIsPersistent = false;

        if (currentDescriptor != null) {
            if (!forceAppend && !getWorkingListDescriptor(listName).getSupportAppends()) {
                currentDocList.clear();
            }

            currentListIsPersistent = currentDescriptor.getPersistent();
        }

        // filter for duplicate
        List<DocumentRef> docRefList = DocumentsListsUtils.getDocRefs(currentDocList);
        for (DocumentModel doc : docList) {
            if (!docRefList.contains(doc.getRef())) {
                if (currentListIsPersistent) {
                    if (getPersistenceManager().addDocumentToPersistentList(userName, listName, doc)) {
                        // Strange, CHECKME;
                    }
                    currentDocList.add(doc);
                } else {
                    currentDocList.add(doc);
                }
            }
        }
        notifyListUpdated(listName);
        return currentDocList;
    }

    public List<DocumentModel> addToWorkingList(String listName, DocumentModel doc) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }

        List<DocumentModel> docList = getWorkingList(listName);
        DocumentsListDescriptor currentDescriptor = getWorkingListDescriptor(listName);
        boolean currentListIsPersistent = false;

        if (currentDescriptor != null) {
            currentListIsPersistent = currentDescriptor.getPersistent();
        }

        List<DocumentRef> docRefList = DocumentsListsUtils.getDocRefs(docList);
        if (!docRefList.contains(doc.getRef())) {
            if (currentListIsPersistent) {
                if (getPersistenceManager().addDocumentToPersistentList(userName, listName, doc)) {
                    // Strange, CHECKME;
                }
                docList.add(doc);
            } else {
                docList.add(doc);
            }
        }
        notifyListUpdated(listName);
        return docList;
    }

    public void setWorkingList(String listName, List<DocumentModel> docList) {
        if (documentsLists.containsKey(listName)) {
            documentsLists.put(listName, docList);
        }
    }

    public List<String> getWorkingListTypes(String listName) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }

        List<String> res = new ArrayList<>();
        for (DocumentModel doc : documentsLists.get(listName)) {
            String dt = doc.getType();
            if (!res.contains(dt)) {
                res.add(dt);
            }
        }
        return res;
    }

    public DocumentsListDescriptor getWorkingListDescriptor(String listName) {
        if (!documentsLists.containsKey(listName)) {
            return null;
        }
        return documentsLists_descriptors.get(listName);
    }

    public List<DocumentModel> getWorkingList(String listName) {
        if (documentsLists.containsKey(listName)) {
            return documentsLists.get(listName);
        } else {
            return null;
        }
    }

}
