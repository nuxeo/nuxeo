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

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.PackageDefinition;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.CommandSetInfo;
import org.nuxeo.launcher.info.PackageInfo;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @since 5.6
 */
public class ConnectBroker {

    private static final Log log = LogFactory.getLog(ConnectBroker.class);

    private static final int PACKAGES_DOWNLOAD_TIMEOUT_SECONDS = 300;

    public static final String PARAM_MP_DIR = "nuxeo.distribution.marketplace.dir";

    public static final String DISTRIBUTION_MP_DIR_DEFAULT = "setupWizardDownloads";

    public static final String PACKAGES_XML = "packages.xml";

    protected static final String LAUNCHER_CHANGED_PROPERTY = "launcher.changed";

    protected static final int LAUNCHER_CHANGED_EXIT_CODE = 128;

    private Environment env;

    private StandaloneUpdateService service;

    private CallbackHolder cbHolder;

    private CommandSetInfo cset = new CommandSetInfo();

    private String targetPlatform;

    private String distributionMPDir;

    private String relax = OPTION_RELAX_DEFAULT;

    public static final String OPTION_RELAX_DEFAULT = "ask";

    private String accept = OPTION_ACCEPT_DEFAULT;

    public static final String OPTION_ACCEPT_DEFAULT = "ask";

    public ConnectBroker(Environment env) throws IOException, PackageException {
        this.env = env;
        service = new StandaloneUpdateService(env);
        service.initialize();
        cbHolder = new StandaloneCallbackHolder(env, service);
        NuxeoConnectClient.setCallBackHolder(cbHolder);
        targetPlatform = env.getProperty(Environment.DISTRIBUTION_NAME) + "-"
                + env.getProperty(Environment.DISTRIBUTION_VERSION);
        distributionMPDir = env.getProperty(PARAM_MP_DIR,
                DISTRIBUTION_MP_DIR_DEFAULT);
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

    public void refreshCache() {
        getPackageManager().flushCache();
        NuxeoConnectClient.getPackageManager().listAllPackages();
    }

    public CommandSetInfo getCommandSet() {
        return cset;
    }

    protected boolean isInstalledPackageName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        boolean foundName = false;
        for (LocalPackage pkg : localPackages) {
            if (pkg.getName().equals(pkgName)
                    && ((pkg.getState() == PackageState.INSTALLING)
                            || (pkg.getState() == PackageState.INSTALLED) || (pkg.getState() == PackageState.STARTED))) {
                foundName = true;
                break;
            }
        }
        return foundName;
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

    protected String getBestIdForNameInList(String pkgName,
            List<? extends Package> pkgList) {
        String foundId = null;
        SortedMap<Version, String> foundPkgs = new TreeMap<Version, String>();
        SortedMap<Version, String> matchingPkgs = new TreeMap<Version, String>();
        for (Package pkg : pkgList) {
            if (pkg.getName().equals(pkgName)) {
                foundPkgs.put(pkg.getVersion(), pkg.getId());
                if (Arrays.asList(pkg.getTargetPlatforms()).contains(
                        targetPlatform)) {
                    matchingPkgs.put(pkg.getVersion(), pkg.getId());
                }
            }
        }
        if (matchingPkgs.size() != 0) {
            foundId = matchingPkgs.get(matchingPkgs.lastKey());
        } else if (foundPkgs.size() != 0) {
            foundId = foundPkgs.get(foundPkgs.lastKey());
        }
        return foundId;

    }

    protected boolean matchesPlatform(String requestPkgStr,
            List<DownloadablePackage> allPackages,
            Map<String, DownloadablePackage> allPackagesByID,
            Map<String, List<DownloadablePackage>> allPackagesByName)
            throws PackageException {
        // Try ID match first
        if (allPackagesByID.containsKey(requestPkgStr)) {
            return allPackagesByID.get(requestPkgStr).getTargetPlatforms().length == 0
                    || Arrays.asList(
                            allPackagesByID.get(requestPkgStr).getTargetPlatforms()).contains(
                            targetPlatform);
        }
        // Fallback on name match
        List<DownloadablePackage> allPackagesForName = allPackagesByName.get(requestPkgStr);
        if (allPackagesForName == null) {
            throw new PackageException("Package not found: " + requestPkgStr);
        }
        for (DownloadablePackage pkg : allPackagesForName) {
            if (requestPkgStr.equals(pkg.getName())) {
                if (pkg.getTargetPlatforms().length == 0
                        || Arrays.asList(pkg.getTargetPlatforms()).contains(
                                targetPlatform)) {
                    return true;
                }
            }
        }
        // No match or not compatible
        return false;
    }

    protected String getLocalPackageIdFromName(String pkgName) {
        return getBestIdForNameInList(pkgName, getPkgList());
    }

    protected List<String> getAllLocalPackageIdsFromName(String pkgName) {
        List<String> foundIds = new ArrayList<String>();
        for (Package pkg : getPkgList()) {
            if (pkg.getName().equals(pkgName)) {
                foundIds.add(pkg.getId());
            }
        }
        return foundIds;
    }

    protected String getInstalledPackageIdFromName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        List<LocalPackage> installedPackages = new ArrayList<LocalPackage>();
        for (LocalPackage pkg : localPackages) {
            if ((pkg.getState() != PackageState.INSTALLING)
                    && (pkg.getState() != PackageState.INSTALLED)
                    && (pkg.getState() != PackageState.STARTED)) {
                continue;
            }
            installedPackages.add(pkg);
        }
        return getBestIdForNameInList(pkgName, installedPackages);
    }

