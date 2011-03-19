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

package org.nuxeo.connect.client.jsf;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.connect.client.we.StudioSnapshotHelper;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.runtime.api.Framework;

/**
 * Manages JSF views for Package Management.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("appsViews")
@Scope(CONVERSATION)
public class AppCenterViewsManager implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(AppCenterViewsManager.class);

    protected final Map<String, String> packageTypeFilters = new HashMap<String, String>();

    protected final Map<String, Boolean> platformFilters = new HashMap<String, Boolean>();

    @In(create = true)
    protected String currentAdminSubViewId;

    protected boolean onlyRemote = false;

    protected String searchString;

    public String getSearchString() {
        if (searchString == null) {
            return "";
        }
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public boolean getOnlyRemote() {
        return onlyRemote;
    }

    public void setOnlyRemote(boolean onlyRemote) {
        this.onlyRemote = onlyRemote;
    }

    public void setPlatformFilter(boolean doFilter) {
        platformFilters.put(currentAdminSubViewId, doFilter);
    }

    public boolean getPlatformFilter() {
        Boolean dofilter = platformFilters.get(currentAdminSubViewId);
        if (dofilter == null) {
            if ("ConnectAppsUpdates".equals(currentAdminSubViewId)) {
                return true; // filter on platform by default for updates
            } else {
                return false;
            }
        }
        return dofilter.booleanValue();
    }

    public String getPackageTypeFilter() {
        String filter = packageTypeFilters.get(currentAdminSubViewId);
        if (filter == null) {
            filter = "";
        }
        return filter;
    }

    public void setPackageTypeFilter(String filter) {
        packageTypeFilters.put(currentAdminSubViewId, filter);
    }

    public List<SelectItem> getPackageTypes() {

        List<SelectItem> types = new ArrayList<SelectItem>();

        SelectItem allItem = new SelectItem("", "label.packagetype.all");
        types.add(allItem);

        for (PackageType ptype : PackageType.values()) {
            // if (!ptype.equals(PackageType.STUDIO)) {
            SelectItem item = new SelectItem(ptype.getValue(),
                    "label.packagetype." + ptype.getValue());
            types.add(item);
            // }
        }
        return types;
    }

    public void flushCache() {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        pm.flushCache();
    }

    public void installStudioSnapshot() throws Exception {
        if (isStudioSnapshopUpdateInProgress) {
            return;
        }
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        List<DownloadablePackage> pkgs = pm.listAllStudioRemotePackages();

        DownloadablePackage snapshotPkg = StudioSnapshotHelper.getSnapshot(pkgs);

        studioSnapshotUpdateError = null;
        if (snapshotPkg != null) {
            isStudioSnapshopUpdateInProgress = true;
            DownloadingPackage downloadingStudioSnapshot = pm.download(snapshotPkg.getId());
            studioAutoInstaller = new StudioAutoInstaller(
                    downloadingStudioSnapshot);

            Thread thread = new Thread(studioAutoInstaller);
            thread.start();
        } else {
            studioSnapshotUpdateError = "No snapshot package found";
        }

    }

    public boolean isStudioSnapshopUpdateInProgress() {
        return isStudioSnapshopUpdateInProgress;
    }

    public void checkStudioSnapshot() {
        return;
    }

    public String getStudioInstallationStatus() {
        if (studioSnapshotUpdateError != null) {
            return "Error : " + studioSnapshotUpdateError;
        }
        if ("downloading".equals(studioSnapshotStatus)) {
            return "downloading : " + studioSnapshotDownloadProgress + " %";
        }

        if ("completed".equals(studioSnapshotStatus)) {
            DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                    Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            return "last update completed : "
                    + df.format(lastStudioSnapshotUpdate.getTime());
        }

        if (studioSnapshotStatus==null) {
            return " No previous Studio package installation";
        }

        return studioSnapshotStatus;
    }

    protected StudioAutoInstaller studioAutoInstaller;

    protected int studioSnapshotDownloadProgress;

    protected String studioSnapshotStatus;

    protected boolean isStudioSnapshopUpdateInProgress = false;

    protected Calendar lastStudioSnapshotUpdate;

    protected String studioSnapshotUpdateError;

    protected class StudioAutoInstaller implements Runnable {

        protected final DownloadingPackage pkg;

        protected StudioAutoInstaller(DownloadingPackage pkg) {
            this.pkg = pkg;
        }

        public void run() {

            try {
                studioSnapshotStatus = "downloading";
                while (!pkg.isCompleted()) {
                    try {
                        studioSnapshotDownloadProgress = pkg.getDownloadProgress();
                        Thread.sleep(100);
                        log.debug("downloading studio snapshot package");
                    } catch (InterruptedException e) {
                        // NOP
                    }
                }
                log.debug("studio snapshot package download completed, starting installation");

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // NOP
                }

                PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);

                studioSnapshotStatus = "saving";
                try {
                    while (pus.getPackage(pkg.getId()) == null) {
                        try {
                            studioSnapshotDownloadProgress = pkg.getDownloadProgress();
                            Thread.sleep(50);
                            log.debug("downloading studio snapshot package");
                        } catch (InterruptedException e) {
                            // NOP
                        }
                    }
                } catch (Exception e) {
                    log.error(
                            "Error while sending studio snapshot to update manager",
                            e);
                    studioSnapshotStatus = "error";
                    studioSnapshotUpdateError = " problem while downloading package "
                            + e.getMessage();
                    return;
                }

                studioSnapshotStatus = "installing";

                try {
                    LocalPackage lpkg = pus.getPackage(pkg.getId());
                    Task installTask = lpkg.getInstallTask();
                    installTask.run(new HashMap<String, String>());
                } catch (Exception e) {
                    log.error("Error while installing studio snapshot", e);
                    studioSnapshotStatus = "error";
                    studioSnapshotUpdateError = " problem during package installation "
                            + e.getMessage();
                }
                lastStudioSnapshotUpdate = Calendar.getInstance();
                studioSnapshotStatus = "completed";
            } finally {
                studioAutoInstaller = null;
                isStudioSnapshopUpdateInProgress = false;
            }
        }
    }

}
