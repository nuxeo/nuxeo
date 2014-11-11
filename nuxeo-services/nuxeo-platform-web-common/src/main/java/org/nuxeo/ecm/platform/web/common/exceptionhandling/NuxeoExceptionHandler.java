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
import java.util.List;
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
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingListener;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.RequestDumper;

/**
 * @author arussel
 *
 */
public class NuxeoExceptionHandler {

    private static final Log log = LogFactory.getLog(NuxeoExceptionHandler.class);

    private String bundleName;

    private String defaultErrorPage;

    private RequestDumper requestDumper;

    private Log errorLog;

    private List<ErrorHandler> handlers;

    private ExceptionHandlingListener listener;

    public NuxeoExceptionHandler(String bundleName,
            RequestDumper requestDumper, List<ErrorHandler> handlers,
            ExceptionHandlingListener listener, String loggerName) {
        this.bundleName = bundleName;
        this.requestDumper = requestDumper;
        this.handlers = handlers;
        this.listener = listener;
        this.errorLog = LogFactory.getLog(loggerName);
    }

    public NuxeoExceptionHandler() {
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public RequestDumper getRequestDumper() {
        return requestDumper;
    }

    public void setRequestDumper(RequestDumper requestDumper) {
        this.requestDumper = requestDumper;
    }

    public ExceptionHandlingListener getListener() {
        return listener;
    }

    public void setListener(ExceptionHandlingListener listener) {
        this.listener = listener;
    }

    public void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException {
        request.setAttribute(
                NuxeoExceptionFilter.EXCEPTION_FILTER_ATTRIBUTE,
                true);
        ErrorHandler handler = getHandler(t);
        listener.startHandling(t, request, response);
        Throwable unwrappedException = unwrapException(t);
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        t.printStackTrace(pwriter);
        String stackTrace = swriter.getBuffer().toString();
        log.error(stackTrace);
        errorLog.error(stackTrace);
        listener.beforeSetErrorPageAttribute(unwrappedException, request,
                response);
        request.setAttribute("exception_message",
                unwrappedException.getLocalizedMessage());
        request.setAttribute("user_message", getUserMessage(
                handler.getMessage(), request.getLocale()));
        request.setAttribute("stackTrace", stackTrace);
        request.setAttribute("securityError",
                ExceptionHelper.isSecurityError(unwrappedException));
        String dumpedRequest = requestDumper.getDump(request);
        errorLog.error(dumpedRequest);
        request.setAttribute("request_dump", dumpedRequest);
        listener.beforeForwardToErrorPage(unwrappedException, request, response);
        Integer error = handler.getCode();
        if (error != null) {
            response.setStatus(error);
        }
        String errorPage = handler.getPage();
        errorPage = (errorPage == null) ? defaultErrorPage : errorPage;
        request.getRequestDispatcher(errorPage).forward(request, response);
        listener.afterDispatch(unwrappedException, request, response);
    }

    private ErrorHandler getHandler(Throwable t) {
        Throwable throwable = unwrapException(t);
        String className = null;
        if (throwable instanceof WrappedException) {
            WrappedException wrappedException = (WrappedException) throwable;
            className = wrappedException.getClassName();
        } else {
            className = throwable.getClass().getName();
        }
        for (ErrorHandler handler : handlers) {
            if (handler.getError() != null
                    && className.matches(handler.getError())) {
                return handler;
            }
        }
        throw new ClientRuntimeException("No error handler set.");
    }

    private Object getUserMessage(String messageKey, Locale locale) {
        return I18NUtils.getMessageString(bundleName, messageKey, null, locale);
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

    public Log getLogger() {
        return errorLog;
    }

    public void setLoggerName(String loggerName) {
        errorLog = LogFactory.getLog(loggerName);
    }

    public List<ErrorHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<ErrorHandler> handlers) {
        this.handlers = handlers;
    }

    public String getDefaultErrorPage() {
        return defaultErrorPage;
    }

    public void setDefaultErrorPage(String defaultErrorPage) {
        this.defaultErrorPage = defaultErrorPage;
    }

}
