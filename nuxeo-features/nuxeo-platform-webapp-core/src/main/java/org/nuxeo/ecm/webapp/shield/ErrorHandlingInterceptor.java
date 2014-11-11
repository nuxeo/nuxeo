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
 * $Id$
 */

package org.nuxeo.ecm.webapp.shield;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.FacesLifecycle;
import  org.jboss.seam.faces.Redirect;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;

/**
 * Error handling interceptor.
 * <p>
 * Redirects to the good error page if an exception is caught: login page on
 * security exception, themed error page on other exceptions and unthemed error
 * page when another error is caught while rendering the error page.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated use org.nuxeo.ecm.platform.ui.web.shield.NuxeoErrorInterceptor
 *             instead - TODO: Remove in 5.2.
 */
@Deprecated
public class ErrorHandlingInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ErrorHandlingInterceptor.class);

    private static final String GENERIC_ERROR_VIEW_ID = "/generic_error_page.xhtml";

    private static final String UNTHEMED_ERROR_VIEW_ID = "/unthemed_generic_error_page.xhtml";

    private static final String LOGIN_VIEW_ID = "/login.jsp";

    @AroundInvoke
    public Object invokeAndWrapExceptions(InvocationContext invocation)
            throws Exception {
        try {
            log.debug("Before invocation...");
            return invocation.proceed();
        } catch (Throwable t) {
            ClientException cException = ClientException.wrap(t);
            // redirect is not allowed during render response phase => throw the
            // error without redirecting
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (FacesLifecycle.getPhaseId() == PhaseId.RENDER_RESPONSE) {
                throw cException;
            }

            // check if previous page was already an error page to avoid
            // redirect cycle
            if (facesContext != null) {
                ExternalContext externalContext = facesContext.getExternalContext();
                if (externalContext != null) {
                    Map<String, String[]> requestMap = externalContext.getRequestHeaderValuesMap();
                    if (requestMap != null) {
                        String[] previousPage = requestMap.get("Referer");
                        if (previousPage != null && previousPage.length != 0) {
                            String pageName = previousPage[0];
                            if (pageName != null
                                    && pageName.contains("error_page")) {
                                redirectToErrorPage(UNTHEMED_ERROR_VIEW_ID);
                                return null;
                            }
                        }
                    }
                }
            }

            String redirectToViewId;
            try {
                log.error("Exception caught, redirecting to the error page...");
                final Context sessionContext = Contexts.getSessionContext();
                // set applicationException in session hoping
                // ErrorPageActionListener will inject it
                sessionContext.set("applicationException", cException);
                if (cException.getCause() instanceof SecurityException) {
                    redirectToViewId = LOGIN_VIEW_ID;
                } else {
                    redirectToViewId = GENERIC_ERROR_VIEW_ID;
                }
            } catch (Throwable e) {
                // might be the case when session context is null
                log.error(e);
                redirectToViewId = UNTHEMED_ERROR_VIEW_ID;
            }

            if (redirectToErrorPage(redirectToViewId)) {
                return null;
            } else {
                log.info("Unable to handle exception in web-context. "
                        + "It might be an external (soap) request. "
                        + "Throwing further...");
                log.error("Original error", t);
                throw cException;
            }
        }
    }

    private boolean redirectToErrorPage(String viewId) {
        final String logPrefix = "<redirectToErrorPage> ";

        final FacesContext facesContext = FacesContext.getCurrentInstance();
        // we cannot call redirect if facesContext is null (Seam internals)
        if (null == facesContext) {
            // TODO decrease debug level
            log.info(logPrefix + "cannot redirect to error page");
            return false;
        }

        // avoid further redirection
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        request.setAttribute(URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY,
                true);

        Redirect.instance().setViewId(viewId);
        Redirect.instance().execute();
        return true;
    }

}
