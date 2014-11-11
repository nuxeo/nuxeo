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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;

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
    public Object doGet() throws OperationException {
        return type.getDocumentation();
    }

    @GET
    @Path("yaml")
    @Produces("application/yaml")
    public Object doGetYaml() throws OperationException {
        return type.getDocumentation();
    }

    @Override
    public Object execute(ExecutionRequest xreq) throws Exception {
        OperationContext ctx = xreq.createContext(request, getCoreSession());
        return service.run(ctx, type.getId(), xreq.getParams());
    }

    protected static String entityType(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
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
