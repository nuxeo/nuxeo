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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Operation to trigger a Hot reload of the Studio Snapshot package. You must be an administrator to trigger it.
 *
 * @since 8.2
 */
@Operation(id = HotReloadStudioSnapshot.ID, category = Constants.CAT_SERVICES, label = "Hot Reload Studio Snapshot Package", description = "Updates Studio project with latest snapshot.")
public class HotReloadStudioSnapshot {

    public static final String ID = "Service.HotReloadStudioSnapshot";

    protected static boolean updateInProgress = false;

    private static final Log log = LogFactory.getLog(HotReloadStudioSnapshot.class);

    @Context
    protected CoreSession session;

    @Context
    protected PackageManager pm;

    @Param(name = "validate", required = false)
    protected boolean validate = true;

    @OperationMethod
    public Blob run() {

        JSONArray result = new JSONArray();
        JSONObject resultJSON = new JSONObject();

        if (updateInProgress) {
            resultJSON.put("status", "inProgress");
            result.add(resultJSON);
            return Blobs.createJSONBlob(result.toString());
        }

        if (!((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
            resultJSON.put("status", "error");
            resultJSON.put("message", "Must be Administrator to use this function");
            result.add(resultJSON);
            return Blobs.createJSONBlob(result.toString());
        }

        if (!Framework.isDevModeSet()) {
            resultJSON.put("status", "error");
            resultJSON.put("message", "You must enable Dev mode to Hot reload your Studio Snapshot package.");
            result.add(resultJSON);
            return Blobs.createJSONBlob(result.toString());
        }

        List<DownloadablePackage> pkgs = pm.listRemoteAssociatedStudioPackages();
        DownloadablePackage snapshotPkg = StudioSnapshotHelper.getSnapshot(pkgs);

        if (snapshotPkg == null) {
            resultJSON.put("status", "error");
            resultJSON.put("message", "No Snapshot Package was found.");
            result.add(resultJSON);
            return Blobs.createJSONBlob(result.toString());
        }

        try {
            updateInProgress = true;
            hotReloadPackage(snapshotPkg, resultJSON);
            result.add(resultJSON);
            return Blobs.createJSONBlob(result.toString());
        } finally {
            updateInProgress = false;
        }
    }

    public void hotReloadPackage(DownloadablePackage remotePkg, JSONObject resultJSON) {

        if (validate) {
            pm.flushCache();

            String targetPlatform = PlatformVersionHelper.getPlatformFilter();
            if (!TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(remotePkg, targetPlatform)) {
                resultJSON.put("status", "error");
                resultJSON.put("message",
                        String.format("This package is not validated for your current platform: %s", targetPlatform));
                return;
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
                    resultJSON.put("status", "dependency_error");
                    resultJSON.put("message",
                            String.format("Dependency check has failed for package '%s' (%s)", packageId, resolution));
                    return;
                } else {
                    List<String> pkgToInstall = resolution.getInstallPackageIds();
                    if (pkgToInstall != null && pkgToInstall.size() == 1 && packageId.equals(pkgToInstall.get(0))) {
                        // ignore
                    } else if (resolution.requireChanges()) {
                        // do not install needed deps: they may not be hot-reloadable and that's not what the
                        // "update snapshot" button is for.
                        // status.addError(resolution.toString().trim().replaceAll("\n", "<br />"));
                        List<String> dependencies = new ArrayList<>();
                        for (String dependency : resolution.getInstallPackageNames()) {
                            if (!dependency.contains(remotePkg.getName())) {
                                dependencies.add(dependency);
                            }
                        }
                        resultJSON.put("status", "dependency_error");
                        resultJSON.put("message",
                                "A dependency mismatch has been detected. Please check your Studio project settings and your server configuration.");
                        resultJSON.put("deps", dependencies);
                        return;
                    }
                }
            }
        }

        // Install
        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
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
                resultJSON.put("status", "success");
                resultJSON.put("message", "Studio package installed.");
            } catch (PackageException e) {
                installTask.rollback();
                throw e;
            }
        } catch (InterruptedException e) {
            ExceptionUtils.checkInterrupt(e);
            throw new NuxeoException("Error while downloading studio snapshot", e);
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
}
