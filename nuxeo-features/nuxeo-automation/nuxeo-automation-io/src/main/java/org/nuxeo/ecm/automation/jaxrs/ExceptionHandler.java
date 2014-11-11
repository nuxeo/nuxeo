/*
 * Copyright (c) 2006-20113Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.jaxrs;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.ConflictOperationException;
import org.nuxeo.ecm.automation.InvalidOperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExceptionHandler {

    protected static final Log log = LogFactory.getLog(ExceptionHandler.class);

    protected int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    protected String type;

    protected Throwable cause;

    protected String message;

    public static WebApplicationException newException(Throwable cause) {
        return newException(null, cause);
    }

    public static WebApplicationException newException(String message,
            Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException(
                    "the cause parameter cannot be null");
        }
        ExceptionHandler e = new ExceptionHandler(message, cause);
        return new WebApplicationException(cause,
                Response.status(e.getStatus()).entity(e).build());
    }

    public static void abort(String message, Throwable cause)
            throws WebApplicationException {
        throw newException(message, cause);
    }

    public static void abort(Throwable cause) throws WebApplicationException {
        throw newException(null, cause);
    }

    /**
     * Tries to find the best matching HTTP status for the given exception.
     */
    public static int getStatus(Throwable cause) {
        // use a max depth of 8 to avoid infinite loops for broken exceptions
        // which are referencing themselves as the cause
        return getStatus(cause, 8);
    }

    public static boolean isSecurityError(Throwable t) {
        return getStatus(t) == HttpServletResponse.SC_FORBIDDEN;
    }

    public static int getStatus(Throwable cause, int depth) {
        if (depth == 0) {
            log.warn("Possible infinite loop! Check the exception wrapping.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        if ((cause instanceof DocumentSecurityException)
                || (cause instanceof SecurityException)
                || "javax.ejb.EJBAccessException".equals(cause.getClass().getName())) {
            return HttpServletResponse.SC_FORBIDDEN;
        } else if (cause instanceof NoSuchDocumentException) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else if (cause instanceof OperationNotFoundException) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else if (cause instanceof ConflictOperationException) {
            return HttpServletResponse.SC_CONFLICT;
        } else if (cause instanceof InvalidOperationException) {
            return HttpServletResponse.SC_BAD_REQUEST;
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

    public ExceptionHandler(String message, Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException(
                    "the cause parameter cannot be null");
        }
        this.status = getStatus(cause);
        this.cause = cause;
        this.message = message == null ? cause.getMessage() : message;
        type = cause.getClass().getName();
    }

    public int getStatus() {
        return status;
    }

    public String getType() {
        return cause.getClass().getName();
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getSerializedStackTrace() {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        cause.printStackTrace(pw);
        pw.flush();
        return writer.toString();
    }

    public Throwable getCause() {
        return cause;
    }

}
