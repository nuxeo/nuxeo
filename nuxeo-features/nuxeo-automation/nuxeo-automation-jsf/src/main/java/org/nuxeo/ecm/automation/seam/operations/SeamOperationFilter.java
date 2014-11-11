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

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.automation.OperationContext;
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

    /**
     * Initialise a workable Seam context as well as a conversion if needed
     *
     * @param context
     * @param conversationId
     */
    public static void handleBeforeRun(OperationContext context,
            String conversationId) {

        CoreSession session = context.getCoreSession();
        HttpServletRequest request = (HttpServletRequest) context.get("request");
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

    /**
     * Manages Seam context and lifecycle cleanup
     *
     * @param context
     * @param conversationId
     */
    public static void handleAfterRun(OperationContext context,
            String conversationId) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");

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
}
