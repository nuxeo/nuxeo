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

package org.nuxeo.ecm.webengine;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebDocumentException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;

public class WebException extends WebApplicationException {

    private static final long serialVersionUID = 176876876786L;
    protected String message;
    protected boolean byPassAppResponse = false;

    public WebException() {
    }

    public WebException(Response response) {
        super(response);
    }

    public WebException(int status) {
        super(status);
    }

    public WebException(Response.Status status) {
        super(status);
    }

    public WebException(Throwable cause, Response response) {
        super(cause, response);
        byPassAppResponse = true;
    }

    public WebException(Throwable cause, Response.Status status) {
        super(cause, status);
    }

    public WebException(Throwable cause, int status) {
        super(cause, status);
    }

    public WebException(Throwable cause) {
        super(cause);
    }

    public WebException(String message) {
        this.message = message;
    }

    public WebException(String message, int code) {
        super(code);
        this.message = message;
    }

    public WebException(String message, Throwable t) {
        super(t);
        this.message = message;
    }

    public WebException(String message, Throwable t, int code) {
        super(t, code);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * for compatibiliy only
     * @return
     */
    @Deprecated
    public int getReturnCode() {
        return super.getResponse().getStatus();
    }

    @Override
    public Response getResponse() {
        Response response = super.getResponse();
        if (!byPassAppResponse) {
            WebContext ctx = WebEngine.getActiveContext();
            if (ctx != null) {
                ModuleResource rs = ctx.getModuleInstance();
                if (rs == null) {
                    // no context print error on screen
                    return toResponse(this);
                } else {
                    Object result = rs.handleError(this);
                    if (result instanceof Response) {
                        response  = (Response)result;
                    } else if (result != null) {
                        response = Response.fromResponse(response).entity(result).build();
                    }
                }
            }
        }
        return response;
    }

    public String getStackTraceString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static Response toResponse(Throwable t) {
        return Response.status(500).entity(toString(t)).build();
    }

    public static WebException wrap(Throwable e) {
        return wrap(null, e);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static WebException wrap(String message, Throwable e) {
        //TODO add EJBAccessException dependency
        if (e instanceof DocumentSecurityException
                || "javax.ejb.EJBAccessException".equals(e.getClass().getName())) {
            return new WebSecurityException(message, e);
        } else if (e instanceof WebException) {
            return (WebException)e;
        } else if (e instanceof ClientException) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null) {
                if (cause.getMessage().contains("org.nuxeo.ecm.core.model.NoSuchDocumentException")) {
                    return new WebResourceNotFoundException(cause.getMessage(), e);
                }
            }
            return new WebDocumentException(message, (ClientException)e);
        } else {
            return new WebException(message, e);
        }
    }

}
