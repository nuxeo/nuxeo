/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

package org.nuxeo.connect.client.jsf;

import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.ArrayUtils;
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
import org.nuxeo.connect.packages.dependencies.DependencyResolution;
import org.nuxeo.connect.packages.dependencies.TargetPlatformFilterHelper;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.admin.AdminViewManager;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
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

    private static final String LABEL_STUDIO_UPDATE_STATUS = "label.studio.update.status.";

    /**
     * FIXME JC: should follow or simply reuse {@link PackageState}
     */
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

    protected String studioSnapshotUpdateError;

    /**
     * Boolean indicating is Studio snapshot package validation should be done.
     *
     * @since 5.7.1
     */
    protected Boolean validateStudioSnapshot;

    /**
     * Last validation status of the Studio snapshot package
     *
     * @since 5.7.1
     */
    protected ValidationStatus studioSnapshotValidationStatus;

    private FileTime lastUpdate = null;

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
        List<SelectItem> types = new ArrayList<>();
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
        resetStudioSnapshotValidationStatus();
        if (snapshotPkg != null) {
            isStudioSnapshopUpdateInProgress = true;
            try {
                StudioAutoInstaller studioAutoInstaller = new StudioAutoInstaller(
                        pm, snapshotPkg.getId(), shouldValidateStudioSnapshot());
                studioAutoInstaller.run();
            } finally {
                isStudioSnapshopUpdateInProgress = false;
            }
        } else {
            studioSnapshotUpdateError = translate("label.studio.update.error.noSnapshotPackageFound");
        }
    }

    public boolean isStudioSnapshopUpdateInProgress() {
        return isStudioSnapshopUpdateInProgress;
    }

    /**
     * Returns true if validation should be performed
     *
     * @since 5.7.1
     */
    public Boolean getValidateStudioSnapshot() {
        return validateStudioSnapshot;
    }

    /**
     * @since 5.7.1
     */
    public void setValidateStudioSnapshot(Boolean validateStudioSnapshot) {
        this.validateStudioSnapshot = validateStudioSnapshot;
    }

    /**
     * Returns true if Studio snapshot module should be validated.
     * <p>
     * Validation can be skipped by user, or can be globally disabled by setting
     * framework property "studio.snapshot.disablePkgValidation" to true.
     *
     * @since 5.7.1
     */
    protected boolean shouldValidateStudioSnapshot() {
        if (Framework.isBooleanPropertyTrue("studio.snapshot.disablePkgValidation")) {
            return false;
        }
        return Boolean.TRUE.equals(getValidateStudioSnapshot());
    }

    protected static String translate(String label, Object... params) {
        return ComponentUtils.translate(FacesContext.getCurrentInstance(),
                label, params);
    }

    protected FileTime getLastUpdateDate() {
        if (lastUpdate == null) {
            PackageManager pm = Framework.getLocalService(PackageManager.class);
            List<DownloadablePackage> pkgs = pm.listAllStudioRemotePackages();
            DownloadablePackage snapshotPkg = StudioSnapshotHelper.getSnapshot(pkgs);
            if (snapshotPkg != null) {
                try {
                    PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
                    LocalPackage pkg = pus.getPackage(snapshotPkg.getId());
                    if (pkg != null) {
                        // it would have been easier if this API returned a
                        // date or FileTime instance, as parsing the date to
                        // format it differently is always a pain
                        lastUpdate = pus.getInstallDate(pkg.getId());
                    }
                } catch (PackageException e) {
                    log.error(e);
                }
            }
            return lastUpdate;
        } else {
            return lastUpdate;
        }
    }

    public String getStudioInstallationStatus() {
        if (studioSnapshotStatus == null) {
            PackageManager pm = Framework.getLocalService(PackageManager.class);
            List<DownloadablePackage> pkgs = pm.listAllStudioRemotePackages();
            LocalPackage pkg = null;
            DownloadablePackage snapshotPkg = StudioSnapshotHelper.getSnapshot(pkgs);
            if (snapshotPkg != null) {
                try {
                    PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
                    pkg = pus.getPackage(snapshotPkg.getId());
                } catch (PackageException e) {
                    log.error(e);
                }
            }
            if (pkg == null) {
                return translate(LABEL_STUDIO_UPDATE_STATUS + "noStatus");
            }
            PackageState studioPkgState = pkg.getPackageState();
            if (studioPkgState == PackageState.DOWNLOADING) {
                studioSnapshotStatus = SnapshotStatus.downloading;
            } else if (studioPkgState == PackageState.DOWNLOADED) {
                studioSnapshotStatus = SnapshotStatus.saving;
            } else if (studioPkgState == PackageState.INSTALLING) {
                studioSnapshotStatus = SnapshotStatus.installing;
            } else if (studioPkgState.isInstalled()) {
                studioSnapshotStatus = SnapshotStatus.completed;
            } else {
                studioSnapshotStatus = SnapshotStatus.error;
            }
        }

        Object[] params = new Object[0];
        if (SnapshotStatus.error.equals(studioSnapshotStatus)) {
            if (studioSnapshotUpdateError == null) {
                studioSnapshotUpdateError = "???";
            }
            params = new Object[] { studioSnapshotUpdateError };
        } else if (SnapshotStatus.downloading.equals(studioSnapshotStatus)) {
            params = new Object[] { String.valueOf(studioSnapshotDownloadProgress) };
        } else {
            FileTime update = getLastUpdateDate();
            if (update != null) {
                DateFormat df = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                df.setTimeZone(TimeZone.getDefault());
                params = new Object[] { df.format(new Date(update.toMillis())) };
            }
        }

        return translate(
                LABEL_STUDIO_UPDATE_STATUS + studioSnapshotStatus.name(),
                params);
    }

    // TODO: plug a notifier for status to be shown to the user
    protected class StudioAutoInstaller implements Runnable {

        protected final String packageId;

        protected final PackageManager pm;

        /**
         * @since 5.7.1
         */
        protected final boolean validate;

        protected StudioAutoInstaller(PackageManager pm, String packageId,
                boolean validate) {
            this.pm = pm;
            this.packageId = packageId;
            this.validate = validate;
        }

        @Override
        public void run() {
            try {
                if (validate) {
                    ValidationStatus status = new ValidationStatus();

                    pm.flushCache();
                    DownloadablePackage remotePkg = pm.findRemotePackageById(packageId);
                    if (remotePkg == null) {
                        status.addError(String.format(
                                "Cannot perform validation: remote package '%s' not found",
                                packageId));
                        return;
                    }
                    PackageDependency[] pkgDeps = remotePkg.getDependencies();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "%s target platforms: %s",
                                remotePkg,
                                ArrayUtils.toString(remotePkg.getTargetPlatforms())));
                        log.debug(String.format("%s dependencies: %s",
                                remotePkg, ArrayUtils.toString(pkgDeps)));
                    }

                    // TODO NXP-11776: replace errors by internationalized
                    // labels
                    String targetPlatform = PlatformVersionHelper.getPlatformFilter();
                    if (!TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(
                            remotePkg, targetPlatform)) {
                        status.addError(String.format(
                                "This package is not validated for your current platform: %s",
                                targetPlatform));
                    }
                    // check deps requirements
                    if (pkgDeps != null && pkgDeps.length > 0) {
                        DependencyResolution resolution = pm.resolveDependencies(
                                packageId, targetPlatform);
                        if (resolution.isFailed() && targetPlatform != null) {
                            // retry without PF filter in case it gives more
                            // information
                            resolution = pm.resolveDependencies(packageId, null);
                        }
                        if (resolution.isFailed()) {
                            status.addError(String.format(
                                    "Dependency check has failed for package '%s' (%s)",
                                    packageId, resolution));
                        } else {
                            List<String> pkgToInstall = resolution.getInstallPackageIds();
                            if (pkgToInstall != null
                                    && pkgToInstall.size() == 1
                                    && packageId.equals(pkgToInstall.get(0))) {
                                // ignore
                            } else if (resolution.requireChanges()) {
                                // do not install needed deps: they may not be
                                // hot-reloadable and that's not what the
                                // "update snapshot" button is for.
                                status.addError(resolution.toString().trim().replaceAll(
                                        "\n", "<br />"));
                            }
                        }
                    }

                    if (status.hasErrors()) {
                        setStatus(
                                SnapshotStatus.error,
                                translate("label.studio.update.validation.error"),
                                status);
                        return;
                    }
                }

                // Effective install
                if (Framework.isDevModeSet()) {
                    try {
                        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
                        LocalPackage pkg = pus.getPackage(packageId);

                        // Uninstall and/or remove if needed
                        if (pkg != null) {
                            log.info(String.format("Updating package %s...",
                                    pkg));
                            if (pkg.getPackageState().isInstalled()) {
                                // First remove it to allow SNAPSHOT upgrade
                                log.info("Uninstalling " + packageId);
                                Task uninstallTask = pkg.getUninstallTask();
                                try {
                                    performTask(uninstallTask);
                                } catch (PackageException e) {
                                    uninstallTask.rollback();
                                    throw e;
                                }
                            }
                            pus.removePackage(packageId);
                        }

                        // Download
                        setStatus(SnapshotStatus.downloading, null);
                        DownloadingPackage downloadingPkg = pm.download(packageId);
                        try {
                            while (!downloadingPkg.isCompleted()) {
                                studioSnapshotDownloadProgress = downloadingPkg.getDownloadProgress();
                                Thread.sleep(100);
                                log.debug("downloading studio snapshot package");
                            }
                            log.debug("studio snapshot package download completed, starting installation");
                            Thread.sleep(200);
                            setStatus(SnapshotStatus.saving, null);
                            // FIXME JC: Is this a workaround for some issue?
                            // downloadingPkg.isCompleted() is true!
                            while (pus.getPackage(downloadingPkg.getId()) == null) {
                                studioSnapshotDownloadProgress = downloadingPkg.getDownloadProgress();
                                Thread.sleep(50);
                                log.debug("downloading studio snapshot package");
                            }
                        } catch (PackageException | InterruptedException e) {
                            log.error(
                                    "Error while downloading studio snapshot",
                                    e);
                            setStatus(
                                    SnapshotStatus.error,
                                    translate(
                                            "label.studio.update.downloading.error",
                                            e.getMessage()));
                            return;
                        }

                        // Install
                        setStatus(SnapshotStatus.installing, null);
                        log.info("Installing " + packageId);
                        pkg = pus.getPackage(packageId);
                        Task installTask = pkg.getInstallTask();
                        try {
                            performTask(installTask);
                        } catch (PackageException e) {
                            installTask.rollback();
                            throw e;
                        }
                        // Refresh state
                        pkg = pus.getPackage(packageId);
                        lastUpdate = pus.getInstallDate(packageId);
                        setStatus(SnapshotStatus.completed, null);
                    } catch (PackageException e) {
                        log.error("Error while installing studio snapshot", e);
                        setStatus(
                                SnapshotStatus.error,
                                translate(
                                        "label.studio.update.installation.error",
                                        e.getMessage()));
                    }
                } else {
                    InstallAfterRestart.addPackageForInstallation(packageId);
                    setStatus(SnapshotStatus.restartNeeded, null);
                    setupWizardAction.setNeedsRestart(true);
                }
            } catch (Exception e) {
                setStatus(SnapshotStatus.error, e.getMessage());
            }
        }

        protected void performTask(Task task) throws PackageException {
            ValidationStatus validationStatus = task.validate();
            if (validationStatus.hasErrors()) {
                throw new PackageException("Failed to validate package "
                        + task.getPackage().getId() + " -> "
                        + validationStatus.getErrors());
            }
            if (validationStatus.hasWarnings()) {
                log.warn("Got warnings on package validation "
                        + task.getPackage().getId() + " -> "
                        + validationStatus.getWarnings());
            }
            task.run(null);
        }
    }

    protected void setStatus(SnapshotStatus status, String errorMessage) {
        studioSnapshotStatus = status;
        studioSnapshotUpdateError = errorMessage;
    }

    protected void setStatus(SnapshotStatus status, String errorMessage,
            ValidationStatus validationStatus) {
        setStatus(status, errorMessage);
        setStudioSnapshotValidationStatus(validationStatus);
    }

    /**
     * @since 5.7.1
     */
    public ValidationStatus getStudioSnapshotValidationStatus() {
        return studioSnapshotValidationStatus;
    }

    /**
     * @since 5.7.1
     */
    public void setStudioSnapshotValidationStatus(ValidationStatus status) {
        this.studioSnapshotValidationStatus = status;
    }

    /**
     * @since 5.7.1
     */
    public void resetStudioSnapshotValidationStatus() {
        setStudioSnapshotValidationStatus(null);
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
        Map<String, String> params = new HashMap<>();
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
