/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *     Mathieu Guillaume
 *
 */

package org.nuxeo.launcher.connect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.dependencies.DependencyResolution;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.CommandSetInfo;
import org.nuxeo.launcher.info.PackageInfo;

/**
 * @since 5.6
 */
public class ConnectBroker {

    private static final Log log = LogFactory.getLog(ConnectBroker.class);

    private static final int PACKAGES_DOWNLOAD_TIMEOUT_SECONDS = 300;

    private Environment env;

    private StandaloneUpdateService service;

    private CallbackHolder cbHolder;

    private CommandSetInfo cset = new CommandSetInfo();

    private String targetPlatform;

    public ConnectBroker(Environment env) throws IOException, PackageException {
        this.env = env;
        service = new StandaloneUpdateService(env);
        service.initialize();
        cbHolder = new StandaloneCallbackHolder(env, service);
        NuxeoConnectClient.setCallBackHolder(cbHolder);
        targetPlatform = env.getProperty(Environment.DISTRIBUTION_NAME) + "-"
                + env.getProperty(Environment.DISTRIBUTION_VERSION);
    }

    public String getCLID() throws NoCLID {
        return LogicalInstanceIdentifier.instance().getCLID();
    }

    public StandaloneUpdateService getUpdateService() {
        return service;
    }

    public PackageManager getPackageManager() {
        return NuxeoConnectClient.getPackageManager();
    }

    public CommandSetInfo getCommandSet() {
        return cset;
    }

    protected boolean isLocalPackageId(String pkgId) {
        List<LocalPackage> localPackages = getPkgList();
        boolean foundId = false;
        for (LocalPackage pkg : localPackages) {
            if (pkg.getId().equals(pkgId)) {
                foundId = true;
                break;
            }
        }
        return foundId;
    }

    protected boolean isRemotePackageId(String pkgId) {
        List<DownloadablePackage> remotePackages = NuxeoConnectClient.getPackageManager().listAllPackages();
        boolean foundId = false;
        for (DownloadablePackage pkg : remotePackages) {
            if (pkg.getId().equals(pkgId)) {
                foundId = true;
                break;
            }
        }
        return foundId;
    }

