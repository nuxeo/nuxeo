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
package org.nuxeo.ecm.webengine;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.runtime.api.Framework;

/**
 * The web entry point of WebEngine. This is the root JAX-RS resource that is
 * dispatching requests to WebEngine modules.
 *
 *
 * TODO :
 *  1. add support for multiple segment paths
 *  2. application must be able to override this class.
 *  May be the best approach would be to create a stateful resource but this depends on the JAX-RS backend
 *  and may not be supported.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Path("/")
public class Main {

    @GET
    public Object doGet() {
        return dispatch("/");
    }

    @Path("{modulePath}")
    public Object dispatch(@PathParam("modulePath") String path) {
        try {
            WebEngine engine = Framework.getService(WebEngine.class);
            ModuleConfiguration md = engine.getModuleManager().getModuleByPath(path);
            if (md != null) {
                return md.get().getRootObject();
            } else {
                throw new WebResourceNotFoundException("No resource found");
            }
        } catch (Exception e) {
            throw WebException.wrap("Failed to dispatch: "+path, e);
        }
    }

}
