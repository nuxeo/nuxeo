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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.data.DownloadablePackage;
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

/*
 * @since 5.6
 */
public class ConnectBroker {

    private static final Log log = LogFactory.getLog(ConnectBroker.class);

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
        targetPlatform = env.getProperty("org.nuxeo.distribution.name" + "-" + "org.nuxeo.distribution.version");
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

    protected boolean isLocalPackageName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        boolean foundName = false;
        for (LocalPackage pkg : localPackages) {
            if (pkg.getName().equals(pkgName)) {
                foundName = true;
                break;
            }
        }
        return foundName;
    }

    protected boolean isInstalledPackageName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        boolean foundName = false;
        for (LocalPackage pkg : localPackages) {
            if (pkg.getState() != PackageState.INSTALLED) {
                continue;
            }
            if (pkg.getName().equals(pkgName)) {
                foundName = true;
                break;
            }
        }
        return foundName;
    }

    protected boolean isRemotePackageName(String pkgName) {
        List<DownloadablePackage> remotePackages = NuxeoConnectClient.getPackageManager().listAllPackages();
        boolean foundName = false;
        for (DownloadablePackage pkg : remotePackages) {
            if (pkg.getName().equals(pkgName)) {
                foundName = true;
                break;
            }
        }
        return foundName;
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
                // Assume this is a filename - try to add
                pkg = pkgAdd(pkgId);
                // Validate "add" went OK
                if (pkg == null) {
                    throw new PackageException("Package not found: " + pkgId);
                }
                pkgId = pkg.getId();
                cmdInfo.param = pkgId;
            }
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

    public boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) {
        // Add local files
        for (String pkgToAdd : pkgsToAdd) {
            pkgAdd(pkgToAdd);
        }
        // Build solver request
        if ((pkgsToInstall.size() != 0) || (pkgsToUninstall.size() != 0)
                || (pkgsToRemove.size() != 0)) {
            List<String> solverInstall = new ArrayList<String>();
            List<String> solverRemove = new ArrayList<String>();
            List<String> solverUpgrade = new ArrayList<String>();
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
                if (isInstalledPackageName(pkgToInstall)) {
                    solverUpgrade.add(pkgToInstall);
                } else {
                    solverInstall.add(pkgToInstall);
                }
            }
            // Add packages to remove to uninstall list
            solverRemove.addAll(pkgsToUninstall);
            solverRemove.addAll(pkgsToRemove);
            DependencyResolution resolution = getPackageManager().resolveDependencies(solverInstall, solverRemove, solverUpgrade, targetPlatform);
            // TODO: act on the resolution
        }
        // Remove "pkgsToRemove" packages from local cache
        // TODO
        return false;
    }

}