    protected String getLocalPackageIdFromName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        String foundId = null;
        for (LocalPackage pkg : localPackages) {
            if (pkg.getName().equals(pkgName)) {
                foundId = pkg.getId();
                break;
            }
        }
        return foundId;
    }

    protected String getInstalledPackageIdFromName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        String foundId = null;
        for (LocalPackage pkg : localPackages) {
            if (pkg.getState() != PackageState.INSTALLED) {
                continue;
            }
            if (pkg.getName().equals(pkgName)) {
                foundId = pkg.getId();
                break;
            }
        }
        return foundId;
    }

    protected String getRemotePackageIdFromName(String pkgName) {
        List<DownloadablePackage> remotePackages = NuxeoConnectClient.getPackageManager().listAllPackages();
        String foundId = null;
        for (DownloadablePackage pkg : remotePackages) {
            if (pkg.getName().equals(pkgName)) {
                foundId = pkg.getId();
                break;
            }
        }
        return foundId;
    }

    protected File getLocalPackageFile(String pkgFile) {
        boolean foundFile = false;
        if (pkgFile.startsWith("file:")) {
            pkgFile = pkgFile.substring(5);
        }
        File fileToCheck = new File(pkgFile);
        if (fileToCheck.exists()) {
            foundFile = true;
        } else {
            fileToCheck = new File(env.getServerHome(), pkgFile);
            if (fileToCheck.exists()) {
                foundFile = true;
            }
        }
        if (foundFile) {
            return fileToCheck;
        } else {
            return null;
        }
    }

    protected boolean isLocalPackageFile(String pkgFile) {
        return (getLocalPackageFile(pkgFile) != null);
    }

    public List<LocalPackage> getPkgList() {
        try {
            return service.getPackages();
        } catch (Exception e) {
            log.error("Could not read package list");
            return null;
        }
    }

    public List<LocalPackage> pkgList() {
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.name = CommandInfo.CMD_LIST;
        try {
            List<LocalPackage> localPackages = getPkgList();
            if (localPackages.isEmpty()) {
                log.info("No local package.");
            } else {
                log.info("Local packages:");
                for (LocalPackage localPackage : localPackages) {
                    cmdInfo.packages.add(new PackageInfo(localPackage));
                    String packageDescription;
                    switch (localPackage.getState()) {
                    case PackageState.DOWNLOADING:
                        packageDescription = "downloading...";
                        break;
                    case PackageState.DOWNLOADED:
                        packageDescription = "downloaded";
                        break;
                    case PackageState.INSTALLING:
                        packageDescription = "installing...";
                        break;
                    case PackageState.INSTALLED:
                        packageDescription = "installed";
                        break;
                    case PackageState.STARTED:
                        packageDescription = "started";
                        break;
                    default:
                        packageDescription = "unknown";
                        break;
                    }
                    packageDescription += "\t" + localPackage.getName()
                            + " (id: " + localPackage.getId() + ")";
                    log.info(packageDescription);
                }
            }
            cmdInfo.exitCode = 0;
            cset.commands.add(cmdInfo);
            return localPackages;
        } catch (Exception e) {
            log.error(e);
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return null;
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

    public boolean pkgReset() {
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.name = CommandInfo.CMD_RESET;
        try {
            service.reset();
            log.info("Packages reset done: All packages were marked as DOWNLOADED");
            List<LocalPackage> localPackages = service.getPackages();
            for (LocalPackage localPackage : localPackages) {
                cmdInfo.packages.add(new PackageInfo(localPackage));
            }
            cmdInfo.exitCode = 0;
            cset.commands.add(cmdInfo);
            return true;
        } catch (Exception e) {
            log.error(e);
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return false;
        }
    }

    public LocalPackage pkgUninstall(String pkgId) {
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.name = CommandInfo.CMD_UNINSTALL;
        cmdInfo.param = pkgId;
        try {
            LocalPackage pkg = service.getPackage(pkgId);
            if (pkg == null) {
                // Check whether this is the name of an installed package
                String realPkgId = getInstalledPackageIdFromName(pkgId);
                if (realPkgId != null) {
                    pkgId = realPkgId;
                    pkg = service.getPackage(realPkgId);
                }
            }
            if (pkg == null) {
                throw new PackageException("Package not found: " + pkgId);
            }
            log.info("Uninstalling " + pkgId);
            Task uninstallTask = pkg.getUninstallTask();
            try {
                performTask(uninstallTask);
            } catch (PackageException e) {
                uninstallTask.rollback();
                throw e;
            }
            // Refresh state
            pkg = service.getPackage(pkgId);
            cmdInfo.packages.add(new PackageInfo(pkg));
            cmdInfo.exitCode = 0;
            cset.commands.add(cmdInfo);
            return pkg;
        } catch (Exception e) {
            log.error("Failed to uninstall package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return null;
        }
    }

    public LocalPackage pkgRemove(String pkgId) {
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.name = CommandInfo.CMD_REMOVE;
        cmdInfo.param = pkgId;
        try {
            LocalPackage pkg = service.getPackage(pkgId);
            if (pkg == null) {
                // Check whether this is the name of a local package
                String realPkgId = getLocalPackageIdFromName(pkgId);
                if (realPkgId != null) {
                    pkgId = realPkgId;
                    pkg = service.getPackage(realPkgId);
                }
            }
            if (pkg == null) {
                throw new PackageException("Package not found: " + pkgId);
            }
            if ((pkg.getState() == PackageState.STARTED)
                    || (pkg.getState() == PackageState.INSTALLED)) {
                pkgUninstall(pkgId);
                // Refresh state
                pkg = service.getPackage(pkgId);
            }
            if (pkg.getState() != PackageState.DOWNLOADED) {
                throw new PackageException(
                        "Can only remove packages in DOWNLOADED, INSTALLED or STARTED state");
            }
            log.info("Removing " + pkgId);
            service.removePackage(pkgId);
            PackageInfo pkgInfo = new PackageInfo(pkg);
            pkgInfo.state = PackageState.REMOTE;
            cmdInfo.packages.add(pkgInfo);
            cmdInfo.exitCode = 0;
            cset.commands.add(cmdInfo);
            return pkg;
        } catch (Exception e) {
            log.error("Failed to remove package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return null;
        }
    }

    public LocalPackage pkgAdd(String packageFileName) {
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.name = CommandInfo.CMD_ADD;
        cmdInfo.param = packageFileName;
        try {
            File fileToAdd = getLocalPackageFile(packageFileName);
            if (fileToAdd == null) {
                throw new FileNotFoundException("File not found");
            }
            log.info("Adding " + packageFileName);
            LocalPackage pkg = service.addPackage(fileToAdd);
            cmdInfo.packages.add(new PackageInfo(pkg));
            cmdInfo.exitCode = 0;
            cset.commands.add(cmdInfo);
            return pkg;
        } catch (FileNotFoundException e) {
            log.error("Cannot find " + packageFileName
                    + " relative to current directory or to NUXEO_HOME");
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return null;
        } catch (PackageException e) {
            log.error("Failed to add package: " + packageFileName, e);
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return null;
        }
    }

    public LocalPackage pkgInstall(String pkgId) {
        CommandInfo cmdInfo = new CommandInfo();
        cmdInfo.name = CommandInfo.CMD_INSTALL;
        cmdInfo.param = pkgId;
        try {
            LocalPackage pkg = service.getPackage(pkgId);
            if (pkg == null) {
                // Check whether this is the name of a local package
                String realPkgId = getLocalPackageIdFromName(pkgId);
                if (realPkgId != null) {
                    pkgId = realPkgId;
                    pkg = service.getPackage(realPkgId);
                }
            }
            if (pkg == null) {
                // Check whether this is the name of a remote package
                String realPkgId = getRemotePackageIdFromName(pkgId);
                if (realPkgId != null) {
                    List<String> downloadList = new ArrayList<String>();
                    downloadList.add(realPkgId);
                    if (!downloadPackages(downloadList)) {
                        throw new PackageException(
                                "Failed to download package " + pkgId);
                    } else {
                        pkgId = realPkgId;
                        pkg = service.getPackage(realPkgId);
                    }
                }
            }
            if (pkg == null) {
                // Assume this is a filename - try to add
                pkg = pkgAdd(pkgId);
            }
            if (pkg == null) {
                // Nothing worked - can't find the package anywhere
                throw new PackageException("Package not found: " + pkgId);
            }
            pkgId = pkg.getId();
            cmdInfo.param = pkgId;
            log.info("Installing " + pkgId);
            Task installTask = pkg.getInstallTask();
            try {
                performTask(installTask);
            } catch (PackageException e) {
                installTask.rollback();
                throw e;
            }
            // Refresh state
            pkg = service.getPackage(pkgId);
            cmdInfo.packages.add(new PackageInfo(pkg));
            cmdInfo.exitCode = 0;
            cset.commands.add(cmdInfo);
            return pkg;
        } catch (Exception e) {
            log.error("Failed to install package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
            return null;
        }
    }

    public boolean listPending(File commandsFile) {
        return executePending(commandsFile, false);
    }

    @SuppressWarnings("unchecked")
    public boolean executePending(File commandsFile, boolean doExecute) {
        int errorValue = 0;
        if (!commandsFile.isFile()) {
            return false;
        }
        List<String> lines;
        try {
            lines = FileUtils.readLines(commandsFile);
            for (String line : lines) {
                line = line.trim();
                String[] split = line.split("\\s+", 2);
                if (split.length == 2) {
                    if (split[0].equals(CommandInfo.CMD_INSTALL)) {
                        if (doExecute) {
                            pkgInstall(split[1]);
                        } else {
                            CommandInfo cmdInfo = new CommandInfo();
                            cmdInfo.name = CommandInfo.CMD_INSTALL;
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            cset.commands.add(cmdInfo);
                            log.info("Pending action: install " + split[1]);
                        }
                    } else if (split[0].equals(CommandInfo.CMD_ADD)) {
                        if (doExecute) {
                            pkgAdd(split[1]);
                        } else {
                            CommandInfo cmdInfo = new CommandInfo();
                            cmdInfo.name = CommandInfo.CMD_ADD;
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            cset.commands.add(cmdInfo);
                            log.info("Pending action: add " + split[1]);
                        }
                    } else if (split[0].equals(CommandInfo.CMD_UNINSTALL)) {
                        if (doExecute) {
                            pkgUninstall(split[1]);
                        } else {
                            CommandInfo cmdInfo = new CommandInfo();
                            cmdInfo.name = CommandInfo.CMD_UNINSTALL;
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            cset.commands.add(cmdInfo);
                            log.info("Pending action: uninstall " + split[1]);
                        }
                    } else if (split[0].equals(CommandInfo.CMD_REMOVE)) {
                        if (doExecute) {
                            pkgRemove(split[1]);
                        } else {
                            CommandInfo cmdInfo = new CommandInfo();
                            cmdInfo.name = CommandInfo.CMD_REMOVE;
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            cset.commands.add(cmdInfo);
                            log.info("Pending action: remove " + split[1]);
                        }
                    } else {
                        errorValue = 1;
                    }
                } else if (split.length == 1) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (doExecute) {
                            pkgInstall(line);
                        } else {
                            CommandInfo cmdInfo = new CommandInfo();
                            cmdInfo.name = CommandInfo.CMD_INSTALL;
                            cmdInfo.param = line;
                            cmdInfo.pending = true;
                            cset.commands.add(cmdInfo);
                            log.info("Pending action: install " + line);
                        }
                    }
                }
                if (errorValue != 0) {
                    log.error("Error processing pending package/command: "
                            + line);
                }
            }
            if (doExecute) {
                if (errorValue != 0) {
                    File bak = new File(commandsFile.getPath() + ".bak");
                    bak.delete();
                    commandsFile.renameTo(bak);
                } else {
                    commandsFile.delete();
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return errorValue == 0;

    }

    @SuppressWarnings("unused")
    protected boolean downloadPackages(List<String> packagesToDownload) {
        if (packagesToDownload == null) {
            return true;
        }
        // Queue downloads
        for (String pkg : packagesToDownload) {
            try {
                getPackageManager().download(pkg);
            } catch (Exception e) {
                log.error("Cannot download packages", e);
                return false;
            }
        }
        // Check progress
        ConnectDownloadManager cdm = NuxeoConnectClient.getDownloadManager();
        List<DownloadingPackage> pkgs = cdm.listDownloadingPackages();
        long startTime = new Date().getTime();
        long deltaTime = 0;
        boolean downloadOk = true;
        do {
            List<DownloadingPackage> pkgsCompleted = new ArrayList<DownloadingPackage>();
            for (DownloadingPackage pkg : pkgs) {
                if (pkg.isCompleted()) {
                    // Digest check not correctly implemented
                    pkgsCompleted.add(pkg);
                    CommandInfo cmdInfo = new CommandInfo();
                    cmdInfo.name = CommandInfo.CMD_ADD;
                    cmdInfo.param = pkg.getId();
                    cset.commands.add(cmdInfo);
                    if (false && !pkg.isDigestOk()) {
                        log.error("Wrong digest for package " + pkg.getName());
                        cmdInfo.exitCode = 1;
                        downloadOk = false;
                    } else {
                        cmdInfo.exitCode = 0;
                    }
                }
            }
            pkgs.removeAll(pkgsCompleted);
            deltaTime = (new Date().getTime() - startTime) / 1000;
        } while (deltaTime < PACKAGES_DOWNLOAD_TIMEOUT_SECONDS
                && pkgs.size() > 0);
        // Did everything get downloaded?
        for (DownloadingPackage pkg : pkgs) {
            CommandInfo cmdInfo = new CommandInfo();
            cmdInfo.name = CommandInfo.CMD_ADD;
            cmdInfo.param = pkg.getId();
            cmdInfo.exitCode = 1;
            cset.commands.add(cmdInfo);
        }
        if (pkgs.size() > 0) {
            log.error("Timeout while trying to download packages");
            downloadOk = false;
        }
        return downloadOk;
    }

    public boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) {
        // Add local files
        if (pkgsToAdd != null) {
            for (String pkgToAdd : pkgsToAdd) {
                pkgAdd(pkgToAdd);
            }
        }
        // Build solver request
        List<String> solverInstall = new ArrayList<String>();
        List<String> solverRemove = new ArrayList<String>();
        List<String> solverUpgrade = new ArrayList<String>();
        if (pkgsToInstall != null) {
            // If install request is a file name, add to cache and get the id
            List<String> namesOrIdsToInstall = new ArrayList<String>();
            for (String pkgToInstall : pkgsToInstall) {
                if (isLocalPackageFile(pkgToInstall)) {
                    LocalPackage addedPkg = pkgAdd(pkgToInstall);
                    namesOrIdsToInstall.add(addedPkg.getId());
                    // TODO: set flag to prefer local package
                } else {
                    namesOrIdsToInstall.add(pkgToInstall);
                }
            }
            // For names, check whether they are new installs or upgrades
            for (String pkgToInstall : namesOrIdsToInstall) {
                if (getInstalledPackageIdFromName(pkgToInstall) != null) {
                    solverUpgrade.add(pkgToInstall);
                } else {
                    solverInstall.add(pkgToInstall);
                }
            }
        }
        // Add packages to remove to uninstall list
        if (pkgsToUninstall != null) {
            solverRemove.addAll(pkgsToUninstall);
        }
        if (pkgsToRemove != null) {
            solverRemove.addAll(pkgsToRemove);
        }
        DependencyResolution resolution = getPackageManager().resolveDependencies(
                solverInstall, solverRemove, solverUpgrade, targetPlatform);
        log.info(resolution);
        if (resolution.isFailed()) {
            return false;
        }
        // Download remote packages
        if (!downloadPackages(resolution.getDownloadPackageIds())) {
            log.error("Aborting packages change request");
            return false;
        }
        // Uninstall packages
        List<String> packageIds = resolution.getRemovePackageIds();
        for (String pkgId : packageIds) {
            if (pkgUninstall(pkgId) == null) {
                return false;
            }
        }
        // Remove "pkgsToRemove" packages from local cache
        if (pkgsToRemove != null) {
            for (String pkg : pkgsToRemove) {
                if (pkgRemove(pkg) == null) {
                    // Don't error out on failed (cache) removal
                }
            }
        }
        // Install packages
        packageIds = resolution.getInstallPackageIds();
        for (String pkgId : packageIds) {
            if (pkgInstall(pkgId) == null) {
                return false;
            }
        }
        return true;
    }

}
