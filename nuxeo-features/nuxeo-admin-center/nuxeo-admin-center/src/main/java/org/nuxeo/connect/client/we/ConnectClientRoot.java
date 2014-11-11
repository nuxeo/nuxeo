/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.connect.client.we;

import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * Root object: mainly acts as a router.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebObject(type = "connectClientRoot")
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

}
