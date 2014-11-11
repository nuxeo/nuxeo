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

package org.nuxeo.ecm.platform.ui.web.shield;

import java.io.IOException;

import javax.faces.event.PhaseId;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.transaction.Transaction;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.WrappedException;

public class NuxeoExceptionFilter implements Filter {

    private static final Log log = LogFactory.getLog(NuxeoExceptionFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    private static void handleException(ServletRequest request,
            ServletResponse response, Throwable t) throws IOException,
            ServletException {

        log.error("Uncaught exception",t);

        rollbackTransactionIfNecessary();

        if (FacesLifecycle.getPhaseId() == PhaseId.RENDER_RESPONSE) {
            if (response.isCommitted()) {
                log.error("Uncaught exception, too late to redirect");
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                } else {
                    throw new ServletException(t);
                }
            }
        }

        Throwable appException = null;
        Boolean securityError = false;
        if (request.getAttribute("securityException") != null) {

            appException = (Throwable) request.getAttribute("securityException");
            securityError = true;
        } else if (request.getAttribute("applicationException") != null) {
            // get Exceptions relayed by ErrorInterceptor
            appException = (Throwable) request.getAttribute("applicationException");
            securityError = ExceptionHelper.isSecurityError(appException);
        } else {
            appException = t;
            securityError = ExceptionHelper.isSecurityError(appException);
        }

        Throwable unwrappedException = unwrapException(appException);
        if (!securityError) {
            securityError = ExceptionHelper.isSecurityError(unwrappedException);
        }
        String userMessage = getMessageForException(unwrappedException);
        String exceptionMessage = unwrappedException.getLocalizedMessage();
        if (exceptionMessage == null) {
            exceptionMessage = unwrappedException.getMessage();
        }
        if (exceptionMessage == null) {
            exceptionMessage = unwrappedException.toString();
        }
        String stackTrace = getStackTrace(appException);

        forwardToErrorPage(request, response, stackTrace, exceptionMessage,
                userMessage, securityError);
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
            if (request.getAttribute("applicationException") != null) {
                log.error(
                        "An exception was swallowed by the component stack : " + ((Exception) request.getAttribute(
                                "applicationException")).getMessage());
            } else if (request.getAttribute("securityException") != null) {
                log.error(
                        "An security exception was swallowed by the component stack : " + ((Exception) request.getAttribute(
                                "securityException")).getMessage());
            }
        } catch (ServletException se) {
            handleException(request, response, se);
        } catch (Exception e) {
            handleException(request, response, e);
        } catch (Throwable t) {
            handleException(request, response, t);
        }
    }

    public static void forwardToErrorPage(ServletRequest request,
            ServletResponse response, String stackTrace,
            String exceptionMessage, String userMessage)
            throws ServletException, IOException {
        forwardToErrorPage(request, response, stackTrace, exceptionMessage,
                userMessage, false);
    }

    public static void forwardToErrorPage(ServletRequest request,
            ServletResponse response, String stackTrace,
            String exceptionMessage, String userMessage, Boolean securityError)
            throws ServletException, IOException {
        request.setAttribute("exception_message", exceptionMessage);
        request.setAttribute("user_message", userMessage);
        request.setAttribute("stackTrace", stackTrace);
        request.setAttribute("securityError", securityError);
        request.getRequestDispatcher("/nuxeo_error.jsp").forward(request,
                response);
    }

    public static String getStackTraceElement(Throwable t) {
        StringBuilder sb = new StringBuilder();

        if (null != t) {
            sb.append("\n\n");
            sb.append(t.getClass().getName());
            sb.append("\n\n");
            sb.append(t.getLocalizedMessage());
            sb.append("\n\n");

            for (StackTraceElement element : t.getStackTrace()) {
                sb.append(element.toString());
                sb.append('\n');
            }
        }

        return sb.toString();
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

    public static String getMessageForException(Throwable t) {
        String message;

        if (t instanceof DocumentSecurityException) {
            DocumentSecurityException e = (DocumentSecurityException) t;
            message = "Error.Insuffisant.Rights";
        }
        else if (t instanceof ClientException) {
            ClientException e = (ClientException) t;
            message = e.getMessage();
        } else if (t instanceof WrappedException) {
            WrappedException e = (WrappedException) t;
            if (e.getClassName() == null) {
                message = e.getLocalizedMessage();
            } else {
                String cName = e.getClassName();
                if (cName.contains("NoSuchDocumentException")) {
                    message = "Error.Document.Not.Found";
                } else if (cName.contains("javax.jcr.ItemNotFoundException")) {
                    message = "Error.Document.Not.Found";
                } else if (cName.contains("NoSuchPropertyException")) {
                    message = "Error.Document.NoSuchProperty";
                } else if (cName.contains("SecurityException")) {
                    message = "Error.Insuffisant.Rights";
                } else {
                    message = cName;
                }
            }
        } else if (t instanceof SecurityException) {
            message = "Error.Insuffisant.Rights";
        } else if (t instanceof NullPointerException) {
            message = "Error.Fatal";
        } else {
            message = "Error.Unknown";
        }
        return message;
    }

    public static String getStackTrace(Throwable e) {
        StringBuilder trace = new StringBuilder();

        if (e != null) {
            Throwable cause;

            if (e instanceof ServletException) {
                cause = ((ServletException) e).getRootCause();
            } else {
                cause = e.getCause();
            }

            trace.append(getStackTraceElement(e));

            if (cause != null) {
                trace.append(getStackTrace(cause));
            }
        }
        return trace.toString();
    }

    private static void rollbackTransactionIfNecessary() {
        if (Contexts.isEventContextActive())
        {
            try {
                if (Transaction.instance().isActiveOrMarkedRollback()) {
                    log.info("killing transaction");
                    Transaction.instance().rollback();
                }
            } catch (Exception te) {
                log.error("could not roll back transaction", te);
            }
        }
    }

}
