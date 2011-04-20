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

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.doc.JSONExporter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonMarshalling;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationResource extends ExecutableResource {

    protected OperationType type;

    public OperationResource(AutomationService service, OperationType type) {
        super(service);
        this.type = type;
    }

    @GET
    @Produces("application/json")
    public Object doGet() {
        OperationDocumentation doc = type.getDocumentation();
        JSONObject json = JSONExporter.toJSON(doc);
        return Response.ok(json).type("application/json").build();
    }

    @Override
    public Object execute(ExecutionRequest xreq) throws Exception {
        OperationContext ctx = xreq.createContext(request, getCoreSession());
        Object result = service.run(ctx, xreq.createChain(type));
        if (result == null) {
            return null;
        }
        Class<?> resultClass = result.getClass();
        JsonMarshalling jm = marshalling();
        if (!jm.canMarshall(resultClass)) {
            return result;
        }
        JSONObject json = new JSONObject();
        json.element("entity-type", entityType(resultClass));
        jm.write(resultClass, json, result);
        return json;
    }

    protected static String entityType(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    protected static JsonMarshalling marshalling() {
        return Framework.getLocalService(JsonMarshalling.class);
    }
        
    @Override
    public String getId() {
        return type.getId();
    }

    @Override
    public boolean isChain() {
        return false;
    }

}
