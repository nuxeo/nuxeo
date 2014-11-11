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
package org.nuxeo.ecm.automation.server.jaxrs;

import javax.ws.rs.GET;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ChainResource extends ExecutableResource {

    protected String chainId;

    public ChainResource(AutomationService service, String chainId) {
        super(service);
        this.chainId = chainId;
    }

    @GET
    public Object doGet() { // TODO
        return null;
    }

    public Object execute(ExecutionRequest xreq) throws Exception {
        OperationContext ctx = xreq.createContext(request, getCoreSession());
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
