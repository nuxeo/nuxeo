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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.connect.client.vindoz.InstallAfterRestart;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.data.SubscriptionStatusType;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides REST binding for {@link Package} listings.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebObject(type = "packageListingProvider")
public class PackageListingProvider extends DefaultObject {

    public String getConnectBaseUrl() {
        return ConnectUrlConfig.getBaseUrl();
    }

    protected List<DownloadablePackage> filterOnPlatform(
            List<DownloadablePackage> pkgs, Boolean filterOnPlatform) {
        if (filterOnPlatform == null || !filterOnPlatform) {
            return pkgs;
        }
        String targetPF = PlatformVersionHelper.getPlatformFilter();
        if (targetPF == null) {
            return pkgs;
        } else {
            List<DownloadablePackage> filteredPackages = new ArrayList<DownloadablePackage>();
            for (DownloadablePackage pkg : pkgs) {
                if (PlatformVersionHelper.isCompatible(pkg.getTargetPlatforms())) {
                    filteredPackages.add(pkg);
                }
            }
            return filteredPackages;
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "list")
    public Object doList(@QueryParam("type") String type,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);

        List<DownloadablePackage> pkgs;
        if (type == null || "".equals(type.trim())) {
            pkgs = pm.listPackages();
        } else {
            pkgs = pm.listPackages(PackageType.getByValue(type));
        }

        pkgs = filterOnPlatform(pkgs, filterOnPlatform);

        return getView("simpleListing").arg("pkgs", pkgs).arg(
                "showCommunityInfo", true).arg("source", "list");
    }

    @GET
    @Produces("text/html")
    @Path(value = "updates")
    public Object getUpdates(@QueryParam("type") String type,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);

        List<DownloadablePackage> pkgs;
        if (type == null || "".equals(type.trim())) {
            pkgs = pm.listUpdatePackages();
        } else {
            pkgs = pm.listUpdatePackages(PackageType.getByValue(type));
        }

        pkgs = filterOnPlatform(pkgs, filterOnPlatform);

        return getView("simpleListing").arg("pkgs", pkgs).arg(
                "showCommunityInfo", true).arg("source", "updates");
    }

    @GET
    @Produces("text/html")
    @Path(value = "local")
    public Object getLocal(@QueryParam("type") String type) {

        PackageManager pm = Framework.getLocalService(PackageManager.class);

        List<DownloadablePackage> pkgs;
        if (type == null || "".equals(type.trim())) {
            pkgs = pm.listLocalPackages();
        } else {
            pkgs = pm.listLocalPackages(PackageType.getByValue(type));
        }

        return getView("simpleListing").arg("pkgs", pkgs).arg(
                "showCommunityInfo", false).arg("source", "local");
    }

    @GET
    @Produces("text/html")
    @Path(value = "remote")
    public Object getRemote(@QueryParam("type") String type,
            @QueryParam("onlyRemote") Boolean onlyRemote,
            @QueryParam("searchString") String searchString,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {

        PackageManager pm = Framework.getLocalService(PackageManager.class);

        boolean useSearch = true;

        if (onlyRemote == null) {
            onlyRemote = false;
        }
        if (searchString == null || "".equals(searchString)) {
            useSearch = false;
        }

        List<DownloadablePackage> pkgs;

        if (useSearch) {
            pkgs = pm.searchPackages(searchString);
        } else {
            if (!onlyRemote) {
                if (type == null || "".equals(type.trim())) {
                    pkgs = pm.listRemoteOrLocalPackages();
                } else {
                    pkgs = pm.listRemoteOrLocalPackages(PackageType.getByValue(type));
                }
            } else {
                if (type == null || "".equals(type.trim())) {
                    pkgs = pm.listOnlyRemotePackages();
                } else {
                    pkgs = pm.listOnlyRemotePackages(PackageType.getByValue(type));
                }
            }
        }
        pkgs = filterOnPlatform(pkgs, filterOnPlatform);

        return getView("simpleListing").arg("pkgs", pkgs).arg(
                "showCommunityInfo", false).arg("source", "remote");
    }

    @GET
    @Produces("text/html")
    @Path(value = "studio")
    public Object getStudio() {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        List<DownloadablePackage> pkgs = pm.listAllStudioRemoteOrLocalPackages();
        List<DownloadablePackage> pkgsWithoutSnapshot = StudioSnapshotHelper.removeSnapshot(pkgs);

        return getView("simpleListing").arg("pkgs", pkgsWithoutSnapshot).arg(
                "showCommunityInfo", false).arg("source", "studio");
    }

    public String getStateLabel(Package pkg) {
        switch (pkg.getState()) {
        case PackageState.REMOTE:
            return "remote";
        case PackageState.DOWNLOADED:
            return "downloaded (but not installed)";
        case PackageState.DOWNLOADING:
            DownloadingPackage dpkg = (DownloadingPackage) pkg;
            return "downloading (" + dpkg.getDownloadProgress() + "%)";
        case PackageState.INSTALLING:
            return "installation in progress";
        case PackageState.INSTALLED:
            return "installed";
        case PackageState.STARTED:
            return "installed and started";
        }
        return "!?!";
    }

    public boolean canInstall(Package pkg) {
        return PackageState.DOWNLOADED == pkg.getState() && !InstallAfterRestart.isMarkedForInstallAfterRestart(pkg.getId());
    }

    public boolean needsRestart(Package pkg) {
        return InstallAfterRestart.isMarkedForInstallAfterRestart(pkg.getId());
    }

    public boolean canUnInstall(Package pkg) {
        return PackageState.INSTALLED == pkg.getState()
                || PackageState.STARTED == pkg.getState();
    }

    public boolean canDownload(Package pkg) {
        return PackageState.REMOTE == pkg.getState()
                && (PackageType.STUDIO == pkg.getType() ||
                   (ConnectStatusHolder.instance().isRegistred() &&
                   ConnectStatusHolder.instance().getStatus().status() == SubscriptionStatusType.OK));
    }

    @GET
    @Produces("text/html")
    @Path(value = "details/{pkgId}")
    public Object getDetails(@PathParam("pkgId") String pkgId) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);

        DownloadablePackage pkg = pm.getPackage(pkgId);

        if (pkg != null) {
            return getView("pkgDetails").arg("pkg", pkg);
        } else {
            return getView("pkgNotFound").arg("pkgId", pkgId);
        }
    }

}
