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
public class ConversationDocumentsListsManager extends
        BaseDocumentsListsManager implements Serializable {

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
                DocumentsListDescriptor desc = getService().getDocumentsListDescriptor(
                        listName);
                if (!desc.getIsSession()) {
                    createWorkingList(listName, desc);
                }
            }
            initialized = true;
        }
    }

    // Event listener
    @Observer(value = { EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED }, create = false)
    public void refreshLists(DocumentModel selectedDocument) {

        if (lastDocumentRef != null
                && lastDocumentRef.equals(selectedDocument.getRef())) {
            return;
        }

        if (!documentsLists_events.containsKey(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED)) {
            return;
        }
        for (String listName : documentsLists_events.get(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED)) {

            List<DocumentModel> docList = documentsLists.get(listName);
            if (!docList.isEmpty()) {
                docList.clear();
                notifyListUpdated(listName);
            }
        }

        lastDocumentRef = selectedDocument.getRef();
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
