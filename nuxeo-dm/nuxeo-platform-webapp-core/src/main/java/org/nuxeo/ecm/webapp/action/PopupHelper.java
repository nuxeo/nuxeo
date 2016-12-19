/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Quentin Lamerand
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.core.Events;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.webapp.edit.lock.LockActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;

@Name("popupHelper")
@Scope(CONVERSATION)
public class PopupHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PopupHelper.class);

    public static final String POPUP_CATEGORY = "POPUP";

    @In(required = true, create = true)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient DeleteActions deleteActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient LockActions lockActions;

    protected DocumentModel currentContainer;

    protected DocumentModel currentParent;

    protected DocumentModel currentPopupDocument;

    protected List<Action> unfiltredActions;

    protected void computeUnfiltredPopupActions() {
        unfiltredActions = webActions.getAllActions(POPUP_CATEGORY);
        // unfiltredActions =
        // webActions.getActionsList(POPUP_CATEGORY, false);
    }

    /**
     * Returns all popup actions: used to construct HTML menu template.
     */
    public List<Action> getUnfiltredPopupActions() {
        if (unfiltredActions == null) {
            computeUnfiltredPopupActions();
        }

        // post filters links to add docId
        for (Action act : unfiltredActions) {
            String lnk = act.getLink();
            if (lnk.startsWith("javascript:")) {
                lnk = lnk.replaceFirst("javascript:", "");
                act.setLink(lnk);
            }
        }
        return unfiltredActions;
    }

    public List<Action> getAvailablePopupActions(String popupDocId) {
        return webActions.getActionsList(POPUP_CATEGORY, createActionContext(popupDocId));
    }

    @WebRemote
    public List<String> getAvailableActionId(String popupDocId) {
        List<Action> availableActions = getAvailablePopupActions(popupDocId);
        List<String> availableActionsIds = new ArrayList<String>(availableActions.size());
        for (Action act : availableActions) {
            availableActionsIds.add(act.getId());
        }
        return availableActionsIds;
    }

    @WebRemote
    public List<String> getUnavailableActionId(String popupDocId) {
        List<String> result = new ArrayList<String>();

        List<Action> allActions = getUnfiltredPopupActions();
        List<String> allActionsIds = new ArrayList<String>(allActions.size());
        for (Action act : allActions) {
            allActionsIds.add(act.getId());
        }

        List<Action> availableActions = getAvailablePopupActions(popupDocId);
        List<String> availableActionsIds = new ArrayList<String>(availableActions.size());
        for (Action act : availableActions) {
            availableActionsIds.add(act.getId());
        }

        for (String act : allActionsIds) {
            if (!availableActionsIds.contains(act)) {
                result.add(act);
            }
        }

        return result;
    }

    protected ActionContext createActionContext(String popupDocId) {
        ActionContext ctx = actionContextProvider.createActionContext();

        DocumentModel currentDocument = ctx.getCurrentDocument();

        DocumentRef popupDocRef = new IdRef(popupDocId);
        try {
            DocumentModel popupDoc = documentManager.getDocument(popupDocRef);
            ctx.setCurrentDocument(popupDoc);
            ctx.putLocalVariable("container", currentDocument);
            currentPopupDocument = popupDoc;
            currentContainer = currentDocument;
        } catch (DocumentNotFoundException e) {
            log.error(e, e);
        }

        return ctx;
    }

    @WebRemote
    public String getNavigationURL(String docId, String tabId) {
        Map<String, String> params = new HashMap<String, String>();

        if (tabId != null) {
            params.put("tabId", tabId);
        }

        DocumentModel doc = documentManager.getDocument(new IdRef(docId));

        return DocumentModelFunctions.documentUrl(null, doc, null, params, false);
    }

    @WebRemote
    public String getNavigationURLOnContainer(String tabId) {
        Map<String, String> params = new HashMap<String, String>();
        if (tabId != null) {
            params.put("tabId", tabId);
        }

        return DocumentModelFunctions.documentUrl(null, currentContainer, null, params, false);
    }

    @WebRemote
    public String getNavigationURLOnPopupdoc(String tabId) {
        return getNavigationURLOnPopupdoc2(tabId, null);
    }

    protected HttpServletRequest getRequest() {
        HttpServletRequest request = ServletContexts.instance().getRequest();
        if (request != null) {
            return request;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            return (HttpServletRequest) context.getExternalContext().getRequest();
        }
        return null;
    }

    @WebRemote
    public String getNavigationURLOnPopupdoc2(String tabId, String subTabId) {
        Map<String, String> params = new HashMap<String, String>();
        if (tabId != null) {
            params.put("tabId", tabId);
        }
        if (subTabId != null) {
            params.put("subTabId", subTabId);
        }
        return DocumentModelFunctions.documentUrl(null, currentPopupDocument, null, params, false, getRequest());
    }

    protected Map<String, String> getCurrentTabParameters() {
        Map<String, String> params = new HashMap<String, String>();
        String tabId = webActions.getCurrentTabId();
        if (tabId != null) {
            params.put("tabId", tabId);
        }
        String subTabId = webActions.getCurrentSubTabId();
        if (subTabId != null) {
            params.put("subTabId", subTabId);
        }
        return params;
    }

    @WebRemote
    public String getCurrentURL() {
        return DocumentModelFunctions.documentUrl(null, currentContainer, null, getCurrentTabParameters(), false);
    }

    @WebRemote
    public String getCurrentURLAfterDelete() {
        if (!isDocumentDeleted(currentContainer)) {
            currentParent = currentContainer;
        }
        return DocumentModelFunctions.documentUrl(null, currentParent, null, getCurrentTabParameters(), false);
    }

    @WebRemote
    public String deleteDocument(String docId) {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        currentParent = getFirstParentAfterDelete(doc);
        List<DocumentModel> docsToDelete = new ArrayList<DocumentModel>(1);
        docsToDelete.add(doc);
        return deleteActions.deleteSelection(docsToDelete);
    }

    @WebRemote
    public String editTitle(String docId, String newTitle) {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        doc.setProperty("dublincore", "title", newTitle);
        documentManager.saveDocument(doc);
        documentManager.save();
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED);
        return "OK";
    }

    public boolean getIsCurrentContainerDirectParent() {
        if (documentManager != null && currentContainer != null && currentPopupDocument != null) {
            DocumentModel parent = documentManager.getParentDocument(currentPopupDocument.getRef());
            return currentContainer.equals(parent);
        }
        return false;
    }

    public boolean isDocumentHasBlobAttached(DocumentModel documentModel) {
        if (documentModel.hasSchema("file")) {
            Blob blob = (Blob) documentModel.getProperty("file", "content");
            return blob != null;
        } else {
            return false;
        }
    }

    public boolean isDocumentHasBlobs(DocumentModel documentModel) {
        BlobHolder bh = documentModel.getAdapter(BlobHolder.class);
        if (bh != null) {
            List<Blob> docBlobs = bh.getBlobs();
            if (docBlobs != null && !docBlobs.isEmpty()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @WebRemote
    public String downloadDocument(String docId, String blobPropertyName, String filenamePropertyName)
            {
        DocumentModel documentModel = documentManager.getDocument(new IdRef(docId));
        String filename = (String) documentModel.getPropertyValue(filenamePropertyName);
        return DocumentModelFunctions.fileUrl("downloadFile", documentModel, blobPropertyName, filename);
    }

    @WebRemote
    public String lockDocument(String docId) {
        DocumentModel documentModel = documentManager.getDocument(new IdRef(docId));
        return lockActions.lockDocument(documentModel);
    }

    @WebRemote
    public String unlockDocument(String docId) {
        DocumentModel documentModel = documentManager.getDocument(new IdRef(docId));
        return lockActions.unlockDocument(documentModel);
    }

    @WebRemote
    public String sendEmail(String docId) {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        return DocumentModelFunctions.documentUrl(null, doc, "send_notification_email", null, false);
    }

    private DocumentModel getFirstParentAfterDelete(DocumentModel doc) {
        List<DocumentModel> parents = documentManager.getParentDocuments(doc.getRef());
        parents.remove(doc);
        Collections.reverse(parents);
        for (DocumentModel currentParent : parents) {
            try {
                documentManager.getDocument(currentParent.getRef());
                return currentParent;
            } catch (DocumentNotFoundException e) {
                continue;
            }
        }
        return null;
    }

    private boolean isDocumentDeleted(DocumentModel doc) {
        // test if the document still exists in the repository
        if (!documentManager.exists(doc.getRef())) {
            return true;
        }
        // test if the document is in the trash
        if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            return true;
        }
        return false;
    }

}
