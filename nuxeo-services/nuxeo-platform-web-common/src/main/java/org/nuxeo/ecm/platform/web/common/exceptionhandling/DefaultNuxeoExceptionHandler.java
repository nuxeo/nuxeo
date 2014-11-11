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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.WrappedException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandler;

/**
 * @author arussel
 *
 */
public class DefaultNuxeoExceptionHandler implements NuxeoExceptionHandler {

    protected static final Log log = LogFactory.getLog(DefaultNuxeoExceptionHandler.class);
    
    protected NuxeoExceptionHandlerParameters parameters = null;
    
    public DefaultNuxeoExceptionHandler() throws Exception {
    }
    
    public void setParameters(NuxeoExceptionHandlerParameters parameters) {
        this.parameters = parameters;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoExceptionHandler#handleException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Throwable)
     */
    public void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException {
        request.setAttribute(
                NuxeoExceptionFilter.EXCEPTION_FILTER_ATTRIBUTE,
                true);
        ErrorHandler handler = getHandler(t);
        parameters.getListener().startHandling(t, request, response);
        Throwable unwrappedException = unwrapException(t);
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        t.printStackTrace(pwriter);
        String stackTrace = swriter.getBuffer().toString();
        log.error(stackTrace);
        parameters.getLogger().error(stackTrace);
        parameters.getListener().beforeSetErrorPageAttribute(unwrappedException, request,
                response);
        request.setAttribute("exception_message",
                unwrappedException.getLocalizedMessage());
        request.setAttribute("user_message", getUserMessage(
                handler.getMessage(), request.getLocale()));
        request.setAttribute("stackTrace", stackTrace);
        request.setAttribute("securityError",
                ExceptionHelper.isSecurityError(unwrappedException));
        String dumpedRequest = parameters.getRequestDumper().getDump(request);
        parameters.getLogger().error(dumpedRequest);
        request.setAttribute("request_dump", dumpedRequest);
        parameters.getListener().beforeForwardToErrorPage(unwrappedException, request, response);
        Integer error = handler.getCode();
        if (error != null) {
            response.setStatus(error);
        }
        String errorPage = handler.getPage();
        errorPage = (errorPage == null) ? parameters.getDefaultErrorPage() : errorPage;
        request.getRequestDispatcher(errorPage).forward(request, response);
        parameters.getListener().afterDispatch(unwrappedException, request, response);
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
            if (handler.getError() != null
                    && className.matches(handler.getError())) {
                return handler;
            }
        }
        throw new ClientRuntimeException("No error handler set.");
    }

    protected Object getUserMessage(String messageKey, Locale locale) {
        return I18NUtils.getMessageString(parameters.getBundleName(), messageKey, null, locale);
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t instanceof ServletException) {
            cause = ((ServletException) t).getRootCause();
        } else if (t instanceof ClientException) {
            cause = t.getCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }


}
