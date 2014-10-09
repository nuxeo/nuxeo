/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *     Mathieu Guillaume
 *
 */

package org.nuxeo.launcher.connect;

import java.io.Console;
import java.io.File;
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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.dependencies.CUDFHelper;
import org.nuxeo.connect.packages.dependencies.DependencyResolution;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageVisibility;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.PackageDefinition;
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

    private boolean allowSNAPSHOT = CUDFHelper.defaultAllowSNAPSHOT;

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

    protected LocalPackage getInstalledPackage(String pkgName) {
        try {
            return service.getPersistence().getActivePackage(pkgName);
        } catch (PackageException e) {
            log.error(e);
            return null;
        }
    }

    protected boolean isInstalledPackage(String pkgName) {
        return service.getPersistence().getActivePackageId(pkgName) != null;
    }

    protected boolean isLocalPackageId(String pkgId) {
        try {
            return service.getPackage(pkgId) != null;
        } catch (PackageException e) {
            log.error("Error looking for local package " + pkgId, e);
            return false;
        }
    }

    protected boolean isRemotePackageId(String pkgId) {
        return NuxeoConnectClient.getPackageManager().findPackageById(pkgId) != null;
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
            if (pkg.getPackageState().isInstalled()) {
                installedPackages.add(pkg);
            }
        }
        return getBestIdForNameInList(pkgName, installedPackages);
    }

    protected String getRemotePackageIdFromName(String pkgName) {
        return getBestIdForNameInList(pkgName,
                NuxeoConnectClient.getPackageManager().listAllPackages());
    }

    /**
     * Looks for a remote package from its name or id
     *
     * @param pkgNameOrId
     * @return the remote package Id; null if not found
     * @since 5.7
     */
    protected String getRemotePackageId(String pkgNameOrId) {
        String pkgId = null;
        if (isRemotePackageId(pkgNameOrId)) {
            // Check whether this is a remote package ID
            pkgId = pkgNameOrId;
        } else {
            // Check whether this is a remote package name
            pkgId = getRemotePackageIdFromName(pkgNameOrId);
        }
        return pkgId;
    }

    /**
     * Looks for a local package from its name or id
     *
     * @since 5.7
     * @param pkgIdOrName
     * @return the local package Id; null if not found
     * @throws PackageException
     */
    protected LocalPackage getLocalPackage(String pkgIdOrName)
            throws PackageException {
        // Try as a package id
        LocalPackage pkg = service.getPackage(pkgIdOrName);
        if (pkg == null) {
            // Check whether this is the name of a local package
            String pkgId = getLocalPackageIdFromName(pkgIdOrName);
            if (pkgId != null) {
                pkg = service.getPackage(pkgId);
            }
        }
        return pkg;
    }

    /**
     * Looks for a package file from its path
     *
     * @param pkgFile Absolute or relative package file path
     * @return the file if found, else null
     */
    protected File getLocalPackageFile(String pkgFile) {
        if (pkgFile.startsWith("file:")) {
            pkgFile = pkgFile.substring(5);
        }
        // Try absolute path
        File fileToCheck = new File(pkgFile);
        if (!fileToCheck.exists()) { // Try relative path
            fileToCheck = new File(env.getServerHome(), pkgFile);
        }
        if (fileToCheck.exists()) {
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

    protected boolean addDistributionPackage(String md5) {
        boolean ret = true;
        File distributionFile = new File(distributionMPDir, md5);
        if (distributionFile.exists()) {
            try {
                ret = pkgAdd(distributionFile.getCanonicalPath()) != null;
            } catch (IOException e) {
                log.warn("Could not add distribution file " + md5);
                ret = false;
            }
        }
        return ret;
    }

    public boolean addDistributionPackages() {
        Map<String, PackageDefinition> distributionPackages = getDistributionDefinitions(getDistributionFilenames());
        if (distributionPackages.isEmpty()) {
            return true;
        }
        List<LocalPackage> localPackages = getPkgList();
        Map<String, LocalPackage> localPackagesById = new HashMap<String, LocalPackage>();
        if (localPackages != null) {
            for (LocalPackage pkg : localPackages) {
                localPackagesById.put(pkg.getId(), pkg);
            }
        }
        boolean ret = true;
        for (String md5 : distributionPackages.keySet()) {
            PackageDefinition md5Pkg = distributionPackages.get(md5);
            if (localPackagesById.containsKey(md5Pkg.getId())) {
                // We have the same package Id in the local cache
                LocalPackage localPackage = localPackagesById.get(md5Pkg.getId());
                if (localPackage.getVersion().isSnapshot()) {
                    // - For snapshots, until we have timestamp support, assume
                    // distribution version is newer than cached version.
                    // - This may (will) break the server if there are
                    // dependencies/compatibility changes or if the package is
                    // in installed state.
                    if (!localPackage.getPackageState().isInstalled()) {
                        pkgRemove(localPackage.getId());
                        ret = addDistributionPackage(md5) && ret;
                    }
                }
            } else {
                // No package with this Id is in cache
                ret = addDistributionPackage(md5) && ret;
            }
        }
        return ret;
    }

    public List<LocalPackage> getPkgList() {
        try {
            return service.getPackages();
        } catch (PackageException e) {
            log.error("Could not read package list", e);
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
                NuxeoConnectClient.getPackageManager().sort(packagesList);
                StringBuilder sb = new StringBuilder();
                for (Package pkg : packagesList) {
                    newPackageInfo(cmdInfo, pkg);
                    PackageState packageState = pkg.getPackageState();
                    String packageDescription = packageState.getLabel();
                    packageDescription = String.format("%6s %11s\t",
                            pkg.getType(), packageDescription);
                    if (packageState == PackageState.REMOTE
                            && pkg.getType() != PackageType.STUDIO
                            && pkg.getVisibility() != PackageVisibility.PUBLIC
                            && !LogicalInstanceIdentifier.isRegistered()) {
                        packageDescription += "Registration required for ";
                    }
                    packageDescription += String.format("%s (id: %s)\n",
                            pkg.getName(), pkg.getId());
                    sb.append(packageDescription);
                }
                log.info(sb.toString());
            }
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
                newPackageInfo(cmdInfo, localPackage);
            }
            service.getRegistry().delete();
            FileUtils.deleteDirectory(service.getBackupDir());
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
        // Remove packages in DOWNLOADED state first
        // This will avoid extending the CUDF universe needlessly
        for (LocalPackage pkg : service.getPackages()) {
            if (pkg.getPackageState() == PackageState.DOWNLOADED) {
                pkgRemove(pkg.getId());
            }
        }
        // Process the remaining packages
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
            newPackageInfo(cmdInfo, pkg);
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
    public boolean pkgRemove(List<String> pkgsToRemove) {
        boolean cmdOk = true;
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
                        cmdOk = false;
                    }
                }
            }
        }
        return cmdOk;
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
            if (pkg.getPackageState().isInstalled()) {
                pkgUninstall(pkgId);
                // Refresh state
                pkg = service.getPackage(pkgId);
            }
            if (pkg.getPackageState() != PackageState.DOWNLOADED) {
                throw new PackageException(
                        "Can only remove packages in DOWNLOADED, INSTALLED or STARTED state");
            }
            service.removePackage(pkgId);
            log.info("Removed " + pkgId);
            newPackageInfo(cmdInfo, pkg).state = PackageState.REMOTE;
            return pkg;
        } catch (Exception e) {
            log.error("Failed to remove package: " + pkgId, e);
            cmdInfo.exitCode = 1;
            return null;
        }
    }

    /**
     * Add a list of packages into the cache, downloading them if needed and
     * possible.
     *
     * @param pkgsToAdd
     * @return true if command succeeded
     * @see #pkgAdd(String)
     */
    public boolean pkgAdd(List<String> pkgsToAdd) {
        return pkgAdd(pkgsToAdd, false);
    }

    /**
     * Add a list of packages into the cache, downloading them if needed and
     * possible.
     *
     * @since 5.9.6
     * @param pkgsToAdd
     * @param ignoreMissing
     * @return true if command succeeded
     * @see #pkgAdd(String)
     */
    public boolean pkgAdd(List<String> pkgsToAdd, boolean ignoreMissing) {
        boolean cmdOk = true;
        if (pkgsToAdd == null || pkgsToAdd.isEmpty()) {
            return cmdOk;
        }
        List<String> pkgIdsToDownload = new ArrayList<String>();
        for (String pkgToAdd : pkgsToAdd) {
            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
            cmdInfo.param = pkgToAdd;
            try {
                File fileToAdd = getLocalPackageFile(pkgToAdd);
                if (fileToAdd == null) {
                    String pkgId = getRemotePackageId(pkgToAdd);
                    if (pkgId == null) {
                        if (ignoreMissing) {
                            log.warn("Could not add package: " + pkgToAdd);
                            cmdInfo.newMessage(SimpleLog.LOG_LEVEL_INFO,
                                    "Could not add package: " + pkgToAdd);
                        } else {
                            throw new PackageException(
                                "Could not find a remote or local (relative to "
                                        + "current directory or to NUXEO_HOME) "
                                        + "package with name or ID " + pkgToAdd);
                        }
                    } else {
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_INFO,
                                "Waiting for download");
                        pkgIdsToDownload.add(pkgId);
                    }
                } else {
                    LocalPackage pkg = service.addPackage(fileToAdd);
                    log.info("Added " + pkg);
                    newPackageInfo(cmdInfo, pkg);
                }
            } catch (PackageException e) {
                cmdOk = false;
                cmdInfo.exitCode = 1;
                cmdInfo.newMessage(e);
            }
        }
        cmdOk = downloadPackages(pkgIdsToDownload) && cmdOk;
        return cmdOk;
    }

    /**
     * Add a package file into the cache
     *
     * @param packageFileName
     * @return The added LocalPackage or null if failed
     */
    public LocalPackage pkgAdd(String packageFileName) {
        return pkgAdd(packageFileName, false);
    }

    /**
     * Add a package file into the cache
     *
     * @since 5.9.6
     * @param packageFileName
     * @param ignoreMissing
     * @return The added LocalPackage or null if failed
     */
    public LocalPackage pkgAdd(String packageFileName, boolean ignoreMissing) {
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
        cmdInfo.param = packageFileName;
        LocalPackage pkg = null;
        try {
            File fileToAdd = getLocalPackageFile(packageFileName);
            if (fileToAdd == null) {
                String pkgId = getRemotePackageId(packageFileName);
                if (pkgId == null) {
                    if (ignoreMissing) {
                        log.warn("Could not add package: " + packageFileName);
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_INFO,
                                "Could not add package: " + packageFileName);
                        return null;
                    } else {
                        throw new PackageException(
                            "Could not find a remote or local (relative to "
                                    + "current directory or to NUXEO_HOME) "
                                    + "package with name or ID "
                                    + packageFileName);
                    }
                } else if (!downloadPackages(Arrays.asList(new String[] { pkgId }))) {
                    throw new PackageException("Could not download package "
                            + pkgId);
                }
                pkg = service.getPackage(pkgId);
                if (pkg == null) {
                    throw new PackageException(
                            "Could not find downloaded package in cache "
                                    + pkgId);
                }
            } else {
                pkg = service.addPackage(fileToAdd);
            }
            log.info("Added " + packageFileName);
            newPackageInfo(cmdInfo, pkg);
        } catch (PackageException e) {
            cmdInfo.exitCode = 1;
            cmdInfo.newMessage(e);
        }
        return pkg;
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
        return pkgInstall(packageIdsToInstall, false);
    }

    /**
     * Install a list of local packages. If the list contains a package name
     * (versus an ID), only the considered as best matching package is
     * installed.
     *
     * @since 5.9.6
     * @param packageIdsToInstall The list can contain package IDs and names
     * @param ignoreMissing If true, doesn't throw an exception on unkown packages
     * @see #pkgInstall(String)
     */
    public boolean pkgInstall(List<String> packageIdsToInstall, boolean ignoreMissing) {
        log.debug("Installing: " + packageIdsToInstall);
        for (String pkgId : packageIdsToInstall) {
            if (pkgInstall(pkgId, ignoreMissing) == null) {
                if (!ignoreMissing) {
                    log.error("Unable to install package: " + pkgId);
                    return false;
                }
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
        return pkgInstall(pkgId, false);
    }

    /**
     * Install a local package.
     *
     * @since 5.9.6
     * @param pkgId Package ID or Name
     * @param ignoreMissing If true, doesn't throw an exception on unkown packages
     * @return The installed LocalPackage or null if failed
     */
    public LocalPackage pkgInstall(String pkgId, boolean ignoreMissing) {
        if (env.getProperty(LAUNCHER_CHANGED_PROPERTY, "false").equals("true")) {
            System.exit(LAUNCHER_CHANGED_EXIT_CODE);
        }
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_INSTALL);
        cmdInfo.param = pkgId;
        try {
            LocalPackage pkg = getLocalPackage(pkgId);
            if (pkg != null && pkg.getPackageState().isInstalled()) {
                if (pkg.getVersion().isSnapshot()) {
                    log.info(String.format("Updating package %s...", pkg));
                    // First remove it to allow SNAPSHOT upgrade
                    pkgRemove(pkgId);
                    pkg = null;
                } else {
                    log.info(String.format("Package %s is already installed.",
                            pkg));
                    return pkg;
                }
            }
            if (pkg == null) {
                // We don't know this package, try to add it first
                pkg = pkgAdd(pkgId, ignoreMissing);
            }
            if (pkg == null) {
                // Nothing worked - can't find the package anywhere
                if (ignoreMissing) {
                    log.warn("Unable to install package: " + pkgId);
                    return null;
                } else {
                    throw new PackageException("Package not found: " + pkgId);
                }
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
            newPackageInfo(cmdInfo, pkg);
            return pkg;
        } catch (Exception e) {
            log.error(String.format("Failed to install package: %s (%s)",
                    pkgId, e.getMessage()));
            cmdInfo.exitCode = 1;
            cmdInfo.newMessage(e);
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
                        }
                    } else {
                        errorValue = 1;
                    }
                } else if (split.length == 1) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (doExecute) {
                            if ("init".equals(line)) {
                                if (!addDistributionPackages()) {
                                    errorValue = 1;
                                }
                            } else {
                                if (useResolver) {
                                    pkgsToInstall.add(line);
                                } else {
                                    pkgInstall(line);
                                }
                            }
                        } else {
                            if ("init".equals(line)) {
                                CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_INIT);
                                cmdInfo.pending = true;
                            } else {
                                CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_INSTALL);
                                cmdInfo.param = line;
                                cmdInfo.pending = true;
                            }
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
            } else {
                cset.log(true);
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
        List<String> packagesAlreadyDownloaded = new ArrayList<String>();
        Map<String, String> packagesToRemove = new HashMap<String, String>();
        for (String pkg : packagesToDownload) {
            try {
                LocalPackage localPackage = getLocalPackage(pkg);
                if (localPackage != null) {
                    if (localPackage.getPackageState().isInstalled()) {
                        log.error(String.format(
                                "Package %s is installed. Download skipped.",
                                pkg));
                        packagesAlreadyDownloaded.add(pkg);
                    } else if (localPackage.getVersion().isSnapshot()) {
                        log.info(String.format(
                                "Download of %s will replace the one already in local cache",
                                pkg));
                        packagesToRemove.put(localPackage.getId(), pkg);
                    } else {
                        log.info(String.format(
                                "Package %s is already in local cache", pkg));
                        packagesAlreadyDownloaded.add(pkg);
                    }
                }
            } catch (PackageException e) {
                log.error(
                        String.format(
                                "Looking for package %s in local cache raised an error. Aborting.",
                                pkg), e);
                return false;
            }
        }

        // First remove SNAPSHOT packages to replace
        for (String pkgToRemove : packagesToRemove.keySet()) {
            if (pkgRemove(pkgToRemove) == null) {
                log.error(String.format(
                        "Failed to remove %s. Download of %s skipped",
                        pkgToRemove, packagesToRemove.get(pkgToRemove)));
                packagesToDownload.remove(packagesToRemove.get(pkgToRemove));
            }
        }

        packagesToDownload.removeAll(packagesAlreadyDownloaded);
        if (packagesToDownload.isEmpty()) {
            return true;
        }
        List<DownloadingPackage> pkgs = new ArrayList<DownloadingPackage>();
        // Queue downloads
        log.info("Downloading " + packagesToDownload + "...");
        for (String pkg : packagesToDownload) {
            try {
                pkgs.add(getPackageManager().download(pkg));
            } catch (Exception e) {
                log.error("Download failed for " + pkg, e);
                return false;
            }
        }
        // Check progress
        boolean downloadOk = true;
        long startTime = new Date().getTime();
        long deltaTime = 0;
        do {
            List<DownloadingPackage> pkgsCompleted = new ArrayList<DownloadingPackage>();
            for (DownloadingPackage pkg : pkgs) {
                if (pkg.isCompleted()) {
                    pkgsCompleted.add(pkg);
                    CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_DOWNLOAD);
                    cmdInfo.param = pkg.getId();
                    // Digest check not correctly implemented
                    if (false && !pkg.isDigestOk()) {
                        downloadOk = false;
                        cmdInfo.exitCode = 1;
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR,
                                "Wrong digest for package " + pkg.getName());
                    } else if (pkg.getPackageState() == PackageState.DOWNLOADED) {
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_DEBUG,
                                "Downloaded " + pkg);
                    } else {
                        downloadOk = false;
                        cmdInfo.exitCode = 1;
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR,
                                String.format("Download failed for %s. %s",
                                        pkg, pkg.getErrorMessage()));
                    }
                }
            }
            pkgs.removeAll(pkgsCompleted);
            deltaTime = (new Date().getTime() - startTime) / 1000;
        } while (deltaTime < PACKAGES_DOWNLOAD_TIMEOUT_SECONDS
                && pkgs.size() > 0);
        // Timeout (not everything get downloaded)?
        if (pkgs.size() > 0) {
            downloadOk = false;
            log.error("Timeout while trying to download packages");
            for (DownloadingPackage pkg : pkgs) {
                CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
                cmdInfo.param = pkg.getId();
                cmdInfo.exitCode = 1;
                cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR,
                        "Download timeout for " + pkg);
            }
        }
        return downloadOk;
    }

    public boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) {
        return pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall,
                pkgsToRemove, true, false);
    }

    /**
     * @param keepExisting If false, the request will remove existing packages
     *            that are not part of the resolution
     * @since 5.9.2
     */
    public boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove, boolean keepExisting) {
        return pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall,
                pkgsToRemove, keepExisting, false);
    }

    /**
     * @param keepExisting If false, the request will remove existing packages
     *            that are not part of the resolution
     * @param ignoreMissing Do not error out on missing packages, just handle the rest
     * @since 5.9.2
     */
    public boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove, boolean keepExisting, boolean ignoreMissing) {
        try {
            boolean cmdOk = true;
            // Add local files
            cmdOk = pkgAdd(pkgsToAdd, ignoreMissing);
            // Build solver request
            List<String> solverInstall = new ArrayList<String>();
            List<String> solverRemove = new ArrayList<String>();
            List<String> solverUpgrade = new ArrayList<String>();
            if (pkgsToInstall != null) {
                // If install request is a file name, add to cache and get the
                // id
                List<String> namesOrIdsToInstall = new ArrayList<String>();
                for (String pkgToInstall : pkgsToInstall) {
                    if (isLocalPackageFile(pkgToInstall)) {
                        LocalPackage addedPkg = pkgAdd(pkgToInstall);
                        if (addedPkg != null) {
                            namesOrIdsToInstall.add(addedPkg.getId());
                        } else {
                            cmdOk = false;
                        }
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
                        if (isInstalledPackage(pkg.getName())) {
                            // The package is installed
                            solverUpgrade.add(pkgToInstall);
                        } else {
                            // The package isn't installed yet
                            solverInstall.add(pkgToInstall);
                        }
                    } else {
                        // This is a name (or a non-existing ID)
                        String id = getInstalledPackageIdFromName(pkgToInstall);
                        if (id != null) {
                            // The package is installed
                            solverUpgrade.add(pkgToInstall);
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
                if (ignoreMissing) {
                    Map<String, List<DownloadablePackage>> knownNames = getPackageManager().getAllPackagesByName();
                    List<String> solverInstallCopy = new ArrayList<String>(solverInstall);
                    for (String pkgToInstall : solverInstallCopy) {
                        if (!knownNames.containsKey(pkgToInstall)) {
                            log.warn("Unable to install pacakge: " + pkgToInstall);
                            solverInstall.remove(pkgToInstall);
                            requestPackages.remove(pkgToInstall);
                        }
                    }
                }
                String nonCompliantPkg = getPackageManager().getNonCompliant(
                        requestPackages, targetPlatform);
                if (nonCompliantPkg != null) {
                    requestPlatform = null;
                    if ("ask".equalsIgnoreCase(relax)) {
                        relax = readConsole(
                                "Package %s is not available on platform version %s.\n"
                                        + "Do you want to relax the constraint (yes/no)? [no] ",
                                "no", nonCompliantPkg, targetPlatform);
                    }

                    if (Boolean.parseBoolean(relax)) {
                        log.warn(String.format(
                                "Relax restriction to target platform %s because of package %s",
                                targetPlatform, nonCompliantPkg));
                    } else {
                        throw new PackageException(
                                String.format(
                                        "Package %s is not available on platform version %s (relax is not allowed)",
                                        nonCompliantPkg, targetPlatform));
                    }
                }

                log.debug("solverInstall: " + solverInstall);
                log.debug("solverRemove: " + solverRemove);
                log.debug("solverUpgrade: " + solverUpgrade);
                DependencyResolution resolution = getPackageManager().resolveDependencies(
                        solverInstall, solverRemove, solverUpgrade,
                        requestPlatform, allowSNAPSHOT, keepExisting);
                log.info(resolution);
                if (resolution.isFailed()) {
                    return false;
                }
                if (resolution.isEmpty()) {
                    pkgRemove(pkgsToRemove);
                    return cmdOk;
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
                List<String> packageIdsToUpgrade = resolution.getUpgradePackageIds();
                List<String> packageIdsToInstall = resolution.getOrderedPackageIdsToInstall();
                List<String> packagesIdsToReInstall = new ArrayList<String>();

                // Download remote packages
                if (!downloadPackages(resolution.getDownloadPackageIds())) {
                    log.error("Aborting packages change request");
                    return false;
                }

                // Uninstall
                if (!packageIdsToUpgrade.isEmpty()) {
                    // Add packages to upgrade to uninstall list
                    // Don't use IDs to avoid downgrade instead of uninstall
                    packageIdsToRemove.addAll(resolution.getLocalPackagesToUpgrade().keySet());
                    DependencyResolution uninstallResolution = getPackageManager().resolveDependencies(
                            null, packageIdsToRemove, null, requestPlatform,
                            allowSNAPSHOT, keepExisting);
                    log.debug("Sub-resolution (uninstall) "
                            + uninstallResolution);
                    if (uninstallResolution.isFailed()) {
                        return false;
                    }
                    List<String> newPackageIdsToRemove = uninstallResolution.getOrderedPackageIdsToRemove();
                    packagesIdsToReInstall = ListUtils.subtract(
                            newPackageIdsToRemove, packageIdsToRemove);
                    packagesIdsToReInstall.removeAll(packageIdsToUpgrade);
                    packageIdsToRemove = newPackageIdsToRemove;
                }
                if (!pkgUninstall(packageIdsToRemove)) {
                    return false;
                }

                // Install
                if (!packagesIdsToReInstall.isEmpty()) {
                    // Add list of packages uninstalled because of upgrade
                    packageIdsToInstall.addAll(packagesIdsToReInstall);
                    DependencyResolution installResolution = getPackageManager().resolveDependencies(
                            packageIdsToInstall, null, null, requestPlatform,
                            allowSNAPSHOT, keepExisting);
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
            return cmdOk;
        } catch (PackageException e) {
            log.error(e);
            log.debug(e, e);
            return false;
        }
    }

    /**
     * Installs a list of packages and uninstalls the rest (no dependency check)
     *
     * @since 5.9.2
     */
    public boolean pkgSet(List<String> pkgList) {
        return pkgSet(pkgList, false);
    }

    /**
     * Installs a list of packages and uninstalls the rest (no dependency check)
     *
     * @since 5.9.6
     */
    public boolean pkgSet(List<String> pkgList, boolean ignoreMissing) {
        boolean cmdOK = true;
        cmdOK = cmdOK && pkgInstall(pkgList, ignoreMissing);
        List<DownloadablePackage> installedPkgs = getPackageManager().listInstalledPackages();
        List<String> pkgsToUninstall = new ArrayList<String>();
        for (DownloadablePackage pkg : installedPkgs) {
            if ((!pkgList.contains(pkg.getName()))
                    && (!pkgList.contains(pkg.getId()))) {
                pkgsToUninstall.add(pkg.getId());
            }
        }
        if (pkgsToUninstall.size() != 0) {
            cmdOK = cmdOK && pkgUninstall(pkgsToUninstall);
        }
        return cmdOK;
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

    /*
     * Helper for adding a new PackageInfo initialized with informations
     * gathered from the given package. It is not put into CommandInfo to avoid
     * adding a dependency on Connect Client
     */
    private PackageInfo newPackageInfo(CommandInfo cmdInfo, Package pkg) {
        PackageInfo packageInfo = new PackageInfo(pkg);
        cmdInfo.packages.add(packageInfo);
        return packageInfo;
    }

    /**
     * @param packages List of packages identified by their ID, name or local
     *            filename.
     * @since 5.7
     */
    public boolean pkgShow(List<String> packages) {
        boolean cmdOk = true;
        if (packages == null || packages.isEmpty()) {
            return cmdOk;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("****************************************");
        for (String pkg : packages) {
            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_SHOW);
            cmdInfo.param = pkg;
            try {
                PackageInfo packageInfo = newPackageInfo(cmdInfo,
                        findPackage(pkg));
                sb.append("\nPackage: " + packageInfo.id);
                sb.append("\nState: " + packageInfo.state);
                sb.append("\nVersion: " + packageInfo.version);
                sb.append("\nName: " + packageInfo.name);
                sb.append("\nType: " + packageInfo.type);
                sb.append("\nVisibility: " + packageInfo.visibility);
                if (packageInfo.state == PackageState.REMOTE
                        && packageInfo.type != PackageType.STUDIO
                        && packageInfo.visibility != PackageVisibility.PUBLIC
                        && !LogicalInstanceIdentifier.isRegistered()) {
                    sb.append(" (registration required)");
                }
                sb.append("\nTarget platforms: "
                        + ArrayUtils.toString(packageInfo.targetPlatforms));
                appendIfNotEmpty(sb, "\nVendor: ", packageInfo.vendor);
                sb.append("\nSupports hot-reload: "
                        + packageInfo.supportsHotReload);
                sb.append("\nSupported: " + packageInfo.supported);
                sb.append("\nProduction state: " + packageInfo.productionState);
                sb.append("\nValidation state: " + packageInfo.validationState);
                appendIfNotEmpty(sb, "\nProvides: ", packageInfo.provides);
                appendIfNotEmpty(sb, "\nDepends: ", packageInfo.dependencies);
                appendIfNotEmpty(sb, "\nConflicts: ", packageInfo.conflicts);
                appendIfNotEmpty(sb, "\nTitle: ", packageInfo.title);
                appendIfNotEmpty(sb, "\nDescription: ", packageInfo.description);
                appendIfNotEmpty(sb, "\nHomepage: ", packageInfo.homePage);
                appendIfNotEmpty(sb, "\nLicense: ", packageInfo.licenseType);
                appendIfNotEmpty(sb, "\nLicense URL: ", packageInfo.licenseUrl);
                sb.append("\n****************************************");
            } catch (PackageException e) {
                cmdOk = false;
                cmdInfo.exitCode = 1;
                cmdInfo.newMessage(e);
            }
        }
        log.info(sb.toString());
        return cmdOk;
    }

    private void appendIfNotEmpty(StringBuilder sb, String label, Object[] array) {
        if (ArrayUtils.isNotEmpty(array)) {
            sb.append(label + ArrayUtils.toString(array));
        }
    }

    private void appendIfNotEmpty(StringBuilder sb, String label, String value) {
        if (StringUtils.isNotEmpty(value)) {
            sb.append(label + value);
        }
    }

    /**
     * Looks for a package. First look if it's a local ZIP file, second if it's
     * a local package and finally if it's a remote package.
     *
     * @param pkg A ZIP filename or file path, or package ID or a package name.
     * @return The first package found matching the given string.
     * @throws PackageException If no package is found or if an issue occurred
     *             while searching.
     *
     * @see PackageDefinition
     * @see LocalPackage
     * @see DownloadablePackage
     */
    protected Package findPackage(String pkg) throws PackageException {
        // Is it a local ZIP file?
        File localPackageFile = getLocalPackageFile(pkg);
        if (localPackageFile != null) {
            return service.loadPackageFromZip(localPackageFile);
        }

        // Is it a local package ID or name?
        LocalPackage localPackage = getLocalPackage(pkg);
        if (localPackage != null) {
            return localPackage;
        }

        // Is it a remote package ID or name?
        String pkgId = getRemotePackageId(pkg);
        if (pkgId != null) {
            return getPackageManager().findPackageById(pkgId);
        }

        throw new PackageException(
                "Could not find a remote or local (relative to "
                        + "current directory or to NUXEO_HOME) "
                        + "package with name or ID " + pkg);
    }

    /**
     * @since 5.9.1
     */
    public void setAllowSNAPSHOT(boolean allow) {
        CUDFHelper.defaultAllowSNAPSHOT = allow;
        allowSNAPSHOT = allow;
    }

}
