/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.error.web;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "error")
@Produces("text/html; charset=UTF-8")
public class WebengineError extends ModuleRoot {

    /**
     * Default view
     */
    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("webException")
    public Object getWebException() {
        throw new WebException("Web exception");
    }

    @Path("checkedError")
    public Object getCheckedError() throws ClientException {
        throw new ClientException("CheckedError in webengine");
    }

    @Path("uncheckedError")
    public Object getUncheckedError() {
        throw new NullPointerException("UncheckedError in webengine");
    }

    @Path("securityError")
    public Object getSecurityError() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error in webengine");
    }

    public Object handleError(WebApplicationException e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("<html>");
        pw.println("<head><title>WebEbgine Error Test</title></head>");
        pw.println("<body>");
        e.printStackTrace(pw);
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
        return Response.status(500).entity(
                "WEBENGINE HANDLED ERROR: \n" + sw.toString()).build();
    }

}