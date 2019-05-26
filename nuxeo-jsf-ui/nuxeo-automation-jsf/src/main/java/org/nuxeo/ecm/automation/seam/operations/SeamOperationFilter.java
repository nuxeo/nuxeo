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
 */
package org.nuxeo.ecm.automation.seam.operations;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.GetActions;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;

/**
 * Utility class used to manage Seam init and cleanup
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class SeamOperationFilter {

    protected static final Log log = LogFactory.getLog(SeamOperationFilter.class);

    private SeamOperationFilter() {
        // Helper class
    }

    /**
     * Initialize a workable Seam context as well as a conversion if needed
     *
     * @param context
     * @param conversationId
     */
    public static void handleBeforeRun(OperationContext context, String conversationId) {

        CoreSession session = context.getCoreSession();

        // Initialize Seam context if needed
        if (!OperationHelper.isSeamContextAvailable()) {
            initializeSeamContext(context, conversationId, session);
        } else {
            // Only set Seam Action context
            setSeamActionContext(context, session);
        }
    }

    /**
     * Manages Seam context and lifecycle cleanup
     *
     * @param context
     * @param conversationId
     */
    public static void handleAfterRun(OperationContext context, String conversationId) {

        // Cannot destroy Seam context if it is not initialized
        if (!OperationHelper.isSeamContextAvailable()) {
            log.error("Cannot destroy Seam context: it is not initialized");
            return;
        }

        HttpServletRequest request = getRequest(context);
        if (request == null) {
            log.error("Can not destroy Seam context: no HttpServletRequest was found");
            return;
        }

        if (conversationId == null) {
            conversationId = (String) context.getChainParameter("conversationId");
        }

        if (conversationId != null) {
            Contexts.getEventContext().remove("documentManager");
        }
        ServletLifecycle.endRequest(request);
    }

    protected static void initializeSeamContext(OperationContext context, String conversationId, CoreSession session) {

        HttpServletRequest request = getRequest(context);
        if (request == null) {
            throw new NuxeoException("Can not init Seam context: no HttpServletRequest was found");
        }
        ServletLifecycle.beginRequest(request);
        ServletContexts.instance().setRequest(request);

        if (conversationId == null) {
            conversationId = (String) context.getChainParameter("conversationId");
        }

        if (conversationId != null) {
            ConversationPropagation.instance().setConversationId(conversationId);
            Manager.instance().restoreConversation();
            ServletLifecycle.resumeConversation(request);
            Contexts.getEventContext().set("documentManager", session);
            setSeamActionContext(context, session);
        }
    }

    /**
     * Gets the request from the Automation context, fallback on the FacesContext.
     */
    protected static HttpServletRequest getRequest(OperationContext context) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request == null) {
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces != null) {
                request = (HttpServletRequest) faces.getExternalContext().getRequest();
            }
        }
        return request;
    }

    protected static void setSeamActionContext(OperationContext context, CoreSession session) {
        ActionContext seamActionContext = new SeamActionContext();
        NavigationContext navigationContext = (NavigationContext) Contexts.getConversationContext()
                                                                          .get("navigationContext");
        if (navigationContext != null) {
            seamActionContext.setCurrentDocument(navigationContext.getCurrentDocument());
        }
        seamActionContext.setDocumentManager(session);
        seamActionContext.putLocalVariable("SeamContext", new SeamContextHelper());
        seamActionContext.setCurrentPrincipal(session.getPrincipal());

        context.put(GetActions.SEAM_ACTION_CONTEXT, seamActionContext);
    }

}
