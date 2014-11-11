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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.client.vindoz.InstallAfterRestart;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
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
    public Object startUninstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
            LocalPackage pkg = pus.getPackage(pkgId);
            Task uninstallTask = pkg.getUninstallTask();
            ValidationStatus status = uninstallTask.validate();
            if (status.hasErrors()) {
                return getView("canNotUninstall").arg("status", status).arg(
                        "pkg", pkg).arg("source", source);
            }
            PackageManager pm = Framework.getLocalService(PackageManager.class);
            List<DownloadablePackage> pkgToRemove = pm.getUninstallDependencies(
                    pkg, getTargetPlatform(filterOnPlatform));
            if (pkgToRemove.size() > 0) {
                return getView("displayDependencies").arg("pkg", pkg).arg(
                        "pkgToRemove", pkgToRemove).arg("source", source);
            }
            return getView("startUninstall").arg("status", status).arg(
                    "uninstallTask", uninstallTask).arg("pkg", pkg).arg(
                    "source", source);
        } catch (Exception e) {
            log.error("Error during first step of installation", e);
            return getView("uninstallError").arg("e", e);
        }
    }

    /**
     * @param filterOnPlatform
     * @return
     */
    private String getTargetPlatform(Boolean filterOnPlatform) {
        if (filterOnPlatform != Boolean.TRUE) {
            return null;
        }
        return PlatformVersionHelper.getPlatformFilter();
    }

    @GET
    @Produces("text/html")
    @Path(value = "run/{pkgId}")
    public Object doUninstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            PackageManager pm = Framework.getLocalService(PackageManager.class);
            List<DownloadablePackage> pkgToRemove = pm.getUninstallDependencies(
                    pkg, getTargetPlatform(filterOnPlatform));
            boolean restartRequired = InstallAfterRestart.isNeededForPackage(pkg);
            if (!restartRequired) {
                for (DownloadablePackage rpkg : pkgToRemove) {
                    if (InstallAfterRestart.isNeededForPackage(rpkg)) {
                        restartRequired = true;
                        break;
                    }
                }
            }
            if (restartRequired) {
                InstallAfterRestart.addPackageForUnInstallation(pkg.getName());
                return getView("uninstallOnRestart").arg("pkg", pkg).arg(
                        "source", source);
            } else {
                log.debug("Uninstalling: " + pkgToRemove);
                Task uninstallTask;
                for (DownloadablePackage rpkg : pkgToRemove) {
                    LocalPackage localPackage = pus.getPackage(rpkg.getId());
                    performUninstall(localPackage);
                }
                uninstallTask = performUninstall(pkg);
                return getView("uninstallDone").arg("uninstallTask",
                        uninstallTask).arg("pkg", pkg).arg("source", source);
            }
        } catch (Exception e) {
            log.error("Error during uninstall of " + pkgId, e);
            return getView("uninstallError").arg("e", e).arg("source", source);
        }
    }

    /**
     * Run UninstallTask of given local package
     *
     * @since 5.6
     * @param localPackage Package to uninstall
     * @return {@link UninstallTask} of {@code localPackage}
     * @throws PackageException If uninstall fails. A rollback is done before
     *             the exception is raised.
     */
    protected Task performUninstall(LocalPackage localPackage)
            throws PackageException {
        log.info("Uninstalling " + localPackage.getId());
        Task uninstallTask = localPackage.getUninstallTask();
        try {
            uninstallTask.run(null);
        } catch (PackageException e) {
            log.error("Error during uninstall of " + localPackage.getId(), e);
            uninstallTask.rollback();
            throw e;
        }
        return uninstallTask;
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
