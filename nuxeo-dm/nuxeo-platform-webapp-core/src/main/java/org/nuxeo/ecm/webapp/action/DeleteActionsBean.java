/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.action;

import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHILDREN_CHANGED;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.edit.lock.LockActions;
import org.nuxeo.ecm.webapp.trashManagement.TrashManager;
import org.nuxeo.runtime.api.Framework;

@Name("deleteActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class DeleteActionsBean implements DeleteActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DeleteActionsBean.class);

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected RepositoryLocation currentServerLocation;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient TrashManager trashManager;

    @In(create = true)
    protected transient LockActions lockActions;

    @In(create = true)
    protected transient WebActions webActions;

    @In
    protected transient Principal currentUser;

    protected transient TrashService trashService;

    protected TrashService getTrashService() {
        if (trashService == null) {
            trashService = Framework.getService(TrashService.class);
        }
        return trashService;
    }

    @Override
    public boolean getCanDeleteItem(DocumentModel container) {
        if (container == null) {
            return false;
        }
        return getTrashService().folderAllowsDelete(container);
    }

    @Override
    public boolean getCanDelete() {
        return getCanDelete(CURRENT_DOCUMENT_SELECTION);
    }

    @Override
    public boolean getCanDelete(String listName) {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(listName);
        return getTrashService().canDelete(docs, currentUser, false);
    }

    @Override
    public boolean getCanDeleteSections() {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SECTION_SELECTION);
        return getTrashService().canDelete(docs, currentUser, true);
    }

    @Override
    public boolean getCanPurge() {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION);
        return getTrashService().canPurgeOrUntrash(docs, currentUser);
    }

    public boolean getCanEmptyTrash() {
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION);
        if (selectedDocuments.size() == 0) {
            DocumentModelList currentTrashDocuments = getTrashService().getDocuments(navigationContext.getCurrentDocument());
            return getTrashService().canPurgeOrUntrash(currentTrashDocuments, currentUser);
        }
        return false;
    }

    @Override
    public boolean checkDeletePermOnParents(List<DocumentModel> docs) {
        return getTrashService().checkDeletePermOnParents(docs);
    }

    @Override
    public String deleteSelection() {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SELECTION)) {
            return deleteSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    @Override
    public String deleteSelectionSections() {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SECTION_SELECTION)) {
            return deleteSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    protected static final int OP_DELETE = 1, OP_PURGE = 2, OP_UNDELETE = 3;

    @Override
    public String deleteSelection(List<DocumentModel> docs) {
        int op = isTrashManagementEnabled() ? OP_DELETE : OP_PURGE;
        return actOnSelection(op, docs);
    }

    public String emptyTrash() {
        DocumentModelList currentTrashDocuments = trashService.getDocuments(navigationContext.getCurrentDocument());
        return purgeSelection(currentTrashDocuments);
    }

    @Override
    public String purgeSelection() {
        return purgeSelection(CURRENT_DOCUMENT_TRASH_SELECTION);
    }

    @Override
    public String purgeSelection(String listName) {
        if (!documentsListsManager.isWorkingListEmpty(listName)) {
            return purgeSelection(documentsListsManager.getWorkingList(listName));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    @Override
    public String purgeSelection(List<DocumentModel> docs) {
        return actOnSelection(OP_PURGE, docs);
    }

    @Override
    public String undeleteSelection() {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_TRASH_SELECTION)) {
            return undeleteSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    @Override
    public String undeleteSelection(List<DocumentModel> docs) {
        return actOnSelection(OP_UNDELETE, docs);

    }

    @SuppressWarnings("deprecation")
    protected String actOnSelection(int op, List<DocumentModel> docs) {
        if (docs == null) {
            return null;
        }
        TrashInfo info = (TrashInfo) getTrashService().getTrashInfo(docs, currentUser, false, false);

        DocumentModel targetContext = getTrashService().getAboveDocument(navigationContext.getCurrentDocument(),
                info.rootPaths);

        // remove from all lists
        documentsListsManager.removeFromAllLists(info.docs);

        Set<DocumentRef> parentRefs;
        String msgid;
        // operation to do
        switch (op) {
        case OP_PURGE:
            getTrashService().purgeDocuments(documentManager, info.rootRefs);
            parentRefs = info.rootParentRefs;
            msgid = "n_deleted_docs";
            break;
        case OP_DELETE:
            getTrashService().trashDocuments(info.docs);
            parentRefs = info.rootParentRefs;
            msgid = "n_deleted_docs";
            break;
        case OP_UNDELETE:
            parentRefs = getTrashService().undeleteDocuments(info.docs);
            msgid = "n_undeleted_docs";
            break;
        default:
            throw new AssertionError(op);
        }

        // Update context if needed
        if (op == OP_UNDELETE) {
            // undelete is problematic because it may change undeleted
            // parent's paths... so we refetch the new context
            targetContext = documentManager.getDocument(new IdRef(targetContext.getId()));
        } else if (targetContext == null) {
            // handle placeless document
            targetContext = documentManager.getRootDocument();
        }
        navigationContext.setCurrentDocument(targetContext);

        // Notify parents
        if (parentRefs.isEmpty()) {
            // Globally refresh content views
            Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED);
        } else {
            for (DocumentRef parentRef : parentRefs) {
                if (documentManager.hasPermission(parentRef, SecurityConstants.READ)) {
                    DocumentModel parent = documentManager.getDocument(parentRef);
                    if (parent != null) {
                        Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED, parent);
                    }
                }
            }
        }

        // User feedback
        if (info.proxies > 0) {
            facesMessages.add(StatusMessage.Severity.WARN, "can_not_delete_proxies");
        }
        Object[] params = { Integer.valueOf(info.docs.size()) };
        facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get(msgid), params);

        return null;
    }

    @Override
    public boolean isTrashManagementEnabled() {
        return trashManager.isTrashManagementEnabled();
    }

    public List<Action> getActionsForTrashSelection() {
        return webActions.getActionsList(CURRENT_DOCUMENT_TRASH_SELECTION + "_LIST", false);
    }

    @Override
    public void create() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void restoreCurrentDocument() {
        List<DocumentModel> doc = new ArrayList<DocumentModel>();
        doc.add(navigationContext.getCurrentDocument());
        undeleteSelection(doc);
    }

    @Override
    public boolean getCanRestoreCurrentDoc() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            // this shouldn't happen, if it happens probably there is a
            // customization bug, we guard this though
            log.warn("Null currentDocument in navigationContext");
            return false;
        }
        return getTrashService().canPurgeOrUntrash(doc, currentUser);
    }

    public boolean restoreActionDisplay() {
        return getCanRestoreCurrentDoc() && isTrashManagementEnabled();
    }
}
