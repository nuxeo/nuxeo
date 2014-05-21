/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.connect.client.we;

import org.nuxeo.ecm.admin.NuxeoCtlManager;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Root object: mainly acts as a router.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Path("/connectClient")
@WebObject(type = "connectClientRoot")
public class ConnectClientRoot extends ModuleRoot {

    private boolean isAdmin;

    @Override
    public void initialize(Object... args) {
        this.isAdmin = ((NuxeoPrincipal) getContext().getPrincipal())
                .isAdministrator();
    }

    @Path(value = "packages")
    public Object listPackages() {
        return isAdmin ? ctx.newObject("packageListingProvider") :
                unauthorized();
    }

    @Path(value = "download")
    public Object download() {
        return isAdmin ? ctx.newObject("downloadHandler") : unauthorized();
    }

    @Path(value = "install")
    public Object install() {
        return isAdmin ? ctx.newObject("installHandler") : unauthorized();
    }

    @Path(value = "uninstall")
    public Object uninstall() {
        return isAdmin ? ctx.newObject("uninstallHandler") : unauthorized();
    }

    @Path(value = "remove")
    public Object remove() {
        return isAdmin ? ctx.newObject("removeHandler") : unauthorized();
    }

    @GET
    @Produces("text/html")
    @Path(value = "restartView")
    public Object restartServerView() {
        return isAdmin ? getView("serverRestart").arg("nuxeoctl",
                new NuxeoCtlManager()) : unauthorized();
    }

    protected Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}
