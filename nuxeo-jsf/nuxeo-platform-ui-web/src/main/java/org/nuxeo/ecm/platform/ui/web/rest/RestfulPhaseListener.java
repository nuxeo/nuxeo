/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.io.IOException;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.transaction.Transaction;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Phase listener helper to perform redirection to meaningful urls.
 */
public class RestfulPhaseListener implements PhaseListener {

    private static final long serialVersionUID = -1064952127559721398L;

    protected URLPolicyService service;

    protected URLPolicyService getURLPolicyService() throws Exception {
        if (service == null) {
            service = Framework.getService(URLPolicyService.class);
        }
        return service;
    }

    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    public void beforePhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletRequest httpRequest = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            URLPolicyService service;
            try {
                service = getURLPolicyService();
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
            if (service.isCandidateForDecoding(httpRequest)) {
                // Make sure we're in a transaction, and that Seam knows it.
                // Sometimes, when there is a page action, SeamPhaseListener
                // commits the transaction in RENDER_RESPONSE before this
                // phase listener executes, but does not start a new one.
                // (SeamPhaseListener.handleTransactionsAfterPageActions)
                if (!Transaction.instance().isActiveOrMarkedRollback()) {
                    Transaction.instance().begin();
                }
                // restore state
                service.navigate(context);
                // apply requests parameters after - they may need the state
                // to be restored first.
                service.applyRequestParameters(context);
            }
        } catch (Exception e) {
            handleException(context, e);
        }
    }

    public void afterPhase(PhaseEvent event) {
        // SeamPhaseListener.handleTransactionsAfterPhase will commit the
        // transaction, so it has to be started using Seam methods.
    }

    protected void handleException(FacesContext context, Exception e)
            throws ClientRuntimeException {
        try {
            ExternalContext externalContext = context.getExternalContext();
            ExceptionHandlingService exceptionHandlingService;
            try {
                exceptionHandlingService = Framework.getService(ExceptionHandlingService.class);
            } catch (Exception e1) {
                // hopeless
                throw new ClientRuntimeException(e1);
            }
            NuxeoExceptionHandler handler = exceptionHandlingService.getExceptionHandler();
            handler.handleException(
                    (HttpServletRequest) externalContext.getRequest(),
                    (HttpServletResponse) externalContext.getResponse(), e);
        } catch (ServletException e1) {
            throw new ClientRuntimeException(e1);
        } catch (IOException e1) {
            throw new ClientRuntimeException(e1);
        }
    }

}
