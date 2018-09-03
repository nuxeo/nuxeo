/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.admin.operation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.client.ConnectClientComponent;
import org.nuxeo.connect.client.we.StudioSnapshotHelper;
import org.nuxeo.connect.connector.ConnectServerError;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.dependencies.DependencyResolution;
import org.nuxeo.connect.packages.dependencies.TargetPlatformFilterHelper;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
import org.nuxeo.ecm.admin.runtime.ReloadHelper;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Operation to trigger a Hot reload of the Studio Snapshot package. You must be an administrator to trigger it.
 *
 * @since 8.2
 */
@Operation(id = HotReloadStudioSnapshot.ID, category = Constants.CAT_SERVICES, label = "Hot Reload Studio Snapshot Package", description = "Updates Studio project with latest snapshot.")
public class HotReloadStudioSnapshot {

    protected static final String inProgress = "updateInProgress";

    protected static final String success = "success";

    protected static final String error = "error";

    protected static final String dependencyMismatch = "dependencyMismatch";

    public static final String ID = "Service.HotReloadStudioSnapshot";

    protected static volatile boolean updateInProgress = false;

    protected static synchronized boolean setInProgress(boolean inProgress) {
        if (updateInProgress == inProgress) {
            return false;
        }
        updateInProgress = inProgress;
        return true;
    }

    private static final Log log = LogFactory.getLog(HotReloadStudioSnapshot.class);

    @Context
    protected CoreSession session;

    @Context
    protected PackageManager pm;

    @Param(name = "validate", required = false)
    protected boolean validate = true;

