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
package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.WrappedException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandler;

/**
 * @author arussel
 */
public class DefaultNuxeoExceptionHandler implements NuxeoExceptionHandler {

    private static final Log log = LogFactory.getLog(DefaultNuxeoExceptionHandler.class);

    protected NuxeoExceptionHandlerParameters parameters;

    public void setParameters(NuxeoExceptionHandlerParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Puts a marker in request to avoid looping over the exception handling mechanism
     *
     * @throws ServletException if request has already been marked as handled. The initial exception is then wrapped.
     */

    protected void startHandlingException(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws ServletException {
        if (request.getAttribute(EXCEPTION_HANDLER_MARKER) == null) {
            if (log.isDebugEnabled()) {
                log.debug("Initial exception", t);
            }
            // mark request as already processed by this mechanism to avoid
            // looping over it
            request.setAttribute(EXCEPTION_HANDLER_MARKER, true);
            // disable further redirect by nuxeo url system
            request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, true);
        } else {
            // avoid looping over exception mechanism
            throw new ServletException(t);
        }
    }

    public void handleException(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws IOException, ServletException {
        startHandlingException(request, response, t);
        try {
            ErrorHandler handler = getHandler(t);
            Integer code = handler.getCode();
            int status = code == null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : code.intValue();
            parameters.getListener().startHandling(t, request, response);

            Throwable unwrappedException = unwrapException(t);
            StringWriter swriter = new StringWriter();
            PrintWriter pwriter = new PrintWriter(swriter);
            t.printStackTrace(pwriter);
            String stackTrace = swriter.getBuffer().toString();
            if (status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) { // 500
                log.debug(t.getMessage(), t);
            } else {
                log.error(stackTrace);
                parameters.getLogger().error(stackTrace);
            }

            parameters.getListener().beforeSetErrorPageAttribute(unwrappedException, request, response);
            request.setAttribute("exception_message", unwrappedException.getLocalizedMessage());
            request.setAttribute("user_message", getUserMessage(handler.getMessage(), request.getLocale()));
            request.setAttribute("stackTrace", stackTrace);
            request.setAttribute("securityError", ExceptionHelper.isSecurityError(unwrappedException));
            request.setAttribute("messageBundle", ResourceBundle.getBundle(parameters.getBundleName(),
                    request.getLocale(), Thread.currentThread().getContextClassLoader()));
            String dumpedRequest = parameters.getRequestDumper().getDump(request);
            if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) { // 500
                parameters.getLogger().error(dumpedRequest);
            }
            request.setAttribute("request_dump", dumpedRequest);

            parameters.getListener().beforeForwardToErrorPage(unwrappedException, request, response);
            if (!response.isCommitted()) {
                // The JSP error page needs the response Writer but somebody may already have retrieved
                // the OutputStream and usage of these two can't be mixed. So we reset the response.
                response.reset();
                response.setStatus(status);
                String errorPage = handler.getPage();
                errorPage = (errorPage == null) ? parameters.getDefaultErrorPage() : errorPage;
                RequestDispatcher requestDispatcher = request.getRequestDispatcher(errorPage);
                if (requestDispatcher != null) {
                    requestDispatcher.forward(request, response);
                } else {
                    log.error("Cannot forward to error page, " + "no RequestDispatcher found for errorPage="
                            + errorPage + " handler=" + handler);
                }
                FacesContext fContext = FacesContext.getCurrentInstance();
                if (fContext != null) {
                    fContext.responseComplete();
                } else {
                    log.error("Cannot set response complete: faces context is null");
                }
            } else {
                // do not throw an error, just log it: afterDispatch needs to
                // be called, and sometimes the initial error is a
                // ClientAbortException
                log.error("Cannot forward to error page: " + "response is already committed");
            }
            parameters.getListener().afterDispatch(unwrappedException, request, response);
        } catch (Throwable newError) {
            throw new ServletException(newError);
        }
    }

    protected ErrorHandler getHandler(Throwable t) {
        Throwable throwable = unwrapException(t);
        String className = null;
        if (throwable instanceof WrappedException) {
            WrappedException wrappedException = (WrappedException) throwable;
            className = wrappedException.getClassName();
        } else {
            className = throwable.getClass().getName();
        }
        for (ErrorHandler handler : parameters.getHandlers()) {
            if (handler.getError() != null && className.matches(handler.getError())) {
                return handler;
            }
        }
        throw new ClientRuntimeException("No error handler set.");
    }

    protected Object getUserMessage(String messageKey, Locale locale) {
        return I18NUtils.getMessageString(parameters.getBundleName(), messageKey, null, locale);
    }

    /**
     * @deprecated use {@link ExceptionHelper#unwrapException(Throwable)}
     */
    @Deprecated
    public static Throwable unwrapException(Throwable t) {
        return ExceptionHelper.unwrapException(t);
    }

}
