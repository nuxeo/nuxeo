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
package org.nuxeo.ecm.webengine.jaxrs.session;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreExceptionMapper implements ExceptionMapper<Throwable> {

    protected static final Log log = LogFactory.getLog(CoreExceptionMapper.class);

    public Response toResponse(Throwable t) {
        log.error("Exception in JAX-RS processing", t);
        if (t instanceof WebApplicationException) {
            return ((WebApplicationException)t).getResponse();
        } else if (t instanceof DocumentSecurityException
                || "javax.ejb.EJBAccessException".equals(t.getClass().getName())) {
            return getResponse(t, 401);
        } else if (t instanceof ClientException) {
            Throwable cause = t.getCause();
            if (cause != null && cause.getMessage() != null) {
                if (cause.getMessage().contains("org.nuxeo.ecm.core.model.NoSuchDocumentException")) {
                    return getResponse(cause, 401);
                }
            }
        }
        return getResponse(t, 500);
    }

    public static Response getResponse(Throwable t, int status) {
        String message = status == 500 ? getStackTrace(t) : null;
        return Response.status(status).entity(message).build();
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }



}
