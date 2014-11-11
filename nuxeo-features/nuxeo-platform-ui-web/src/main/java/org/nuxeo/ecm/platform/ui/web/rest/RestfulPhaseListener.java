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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.transaction.Transaction;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Phase listener helper to perform redirection to meaningful urls.
 *
 * @author tiry
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author Florent Guillaume
 *
 */

public class RestfulPhaseListener implements PhaseListener {

    private static final long serialVersionUID = -1064952127559721398L;

    private static final Log log = LogFactory.getLog(RestfulPhaseListener.class);

    protected URLPolicyService service;

    protected URLPolicyService getURLPolicyService() throws Exception {
        if (service == null) {
            service = Framework.getService(URLPolicyService.class);
        }
        return service;
    }

    public PhaseId getPhaseId() {
        return RENDER_RESPONSE;
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
                // restore state
                service.navigate(context);
                // apply requests parameters after - they may need the state
                // to be restored first.
                service.applyRequestParameters(context);
            }
        } catch (Exception e) {
            FacesLifecycle.setPhaseId(INVOKE_APPLICATION); // XXX Hack !
            try {
                if (Transaction.instance().isMarkedRollback()) {
                    Transaction.instance().rollback();
                }
            } catch (Exception te) {
                throw new IllegalStateException(
                        "Could not rollback transaction", te);
            }

            try {
                ExternalContext externalContext = context.getExternalContext();
                ExceptionHandlingService exceptionHandlingService;
                try {
                    exceptionHandlingService = Framework.getService(ExceptionHandlingService.class);
                } catch (Exception e1) {
                    //hopeless
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

    public void afterPhase(PhaseEvent event) {
        // nothing to do
    }

}
