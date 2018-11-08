/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.webapp.documentsLists;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.webapp.helpers.EventNames;

@Name("conversationDocumentsListsManager")
@Scope(CONVERSATION)
public class ConversationDocumentsListsManager extends BaseDocumentsListsManager implements Serializable {

    private static final long serialVersionUID = 9876098763432L;

    private Boolean initialized = false;

    private DocumentRef lastDocumentRef;

    @Override
    protected void notifyListUpdated(String listName) {
        Events.instance().raiseEvent(listName + "Updated");
    }

    @Create
    public void initListManager() {
        if (!initialized) {
            List<String> listContribNames = getService().getDocumentsListDescriptorsName();
            for (String listName : listContribNames) {
                DocumentsListDescriptor desc = getService().getDocumentsListDescriptor(listName);
                if (!desc.getIsSession()) {
                    createWorkingList(listName, desc);
                }
            }
            initialized = true;
        }
    }

    // Event listeners
    @Observer(value = { EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED }, create = false)
    public void refreshLists(DocumentModel selectedDocument) {

        if (selectedDocument != null) {
            refreshLists(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED, selectedDocument);
        }
    }

    /**
     * @since 5.6
     */
    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false)
    public void refreshListsOnDocumentSelectionChanged(DocumentModel selectedDocument) {

        if (selectedDocument != null) {
            refreshLists(EventNames.DOCUMENT_SELECTION_CHANGED, selectedDocument);
        }
    }

    /**
     * Refresh lists when main tab is changed
     *
     * @since 10.3
     */
    @Observer(value = { EventNames.MAIN_TABS_CHANGED }, create = false)
    public void refreshListsOnMainTabsChanged() {
        refreshLists(EventNames.MAIN_TABS_CHANGED);
    }

    /**
     * @since 5.6
     */
    public void refreshLists(String eventName, DocumentModel selectedDocument) {

        if (lastDocumentRef != null && lastDocumentRef.equals(selectedDocument.getRef())) {
            return;
        }

        refreshLists(eventName);

        lastDocumentRef = selectedDocument.getRef();
    }

    /**
     * Refresh lists for the event in parameter
     *
     * @since 10.3
     */
    protected void refreshLists(String eventName) {
        if (!documentsLists_events.containsKey(eventName)) {
            return;
        }
        for (String listName : documentsLists_events.get(eventName)) {

            List<DocumentModel> docList = documentsLists.get(listName);
            if (!docList.isEmpty()) {
                docList.clear();
                notifyListUpdated(listName);
            }
        }
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

}
