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
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;

/**
 * Utility class used to manage Seam init and cleanup
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class SeamOperationFilter {

    protected static final Log log = LogFactory.getLog(SeamOperationFilter.class);

    /**
     * Initialize a workable Seam context as well as a conversion if needed
     *
     * @param context
     * @param conversationId
     */
    public static void handleBeforeRun(OperationContext context,
            String conversationId) {

        CoreSession session = context.getCoreSession();

        // Initialize Seam context if needed
        if (!OperationHelper.isSeamContextAvailable()) {
            try {
                initializeSeamContext(context, conversationId, session);
            } catch (ClientException e) {
                log.error(e.getMessage());
                return;
            }
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
    public static void handleAfterRun(OperationContext context,
            String conversationId) {

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
            conversationId = (String) context.get("conversationId");
        }

        if (conversationId != null) {
            // CoreSession seamDocumentManager = (CoreSession)
            // Contexts.getConversationContext().get("seamDocumentManager");
            Contexts.getEventContext().remove("documentManager");
            // Manager.instance().endConversation(true);
        }
        ServletLifecycle.endRequest(request);
    }

    protected static void initializeSeamContext(OperationContext context,
            String conversationId, CoreSession session) throws ClientException {

        HttpServletRequest request = getRequest(context);
        if (request == null) {
            throw new ClientException(
                    "Can not init Seam context: no HttpServletRequest was found");
        }
        ServletLifecycle.beginRequest(request);
        ServletContexts.instance().setRequest(request);

        if (conversationId == null) {
            conversationId = (String) context.get("conversationId");
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
     * Gets the request from the Automation context, fallback on the
     * FacesContext.
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

    protected static void setSeamActionContext(OperationContext context,
            CoreSession session) {
        ActionContext seamActionContext = new ActionContext();
        NavigationContext navigationContext = (NavigationContext) Contexts.getConversationContext().get(
                "navigationContext");
        seamActionContext.setCurrentDocument(navigationContext.getCurrentDocument());
        seamActionContext.setDocumentManager(session);
        seamActionContext.put("SeamContext", new SeamContextHelper());
        seamActionContext.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());

        context.put("seamActionContext", seamActionContext);
    }

}
