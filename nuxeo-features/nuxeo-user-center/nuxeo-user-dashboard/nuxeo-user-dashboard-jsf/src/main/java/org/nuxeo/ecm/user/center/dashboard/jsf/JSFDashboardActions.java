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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.user.center.dashboard.jsf;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.UserAgentMatcher;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles JSF dashboard actions.
 *
 * @since 5.4.2
 */
@Name("jsfDashboardActions")
@Scope(CONVERSATION)
public class JSFDashboardActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CONTENT_VIEW_OBSERVER_WORKFLOW_EVENT = "workflowEvent";

    public static final String USER_DOMAINS_CONTENT_VIEW = "user_domains";

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    protected DocumentModel selectedDomain;

    protected List<DocumentModel> availableDomains;

    @Factory(value = "userDomains", scope = ScopeType.EVENT)
    @SuppressWarnings("unchecked")
    public List<DocumentModel> getUserDomains() {
        if (documentManager == null) {
            return new ArrayList<>();
        }
        if (availableDomains == null) {
            ContentView cv = contentViewActions.getContentView(USER_DOMAINS_CONTENT_VIEW);
            availableDomains = (List<DocumentModel>) cv.getPageProvider().getCurrentPage();
        }
        return availableDomains;
    }

    public DocumentModel getSelectedDomain() {
        List<DocumentModel> domains = getUserDomains();
        if (selectedDomain == null) {
            // initialize to current domain, or take first domain found
            DocumentModel currentDomain = navigationContext.getCurrentDomain();
            if (currentDomain != null) {
                selectedDomain = currentDomain;
            } else {
                if (domains != null && !domains.isEmpty()) {
                    selectedDomain = domains.get(0);
                }
            }
        } else if (domains != null && !domains.isEmpty() && !domains.contains(selectedDomain)) {
            // reset old domain: it's not available anymore
            selectedDomain = domains.get(0);
        }
        return selectedDomain;
    }

    public String getSelectedDomainId() {
        DocumentModel selectedDomain = getSelectedDomain();
        if (selectedDomain != null) {
            return selectedDomain.getId();
        }
        return null;
    }

    public void setSelectedDomainId(String selectedDomainId) {
        selectedDomain = documentManager.getDocument(new IdRef(selectedDomainId));
    }

    public String getSelectedDomainPath() {
        DocumentModel domain = getSelectedDomain();
        if (domain == null) {
            return "/";
        }
        return domain.getPathAsString() + "/";
    }

    public String getSelectedDomainTemplatesPath() {
        return getSelectedDomainPath() + "templates";
    }

    /**
     * Refreshes and resets content views that have declared the event "workflowEvent" as a refresh or reset event, on
     * every kind of workflow/task event.
     */
    @Observer(value = { TaskEventNames.WORKFLOW_ENDED, TaskEventNames.WORKFLOW_NEW_STARTED,
            TaskEventNames.WORKFLOW_TASK_STOP, TaskEventNames.WORKFLOW_TASK_REJECTED,
            TaskEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED, TaskEventNames.WORKFLOW_TASK_COMPLETED,
            TaskEventNames.WORKFLOW_TASK_REMOVED, TaskEventNames.WORK_ITEMS_LIST_LOADED,
            TaskEventNames.WORKFLOW_TASKS_COMPUTED, TaskEventNames.WORKFLOW_ABANDONED,
            TaskEventNames.WORKFLOW_CANCELED, EventNames.DOCUMENT_PUBLICATION_REJECTED,
            EventNames.DOCUMENT_PUBLICATION_APPROVED, EventNames.DOCUMENT_PUBLISHED }, create = false)
    public void onWorkflowEvent() {
        contentViewActions.refreshOnSeamEvent(CONTENT_VIEW_OBSERVER_WORKFLOW_EVENT);
        contentViewActions.resetPageProviderOnSeamEvent(CONTENT_VIEW_OBSERVER_WORKFLOW_EVENT);
    }

    @Factory(value = "isMSIE6or7", scope = ScopeType.SESSION)
    public boolean isMSIE6or7() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            String ua = request.getHeader("User-Agent");
            return UserAgentMatcher.isMSIE6or7(ua);
        } else {
            return false;
        }

    }

    public String[] getCurrentUserPrincipalsToCheck() {
        return SecurityService.getPrincipalsToCheck(documentManager.getPrincipal());
    }
}
