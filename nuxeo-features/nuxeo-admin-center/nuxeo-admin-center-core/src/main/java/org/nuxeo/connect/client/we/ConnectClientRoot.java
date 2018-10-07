/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.connect.client.we;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.admin.NuxeoCtlManager;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * Root object: mainly acts as a router.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Path("/connectClient")
@WebObject(type = "connectClientRoot", administrator = Access.GRANT)
public class ConnectClientRoot extends ModuleRoot {

    @Path(value = "packages")
    public Resource listPackages() {
        return ctx.newObject("packageListingProvider");
    }

    @Path(value = "download")
    public Resource download() {
        return ctx.newObject("downloadHandler");
    }

    @Path(value = "install")
    public Resource install() {
        return ctx.newObject("installHandler");
    }

    @Path(value = "uninstall")
    public Resource uninstall() {
        return ctx.newObject("uninstallHandler");
    }

    @Path(value = "remove")
    public Resource remove() {
        return ctx.newObject("removeHandler");
    }

    @GET
    @Produces("text/html")
    @Path(value = "restartView")
    public Object restartServerView() {
        if (getContext().getPrincipal().isAdministrator()) {
            return getView("serverRestart").arg("nuxeoctl", new NuxeoCtlManager());
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "registerInstanceCallback")
    public Object registerInstanceCallback(@QueryParam("ConnectRegistrationToken") String token) {
        return getView("registerInstanceCallback").arg("token", token);
    }
}
