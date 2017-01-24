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

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// Seems useless
public class ChainResource extends ExecutableResource {

    protected final String chainId;

    public ChainResource(AutomationService service, String chainId) {
        super(service);
        this.chainId = chainId;
    }

    @GET
    public Object doGet() { // TODO
        return null;
    }

    @Override
    public Object execute(ExecutionRequest xreq) throws OperationException {
        OperationContext ctx = xreq.createContext(request, getCoreSession());
        // Copy params in the Chain context
        ctx.putAll(xreq.getParams());
        return service.run(ctx, chainId);
    }

    @Override
    public String getId() {
        return chainId;
    }

    @Override
    public boolean isChain() {
        return true;
    }
}
