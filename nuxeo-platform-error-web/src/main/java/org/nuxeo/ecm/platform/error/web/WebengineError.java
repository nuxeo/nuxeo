/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@Path("/error")
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

    @Path("nuxeoException")
    public Object getNuxeoException() {
        throw new NuxeoException("Nuxeo exception");
    }

    @Path("checkedError")
    public Object getCheckedError() throws Exception {
        throw new Exception("CheckedError in webengine");
    }

    @Path("uncheckedError")
    public Object getUncheckedError() {
        throw new NullPointerException("UncheckedError in webengine");
    }

    @Path("securityError")
    public Object getSecurityError() throws DocumentSecurityException {
        throw new DocumentSecurityException("Security error in webengine");
    }

    public Object handleError(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("<html>");
        pw.println("<head><title>WebEbgine Error Test</title></head>");
        pw.println("<body>");
        pw.println("WEBENGINE HANDLED ERROR: ");
        t.printStackTrace(pw);
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
        return Response.status(500).type(MediaType.TEXT_HTML_TYPE).entity(sw.toString()).build();
    }

}