    protected String getRemotePackageIdFromName(String pkgName) {
        return getBestIdForNameInList(pkgName,
                NuxeoConnectClient.getPackageManager().listAllPackages());
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

    protected List<String> getDistributionFilenames() {
        File distributionMPFile = new File(distributionMPDir, PACKAGES_XML);
        List<String> md5Filenames = new ArrayList<String>();
        // Try to get md5 files from packages.xml
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.parse(distributionMPFile);
            XPathFactory xpFactory = XPathFactory.newInstance();
            XPath xpath = xpFactory.newXPath();
            XPathExpression expr = xpath.compile("//package/@md5");
            NodeList nodes = (NodeList) expr.evaluate(doc,
                    XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                String md5 = nodes.item(i).getNodeValue();
                if ((md5 != null) && (md5.length() > 0)) {
                    md5Filenames.add(md5);
                }
            }
        } catch (Exception e) {
            // Parsing failed - return empty list
            log.error("Failed parsing " + distributionMPFile, e);
            return new ArrayList<String>();
        }
        return md5Filenames;
    }

    protected Map<String, PackageDefinition> getDistributionDefinitions(
            List<String> md5Filenames) {
        Map<String, PackageDefinition> allDefinitions = new HashMap<String, PackageDefinition>();
        if (md5Filenames == null) {
            return allDefinitions;
        }
        for (String md5Filename : md5Filenames) {
            File md5File = new File(distributionMPDir, md5Filename);
            if (!md5File.exists()) {
                // distribution file has been deleted
                continue;
            }
            ZipFile zipFile;
            try {
                zipFile = new ZipFile(md5File);
            } catch (ZipException e) {
                log.warn("Unzip error reading file " + md5File, e);
                continue;
            } catch (IOException e) {
                log.warn("Could not read file " + md5File, e);
                continue;
            }
            try {
                ZipEntry zipEntry = zipFile.getEntry("package.xml");
                InputStream in = zipFile.getInputStream(zipEntry);
                PackageDefinition pd = NuxeoConnectClient.getPackageUpdateService().loadPackage(
                        in);
                allDefinitions.put(md5Filename, pd);
            } catch (Exception e) {
                log.error("Could not read package description", e);
                continue;
            } finally {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    log.warn("Unexpected error closing file " + md5File, e);
                }
            }
        }
        return allDefinitions;
    }

    protected void addDistributionPackage(String md5) {
        File distributionFile = new File(distributionMPDir, md5);
        if (distributionFile.exists()) {
            try {
                pkgAdd(distributionFile.getCanonicalPath());
            } catch (IOException e) {
                log.warn("Could not add distribution file " + md5);
            }
        }
    }

    public void addDistributionPackages() {
        Map<String, PackageDefinition> distributionPackages = getDistributionDefinitions(getDistributionFilenames());
        if (distributionPackages.isEmpty()) {
            return;
        }
        List<LocalPackage> localPackages = getPkgList();
        Map<String, LocalPackage> localPackagesById = new HashMap<String, LocalPackage>();
        if (localPackages != null) {
            for (LocalPackage pkg : localPackages) {
                localPackagesById.put(pkg.getId(), pkg);
            }
        }
        for (String md5 : distributionPackages.keySet()) {
            PackageDefinition md5Pkg = distributionPackages.get(md5);
            if (localPackagesById.containsKey(md5Pkg.getId())) {
                // We have the same package Id in the local cache
                LocalPackage localPackage = localPackagesById.get(md5Pkg.getId());
                if (localPackage.getVersion().isSnapshot()) {
                    // - For snapshots, until we have timestamp support, assume
                    // distribution version is newer than cached version.
                    // - This may (will) break the server if there are
                    // dependencies/compatibility changes or it the package is
                    // in installed state.
                    if (localPackage.getState() != PackageState.STARTED) {
                        pkgRemove(localPackage.getId());
                        addDistributionPackage(md5);
                    }
                }
            } else {
                // No package with this Id is in cache
                addDistributionPackage(md5);
            }
        }
    }

    public List<LocalPackage> getPkgList() {
        try {
            return service.getPackages();
        } catch (Exception e) {
            log.error("Could not read package list");
            return null;
        }
    }

    public void pkgList() {
        log.info("Local packages:");
        pkgList(getPkgList());
    }

    public void pkgListAll() {
        log.info("All packages:");
        pkgList(NuxeoConnectClient.getPackageManager().listAllPackages());
    }

    public void pkgList(List<? extends Package> packagesList) {
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_LIST);
        try {
            if (packagesList.isEmpty()) {
                log.info("None");
            } else {
                // TODO JC: Collections.sort(packagesList);
                for (Package pkg : packagesList) {
                    cmdInfo.packages.add(new PackageInfo(pkg));
                    String packageDescription;
                    switch (pkg.getState()) {
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
                    case PackageState.REMOTE:
                        packageDescription = "remote";
                        break;
                    default:
                        packageDescription = "unknown";
                        break;
                    }
                    packageDescription += "\t" + pkg.getName() + " (id: "
                            + pkg.getId() + ")";
                    log.info(packageDescription);
                }
            }
            cmdInfo.exitCode = 0;
        } catch (Exception e) {
            log.error(e);
            cmdInfo.exitCode = 1;
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
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_RESET);
        if ("ask".equalsIgnoreCase(accept)) {
            accept = readConsole(
                    "The reset will erase Marketplace packages history.\n"
                            + "Do you want to continue (yes/no)? [yes] ", "yes");
        }
        if (!Boolean.parseBoolean(accept)) {
            cmdInfo.exitCode = 1;
            return false;
        }
        try {
            service.reset();
            log.info("Packages reset done: all packages were marked as DOWNLOADED");
            List<LocalPackage> localPackages = service.getPackages();
            for (LocalPackage localPackage : localPackages) {
                localPackage.getUninstallFile().delete();
                FileUtils.deleteDirectory(localPackage.getData().getEntry(
                        LocalPackage.BACKUP_DIR));
                cmdInfo.packages.add(new PackageInfo(localPackage));
            }
            service.getRegistry().delete();
            FileUtils.deleteDirectory(service.getBackupDir());
            cmdInfo.exitCode = 0;
        } catch (PackageException e) {
            log.error(e);
            cmdInfo.exitCode = 1;
        } catch (IOException e) {
            log.error(e);
            cmdInfo.exitCode = 1;
        }
        return cmdInfo.exitCode == 0;
    }

    public boolean pkgPurge() throws PackageException {
        List<String> localNames = new ArrayList<String>();
        for (LocalPackage pkg : service.getPackages()) {
            localNames.add(pkg.getName());
        }
        return pkgRequest(null, null, null, localNames);
    }

    /**
     * Uninstall a list of packages. If the list contains a package name
     * (versus an ID), only the considered as best matching package is
     * uninstalled.
     *
     * @param packageIdsToRemove The list can contain package IDs and names
     * @see #pkgUninstall(String)
     */
    public boolean pkgUninstall(List<String> packageIdsToRemove) {
        log.debug("Uninstalling: " + packageIdsToRemove);
        for (String pkgId : packageIdsToRemove) {
            if (pkgUninstall(pkgId) == null) {
                log.error("Unable to uninstall " + pkgId);
                return false;
            }
        }
        return true;
    }

    /**
     * Uninstall a local package. The package is not removed from cache.
     *
     * @param pkgId Package ID or Name
     * @return The uninstalled LocalPackage or null if failed
     */
    public LocalPackage pkgUninstall(String pkgId) {
        if (env.getProperty(LAUNCHER_CHANGED_PROPERTY, "false").equals("true")) {
            System.exit(LAUNCHER_CHANGED_EXIT_CODE);
        }
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_UNINSTALL);
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
            return pkg;
        } catch (Exception e) {
            log.error("Failed to uninstall package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            return null;
        }
    }

    /**
     * Remove a list of packages from cache. If the list contains a package name
     * (versus an ID), all matching packages are removed.
     *
     * @param pkgsToRemove The list can contain package IDs and names
     * @see #pkgRemove(String)
     */
    public void pkgRemove(List<String> pkgsToRemove) {
        if (pkgsToRemove != null) {
            log.debug("Removing: " + pkgsToRemove);
            for (String pkgNameOrId : pkgsToRemove) {
                List<String> allIds;
                if (isLocalPackageId(pkgNameOrId)) {
                    allIds = new ArrayList<String>();
                    allIds.add(pkgNameOrId);
                } else {
                    // Request made on a name: remove all matching packages
                    allIds = getAllLocalPackageIdsFromName(pkgNameOrId);
                }
                for (String pkgId : allIds) {
                    if (pkgRemove(pkgId) == null) {
                        log.warn("Unable to remove " + pkgId);
                        // Don't error out on failed (cache) removal
                    }
                }
            }
        }
    }

    /**
     * Remove a package from cache. If it was installed, the package is
     * uninstalled then removed.
     *
     * @param pkgId Package ID or Name
     * @return The removed LocalPackage or null if failed
     */
    public LocalPackage pkgRemove(String pkgId) {
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_REMOVE);
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
            return pkg;
        } catch (Exception e) {
            log.error("Failed to remove package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            return null;
        }
    }

    /**
     * Add a list of package files into the cache
     *
     * @param pkgsToAdd
     * @see #pkgAdd(String)
     */
    public void pkgAdd(List<String> pkgsToAdd) {
        if (pkgsToAdd != null) {
            for (String pkgToAdd : pkgsToAdd) {
                pkgAdd(pkgToAdd);
            }
        }
    }

    /**
     * Add a package file into the cache
     *
     * @param packageFileName
     * @return The added LocalPackage or null if failed
     */
    public LocalPackage pkgAdd(String packageFileName) {
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
        cmdInfo.param = packageFileName;
        try {
            File fileToAdd = getLocalPackageFile(packageFileName);
            if (fileToAdd == null) {
                String pkgId = null;
                if (isRemotePackageId(packageFileName)) {
                    // Check whether this is a remote package ID
                    pkgId = packageFileName;
                } else {
                    // Check whether this is a remote package name
                    pkgId = getRemotePackageIdFromName(packageFileName);
                }
                if (pkgId == null) {
                    throw new FileNotFoundException("File not found");
                }
                List<String> downloadList = new ArrayList<String>();
                downloadList.add(pkgId);
                if (!downloadPackages(downloadList)) {
                    throw new PackageException("Failed to download package "
                            + pkgId);
                } else {
                    LocalPackage pkg = service.getPackage(pkgId);
                    if (pkg == null) {
                        throw new PackageException(
                                "Failed to find downloaded package in cache "
                                        + pkgId);
                    }
                    return pkg;
                }
            } else {
                log.info("Adding " + packageFileName);
                LocalPackage pkg = service.addPackage(fileToAdd);
                cmdInfo.packages.add(new PackageInfo(pkg));
                cmdInfo.exitCode = 0;
                return pkg;
            }
        } catch (FileNotFoundException e) {
            log.error("Cannot find " + packageFileName
                    + " relative to current directory or to NUXEO_HOME");
            cmdInfo.exitCode = 1;
            return null;
        } catch (PackageException e) {
            log.error("Failed to add package: " + packageFileName, e);
            cmdInfo.exitCode = 1;
            return null;
        }
    }

    /**
     * Install a list of local packages. If the list contains a package name
     * (versus an ID), only the considered as best matching package is
     * installed.
     *
     * @param packageIdsToInstall The list can contain package IDs and names
     * @see #pkgInstall(String)
     */
    public boolean pkgInstall(List<String> packageIdsToInstall) {
        log.debug("Installing: " + packageIdsToInstall);
        for (String pkgId : packageIdsToInstall) {
            if (pkgInstall(pkgId) == null) {
                log.error("Unable to install " + pkgId);
                return false;
            }
        }
        return true;
    }

    /**
     * Install a local package.
     *
     * @param pkgId Package ID or Name
     * @return The installed LocalPackage or null if failed
     */
    public LocalPackage pkgInstall(String pkgId) {
        if (env.getProperty(LAUNCHER_CHANGED_PROPERTY, "false").equals("true")) {
            System.exit(LAUNCHER_CHANGED_EXIT_CODE);
        }
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_INSTALL);
        cmdInfo.param = pkgId;
        try {
            LocalPackage pkg = service.getPackage(pkgId);
            if (pkg == null) {
                // Check whether this is the name of a local package
                String realPkgId = getLocalPackageIdFromName(pkgId);
                if (realPkgId != null) {
                    pkg = service.getPackage(realPkgId);
                }
            }
            if (pkg == null) {
                // We don't know this package, try to add it first
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
            return pkg;
        } catch (Exception e) {
            log.error("Failed to install package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            return null;
        }
    }

    public boolean listPending(File commandsFile) {
        return executePending(commandsFile, false, false);
    }

    /**
     * @since 5.6
     * @param commandsFile File containing the commands to execute
     * @param doExecute Whether to execute or list the actions
     * @param useResolver Whether to use full resolution or just execute
     *            individual actions
     */
    @SuppressWarnings("unchecked")
    public boolean executePending(File commandsFile, boolean doExecute,
            boolean useResolver) {
        int errorValue = 0;
        if (!commandsFile.isFile()) {
            return false;
        }
        List<String> pkgsToAdd = new ArrayList<String>();
        List<String> pkgsToInstall = new ArrayList<String>();
        List<String> pkgsToUninstall = new ArrayList<String>();
        List<String> pkgsToRemove = new ArrayList<String>();
        List<String> lines;
        try {
            lines = FileUtils.readLines(commandsFile);
            for (String line : lines) {
                line = line.trim();
                String[] split = line.split("\\s+", 2);
                if (split.length == 2) {
                    if (split[0].equals(CommandInfo.CMD_INSTALL)) {
                        if (doExecute) {
                            if (useResolver) {
                                pkgsToInstall.add(split[1]);
                            } else {
                                pkgInstall(split[1]);
                            }
                        } else {
                            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_INSTALL);
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            log.info("Pending action: install " + split[1]);
                        }
                    } else if (split[0].equals(CommandInfo.CMD_ADD)) {
                        if (doExecute) {
                            if (useResolver) {
                                pkgsToAdd.add(split[1]);
                            } else {
                                pkgAdd(split[1]);
                            }
                        } else {
                            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            log.info("Pending action: add " + split[1]);
                        }
                    } else if (split[0].equals(CommandInfo.CMD_UNINSTALL)) {
                        if (doExecute) {
                            if (useResolver) {
                                pkgsToUninstall.add(split[1]);
                            } else {
                                pkgUninstall(split[1]);
                            }
                        } else {
                            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_UNINSTALL);
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            log.info("Pending action: uninstall " + split[1]);
                        }
                    } else if (split[0].equals(CommandInfo.CMD_REMOVE)) {
                        if (doExecute) {
                            if (useResolver) {
                                pkgsToRemove.add(split[1]);
                            } else {
                                pkgRemove(split[1]);
                            }
                        } else {
                            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_REMOVE);
                            cmdInfo.param = split[1];
                            cmdInfo.pending = true;
                            log.info("Pending action: remove " + split[1]);
                        }
                    } else {
                        errorValue = 1;
                    }
                } else if (split.length == 1) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (doExecute) {
                            if ("init".equals(line)) {
                                addDistributionPackages();
                            } else {
                                if (useResolver) {
                                    pkgsToInstall.add(line);
                                } else {
                                    pkgInstall(line);
                                }
                            }
                        } else {
                            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_INSTALL);
                            cmdInfo.param = line;
                            cmdInfo.pending = true;
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
                if (useResolver) {
                    String oldAccept = accept;
                    String oldRelax = relax;
                    accept = "true";
                    relax = "true";
                    boolean success = pkgRequest(pkgsToAdd, pkgsToInstall,
                            pkgsToUninstall, pkgsToRemove);
                    accept = oldAccept;
                    relax = oldRelax;
                    if (!success) {
                        errorValue = 2;
                    }
                }
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
        if (packagesToDownload == null || packagesToDownload.isEmpty()) {
            return true;
        }
        // Queue downloads
        log.info("Downloading " + packagesToDownload + "...");
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
                    CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
                    cmdInfo.param = pkg.getId();
                    if (false && !pkg.isDigestOk()) {
                        log.error("Wrong digest for package " + pkg.getName());
                        cmdInfo.exitCode = 1;
                        downloadOk = false;
                    } else {
                        log.debug("Completed " + pkg);
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
            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
            cmdInfo.param = pkg.getId();
            cmdInfo.exitCode = 1;
        }
        if (pkgs.size() > 0) {
            log.error("Timeout while trying to download packages");
            downloadOk = false;
        }
        return downloadOk;
    }

    @SuppressWarnings("unchecked")
    public boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) {
        // Add local files
        pkgAdd(pkgsToAdd);
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
            // Check whether we have new installs or upgrades
            for (String pkgToInstall : namesOrIdsToInstall) {
                Map<String, DownloadablePackage> allPackagesByID = NuxeoConnectClient.getPackageManager().getAllPackagesByID();
                DownloadablePackage pkg = allPackagesByID.get(pkgToInstall);
                if (pkg != null) {
                    // This is a known ID
                    if (isInstalledPackageName(pkg.getName())) {
                        // The package is installed in another version
                        solverUpgrade.add(pkgToInstall);
                    } else {
                        // The package isn't installed yet
                        solverInstall.add(pkgToInstall);
                    }
                } else {
                    // This is a name (or a non-existing ID)
                    String id = getInstalledPackageIdFromName(pkgToInstall);
                    if (id != null) {
                        // The package is installed in another version
                        solverUpgrade.add(id);
                    } else {
                        // The package isn't installed yet
                        solverInstall.add(pkgToInstall);
                    }
                }
            }
        }
        if (pkgsToUninstall != null) {
            solverRemove.addAll(pkgsToUninstall);
        }
        if (pkgsToRemove != null) {
            // Add packages to remove to uninstall list
            solverRemove.addAll(pkgsToRemove);
        }
        if ((solverInstall.size() != 0) || (solverRemove.size() != 0)
                || (solverUpgrade.size() != 0)) {
            // Check whether we need to relax restriction to targetPlatform
            String requestPlatform = targetPlatform;
            List<String> requestPackages = new ArrayList<String>();
            requestPackages.addAll(solverInstall);
            requestPackages.addAll(solverRemove);
            requestPackages.addAll(solverUpgrade);
            List<DownloadablePackage> allPackages = NuxeoConnectClient.getPackageManager().listAllPackages();
            Map<String, DownloadablePackage> allPackagesByID = NuxeoConnectClient.getPackageManager().getAllPackagesByID();
            Map<String, List<DownloadablePackage>> allPackagesByName = NuxeoConnectClient.getPackageManager().getAllPackagesByName();
            try {
                for (String requestPackage : requestPackages) {
                    if (!matchesPlatform(requestPackage, allPackages,
                            allPackagesByID, allPackagesByName)) {
                        requestPlatform = null;
                        if ("ask".equalsIgnoreCase(relax)) {
                            relax = readConsole(
                                    "Package %s is not available on platform version %s.\n"
                                            + "Do you want to relax the constraint (yes/no)? [no] ",
                                    "no", requestPackage, targetPlatform);
                        }

                        if (Boolean.parseBoolean(relax)) {
                            log.warn(String.format(
                                    "Relax restriction to target platform %s because of package %s",
                                    targetPlatform, requestPackage));
                        } else {
                            throw new PackageException(
                                    String.format(
                                            "Package %s is not available on platform version %s (relax is not allowed)",
                                            requestPackage, targetPlatform));
                        }
                        break;
                    }
                }
            } catch (PackageException e) {
                log.error(e);
                return false;
            }

            DependencyResolution resolution = getPackageManager().resolveDependencies(
                    solverInstall, solverRemove, solverUpgrade, requestPlatform);
            log.info(resolution);
            if (resolution.isFailed()) {
                return false;
            }
            if (resolution.isEmpty()) {
                pkgRemove(pkgsToRemove);
                return true;
            }
            if ("ask".equalsIgnoreCase(accept)) {
                accept = readConsole(
                        "Do you want to continue (yes/no)? [yes] ", "yes");
            }
            if (!Boolean.parseBoolean(accept)) {
                log.warn("Exit");
                return false;
            }

            List<String> packageIdsToRemove = resolution.getOrderedPackageIdsToRemove();
            Map<String, Version> packagesToUpgrade = resolution.getLocalPackagesToUpgrade();
            List<String> packageIdsToUpgrade = resolution.getUpgradePackageIds();
            List<String> packageIdsToInstall = resolution.getOrderedPackageIdsToInstall();
            List<String> packagesIdsUninstalledBecauseOfUpgrade = new ArrayList<String>();

            // Download remote packages
            if (!downloadPackages(resolution.getDownloadPackageIds())) {
                log.error("Aborting packages change request");
                return false;
            }

            // Uninstall
            if (!packagesToUpgrade.isEmpty()) {
                // Add packages to upgrade to uninstall list
                List<String> uninstallList = new ArrayList<String>();
                List<String> uninstallIdsList = new ArrayList<String>();
                uninstallList.addAll(packageIdsToRemove);
                uninstallIdsList.addAll(packageIdsToRemove);
                for (String pkg : packagesToUpgrade.keySet()) {
                    uninstallList.add(pkg);
                    uninstallIdsList.add(getInstalledPackageIdFromName(pkg));
                }
                DependencyResolution uninstallResolution = getPackageManager().resolveDependencies(
                        null, uninstallList, null, requestPlatform);
                log.debug("Sub-resolution (uninstall) " + uninstallResolution);
                if (uninstallResolution.isFailed()) {
                    return false;
                }
                packageIdsToRemove = uninstallResolution.getOrderedPackageIdsToRemove();
                packagesIdsUninstalledBecauseOfUpgrade = ListUtils.subtract(
                        packageIdsToRemove, uninstallIdsList);
            }
            if (!pkgUninstall(packageIdsToRemove)) {
                return false;
            }

            // Install
            if (!packagesToUpgrade.isEmpty()) {
                // Add to install list the packages to upgrade + the packages
                // uninstalled because of upgrade
                List<String> installList = new ArrayList<String>();
                installList.addAll(solverInstall);
                installList.addAll(packageIdsToUpgrade);
                installList.addAll(packagesIdsUninstalledBecauseOfUpgrade);
                DependencyResolution installResolution = getPackageManager().resolveDependencies(
                        installList, null, null, requestPlatform);
                log.debug("Sub-resolution (install) " + installResolution);
                if (installResolution.isFailed()) {
                    return false;
                }
                packageIdsToInstall = installResolution.getOrderedPackageIdsToInstall();
            }
            if (!pkgInstall(packageIdsToInstall)) {
                return false;
            }

            pkgRemove(pkgsToRemove);
        }
        return true;
    }

    /**
     * Prompt user for yes/no answer
     *
     * @param message The message to display
     * @param defaultValue The default answer if there's no console or if
     *            "Enter" key is pressed.
     * @param objects Parameters to use in the message (like in
     *            {@link String#format(String, Object...)})
     * @return "true" if answer is "yes" or "y", else "false".
     */
    protected String readConsole(String message, String defaultValue,
            Object... objects) {
        String answer;
        Console console = System.console();
        if (console == null
                || StringUtils.isEmpty(answer = console.readLine(message,
                        objects))) {
            answer = defaultValue;
        }
        answer = answer.trim().toLowerCase();
        if ("yes".equals(answer) || "y".equals(answer)) {
            return "true";
        } else {
            return "false";
        }
    }

    protected boolean pkgUpgradeByType(PackageType type) {
        List<DownloadablePackage> upgrades = NuxeoConnectClient.getPackageManager().listUpdatePackages(
                type, targetPlatform);
        List<String> upgradeIds = new ArrayList<String>();
        for (DownloadablePackage upgrade : upgrades) {
            upgradeIds.add(upgrade.getId());
        }
        return pkgRequest(null, upgradeIds, null, null);

    }

    public boolean pkgHotfix() {
        return pkgUpgradeByType(PackageType.HOT_FIX);
    }

    public boolean pkgUpgrade() {
        return pkgUpgradeByType(PackageType.ADDON);
    }

    /**
     * Must be called after {@link #setAccept(String)} which overwrites its
     * value.
     *
     * @param relaxValue true, false or ask; ignored if null
     */
    public void setRelax(String relaxValue) {
        if (relaxValue != null) {
            relax = relaxValue;
        }
    }

    /**
     * @param acceptValue true, false or ask; if true or ask, then calls
     *            {@link #setRelax(String)} with the same value; ignored if null
     */
    public void setAccept(String acceptValue) {
        if (acceptValue != null) {
            accept = acceptValue;
            if (!"false".equalsIgnoreCase(acceptValue)) {
                setRelax(acceptValue);
            }
        }
    }

}
