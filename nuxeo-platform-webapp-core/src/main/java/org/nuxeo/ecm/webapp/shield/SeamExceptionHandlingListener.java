/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.webapp.shield;

import java.io.IOException;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.SystemException;

import org.jboss.seam.Seam;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.mock.MockApplication;
import org.jboss.seam.mock.MockExternalContext;
import org.jboss.seam.mock.MockFacesContext;
import org.jboss.seam.transaction.Transaction;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.NullExceptionHandlingListener;

/**
 * @author arussel
 *
 */
public class SeamExceptionHandlingListener extends NullExceptionHandlingListener {

    @Override
    public void beforeSetErrorPageAttribute(Throwable t,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        // cut/paste from seam Exception filter.
        // we recreate the seam context to be able to use the messages.
        MockFacesContext facesContext = createFacesContext(request, response);
        facesContext.setCurrent();

        // if the event context was cleaned up, fish the conversation id
        // directly out of the ServletRequest attributes, else get it from
        // the event context
        Manager manager = Contexts.isEventContextActive()
                ? (Manager) Contexts.getEventContext().get(Manager.class)
                : (Manager) request.getAttribute(Seam.getComponentName(Manager.class));
        String conversationId = manager == null ? null
                : manager.getCurrentConversationId();
        FacesLifecycle.beginExceptionRecovery(facesContext.getExternalContext());
        // If there is an existing long-running conversation on
        // the failed request, propagate it
        if (conversationId == null) {
            Manager.instance().initializeTemporaryConversation();
        } else {
            ConversationPropagation.instance().setConversationId(conversationId);
            Manager.instance().restoreConversation();
        }
        // we get the message from the seam attribute as the EL won't work in
        // xhtml
        FacesLifecycle.beginExceptionRecovery(facesContext.getExternalContext());
    }

    @Override
    public void afterDispatch(Throwable t, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        FacesContext context = FacesContext.getCurrentInstance();
        FacesLifecycle.endRequest(context.getExternalContext());
        context.release();
    }

    @Override
    public void startHandling(Throwable t, HttpServletRequest request,
            HttpServletResponse response) throws ServletException {
        try {
            rollbackTransactionIfNecessary(t, response);
        } catch (IllegalStateException e) {
            throw new ServletException(e);
        } catch (SystemException e) {
            throw new ServletException(e);
        }
    }

    private void rollbackTransactionIfNecessary(Throwable t,
            HttpServletResponse response) throws ServletException,
            IllegalStateException, SecurityException, SystemException {
        if (Contexts.isEventContextActive()) {
            if (Transaction.instance().isActiveOrMarkedRollback()) {
                Transaction.instance().rollback();
            }
        }
        if (FacesLifecycle.getPhaseId() == PhaseId.RENDER_RESPONSE) {
            if (response.isCommitted()) {
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                } else {
                    throw new ServletException(t);
                }
            }
        }
    }

    private MockFacesContext createFacesContext(HttpServletRequest request,
            HttpServletResponse response) {
        MockFacesContext mockFacesContext = new MockFacesContext(
                new MockExternalContext(
                        request.getSession().getServletContext(), request,
                        response),
                new MockApplication());
        mockFacesContext.setViewRoot(new UIViewRoot());
        return mockFacesContext;
    }

}
