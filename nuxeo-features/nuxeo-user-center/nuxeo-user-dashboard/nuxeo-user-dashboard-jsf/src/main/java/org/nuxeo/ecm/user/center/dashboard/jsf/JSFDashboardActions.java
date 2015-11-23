/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.user.center.dashboard.jsf;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles JSF dashboard actions.
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
@Name("jsfDashboardActions")
@Scope(CONVERSATION)
public class JSFDashboardActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CONTENT_VIEW_OBSERVER_WORKFLOW_EVENT = "workflowEvent";

    public enum DASHBOARD_CONTENT_VIEWS {

        USER_TASKS, USER_PROCESSES, USER_DOMAINS, USER_DOCUMENTS, USER_DELETED_DOCUMENTS, DOMAIN_DOCUMENTS(
                true), DOMAIN_PUBLISHED_DOCUMENTS(true), USER_SECTIONS(true), USER_SITES(
                true), USER_WORKSPACES(true);

        boolean needsDomainContext;

        DASHBOARD_CONTENT_VIEWS() {
            this.needsDomainContext = false;
        }

        DASHBOARD_CONTENT_VIEWS(boolean needsDomainContext) {
            this.needsDomainContext = needsDomainContext;
        }

        public boolean isContextualToDomain() {
            return needsDomainContext;
        }

    }

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    protected DocumentModel selectedDomain;

    protected List<DocumentModel> availableDomains;

    @Factory(value = "domains", scope = ScopeType.EVENT)
    @SuppressWarnings("unchecked")
    public List<DocumentModel> getDomains() throws ClientException {
        if (documentManager == null) {
            return new ArrayList<DocumentModel>();
        }
        if (availableDomains == null) {
            ContentView cv = contentViewActions.getContentView(DASHBOARD_CONTENT_VIEWS.USER_DOMAINS.name());
            availableDomains = (List<DocumentModel>) cv.getPageProvider().getCurrentPage();
        }
        return availableDomains;
    }

    public DocumentModel getSelectedDomain() throws ClientException {
        List<DocumentModel> availableDomains = getDomains();
        if (selectedDomain == null) {
            // initialize to current domain, or take first domain found
            DocumentModel currentDomain = navigationContext.getCurrentDomain();
            if (currentDomain != null) {
                selectedDomain = currentDomain;
            } else {
                if (availableDomains != null && !availableDomains.isEmpty()) {
                    selectedDomain = availableDomains.get(0);
                }
            }
        } else if (availableDomains != null && !availableDomains.isEmpty()
                && !availableDomains.contains(selectedDomain)) {
            // reset old domain: it's not available anymore
            selectedDomain = availableDomains.get(0);
        }
        return selectedDomain;
    }

    public String getSelectedDomainId() throws ClientException {
        DocumentModel selectedDomain = getSelectedDomain();
        if (selectedDomain != null) {
            return selectedDomain.getId();
        }
        return null;
    }

    public void setSelectedDomainId(String selectedDomainId)
            throws ClientException {
        selectedDomain = documentManager.getDocument(new IdRef(selectedDomainId));
    }

    public String submitSelectedDomainChange() {
        refreshDashboardContentViews();
        return null;
    }

    public String getSelectedDomainPath() throws ClientException {
        DocumentModel domain = getSelectedDomain();
        if (domain == null) {
            return "";
        }
        return domain.getPathAsString() + "/";
    }

    public String getSelectedDomainTemplatesPath() throws ClientException {
        return getSelectedDomainPath() + "templates";
    }

    protected void refreshDashboardContentViews() {
        for (DASHBOARD_CONTENT_VIEWS cv : DASHBOARD_CONTENT_VIEWS.values()) {
            if (cv.isContextualToDomain()) {
                contentViewActions.refresh(cv.name());
            }
        }
    }

    /**
     * Refreshes content views that have declared the event "workflowEvent" as
     * a refresh event, on every kind of workflow/task event.
     */
    @Observer(value = { JbpmEventNames.WORKFLOW_ENDED,
            JbpmEventNames.WORKFLOW_NEW_STARTED,
            JbpmEventNames.WORKFLOW_TASK_STOP,
            JbpmEventNames.WORKFLOW_TASK_REJECTED,
            JbpmEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            JbpmEventNames.WORKFLOW_TASK_COMPLETED,
            JbpmEventNames.WORKFLOW_TASK_REMOVED,
            JbpmEventNames.WORK_ITEMS_LIST_LOADED,
            JbpmEventNames.WORKFLOW_TASKS_COMPUTED,
            JbpmEventNames.WORKFLOW_ABANDONED,
            JbpmEventNames.WORKFLOW_CANCELED,
            EventNames.DOMAIN_SELECTION_CHANGED,
            EventNames.DOCUMENT_PUBLICATION_REJECTED,
            EventNames.DOCUMENT_PUBLICATION_APPROVED,
            EventNames.DOCUMENT_PUBLISHED }, create = false)
    public void refreshOnWorkflowEvent() {
        contentViewActions.refreshOnSeamEvent(CONTENT_VIEW_OBSERVER_WORKFLOW_EVENT);
    }

    /**
     * Resets content views that have declared the event "workflowEvent" as a
     * reset event, on every kind of workflow/task event.
     */
    @Observer(value = { JbpmEventNames.WORKFLOW_ENDED,
            JbpmEventNames.WORKFLOW_NEW_STARTED,
            JbpmEventNames.WORKFLOW_TASK_START,
            JbpmEventNames.WORKFLOW_TASK_STOP,
            JbpmEventNames.WORKFLOW_TASK_REJECTED,
            JbpmEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            JbpmEventNames.WORKFLOW_TASK_COMPLETED,
            JbpmEventNames.WORKFLOW_TASK_REMOVED,
            JbpmEventNames.WORK_ITEMS_LIST_LOADED,
            JbpmEventNames.WORKFLOW_TASKS_COMPUTED,
            JbpmEventNames.WORKFLOW_ABANDONED,
            JbpmEventNames.WORKFLOW_CANCELED,
            EventNames.DOMAIN_SELECTION_CHANGED,
            EventNames.DOCUMENT_PUBLICATION_REJECTED,
            EventNames.DOCUMENT_PUBLICATION_APPROVED,
            EventNames.DOCUMENT_PUBLISHED }, create = false)
    public void resetPageProviderOnWorkflowEvent() {
        contentViewActions.resetPageProviderOnSeamEvent(CONTENT_VIEW_OBSERVER_WORKFLOW_EVENT);
    }

}
