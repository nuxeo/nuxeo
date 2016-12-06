/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "operation")
public class OperationResource extends ExecutableResource {

    protected OperationType type;

    @Override
    protected void initialize(Object... args) {
        type = (OperationType) args[0];
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
    public Object execute(ExecutionRequest xreq) throws OperationException {
        return service.run(createContext(xreq), type.getId(), xreq.getParams());
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
        return type instanceof ChainTypeImpl;
    }

}
