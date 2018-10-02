/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.admin.runtime;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.connector.ConnectServerError;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.task.standalone.InstallTask;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.task.update.Rollback;
import org.nuxeo.connect.update.task.update.RollbackOptions;
import org.nuxeo.connect.update.task.update.Update;
import org.nuxeo.connect.update.task.update.UpdateOptions;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadContext;
import org.nuxeo.runtime.reload.ReloadResult;
import org.nuxeo.runtime.reload.ReloadService;
import org.osgi.framework.BundleException;

/**
 * Helper to hot reload studio bundles.
 *
 * @since 9.3
 */
public class ReloadHelper {

    private static final Log log = LogFactory.getLog(ReloadHelper.class);

    public static synchronized void hotReloadPackage(String packageId) {
        log.info("Reload Studio package with id=" + packageId);
        LocalPackage pkg = null;
        InstallTask installTask = null;
        try {
            ReloadService reloadService = Framework.getService(ReloadService.class);
            ReloadContext reloadContext = new ReloadContext();

            PackageManager pm = Framework.getService(PackageManager.class);

            PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
            pkg = pus.getPackage(packageId);

            // Remove package from PackageUpdateService and get its bundleName to hot reload it
            if (pkg != null) {
                if (pkg.getPackageState().isInstalled()) {
                    // get the bundle symbolic names to hot reload
                    UninstallTask uninstallTask = (UninstallTask) pkg.getUninstallTask();
                    // in our hot reload case, we just care about the bundle
                    // so get the rollback commands and then the target
                    uninstallTask.getCommands()
                                 .stream()
                                 .filter(Rollback.class::isInstance)
                                 .map(Rollback.class::cast)
                                 .map(Rollback::getRollbackOptions)
                                 .map(uninstallTask.getUpdateManager()::getRollbackTarget)
                                 .map(reloadService::getOSGIBundleName)
                                 .forEachOrdered(reloadContext::undeploy);
                }
                // remove the package from package update service, unless download will fail
                pus.removePackage(pkg.getId());
            }

            // Download
            DownloadingPackage downloadingPkg = pm.download(packageId);
            while (!downloadingPkg.isCompleted()) {
                if (log.isTraceEnabled()) {
                    log.trace("Downloading studio snapshot package: " + packageId);
                }
                Thread.sleep(100); // NOSONAR (we want the whole hot-reload to be synchronized)
            }

            log.info("Installing " + packageId);
            pkg = pus.getPackage(packageId);
            if (pkg == null || PackageState.DOWNLOADED != pkg.getPackageState()) {
                throw new NuxeoException("Error while downloading studio snapshot " + pkg);
            }

            // get bundles to deploy
            installTask = (InstallTask) pkg.getInstallTask();
            pus.setPackageState(pkg, PackageState.INSTALLING);

            // in our hot reload case, we just care about the bundle
            // so get the rollback commands and then the target
            installTask.getCommands()
                       .stream()
                       .filter(Update.class::isInstance)
                       .map(Update.class::cast)
                       .map(Update::getFile)
                       .forEachOrdered(reloadContext::deploy);

            // Reload
            ReloadResult result = reloadService.reloadBundles(reloadContext);

            // set package as started
            pus.setPackageState(pkg, PackageState.STARTED);
            // we need to write uninstall.xml otherwise next hot reload will fail :/
            // as we don't use the install task, commandLogs is empty
            // fill it with deployed bundles
            String id = pkg.getId();
            Version version = pkg.getVersion();
            result.deployedFilesAsStream()
                  // first convert it to UpdateOptions
                  .map(f -> UpdateOptions.newInstance(id, f, f.getParentFile()))
                  // then get key
                  .map(installTask.getUpdateManager()::getKey)
                  // then build the Rollback command to append to commandLogs
                  .map(key -> new RollbackOptions(id, key, version.toString()))
                  .map(Rollback::new)
                  .forEachOrdered(installTask.getCommandLog()::add);
        } catch (BundleException | PackageException | ConnectServerError e) {
            throw new NuxeoException("Error while updating studio snapshot", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException("Error while downloading studio snapshot", e);
        } finally {
            if (pkg != null && installTask != null) {
                // write the log
                File file = pkg.getData().getEntry(LocalPackage.UNINSTALL);
                try {
                    installTask.writeLog(file);
                } catch (PackageException e) {
                    // don't rethrow inside finally
                    log.error("Exception when writing uninstall.xml", e);
                }
            }
        }
    }

}