    @OperationMethod
    public Blob run() throws Exception {
        try {
            if (!setInProgress(true)) {
                return jsonHelper(inProgress, "Update in progress.", null);
            }

            if (!((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
                return jsonHelper(error, "Must be Administrator to use this function.", null);
            }

            if (!Framework.isDevModeSet()) {
                return jsonHelper(error, "You must enable Dev mode to Hot reload your Studio Snapshot package.", null);
            }

            List<DownloadablePackage> pkgs = pm.listRemoteAssociatedStudioPackages();
            DownloadablePackage snapshotPkg = StudioSnapshotHelper.getSnapshot(pkgs);

            if (snapshotPkg == null) {
                return jsonHelper(error, "No Snapshot Package was found.", null);
            }

            return hotReloadPackage(snapshotPkg);
        } catch (RuntimeException e) {
            throw new OperationException(e);
        } finally {
            setInProgress(false);
        }
    }

    protected boolean shouldValidate() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        if (cs.isBooleanPropertyTrue(ConnectClientComponent.STUDIO_SNAPSHOT_DISABLE_VALIDATION_PROPERTY)) {
            return false;
        }
        return validate;
    }

    public Blob hotReloadPackage(DownloadablePackage remotePkg) {

        if (shouldValidate()) {
            pm.flushCache();

            String targetPlatform = PlatformVersionHelper.getPlatformFilter();
            if (!TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(remotePkg, targetPlatform)) {
                return jsonHelper(error,
                        String.format("This package is not validated for your current platform: %s", targetPlatform),
                        null);
            }

            PackageDependency[] pkgDeps = remotePkg.getDependencies();
            if (log.isDebugEnabled()) {
                log.debug(String.format("%s target platforms: %s", remotePkg,
                        ArrayUtils.toString(remotePkg.getTargetPlatforms())));
                log.debug(String.format("%s dependencies: %s", remotePkg, ArrayUtils.toString(pkgDeps)));
            }

            String packageId = remotePkg.getId();

            // check deps requirements
            if (pkgDeps != null && pkgDeps.length > 0) {
                DependencyResolution resolution = pm.resolveDependencies(packageId, targetPlatform);
                if (resolution.isFailed() && targetPlatform != null) {
                    // retry without PF filter in case it gives more information
                    resolution = pm.resolveDependencies(packageId, null);
                }
                if (resolution.isFailed()) {
                    return jsonHelper(dependencyMismatch,
                            String.format("Dependency check has failed for package '%s' (%s)", packageId, resolution),
                            null);
                } else {
                    List<String> pkgToInstall = resolution.getInstallPackageIds();
                    if (pkgToInstall != null && pkgToInstall.size() == 1 && packageId.equals(pkgToInstall.get(0))) {
                        // ignore
                    } else if (resolution.requireChanges()) {
                        // do not install needed deps: they may not be hot-reloadable and that's not what the
                        // "update snapshot" button is for.
                        // Returns missing dependencies in message instead of status
                        List<String> dependencies = new ArrayList<>();
                        for (String dependency : resolution.getInstallPackageNames()) {
                            if (!dependency.contains(remotePkg.getName())) {
                                dependencies.add(dependency);
                            }
                        }
                        return jsonHelper(dependencyMismatch,
                                "A dependency mismatch has been detected. Please check your Studio project settings and your server configuration.",
                                dependencies);
                    }
                }
            }
        }

        boolean useCompatReload = Framework.isBooleanPropertyTrue(ReloadService.USE_COMPAT_HOT_RELOAD);
        if (!useCompatReload) {
            log.info("Use hot reload update mechanism");
            ReloadHelper.hotReloadPackage(remotePkg.getId());
            return jsonHelper(success, "Studio package installed.", null);
        }
        // Install
        try {
            PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
            String packageId = remotePkg.getId();
            LocalPackage pkg = pus.getPackage(packageId);

            // Uninstall and/or remove if needed
            if (pkg != null) {
                removePackage(pus, pkg);
            }

            // Download
            DownloadingPackage downloadingPkg = pm.download(packageId);
            while (!downloadingPkg.isCompleted()) {
                log.debug("Downloading studio snapshot package: " + packageId);
                Thread.sleep(100);
            }

            log.info("Installing " + packageId);
            pkg = pus.getPackage(packageId);
            if (pkg == null || PackageState.DOWNLOADED != pkg.getPackageState()) {
                throw new NuxeoException("Error while downloading studio snapshot " + pkg);
            }
            Task installTask = pkg.getInstallTask();
            try {
                performTask(installTask);
                return jsonHelper(success, "Studio package installed.", null);
            } catch (PackageException e) {
                installTask.rollback();
                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        } catch (PackageException | ConnectServerError e) {
            throw new NuxeoException("Error while installing studio snapshot", e);
        }

    }

    protected static void removePackage(PackageUpdateService pus, LocalPackage pkg) throws PackageException {
        log.info(String.format("Removing package %s before update...", pkg.getId()));
        if (pkg.getPackageState().isInstalled()) {
            // First remove it to allow SNAPSHOT upgrade
            log.info("Uninstalling " + pkg.getId());
            Task uninstallTask = pkg.getUninstallTask();
            try {
                performTask(uninstallTask);
            } catch (PackageException e) {
                uninstallTask.rollback();
                throw e;
            }
        }
        pus.removePackage(pkg.getId());
    }

    protected static void performTask(Task task) throws PackageException {
        ValidationStatus validationStatus = task.validate();
        if (validationStatus.hasErrors()) {
            throw new PackageException(
                    "Failed to validate package " + task.getPackage().getId() + " -> " + validationStatus.getErrors());
        }
        if (validationStatus.hasWarnings()) {
            log.warn("Got warnings on package validation " + task.getPackage().getId() + " -> "
                    + validationStatus.getWarnings());
        }
        task.run(null);
    }

    protected static Blob jsonHelper(String status, String message, List<String> dependencies) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> resultJSON = new LinkedHashMap<>();
        resultJSON.put("status", status);
        resultJSON.put("message", message);
        if (dependencies != null) {
            resultJSON.put("deps", dependencies);
        }
        result.add(resultJSON);
        try {
            return Blobs.createJSONBlobFromValue(result);
        } catch (IOException e) {
            throw new NuxeoException("Unable to create json response", e);
        }
    }
}
