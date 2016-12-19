/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.client.we;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.connect.client.ui.SharedPackageListingsSettings;
import org.nuxeo.connect.client.vindoz.InstallAfterRestart;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.data.SubscriptionStatusType;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageVisibility;
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

    @GET
    @Produces("text/html")
    @Path(value = "list")
    public Object doList(@QueryParam("type") String pkgType, @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        String targetPlatform = getTargetPlatform(filterOnPlatform);
        List<DownloadablePackage> pkgs;
        if (StringUtils.isBlank(pkgType)) {
            pkgs = pm.listPackages(targetPlatform);
        } else {
            pkgs = pm.listPackages(PackageType.getByValue(pkgType), targetPlatform);
        }
        return getView("simpleListing").arg("pkgs", pm.sort(pkgs))
                                       .arg("showCommunityInfo", true)
                                       .arg("source", "list")
                                       .arg("filterOnPlatform", filterOnPlatform);
    }

    @GET
    @Produces("text/html")
    @Path(value = "updates")
    public Object getUpdates(@QueryParam("type") String pkgType,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        if (pkgType == null) {
            pkgType = SharedPackageListingsSettings.instance().get("updates").getPackageTypeFilter();
        }
        if (filterOnPlatform == null) {
            filterOnPlatform = SharedPackageListingsSettings.instance().get("updates").getPlatformFilter();
        }
        String targetPlatform = getTargetPlatform(filterOnPlatform);
        List<DownloadablePackage> pkgs;
        if (StringUtils.isBlank(pkgType)) {
            pkgs = pm.listUpdatePackages(null, targetPlatform);
        } else {
            pkgs = pm.listUpdatePackages(PackageType.getByValue(pkgType), targetPlatform);
        }
        return getView("simpleListing").arg("pkgs", pm.sort(pkgs))
                                       .arg("showCommunityInfo", true)
                                       .arg("source", "updates")
                                       .arg("filterOnPlatform", filterOnPlatform);
    }

    @GET
    @Produces("text/html")
    @Path(value = "private")
    public Object getPrivate(@QueryParam("type") String pkgType,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        if (pkgType == null) {
            pkgType = SharedPackageListingsSettings.instance().get("private").getPackageTypeFilter();
        }
        if (filterOnPlatform == null) {
            filterOnPlatform = SharedPackageListingsSettings.instance().get("private").getPlatformFilter();
        }
        String targetPlatform = getTargetPlatform(filterOnPlatform);
        List<DownloadablePackage> pkgs;
        if (StringUtils.isBlank(pkgType)) {
            pkgs = pm.listPrivatePackages(targetPlatform);
        } else {
            pkgs = pm.listPrivatePackages(PackageType.getByValue(pkgType), targetPlatform);
        }
        return getView("simpleListing").arg("pkgs", pm.sort(pkgs))
                                       .arg("showCommunityInfo", true)
                                       .arg("source", "private")
                                       .arg("filterOnPlatform", filterOnPlatform);
    }

    @GET
    @Produces("text/html")
    @Path(value = "local")
    public Object getLocal(@QueryParam("type") String pkgType) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        if (pkgType == null) {
            pkgType = SharedPackageListingsSettings.instance().get("local").getPackageTypeFilter();
        }
        List<DownloadablePackage> pkgs;
        if (StringUtils.isBlank(pkgType)) {
            pkgs = pm.listLocalPackages();
        } else {
            pkgs = pm.listLocalPackages(PackageType.getByValue(pkgType));
        }
        return getView("simpleListing").arg("pkgs", pm.sort(pkgs)).arg("showCommunityInfo", false).arg("source",
                "local");
    }

    @GET
    @Produces("text/html")
    @Path(value = "remote")
    public Object getRemote(@QueryParam("type") String pkgType, @QueryParam("onlyRemote") Boolean onlyRemote,
            @QueryParam("searchString") String searchString, @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        if (pkgType == null) {
            pkgType = SharedPackageListingsSettings.instance().get("remote").getPackageTypeFilter();
        }
        if (filterOnPlatform == null) {
            filterOnPlatform = SharedPackageListingsSettings.instance().get("remote").getPlatformFilter();
        }
        if (onlyRemote == null) {
            onlyRemote = SharedPackageListingsSettings.instance().get("remote").isOnlyRemote();
        }
        List<DownloadablePackage> pkgs;
        String targetPlatform = getTargetPlatform(filterOnPlatform);
        if (!StringUtils.isEmpty(searchString)) { // SEARCH IS NOT IMPLEMENTED
            pkgs = pm.searchPackages(searchString);
        } else if (onlyRemote) {
            if (StringUtils.isBlank(pkgType)) {
                pkgs = pm.listOnlyRemotePackages(targetPlatform);
            } else {
                pkgs = pm.listOnlyRemotePackages(PackageType.getByValue(pkgType), targetPlatform);
            }
        } else {
            if (StringUtils.isBlank(pkgType)) {
                pkgs = pm.listRemoteOrLocalPackages(targetPlatform);
            } else {
                pkgs = pm.listRemoteOrLocalPackages(PackageType.getByValue(pkgType), targetPlatform);
            }
        }
        return getView("simpleListing").arg("pkgs", pm.sort(pkgs))
                                       .arg("showCommunityInfo", false)
                                       .arg("source", "remote")
                                       .arg("filterOnPlatform", filterOnPlatform.toString())
                                       .arg("type", pkgType.toString())
                                       .arg("onlyRemote", onlyRemote.toString());
    }

    /**
     * @return target platform if {@code filterOnPlatform==true} else null
     * @since 5.6
     */
    private String getTargetPlatform(Boolean filterOnPlatform) {
        if (filterOnPlatform != Boolean.TRUE) {
            return null;
        }
        return PlatformVersionHelper.getPlatformFilter();
    }

    @GET
    @Produces("text/html")
    @Path(value = "studio")
    public Object getStudio() {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        List<DownloadablePackage> pkgs = pm.listAllStudioRemoteOrLocalPackages();
        List<DownloadablePackage> pkgsWithoutSnapshot = StudioSnapshotHelper.removeSnapshot(pkgs);
        return getView("simpleListing").arg("pkgs", pm.sort(pkgsWithoutSnapshot))
                                       .arg("showCommunityInfo", false)
                                       .arg("source", "studio");
    }

    public String getStateLabel(Package pkg) {
        PackageState state = pkg.getPackageState();
        switch (state) {
        case REMOTE:
        case DOWNLOADED:
        case INSTALLED:
            return state.getLabel();
        case DOWNLOADING:
            DownloadingPackage dpkg = (DownloadingPackage) pkg;
            return state.getLabel() + " (" + dpkg.getDownloadProgress() + "%)";
        case INSTALLING:
            return "installation in progress";
        case STARTED:
            return "running";
        case UNKNOWN:
        default:
            return "!?!";
        }
    }

    public boolean canInstall(Package pkg) {
        return PackageState.DOWNLOADED == pkg.getPackageState()
                && !InstallAfterRestart.isMarkedForInstallAfterRestart(pkg.getId());
    }

    public boolean needsRestart(Package pkg) {
        return InstallAfterRestart.isMarkedForInstallAfterRestart(pkg.getId())
                || PackageState.INSTALLED == pkg.getPackageState()
                || InstallAfterRestart.isMarkedForUninstallAfterRestart(pkg.getName());
    }

    public boolean canUnInstall(Package pkg) {
        return pkg.getPackageState().isInstalled()
                && !InstallAfterRestart.isMarkedForUninstallAfterRestart(pkg.getName());
    }

    /**
     * @since 5.8
     */
    public boolean canUpgrade(Package pkg) {
        return pkg.getPackageState().isInstalled() && pkg.getVersion().isSnapshot()
                && !InstallAfterRestart.isMarkedForInstallAfterRestart(pkg.getName());
    }

    public boolean canRemove(Package pkg) {
        return pkg.isLocal();
    }

    /**
     * @since 5.6
     */
    public boolean canCancel(Package pkg) {
        return PackageState.DOWNLOADING == pkg.getPackageState();
    }

    public boolean canDownload(Package pkg) {
        return pkg.getPackageState() == PackageState.REMOTE
                && (pkg.getType() == PackageType.STUDIO || pkg.getVisibility() == PackageVisibility.PUBLIC //
                        || (ConnectStatusHolder.instance().isRegistred() //
                                && ConnectStatusHolder.instance().getStatus().status() == SubscriptionStatusType.OK));
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

    /**
     * @since 5.6
     * @return true if registration is required for download
     */
    public boolean registrationRequired(Package pkg) {
        return pkg.getPackageState() == PackageState.REMOTE && pkg.getType() != PackageType.STUDIO
                && pkg.getVisibility() != PackageVisibility.PUBLIC && (!ConnectStatusHolder.instance().isRegistred() //
                        || ConnectStatusHolder.instance().getStatus().status() != SubscriptionStatusType.OK);
    }

}
