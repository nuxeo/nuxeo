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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import static javax.faces.event.PhaseId.INVOKE_APPLICATION;
import static javax.faces.event.PhaseId.RENDER_RESPONSE;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.jsf.TransactionalSeamPhaseListener;
import org.jboss.seam.util.Transactions;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.shield.ExceptionHelper;
import org.nuxeo.ecm.platform.ui.web.shield.NuxeoExceptionFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Phase listener helper to perform redirection to meaningful urls.
 *
 * @author tiry
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class RestfulPhaseListener extends TransactionalSeamPhaseListener {

    private static final long serialVersionUID = -1064952127559721398L;

    private static final Log log = LogFactory.getLog(RestfulPhaseListener.class);

    protected URLPolicyService service;

    protected URLPolicyService getURLPolicyService() throws Exception {
        if (service == null) {
            service = Framework.getService(URLPolicyService.class);
        }
        return service;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        if (RENDER_RESPONSE.equals(event.getPhaseId())) {
            FacesContext context = event.getFacesContext();
            HttpServletRequest httpRequest = (HttpServletRequest) context.getExternalContext().getRequest();
            try {
                URLPolicyService service = getURLPolicyService();
                if (service.isCandidateForDecoding(httpRequest)) {
                    super.beforePhase(event);

                    // restore state
                    service.navigate(context);

                    // apply requests parameters after - they may need the state
                    // to be restored first.
                    service.applyRequestParameters(context);

                    return;
                }
            } catch (Exception e) {
                Lifecycle.setPhaseId(INVOKE_APPLICATION); // XXX Hack !
                Throwable unwrappedException = NuxeoExceptionFilter.unwrapException(e);
                String userMessage = NuxeoExceptionFilter.getMessageForException(unwrappedException);
                String exceptionMessage = unwrappedException.getLocalizedMessage();
                HttpServletResponse httpResponse = (HttpServletResponse) context.getExternalContext().getResponse();
                httpRequest.setAttribute("applicationException", null);
                // Do the rollback
                try {
                    if ( Transactions.isTransactionMarkedRollback() )
                    {
                       Transactions.getUserTransaction().rollback();
                    }
                }  catch (Exception te)
                {
                    throw new IllegalStateException("Could not rollback transaction", te);
                }

                try {
                    NuxeoExceptionFilter.forwardToErrorPage(httpRequest,
                            httpResponse,
                            NuxeoExceptionFilter.getStackTrace(e),
                            exceptionMessage, userMessage, ExceptionHelper.isSecurityError(e));

                    Lifecycle.endRequest(context.getExternalContext());
                    context.responseComplete();
                    return;
                } catch (ServletException e1) {
                    log.error("Error During redirect in PhaseListener : "
                            + e1.getMessage());
                } catch (IOException e1) {
                    log.error("Error During redirect in PhaseListener : "
                            + e1.getMessage());
                }
            }
        }
        super.beforePhase(event);
    }

}
