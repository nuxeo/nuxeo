/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.connect.client.we;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides REST bindings for {@link Package} removal.
 *
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
@WebObject(type = "removeHandler")
public class RemoveHandler extends DefaultObject {

    protected static final Log log = LogFactory.getLog(RemoveHandler.class);

    @GET
    @Produces("text/html")
    @Path(value = "start/{pkgId}")
    public Object startInstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source) {
        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
            pus.removePackage(pkgId);
            return getView("removeDone").arg("pkgId", pkgId).arg("source",
                    source);
        } catch (Exception e) {
            log.error("Error during first step of installation", e);
            return getView("removeError").arg("e", e);
        }
    }

}
