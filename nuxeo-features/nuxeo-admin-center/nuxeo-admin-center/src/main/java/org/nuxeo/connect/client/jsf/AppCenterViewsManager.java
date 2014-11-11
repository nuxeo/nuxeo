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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.connect.client.ui.SharedPackageListingsSettings;
import org.nuxeo.connect.client.vindoz.InstallAfterRestart;
import org.nuxeo.connect.client.we.StudioSnapshotHelper;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.admin.AdminViewManager;
import org.nuxeo.ecm.admin.setup.SetupWizardActionBean;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.seam.NuxeoSeamHotReloadContextKeeper;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.api.Framework;

/**
 * Manages JSF views for Package Management.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("appsViews")
@Scope(ScopeType.CONVERSATION)
public class AppCenterViewsManager implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(AppCenterViewsManager.class);

    protected enum SnapshotStatus {
        downloading, saving, installing, error, completed, restartNeeded;
    }

    protected static final Map<String, String> view2PackageListName = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("ConnectAppsUpdates", "updates");
            put("ConnectAppsStudio", "studio");
            put("ConnectAppsRemote", "remote");
            put("ConnectAppsLocal", "local");
        }
    };

    @In(create = true)
    protected String currentAdminSubViewId;

    @In(create = true)
    protected NuxeoSeamHotReloadContextKeeper seamReloadContext;

    @In(create = true)
    protected SetupWizardActionBean setupWizardAction;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String searchString;

    protected SnapshotStatus studioSnapshotStatus;

    protected int studioSnapshotDownloadProgress;

    protected boolean isStudioSnapshopUpdateInProgress = false;

    // FIXME: this should be persisted instead of being local to the seam
    // component, as other potential users will not see the same information in
    // the admin center
    protected Calendar lastStudioSnapshotUpdate;

    protected String studioSnapshotUpdateError;

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
        return SharedPackageListingsSettings.instance().get("remote").isOnlyRemote();
    }

    public void setOnlyRemote(boolean onlyRemote) {
        SharedPackageListingsSettings.instance().get("remote").setOnlyRemote(
                onlyRemote);
    }

    protected String getListName() {
        return view2PackageListName.get(currentAdminSubViewId);
    }

    public void setPlatformFilter(boolean doFilter) {
        SharedPackageListingsSettings.instance().get(getListName()).setPlatformFilter(
                doFilter);
    }

    public boolean getPlatformFilter() {
        return SharedPackageListingsSettings.instance().get(getListName()).getPlatformFilter();
    }

    public String getPackageTypeFilter() {
        return SharedPackageListingsSettings.instance().get(getListName()).getPackageTypeFilter();
    }

    public void setPackageTypeFilter(String filter) {
        SharedPackageListingsSettings.instance().get(getListName()).setPackageTypeFilter(
                filter);
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

    /**
     * Method binding for the update button: needs to perform a real
     * redirection (as ajax context is broken after hot reload) and to provide
     * an outcome so that redirection through the URL service goes ok (even if
     * it just reset its navigation handler cache).
     *
     * @since 5.6
     */
    public String installStudioSnapshotAndRedirect() throws Exception {
        installStudioSnapshot();
        return AdminViewManager.VIEW_ADMIN;
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
            try {
                StudioAutoInstaller studioAutoInstaller = new StudioAutoInstaller(
                        pm, snapshotPkg.getId());
                studioAutoInstaller.run();
            } finally {
                isStudioSnapshopUpdateInProgress = false;
            }
        } else {
            studioSnapshotUpdateError = translate("label.studio.error.noSnapshotPackageFound");
        }
    }

    public boolean isStudioSnapshopUpdateInProgress() {
        return isStudioSnapshopUpdateInProgress;
    }

    protected static String translate(String label, Object... params) {
        return ComponentUtils.translate(FacesContext.getCurrentInstance(),
                label, params);
    }

    protected String getLastUpdateDate() {
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(lastStudioSnapshotUpdate.getTime());
    }

    public String getStudioInstallationStatus() {
        String prefix = "label.studio.update.status.";
        if (studioSnapshotStatus == null) {
            // TODO: should initialize status according to Studio snapshot
            // package installation
            return translate(prefix + "noStatus");
        }

        Object[] params = new Object[0];
        if (SnapshotStatus.error.equals(studioSnapshotStatus)) {
            if (studioSnapshotUpdateError == null) {
                studioSnapshotUpdateError = "???";
            }
            params = new Object[] { studioSnapshotUpdateError };
        } else if (SnapshotStatus.downloading.equals(studioSnapshotStatus)) {
            params = new Object[] { String.valueOf(studioSnapshotDownloadProgress) };
        } else if (SnapshotStatus.completed.equals(studioSnapshotStatus)
                || SnapshotStatus.restartNeeded.equals(studioSnapshotStatus)) {
            params = new Object[] { getLastUpdateDate() };
        }

        return translate(prefix + studioSnapshotStatus.name(), params);
    }

    // TODO: plug a notifier for status to be shown to the user
    protected class StudioAutoInstaller implements Runnable {

        protected final String packageId;

        protected final PackageManager pm;

        protected StudioAutoInstaller(PackageManager pm, String packageId) {
            this.pm = pm;
            this.packageId = packageId;
        }

        @Override
        public void run() {
            try {
                setStatus(SnapshotStatus.downloading, null);

                DownloadingPackage pkg = pm.download(packageId);

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

                setStatus(SnapshotStatus.saving, null);
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
                    setStatus(
                            SnapshotStatus.error,
                            translate("label.studio.update.downloading.error",
                                    e.getMessage()));
                    return;
                }

                if (Framework.isDevModeSet()) {
                    setStatus(SnapshotStatus.installing, null);
                    try {
                        LocalPackage lpkg = pus.getPackage(pkg.getId());
                        Task installTask = lpkg.getInstallTask();
                        installTask.run(new HashMap<String, String>());
                        lastStudioSnapshotUpdate = Calendar.getInstance();
                        setStatus(SnapshotStatus.completed, null);
                    } catch (Exception e) {
                        log.error("Error while installing studio snapshot", e);
                        setStatus(
                                SnapshotStatus.error,
                                translate(
                                        "label.studio.update.installation.error",
                                        e.getMessage()));
                    }
                } else {
                    InstallAfterRestart.addPackageForInstallation(pkg.getId());
                    lastStudioSnapshotUpdate = Calendar.getInstance();
                    setStatus(SnapshotStatus.restartNeeded, null);
                    setupWizardAction.setNeedsRestart(true);
                }
            } catch (Exception e) {
                setStatus(SnapshotStatus.error, e.getMessage());
            }
        }

    }

    protected void setStatus(SnapshotStatus status, String errorMessage) {
        studioSnapshotStatus = status;
        studioSnapshotUpdateError = errorMessage;
    }

    public void setDevMode(boolean value) {
        String feedbackCompId = "changeDevModeForm";
        ConfigurationGenerator conf = setupWizardAction.getConfigurationGenerator();
        boolean configurable = conf.isConfigurable();
        if (!configurable) {
            facesMessages.addToControl(
                    feedbackCompId,
                    StatusMessage.Severity.ERROR,
                    translate("label.setup.nuxeo.org.nuxeo.dev.changingDevModeNotConfigurable"));
            return;
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put(Framework.NUXEO_DEV_SYSTEM_PROP, Boolean.toString(value));
        try {
            conf.saveFilteredConfiguration(params);
            Properties props = conf.getUserConfig();
            conf.getServerConfigurator().dumpProperties(props);
            // force reload of framework properties to ensure it's immediately
            // taken into account by all code checking for
            // Framework#isDevModeSet
            Framework.getRuntime().reloadProperties();

            if (value) {
                facesMessages.addToControl(feedbackCompId,
                        StatusMessage.Severity.WARN,
                        translate("label.admin.center.devMode.justActivated"));
            } else {
                facesMessages.addToControl(feedbackCompId,
                        StatusMessage.Severity.INFO,
                        translate("label.admin.center.devMode.justDisabled"));
            }
        } catch (Exception e) {
            log.error(e, e);
            facesMessages.addToControl(
                    feedbackCompId,
                    StatusMessage.Severity.ERROR,
                    translate("label.admin.center.devMode.errorSaving",
                            e.getMessage()));
        } finally {
            setupWizardAction.setNeedsRestart(true);
            setupWizardAction.resetParameters();
        }
    }
}
