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

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides REST bindings for {@link Package} install management.
 * 
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebObject(type = "uninstallHandler")
public class UninstallHandler extends DefaultObject {

    protected static final Log log = LogFactory.getLog(UninstallHandler.class);

    @GET
    @Produces("text/html")
    @Path(value = "start/{pkgId}")
    public Object startInstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source) {

        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
            LocalPackage pkg = pus.getPackage(pkgId);

            Task uninstallTask = pkg.getUninstallTask();

            ValidationStatus status = uninstallTask.validate();

            if (status.hasErrors()) {
                return getView("canNotUninstall").arg("status", status).arg(
                        "pkg", pkg).arg("source", source);
            }

            return getView("startUninstall").arg("status", status).arg(
                    "uninstallTask", uninstallTask).arg("pkg", pkg).arg(
                    "source", source);
        } catch (Exception e) {
            log.error("Error during first step of installation", e);
            return getView("uninstallError").arg("e", e);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "run/{pkgId}")
    public Object doUninstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source) {

        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);

        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            Task uninstallTask = pkg.getUninstallTask();
            try {
                uninstallTask.run(new HashMap<String, String>());
            } catch (Throwable e) {
                log.error("Error during uninstall of " + pkgId, e);
                uninstallTask.rollback();
                return getView("uninstallError").arg("e", e).arg("source",
                        source);
            }
            return getView("uninstallDone").arg("uninstallTask", uninstallTask).arg(
                    "pkg", pkg).arg("source", source);

        } catch (Exception e) {
            log.error("Error during uninstall of " + pkgId, e);
            return getView("uninstallError").arg("e", e).arg("source", source);
        }

    }

    @POST
    @Path("restart")
    public Object restartServer() {
        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
        try {
            pus.restart();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
        // TODO create a page that waits for the server to restart
        return Response.ok().build();
    }

}
