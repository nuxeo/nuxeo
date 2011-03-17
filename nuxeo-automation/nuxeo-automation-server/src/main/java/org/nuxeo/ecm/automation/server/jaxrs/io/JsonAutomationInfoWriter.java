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

import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.core.doc.JSONExporter;
import org.nuxeo.ecm.automation.server.jaxrs.AutomationInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces( { "application/json+nxautomation", "application/json" })
public class JsonAutomationInfoWriter implements
        MessageBodyWriter<AutomationInfo> {

    public long getSize(AutomationInfo arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return AutomationInfo.class.isAssignableFrom(arg0);
    }

    public void writeTo(AutomationInfo arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {
        JSONObject json = new JSONObject();
        JSONObject paths = new JSONObject();
        paths.element("login", "login");
        json.element("paths", paths);
        // operations
        JSONArray ops = new JSONArray();
        for (OperationDocumentation doc : arg0.getOperations()) {
            JSONObject op = JSONExporter.toJSON(doc);
            ops.add(op);
        }
        json.element("operations", ops);
        // operation chains
        JSONArray chains = new JSONArray();
        for (OperationDocumentation doc : arg0.getChains()) {
            JSONObject op = JSONExporter.toJSON(doc);
            op.element("url", "Chain." + doc.id);
            chains.add(op);
        }
        json.element("chains", chains);
        arg6.write(json.toString(2).getBytes("UTF-8"));
    }
}
