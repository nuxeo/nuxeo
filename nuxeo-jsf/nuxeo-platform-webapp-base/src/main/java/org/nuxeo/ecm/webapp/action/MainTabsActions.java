/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.NAVIGATE_TO_DOCUMENT;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle Main tab related actions.
 * <p>
 * Maintains a Map of tab id -> contextual document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
@Name("mainTabsActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class MainTabsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_VIEW = "view_documents";

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient ActionManager actionManager;

    protected Map<String, DocumentModel> documentsByMainTabs = new HashMap<String, DocumentModel>();

    @Observer({ NAVIGATE_TO_DOCUMENT })
    public void updateContextualDocument() {
        if (!shouldHandleRequest()) {
            return;
        }
        String currentMainTab = getCurrentMainTabFromRequest();
        if (currentMainTab == null) {
            currentMainTab = webActions.getCurrentTabId(WebActions.MAIN_TABS_CATEGORY);
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        documentsByMainTabs.put(currentMainTab, currentDocument);
    }

    /**
     * Only handle non POST requests
     */
    protected boolean shouldHandleRequest() {
        ServletRequest request = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            return !httpServletRequest.getMethod().equalsIgnoreCase("post");
        }
        return false;
    }

    protected String getCurrentMainTabFromRequest() {
        URLPolicyService service = Framework.getService(URLPolicyService.class);
        ServletRequest request = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request instanceof HttpServletRequest) {
            DocumentView docView = service.getDocumentViewFromRequest((HttpServletRequest) request);
            if (docView == null) {
                return null;
            }
            String tabIds = docView.getParameter(WebActions.TAB_IDS_PARAMETER);
            String mainTabId = docView.getParameter(WebActions.MAIN_TAB_ID_PARAMETER);
            if (mainTabId != null && !mainTabId.isEmpty()) {
                tabIds = mainTabId;
            }
            if (tabIds != null && tabIds.contains(WebActions.MAIN_TABS_CATEGORY)) {
                String[] encodedActions = tabIds.split(",");
                for (String encodedAction : encodedActions) {
                    if (encodedAction.startsWith(WebActions.MAIN_TABS_CATEGORY)) {
                        String[] actionInfo = encodedAction.split(":");
                        if (actionInfo != null && actionInfo.length > 1) {
                            return actionInfo[1];
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set the document used for a given {@code mainTabId}.
     *
     * @since 5.7
     */
    public void setDocumentFor(String mainTabId, DocumentModel doc) {
        documentsByMainTabs.put(mainTabId, doc);
    }

    public DocumentModel getDocumentFor(String mainTabId) {
        return getDocumentFor(mainTabId, navigationContext.getCurrentDocument());
    }

    public DocumentModel getDocumentFor(String mainTabId, DocumentModel defaultDocument) {
        DocumentModel doc = documentsByMainTabs.get(mainTabId);
        if (doc == null || !documentManager.exists(doc.getRef())
                || !documentManager.hasPermission(doc.getRef(), SecurityConstants.READ)) {
            documentsByMainTabs.put(mainTabId, defaultDocument);
            doc = null;
        }

        if (doc != null && !documentManager.exists(new PathRef(doc.getPathAsString()))) {
            // path has changed, refresh the document to have a correct URL
            doc = documentManager.getDocument(doc.getRef());
            documentsByMainTabs.put(mainTabId, doc);
        }

        return doc != null ? doc : defaultDocument;
    }

    public String getViewFor(Action mainTabAction) {
        if (!mainTabAction.getId().equals(WebActions.DOCUMENTS_MAIN_TAB_ID)) {
            return mainTabAction.getLink();
        }

        DocumentModel doc = getDocumentFor(mainTabAction.getId(), navigationContext.getCurrentDocument());
        if (doc != null) {
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            return typeInfo.getDefaultView();
        }
        return DEFAULT_VIEW;
    }

    public String getViewFor(String mainTabId) {
        Action mainTabAction = actionManager.getAction(mainTabId);
        return mainTabAction != null ? getViewFor(mainTabAction) : null;
    }

    public String getPatternFor(String mainTabId) {
        URLPolicyService service = Framework.getService(URLPolicyService.class);
        // FIXME: find some way to reference the pattern in the action,
        // assume the pattern will be the same than the default one for
        // now, or use the default one.
        if (!WebActions.DOCUMENTS_MAIN_TAB_ID.equals(mainTabId) && service.hasPattern(mainTabId)) {
            return mainTabId;
        }
        return service.getDefaultPatternName();
    }

    public boolean isOnMainTab(String mainTabId) {
        if (mainTabId != null && mainTabId.equals(webActions.getCurrentTabId(WebActions.MAIN_TABS_CATEGORY))) {
            return true;
        }
        return false;
    }

}
