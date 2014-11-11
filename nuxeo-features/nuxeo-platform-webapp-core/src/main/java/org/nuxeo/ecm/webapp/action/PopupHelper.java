/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Quentin Lamerand
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Name("popupHelper")
@Scope(CONVERSATION)
@SerializedConcurrentAccess
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

    protected DocumentModel currentContainer;

    protected DocumentModel currentPopupDocument;

    protected List<Action> unfiltredActions;

    protected void computeUnfiltredPopupActions() {
        unfiltredActions = webActions.getUnfiltredActionsList(POPUP_CATEGORY);
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
        return webActions.getActionsList(POPUP_CATEGORY,
                createActionContext(popupDocId));
    }

    @WebRemote
    public List<String> getAvailableActionId(String popupDocId) {
        List<Action> availableActions = getAvailablePopupActions(popupDocId);
        List<String> availableActionsIds = new ArrayList<String>(
                availableActions.size());
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
        List<String> availableActionsIds = new ArrayList<String>(
                availableActions.size());
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
            ctx.put("container", currentDocument);
            currentPopupDocument = popupDoc;
            currentContainer = currentDocument;
        } catch (ClientException e) {
            log.error(e);
        }

        return ctx;
    }

    @WebRemote
    public String getNavigationURL(String docId, String tabId)
            throws ClientException {
        Map<String, String> params = new HashMap<String, String>();

        if (tabId != null) {
            params.put("tabId", tabId);
        }

        DocumentModel doc = documentManager.getDocument(new IdRef(docId));

        return DocumentModelFunctions.documentUrl(null, doc, null, params,
                false);
    }

    @WebRemote
    public String getNavigationURLOnContainer(String tabId) {
        Map<String, String> params = new HashMap<String, String>();

        if (tabId != null) {
            params.put("tabId", tabId);
        }

        return DocumentModelFunctions.documentUrl(null, currentContainer, null,
                params, false);
    }

    @WebRemote
    public String getNavigationURLOnPopupdoc(String tabId) {
        Map<String, String> params = new HashMap<String, String>();

        if (tabId != null) {
            params.put("tabId", tabId);
        }

        return DocumentModelFunctions.documentUrl(null, currentPopupDocument,
                null, params, false);
    }

    @WebRemote
    public String getCurrentURL() {
        Map<String, String> params = new HashMap<String, String>();

        String tabId = webActions.getCurrentTabId();

        if (tabId != null) {
            params.put("tabId", tabId);
        }

        return DocumentModelFunctions.documentUrl(null, currentContainer, null,
                params, false);
    }

    @WebRemote
    public String deleteDocument(String docId) throws ClientException {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        List<DocumentModel> docsToDelete = new ArrayList<DocumentModel>(1);
        docsToDelete.add(doc);
        return deleteActions.deleteSelection(docsToDelete);
    }

    @WebRemote
    public String editTitle(String docId, String newTitle)
            throws ClientException {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        doc.setProperty("dublincore", "title", newTitle);
        documentManager.saveDocument(doc);
        documentManager.save();
        return "OK";
    }

    public boolean getIsCurrentContainerDirectParent() throws ClientException {
        if (documentManager != null && currentContainer != null
                && currentPopupDocument != null) {
            DocumentModel parent = documentManager.getParentDocument(currentPopupDocument.getRef());
            return currentContainer.equals(parent);
        }
        return false;
    }

}
