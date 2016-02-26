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

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.transaction.Transaction;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Phase listener helper to perform redirection to meaningful urls.
 */
public class RestfulPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RestfulPhaseListener.class);

    public static final String SEAM_HOTRELOAD_TRIGGER_ACTION = "#{seamReloadContext.triggerReloadIdNeeded()}";

    protected URLPolicyService service;

    protected URLPolicyService getURLPolicyService() {
        if (service == null) {
            service = Framework.getService(URLPolicyService.class);
        }
        return service;
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        HttpServletRequest httpRequest = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            URLPolicyService service = getURLPolicyService();
            if (service.isCandidateForDecoding(httpRequest)) {
                // Make sure we're in a transaction, and that Seam knows it.
                // Sometimes, when there is a page action, SeamPhaseListener
                // commits the transaction in RENDER_RESPONSE before this
                // phase listener executes, but does not start a new one.
                // (SeamPhaseListener.handleTransactionsAfterPageActions)
                if (!Transaction.instance().isActiveOrMarkedRollback()) {
                    Transaction.instance().begin();
                }
                // hot reload hook, maybe to move up so that it's handled on
                // all requests, not only the ones using the URLservice
                // framework (?)
                resetHotReloadContext(context);
                // restore state
                service.navigate(context);
                // apply requests parameters after - they may need the state
                // to be restored first.
                service.applyRequestParameters(context);
            }
        } catch (RuntimeException | SystemException | NotSupportedException e) {
            handleException(context, e);
        }
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        // SeamPhaseListener.handleTransactionsAfterPhase will commit the
        // transaction, so it has to be started using Seam methods.
    }

    protected void handleException(FacesContext context, Exception e) {
        try {
            ExternalContext externalContext = context.getExternalContext();
            ExceptionHandlingService exceptionHandlingService = Framework.getService(ExceptionHandlingService.class);
            NuxeoExceptionHandler handler = exceptionHandlingService.getExceptionHandler();
            handler.handleException((HttpServletRequest) externalContext.getRequest(),
                    (HttpServletResponse) externalContext.getResponse(), e);
        } catch (ServletException | IOException e1) {
            throw new NuxeoException(e1);
        }
    }

    /**
     * Hack trigger of a Seam component that will trigger reset of most Seam components caches, only if dev mode is
     * enabled
     * <p>
     * This is handled here to be done very early, before response is constructed.
     *
     * @since 5.6
     * @see Framework#isDevModeSet()
     */
    protected void resetHotReloadContext(FacesContext facesContext) {
        if (Framework.isDevModeSet()) {
            try {
                ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
                ELContext context = facesContext.getELContext();
                String actionBinding = SEAM_HOTRELOAD_TRIGGER_ACTION;
                MethodExpression action = ef.createMethodExpression(context, actionBinding, String.class,
                        new Class[] { DocumentView.class });
                action.invoke(context, new Object[0]);
            } catch (ELException | NullPointerException e) {
                String msg = "Error while trying to flush seam context after a reload, executing method expression '"
                        + SEAM_HOTRELOAD_TRIGGER_ACTION + "'";
                log.error(msg, e);
            }
        }
    }
}
