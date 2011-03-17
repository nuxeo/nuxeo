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
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.LoginInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces( { "application/json+nxentity", "application/json" })
public class JsonLoginInfoWriter implements MessageBodyWriter<LoginInfo> {

    public long getSize(LoginInfo arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return LoginInfo.class.isAssignableFrom(arg0);
    }

    public void writeTo(LoginInfo login, Class<?> arg1, Type arg2, Annotation[] arg3,
            MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {
        JSONObject json = new JSONObject();
        json.element("entity-type", "login");
        json.element("username", login.getUsername());
        json.element("isAdministrator", login.isAdministrator());
        JSONArray g = new JSONArray();
        for (String group : login.getGroups()) {
            g.add(group);
        }
        json.element("groups", g);
        arg6.write(json.toString().getBytes("UTF-8"));
    }

}
