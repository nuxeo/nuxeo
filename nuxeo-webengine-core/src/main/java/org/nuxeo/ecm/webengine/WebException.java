/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.webengine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebDocumentException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;

public class WebException extends WebApplicationException {

    protected static final Log log = LogFactory.getLog(WebException.class);

    private static final long serialVersionUID = 176876876786L;

    protected String type;

    protected Throwable cause;

    protected String message;

    protected int status;

    protected boolean byPassAppResponse = false;

    public WebException() {
    }

    public WebException(Response response) {
        super(response);
    }

    public WebException(int status) {
        super(status);
        this.status = status;
    }

    public WebException(Response.Status status) {
        super(status);
        this.status = status.getStatusCode();
    }

    protected WebException(Throwable cause, Response response) {
        super(cause, response);
        this.cause = cause;
        this.status = response.getStatus();
        byPassAppResponse = true;
    }

    /**
     * Use WebException.wrap() and not the constructor.
     */
    protected WebException(Throwable cause, Response.Status status) {
        super(cause, status);
        this.cause = cause;
        this.status = status.getStatusCode();
    }

    protected WebException(Throwable cause, int status) {
        super(cause, status);
        this.cause = cause;
        this.status = status;
    }

    protected WebException(Throwable cause) {
        super(cause);
        this.cause = cause;
    }

    public WebException(String message) {
        this.message = message;
    }

    public WebException(String message, int code) {
        super(code);
        this.message = message;
        this.status = code;
    }

    public WebException(String message, Throwable cause, int status) {
        if (cause == null) {
            throw new IllegalArgumentException(
                    "the cause parameter cannot be null");
        }
        this.status = status == -1 ? getStatus(cause) : status;
        this.cause = cause;
        this.message = message == null ? cause.getMessage() : message;
        this.type = cause.getClass().getName();
    }

    public WebException(String message, Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException(
                    "the cause parameter cannot be null");
        }
        this.status = getStatus(cause);
        this.cause = cause;
        this.message = message == null ? cause.getMessage() : message;
        this.type = cause.getClass().getName();
    }

    public static WebException newException(String message, Throwable cause) {
        return newException(message, cause, -1);
    }

    public static WebException newException(Throwable cause) {
        return newException(null, cause);
    }

    public static WebException newException(String message,
            Throwable cause, int status) {
        if (cause == null) {
            throw new IllegalArgumentException(
                    "the cause parameter cannot be null");
        }
        return new WebException(message, cause, status);
    }

    public static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static WebException wrap(Throwable e) {
        return wrap(null, e);
    }

    public static WebException wrap(String message, Throwable exception) {
        if (exception instanceof DocumentSecurityException
                || "javax.ejb.EJBAccessException".equals(exception.getClass()
                .getName())) {
            return new WebSecurityException(message, exception);
        } else if (exception instanceof WebException) {
            return (WebException) exception;
        } else if (exception instanceof ClientException) {
            Throwable cause = exception.getCause();
            boolean notFound = false;
            if (cause instanceof NoSuchDocumentException) {
                notFound = true;
            } else if (cause != null && cause.getMessage() != null) {
                // not sure if this is still needed (?) see NXP-9636
                if (cause.getMessage().contains(
                        "org.nuxeo.ecm.core.model.NoSuchDocumentException")) {
                    notFound = true;
                }
            }
            if (notFound) {
                return new WebResourceNotFoundException(message,
                        cause);
            } else {
                return new WebDocumentException(message, exception);
            }
        } else {
            return new WebException(message, exception);
        }
    }

    public static Object handleError(WebApplicationException e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return Response.status(500).entity(sw.toString()).build();
    }

    /**
     * Tries to find the best matching HTTP status for the given exception.
     */
    public static int getStatus(Throwable cause) {
        // use a max depth of 8 to avoid infinite loops for broken exceptions
        // which are referencing themselves as the cause
        return getStatus(cause, 8);
    }

    public static int getStatus(Throwable cause, int depth) {
        if (depth == 0) {
            log.warn("Possible infinite loop! Check the exception wrapping.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        if ((cause instanceof DocumentSecurityException)
                || (cause instanceof SecurityException)
                || "javax.ejb.EJBAccessException".equals(cause.getClass()
                .getName())) {
            return HttpServletResponse.SC_FORBIDDEN;
        } else if (cause instanceof NoSuchDocumentException || cause
                instanceof WebResourceNotFoundException) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else if (cause instanceof InvalidOperationException) {
            return HttpServletResponse.SC_BAD_REQUEST;
        } else if (cause instanceof WebSecurityException) {
            return HttpServletResponse.SC_UNAUTHORIZED;
        }
        Throwable parent = cause.getCause();
        if (parent == cause) {
            log.warn("Infinite loop detected! Check the exception wrapping.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        if (parent != null) {
            return getStatus(parent, depth - 1);
        }
        if (cause.getMessage() != null
                && cause.getMessage().contains(
                "org.nuxeo.ecm.core.model.NoSuchDocumentException")) {
            log.warn("Badly wrapped exception: found a NoSuchDocumentException"
                    + " message but no NoSuchDocumentException", cause);
            return HttpServletResponse.SC_NOT_FOUND;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    public static boolean isSecurityError(Throwable t) {
        return getStatus(t) == HttpServletResponse.SC_FORBIDDEN;
    }

    /**
     * For compatibility only.
     */
    @Deprecated
    public static Response toResponse(Throwable t) {
        return Response.status(500).entity(toString(t)).build();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return status;
    }

    public String getStackTraceString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * For compatibility only.
     */
    @Deprecated
    public int getReturnCode() {
        return super.getResponse().getStatus();
    }

    /**
     * Handle if needed custom error webengine module handler.
     */
    public Response toResponse() {
        Response response = super.getResponse();
        if (!byPassAppResponse) {
            WebContext ctx = WebEngine.getActiveContext();
            if (ctx != null) {
                if (ctx.head() instanceof ModuleResource) {
                    ModuleResource mr = (ModuleResource) ctx.head();
                    Object result = mr.handleError((WebApplicationException)
                            this.getCause());
                    if (result instanceof Response) {
                        response = (Response) result;
                    } else if (result != null) {
                        response = Response.fromResponse(response).status
                                (status).entity(result).build();
                    }
                    return response;
                }
            }
        }
        return Response.status(status).entity(this).build();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public Throwable getCause() {
        return cause;
    }

    public String getRequestId() {
        return "";
    }

    public String getHelpUrl() {
        return "";
    }

}
