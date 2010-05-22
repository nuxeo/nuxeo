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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Provider
@Produces({"application/json+nxentity", "application/json"})
public class JsonDocumentWriter implements MessageBodyWriter<DocumentModel> {

    @Context
    protected HttpHeaders headers;

    public long getSize(DocumentModel arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return DocumentModel.class.isAssignableFrom(arg0);
    }

    public void writeTo(DocumentModel doc, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {
        try {
            List<String> props = headers.getRequestHeader("X-NXRequireProperties");
            System.out.println(props); //TODO fetch all properties specified here
            arg6.write(getJSON(doc).toString(2).getBytes("UTF-8"));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public static JSONObject getCompleteJSON(DocumentModel doc) throws Exception {
        return getJSON(doc);
    }

    public static JSONObject getJSON(DocumentModel doc) throws Exception {
        JSONObject json = new JSONObject();
        json.element("entity-type", "document");
        json.element("uid", doc.getId());
        json.element("path", doc.getPathAsString());
        json.element("type", doc.getType());
        json.element("state", doc.getCurrentLifeCycleState());
        json.element("lock", doc.getLock());
        json.element("title", doc.getTitle());
        Calendar cal = (Calendar)doc.getPart("dublincore").getValue("modified");
        if (cal != null) {
            json.element("lastModified", DateParser.formatW3CDateTime(cal.getTime()));
        }

//        Set<String> facets = doc.getDeclaredFacets();
//        JSONArray ar = new JSONArray();
//        String val = "";
//        for () {
//
//        }
//        json.element("facets", )
        return json;
    }
}
