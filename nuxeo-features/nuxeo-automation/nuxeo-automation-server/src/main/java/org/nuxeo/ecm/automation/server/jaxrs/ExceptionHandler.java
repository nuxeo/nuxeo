/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExceptionHandler {

    protected int status = 500;

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

    public static int getStatus(Throwable cause, int depth) {
        if (depth == 0) {
            return 500;
        }
        if (cause instanceof DocumentSecurityException
                || "javax.ejb.EJBAccessException".equals(cause.getClass().getName())) {
            return 401;
        } else if (cause instanceof ClientException) {
            Throwable ccause = cause.getCause();
            if (ccause != null && ccause.getMessage() != null) {
                if (ccause.getMessage().contains(
                        "org.nuxeo.ecm.core.model.NoSuchDocumentException")) {
                    return 404;
                }
            }
        } else if (cause instanceof OperationNotFoundException) {
            return 404;
        }
        Throwable parent = cause.getCause();
        if (parent != null) {
            return getStatus(parent, depth - 1);
        }
        return 500;
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

}
