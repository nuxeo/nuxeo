/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *     Mathieu Guillaume
 *     Yannis JULIENNE
 *
 */

package org.nuxeo.launcher.connect;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.ConnectServerError;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.InvalidCLID;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.dependencies.CUDFHelper;
import org.nuxeo.connect.packages.dependencies.DependencyResolution;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUtils;
import org.nuxeo.connect.update.PackageVisibility;
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

    public static final String PARAM_MP_DIR = "nuxeo.distribution.marketplace.dir";

    public static final String DISTRIBUTION_MP_DIR_DEFAULT = "setupWizardDownloads";

    public static final String PACKAGES_XML = "packages.xml";

    public static final String[] POSITIVE_ANSWERS = { "true", "yes", "y" };

    protected static final String LAUNCHER_CHANGED_PROPERTY = "launcher.changed";

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

    private Path pendingFile;

    public static final String OPTION_ACCEPT_DEFAULT = "ask";

    public ConnectBroker(Environment env) throws IOException, PackageException {
        this.env = env;
        service = new StandaloneUpdateService(env);
        service.initialize();
        cbHolder = new StandaloneCallbackHolder(env, service);
        NuxeoConnectClient.setCallBackHolder(cbHolder);
        targetPlatform = env.getProperty(Environment.DISTRIBUTION_NAME) + "-"
                + env.getProperty(Environment.DISTRIBUTION_VERSION);
        distributionMPDir = env.getProperty(PARAM_MP_DIR, DISTRIBUTION_MP_DIR_DEFAULT);
    }

    /**
     * @since 10.2
     */
    public Path getPendingFile() {
        return pendingFile;
    }

    /**
     * @since 10.2
     */
    public void setPendingFile(Path pendingFile) {
        this.pendingFile = pendingFile;
    }

    public String getCLID() throws NoCLID {
        return LogicalInstanceIdentifier.instance().getCLID();
    }

    /**
     * @throws NoCLID if the CLID is absent or invalid
     * @since 6.0
     */
    public void setCLID(String file) throws NoCLID {
        try {
            LogicalInstanceIdentifier.load(file);
        } catch (IOException | InvalidCLID e) {
            throw new NoCLID("can not load CLID", e);
        }
    }

    /**
     * @since 8.10-HF15
     */
    public void saveCLID() throws IOException, NoCLID {
        LogicalInstanceIdentifier.instance().save();
    }

    public StandaloneUpdateService getUpdateService() {
        return service;
    }

    public PackageManager getPackageManager() {
        return NuxeoConnectClient.getPackageManager(targetPlatform);
    }

    public void refreshCache() {
        getPackageManager().flushCache();
        getPackageManager().listAllPackages();
    }

    public CommandSetInfo getCommandSet() {
        return cset;
    }

    protected LocalPackage getInstalledPackageByName(String pkgName) {
        try {
            return service.getPersistence().getActivePackage(pkgName);
        } catch (PackageException e) {
            log.error(e);
            return null;
        }
    }

    protected boolean isInstalledPackage(String pkgName) {
        try {
            return service.getPersistence().getActivePackageId(pkgName) != null;
        } catch (PackageException e) {
            log.error("Error checking installation of package " + pkgName, e);
            return false;
        }
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
        return PackageUtils.isValidPackageId(pkgId) && getPackageManager().getRemotePackage(pkgId) != null;
    }

    protected String getBestIdForNameInList(String pkgName, List<? extends Package> pkgList) {
        String foundId = null;
        SortedMap<Version, String> foundPkgs = new TreeMap<>();
        SortedMap<Version, String> matchingPkgs = new TreeMap<>();
        for (Package pkg : pkgList) {
            if (pkg.getName().equals(pkgName)) {
                foundPkgs.put(pkg.getVersion(), pkg.getId());
                if (Arrays.asList(pkg.getTargetPlatforms()).contains(targetPlatform)) {
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
        List<String> foundIds = new ArrayList<>();
        for (Package pkg : getPkgList()) {
            if (pkg.getName().equals(pkgName)) {
                foundIds.add(pkg.getId());
            }
        }
        return foundIds;
    }

    protected String getInstalledPackageIdFromName(String pkgName) {
        List<LocalPackage> localPackages = getPkgList();
        List<LocalPackage> installedPackages = new ArrayList<>();
        for (LocalPackage pkg : localPackages) {
            if (pkg.getPackageState().isInstalled()) {
                installedPackages.add(pkg);
            }
        }
        return getBestIdForNameInList(pkgName, installedPackages);
    }

    protected String getRemotePackageIdFromName(String pkgName) {
        return getBestIdForNameInList(pkgName, getPackageManager().findRemotePackages(pkgName));
    }

    /**
     * Looks for a remote package from its name or id
     *
     * @return the remote package Id; null if not found
     * @since 5.7
     */
    protected String getRemotePackageId(String pkgNameOrId) {
        String pkgId;
        if (isRemotePackageId(pkgNameOrId)) {
            pkgId = pkgNameOrId;
        } else {
            pkgId = getRemotePackageIdFromName(pkgNameOrId);
        }
        return pkgId;
    }

    /**
     * Looks for a local package from its name or id
     *
     * @since 5.7
     * @return the local package Id; null if not found
     */
    protected LocalPackage getLocalPackage(String pkgIdOrName) throws PackageException {
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

    /**
     * Load package definition from a local file or directory and get package Id from it.
     *
     * @return null the package definition cannot be loaded for any reason.
     * @since 8.4
     */
    protected String getLocalPackageFileId(File pkgFile) {
        PackageDefinition packageDefinition;
        try {
            if (pkgFile.isFile()) {
                packageDefinition = service.loadPackageFromZip(pkgFile);
            } else if (pkgFile.isDirectory()) {
                File manifest = new File(pkgFile, LocalPackage.MANIFEST);
                packageDefinition = service.loadPackage(manifest);
            } else {
                throw new PackageException("Unknown file type (not a file and not a directory) for " + pkgFile);
            }
        } catch (PackageException e) {
            log.error("Error trying to load package id from " + pkgFile, e);
            return null;
        }
        return packageDefinition == null ? null : packageDefinition.getId();
    }

    protected boolean isLocalPackageFile(String pkgFile) {
        return (getLocalPackageFile(pkgFile) != null);
    }

    protected List<String> getDistributionFilenames() {
        File distributionMPFile = new File(distributionMPDir, PACKAGES_XML);
        List<String> md5Filenames = new ArrayList<>();
        // Try to get md5 files from packages.xml
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.parse(distributionMPFile);
            XPathFactory xpFactory = XPathFactory.newInstance();
            XPath xpath = xpFactory.newXPath();
            XPathExpression expr = xpath.compile("//package/@md5");
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                String md5 = nodes.item(i).getNodeValue();
                if ((md5 != null) && (md5.length() > 0)) {
                    md5Filenames.add(md5);
                }
            }
        } catch (Exception e) {
            // Parsing failed - return empty list
            log.error("Failed parsing " + distributionMPFile, e);
            return new ArrayList<>();
        }
        return md5Filenames;
    }

    protected Map<String, PackageDefinition> getDistributionDefinitions(List<String> md5Filenames) {
        Map<String, PackageDefinition> allDefinitions = new HashMap<>();
        if (md5Filenames == null) {
            return allDefinitions;
        }
        for (String md5Filename : md5Filenames) {
            File md5File = new File(distributionMPDir, md5Filename);
            if (!md5File.exists()) {
                // distribution file has been deleted
                continue;
            }
            try (ZipFile zipFile = new ZipFile(md5File)) {
                ZipEntry zipEntry = zipFile.getEntry("package.xml");
                PackageDefinition pd;
                try (InputStream in = zipFile.getInputStream(zipEntry)) {
                    pd = NuxeoConnectClient.getPackageUpdateService().loadPackage(in);
                }
                allDefinitions.put(md5Filename, pd);
            } catch (IOException e) {
                log.warn("Could not read file " + md5File, e);
                continue;
            } catch (PackageException e) {
                log.error("Could not read package description", e);
                continue;
            }
        }
        return allDefinitions;
    }

    protected boolean addDistributionPackage(String md5) {
        boolean ret = true;
        File distributionFile = new File(distributionMPDir, md5);
        if (distributionFile.exists()) {
            try {
                ret = pkgAdd(distributionFile.getCanonicalPath(), false) != null;
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
        Map<String, LocalPackage> localPackagesById = new HashMap<>();
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
        pkgList(getPackageManager().listAllPackages());
    }

    public void pkgList(List<? extends Package> packagesList) {
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_LIST);
        try {
            if (packagesList.isEmpty()) {
                log.info("None");
            } else {
                getPackageManager().sort(packagesList);
                StringBuilder sb = new StringBuilder();
                for (Package pkg : packagesList) {
                    newPackageInfo(cmdInfo, pkg);
                    PackageState packageState = pkg.getPackageState();
                    String packageDescription = packageState.getLabel();
                    packageDescription = String.format("%6s %11s\t", pkg.getType(), packageDescription);
                    if (packageState == PackageState.REMOTE && pkg.getType() != PackageType.STUDIO
                            && pkg.getVisibility() != PackageVisibility.PUBLIC
                            && !LogicalInstanceIdentifier.isRegistered()) {
                        packageDescription += "Registration required for ";
                    }
                    packageDescription += String.format("%s (id: %s)\n", pkg.getName(), pkg.getId());
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
            throw new PackageException(
                    "Failed to validate package " + task.getPackage().getId() + " -> " + validationStatus.getErrors());
        }
        if (validationStatus.hasWarnings()) {
            log.warn("Got warnings on package validation " + task.getPackage().getId() + " -> "
                    + validationStatus.getWarnings());
        }
        task.run(null);
    }

    public boolean pkgReset() {
        CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_RESET);
        if ("ask".equalsIgnoreCase(accept)) {
            accept = readConsole(
                    "The reset will erase the Nuxeo Packages history.\n" + "Do you want to continue (yes/no)? [yes] ",
                    "yes");
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
                FileUtils.deleteDirectory(localPackage.getData().getEntry(LocalPackage.BACKUP_DIR));
                newPackageInfo(cmdInfo, localPackage);
            }
            service.getRegistry().delete();
            FileUtils.deleteDirectory(service.getBackupDir());
        } catch (PackageException | IOException e) {
            log.error(e);
            cmdInfo.exitCode = 1;
        }
        return cmdInfo.exitCode == 0;
    }

    public boolean pkgPurge() throws PackageException {
        List<String> localNames = new ArrayList<>();
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
        return pkgRequest(null, null, null, localNames, true, false);
    }

    /**
     * Uninstall a list of packages. If the list contains a package name (versus an ID), only the considered as best
     * matching package is uninstalled.
     *
     * @param packageIdsToRemove The list can contain package IDs and names
     * @see #pkgUninstall(String)
     */
    public boolean pkgUninstall(List<String> packageIdsToRemove) {
        log.debug("Uninstalling: " + packageIdsToRemove);
        Queue<String> remaining = new LinkedList<>(packageIdsToRemove);
        while (!remaining.isEmpty()) {
            String pkgId = remaining.poll();
            if (pkgUninstall(pkgId) == null) {
                log.error("Unable to uninstall " + pkgId);
                return false;
            }
            if (isRestartRequired()) {
                remaining.forEach(pkg -> persistCommand(CommandInfo.CMD_UNINSTALL + " " + pkg));
                throw new LauncherRestartException();
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
     * Remove a list of packages from cache. If the list contains a package name (versus an ID), all matching packages
     * are removed.
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
                    allIds = new ArrayList<>();
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
     * Remove a package from cache. If it was installed, the package is uninstalled then removed.
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
                throw new PackageException("Can only remove packages in DOWNLOADED, INSTALLED or STARTED state");
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
     * Add a list of packages into the cache, downloading them if needed and possible.
     *
     * @return true if command succeeded
     * @see #pkgAdd(List, boolean)
     * @see #pkgAdd(String, boolean)
     * @deprecated Since 7.10. Use a method with an explicit value for {@code ignoreMissing}.
     */
    @Deprecated
    public boolean pkgAdd(List<String> pkgsToAdd) {
        return pkgAdd(pkgsToAdd, false);
    }

    /**
     * Add a list of packages into the cache, downloading them if needed and possible.
     *
     * @since 6.0
     * @return true if command succeeded
     * @see #pkgAdd(String, boolean)
     */
    public boolean pkgAdd(List<String> pkgsToAdd, boolean ignoreMissing) {
        boolean cmdOk = true;
        if (pkgsToAdd == null || pkgsToAdd.isEmpty()) {
            return cmdOk;
        }
        List<String> pkgIdsToDownload = new ArrayList<>();
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
                            cmdInfo.newMessage(SimpleLog.LOG_LEVEL_INFO, "Could not add package.");
                        } else {
                            throw new PackageException("Could not find a remote or local (relative to "
                                    + "current directory or to NUXEO_HOME) " + "package with name or ID " + pkgToAdd);
                        }
                    } else {
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_INFO, "Waiting for download...");
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
     * @return The added LocalPackage or null if failed
     * @see #pkgAdd(List, boolean)
     * @see #pkgAdd(String, boolean)
     * @deprecated Since 7.10. Use a method with an explicit value for {@code ignoreMissing}.
     */
    @Deprecated
    public LocalPackage pkgAdd(String packageFileName) {
        return pkgAdd(packageFileName, false);
    }

    /**
     * Add a package file into the cache
     *
     * @since 6.0
     * @return The added LocalPackage or null if failed
     * @see #pkgAdd(List, boolean)
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
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_INFO, "Could not add package.");
                        return null;
                    } else {
                        throw new PackageException("Could not find a remote or local (relative to "
                                + "current directory or to NUXEO_HOME) " + "package with name or ID "
                                + packageFileName);
                    }
                } else if (!downloadPackages(Collections.singletonList(pkgId))) {
                    throw new PackageException("Could not download package " + pkgId);
                }
                pkg = service.getPackage(pkgId);
                if (pkg == null) {
                    throw new PackageException("Could not find downloaded package in cache " + pkgId);
                }
            } else {
                pkg = service.addPackage(fileToAdd);
                log.info("Added " + packageFileName);
            }
            newPackageInfo(cmdInfo, pkg);
        } catch (PackageException e) {
            cmdInfo.exitCode = 1;
            cmdInfo.newMessage(e);
        }
        return pkg;
    }

    /**
     * Install a list of local packages. If the list contains a package name (versus an ID), only the considered as best
     * matching package is installed.
     *
     * @param packageIdsToInstall The list can contain package IDs and names
     * @see #pkgInstall(List, boolean)
     * @see #pkgInstall(String, boolean)
     * @deprecated Since 7.10. Use a method with an explicit value for {@code ignoreMissing}.
     */
    @Deprecated
    public boolean pkgInstall(List<String> packageIdsToInstall) {
        return pkgInstall(packageIdsToInstall, false);
    }

    /**
     * Install a list of local packages. If the list contains a package name (versus an ID), only the considered as best
     * matching package is installed.
     *
     * @since 6.0
     * @param packageIdsToInstall The list can contain package IDs and names
     * @param ignoreMissing If true, doesn't throw an exception on unknown packages
     * @see #pkgInstall(String, boolean)
     */
    public boolean pkgInstall(List<String> packageIdsToInstall, boolean ignoreMissing) {
        log.debug("Installing: " + packageIdsToInstall);
        Queue<String> remaining = new LinkedList<>(packageIdsToInstall);
        while (!remaining.isEmpty()) {
            String pkgId = remaining.poll();
            if (pkgInstall(pkgId, ignoreMissing) == null && !ignoreMissing) {
                return false;
            }
            if (isRestartRequired()) {
                remaining.forEach(pkg -> persistCommand(CommandInfo.CMD_INSTALL + " " + pkg));
                throw new LauncherRestartException();
            }
        }
        return true;
    }

    /**
     * Persists the pending package operation into file system. It's useful when Nuxeo launcher is about to exit. Empty
     * command line won't be persisted.
     * <p>
     * The given command will be appended as a new line into target file {@link #pendingFile}. NOTE: the command line
     * options are not serialized. Therefore, they should be provided by nuxeoctl again after launcher's restart.
     *
     * @param command command to persist (appended as a new line)
     * @throws IllegalStateException if any exception occurs
     * @see #pendingFile
     * @since 10.2
     */
    protected void persistCommand(String command) {
        if (command.isEmpty()) {
            return;
        }
        try {
            Files.write(pendingFile, Collections.singletonList(command), StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write to file " + pendingFile, e);
        }
    }

    /**
     * Install a local package.
     *
     * @param pkgId Package ID or Name
     * @return The installed LocalPackage or null if failed
     * @see #pkgInstall(List, boolean)
     * @see #pkgInstall(String, boolean)
     * @deprecated Since 7.10. Use a method with an explicit value for {@code ignoreMissing}.
     */
    @Deprecated
    public LocalPackage pkgInstall(String pkgId) {
        return pkgInstall(pkgId, false);
    }

    /**
     * @since 10.2
     */
    protected boolean isRestartRequired() {
        return "true".equals(env.getProperty(LAUNCHER_CHANGED_PROPERTY));
    }

    /**
     * Install a local package.
     *
     * @since 6.0
     * @param pkgId Package ID or Name
     * @param ignoreMissing If true, doesn't throw an exception on unknown packages
     * @return The installed LocalPackage or null if failed
     * @see #pkgInstall(List, boolean)
     */
    public LocalPackage pkgInstall(String pkgId, boolean ignoreMissing) {
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
                    log.info(String.format("Package %s is already installed.", pkg));
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
        } catch (PackageException e) {
            log.error(String.format("Failed to install package: %s (%s)", pkgId, e.getMessage()));
            log.debug(e, e);
            cmdInfo.exitCode = 1;
            cmdInfo.newMessage(e);
            return null;
        }
    }

    public boolean listPending(File commandsFile) {
        return executePending(commandsFile, false, false, false);
    }

    /**
     * @since 5.6
     * @param commandsFile File containing the commands to execute
     * @param doExecute Whether to execute or list the actions
     * @param useResolver Whether to use full resolution or just execute individual actions
     */
    public boolean executePending(File commandsFile, boolean doExecute, boolean useResolver, boolean ignoreMissing) {
        int errorValue = 0;
        if (!commandsFile.isFile()) {
            return false;
        }
        List<String> pkgsToAdd = new ArrayList<>();
        List<String> pkgsToInstall = new ArrayList<>();
        List<String> pkgsToUninstall = new ArrayList<>();
        List<String> pkgsToRemove = new ArrayList<>();

        Path commandsPath = commandsFile.toPath();
        pendingFile = commandsPath;
        Path backup = commandsPath.resolveSibling(commandsFile.getName() + ".bak");
        try {
            Queue<String> remainingCmds = new LinkedList<>(Files.readAllLines(commandsFile.toPath()));
            if (doExecute) {
                // backup the commandsFile before any real execution
                Files.move(commandsPath, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            while (!remainingCmds.isEmpty()) {
                String line = remainingCmds.poll().trim();
                String[] split = line.split("\\s+", 2);
                if (split.length == 2) {
                    if (split[0].equals(CommandInfo.CMD_INSTALL)) {
                        if (doExecute) {
                            if (useResolver) {
                                pkgsToInstall.add(split[1]);
                            } else {
                                pkgInstall(split[1], ignoreMissing);
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
                                pkgAdd(split[1], ignoreMissing);
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
                                    pkgInstall(line, ignoreMissing);
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
                    log.error("Error processing pending package/command: " + line);
                }
                if (doExecute && !useResolver && isRestartRequired()) {
                    remainingCmds.forEach(this::persistCommand);
                    throw new LauncherRestartException();
                }
            }
            if (doExecute) {
                if (useResolver) {
                    String oldAccept = accept;
                    String oldRelax = relax;
                    accept = "true";
                    if ("ask".equalsIgnoreCase(relax)) {
                        log.info("Relax mode changed from 'ask' to 'false' for executing the pending actions.");
                        relax = "false";
                    }
                    boolean success = pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall, pkgsToRemove, true,
                            ignoreMissing);
                    accept = oldAccept;
                    relax = oldRelax;
                    if (!success) {
                        errorValue = 2;
                    }
                }
                if (errorValue != 0) {
                    log.error("Pending actions execution failed. The commands file has been moved to: " + backup);
                }
            } else {
                cset.log(true);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e.getCause());
        }
        return errorValue == 0;
    }

    @SuppressWarnings("unused")
    protected boolean downloadPackages(List<String> packagesToDownload) {
        boolean isRegistered = LogicalInstanceIdentifier.isRegistered();
        List<String> packagesAlreadyDownloaded = new ArrayList<>();
        for (String pkg : packagesToDownload) {
            LocalPackage localPackage;
            try {
                localPackage = getLocalPackage(pkg);
            } catch (PackageException e) {
                log.error(String.format("Looking for package '%s' in local cache raised an error. Aborting.", pkg), e);
                return false;
            }
            if (localPackage == null) {
                continue;
            }
            if (localPackage.getPackageState().isInstalled()) {
                log.error(String.format("Package '%s' is installed. Download skipped.", pkg));
                packagesAlreadyDownloaded.add(pkg);
            } else if (localPackage.getVersion().isSnapshot()) {
                if (localPackage.getVisibility() != PackageVisibility.PUBLIC && !isRegistered) {
                    log.info(String.format("Update of '%s' requires being registered.", pkg));
                    packagesAlreadyDownloaded.add(pkg);
                } else {
                    log.info(String.format("Download of '%s' will replace the one already in local cache.", pkg));
                }
            } else {
                log.info(String.format("Package '%s' is already in local cache.", pkg));
                packagesAlreadyDownloaded.add(pkg);
            }
        }

        packagesToDownload.removeAll(packagesAlreadyDownloaded);
        if (packagesToDownload.isEmpty()) {
            return true;
        }
        // Queue downloads
        log.info("Downloading " + packagesToDownload + "...");
        boolean downloadOk = true;
        List<DownloadingPackage> pkgs = new ArrayList<>();
        for (String pkg : packagesToDownload) {
            CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_DOWNLOAD);
            cmdInfo.param = pkg;

            // Check registration and package visibility
            DownloadablePackage downloadablePkg = getPackageManager().findRemotePackageById(pkg);
            if (downloadablePkg != null && downloadablePkg.getVisibility() != PackageVisibility.PUBLIC
                    && !isRegistered) {
                downloadOk = false;
                cmdInfo.exitCode = 1;
                cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR, "Registration required.");
                continue;
            }

            // Download
            try {
                DownloadingPackage download = getPackageManager().download(pkg);
                if (download != null) {
                    pkgs.add(download);
                    cmdInfo.param = download.getId();
                    cmdInfo.newMessage(SimpleLog.LOG_LEVEL_DEBUG, "Downloading...");
                } else {
                    downloadOk = false;
                    cmdInfo.exitCode = 1;
                    cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR, "Download failed (not found).");
                }
            } catch (ConnectServerError e) {
                log.debug(e, e);
                downloadOk = false;
                cmdInfo.exitCode = 1;
                cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR, "Download failed: " + e.getMessage());
            }
        }
        // Check and display progress
        final String progress = "|/-\\";
        int x = 0;
        boolean stopDownload = false;
        do {
            System.out.print(progress.charAt(x++ % progress.length()) + "\r");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            List<DownloadingPackage> pkgsCompleted = new ArrayList<>();
            for (DownloadingPackage pkg : pkgs) {
                if (pkg.isCompleted()) {
                    pkgsCompleted.add(pkg);
                    CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_DOWNLOAD);
                    cmdInfo.param = pkg.getId();
                    // Digest check not correctly implemented
                    if (false && !pkg.isDigestOk()) {
                        downloadOk = false;
                        cmdInfo.exitCode = 1;
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR, "Wrong digest.");
                    } else if (pkg.getPackageState() == PackageState.DOWNLOADED) {
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_DEBUG, "Downloaded.");
                    } else {
                        downloadOk = false;
                        cmdInfo.exitCode = 1;
                        cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR, "Download failed: " + pkg.getErrorMessage());
                        if (pkg.isServerError()) { // Wasted effort to continue other downloads
                            stopDownload = true;
                        }
                    }
                }
            }
            pkgs.removeAll(pkgsCompleted);
        } while (!stopDownload && pkgs.size() > 0);
        if (pkgs.size() > 0) {
            downloadOk = false;
            log.error("Packages download was interrupted");
            for (DownloadingPackage pkg : pkgs) {
                CommandInfo cmdInfo = cset.newCommandInfo(CommandInfo.CMD_ADD);
                cmdInfo.param = pkg.getId();
                cmdInfo.exitCode = 1;
                cmdInfo.newMessage(SimpleLog.LOG_LEVEL_ERROR, "Download interrupted.");
            }
        }
        return downloadOk;
    }

    /**
     * @deprecated Since 7.10. Use {@link #pkgRequest(List, List, List, List, boolean, boolean)} instead.
     */
    @Deprecated
    public boolean pkgRequest(List<String> pkgsToAdd, List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) {
        return pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall, pkgsToRemove, true, false);
    }

    /**
     * @param keepExisting If false, the request will remove existing packages that are not part of the resolution
     * @since 5.9.2
     * @deprecated Since 7.10. Use {@link #pkgRequest(List, List, List, List, boolean, boolean)} instead.
     */
    @Deprecated
    public boolean pkgRequest(List<String> pkgsToAdd, List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove, boolean keepExisting) {
        return pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall, pkgsToRemove, keepExisting, false);
    }

    /**
     * @param keepExisting If false, the request will remove existing packages that are not part of the resolution
     * @param ignoreMissing Do not error out on missing packages, just handle the rest
     * @since 5.9.2
     */
    public boolean pkgRequest(List<String> pkgsToAdd, List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove, boolean keepExisting, boolean ignoreMissing) {
        // default is install mode
        return pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall, pkgsToRemove, keepExisting, ignoreMissing, false);
    }

    /**
     * @param keepExisting If false, the request will remove existing packages that are not part of the resolution
     * @param ignoreMissing Do not error out on missing packages, just handle the rest
     * @param upgradeMode If true, all packages will be upgraded to their last compliant version
     * @throws LauncherRestartException if launcher is required to restart
     * @since 8.4
     */
    public boolean pkgRequest(List<String> pkgsToAdd, List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove, boolean keepExisting, boolean ignoreMissing, boolean upgradeMode) {
        try {
            boolean cmdOk;
            // Add local files
            cmdOk = pkgAdd(pkgsToAdd, ignoreMissing);
            // Build solver request
            List<String> solverInstall = new ArrayList<>();
            List<String> solverRemove = new ArrayList<>();
            List<String> solverUpgrade = new ArrayList<>();
            // Potential local cache snapshots to replace
            Set<String> localSnapshotsToMaybeReplace = new HashSet<>();
            if (pkgsToInstall != null) {
                List<String> namesOrIdsToInstall = new ArrayList<>();
                Set<String> localSnapshotsToUninstall = new HashSet<>();
                Set<String> localSnapshotsToReplace = new HashSet<>();
                cmdOk = checkLocalPackagesAndAddLocalFiles(pkgsToInstall, upgradeMode, ignoreMissing,
                        namesOrIdsToInstall, localSnapshotsToUninstall, localSnapshotsToReplace,
                        localSnapshotsToMaybeReplace);

                // Replace snapshots to install but already in cache (requested by id or filename)
                if (CollectionUtils.isNotEmpty(localSnapshotsToReplace)) {
                    log.info(String.format(
                            "The following SNAPSHOT package(s) will be replaced in local cache (if available): %s",
                            localSnapshotsToReplace));
                    String initialAccept = accept;
                    if ("ask".equalsIgnoreCase(accept)) {
                        accept = readConsole("Do you want to continue (yes/no)? [yes] ", "yes");
                    }
                    if (!Boolean.parseBoolean(accept)) {
                        log.warn("Exit");
                        return false;
                    }
                    accept = initialAccept;
                    for (String pkgId : localSnapshotsToUninstall) {
                        LocalPackage uninstalledPkg = pkgUninstall(pkgId);
                        if (uninstalledPkg == null) {
                            cmdOk = false;
                        }
                    }
                    for (String pkgIdOrFileName : localSnapshotsToReplace) {
                        if (isLocalPackageFile(pkgIdOrFileName) || isRemotePackageId(pkgIdOrFileName)) {
                            LocalPackage addedPkg = pkgAdd(pkgIdOrFileName, ignoreMissing);
                            if (addedPkg == null) {
                                cmdOk = false;
                            }
                        } else {
                            log.info(String.format(
                                    "The SNAPSHOT package %s is not available remotely, local cache will be used.",
                                    pkgIdOrFileName));
                        }
                    }
                }

                if (upgradeMode) {
                    solverUpgrade.addAll(namesOrIdsToInstall);
                } else {
                    solverInstall.addAll(namesOrIdsToInstall);
                }
            }
            if (pkgsToUninstall != null) {
                solverRemove.addAll(pkgsToUninstall);
            }
            if (pkgsToRemove != null) {
                // Add packages to remove to uninstall list
                solverRemove.addAll(pkgsToRemove);
            }
            if ((solverInstall.size() != 0) || (solverRemove.size() != 0) || (solverUpgrade.size() != 0)) {
                // Check whether we need to relax restriction to targetPlatform
                String requestPlatform = targetPlatform;
                List<String> requestPackages = new ArrayList<>();
                requestPackages.addAll(solverInstall);
                requestPackages.addAll(solverRemove);
                requestPackages.addAll(solverUpgrade);
                if (ignoreMissing) {
                    // Remove unknown packages from the list
                    Map<String, List<DownloadablePackage>> knownNames = getPackageManager().getAllPackagesByName();
                    List<String> solverInstallCopy = new ArrayList<>(solverInstall);
                    for (String pkgToInstall : solverInstallCopy) {
                        if (!knownNames.containsKey(pkgToInstall)) {
                            log.warn("Unable to install unknown package: " + pkgToInstall);
                            solverInstall.remove(pkgToInstall);
                            requestPackages.remove(pkgToInstall);
                        }
                    }
                }
                List<String> nonCompliantPkg = getPackageManager().getNonCompliantList(requestPackages, targetPlatform);
                if (nonCompliantPkg.size() > 0) {
                    requestPlatform = null;
                    if ("ask".equalsIgnoreCase(relax)) {
                        relax = readConsole(
                                "Package(s) %s not available on platform version %s.\n"
                                        + "Do you want to relax the constraint (yes/no)? [no] ",
                                "no", StringUtils.join(nonCompliantPkg, ", "), targetPlatform);
                    }

                    if (Boolean.parseBoolean(relax)) {
                        log.warn(String.format("Relax restriction to target platform %s because of package(s) %s",
                                targetPlatform, StringUtils.join(nonCompliantPkg, ", ")));
                    } else {
                        if (ignoreMissing) {
                            for (String pkgToInstall : nonCompliantPkg) {
                                log.warn("Unable to install package: " + pkgToInstall);
                                solverInstall.remove(pkgToInstall);
                            }
                        } else {
                            throw new PackageException(String.format(
                                    "Package(s) %s not available on platform version %s (relax is not allowed)",
                                    StringUtils.join(nonCompliantPkg, ", "), targetPlatform));
                        }
                    }
                }

                log.debug("solverInstall: " + solverInstall);
                log.debug("solverRemove: " + solverRemove);
                log.debug("solverUpgrade: " + solverUpgrade);
                DependencyResolution resolution = getPackageManager().resolveDependencies(solverInstall, solverRemove,
                        solverUpgrade, requestPlatform, allowSNAPSHOT, keepExisting);
                log.info(resolution);
                if (resolution.isFailed()) {
                    return false;
                }
                if (resolution.isEmpty()) {
                    pkgRemove(pkgsToRemove);
                    return cmdOk;
                }
                if ("ask".equalsIgnoreCase(accept)) {
                    accept = readConsole("Do you want to continue (yes/no)? [yes] ", "yes");
                }
                if (!Boolean.parseBoolean(accept)) {
                    log.warn("Exit");
                    return false;
                }

                LinkedList<String> packageIdsToRemove = new LinkedList<>(resolution.getOrderedPackageIdsToRemove());
                LinkedList<String> packageIdsToUpgrade = new LinkedList<>(resolution.getUpgradePackageIds());
                LinkedList<String> packageIdsToInstall = new LinkedList<>(resolution.getOrderedPackageIdsToInstall());
                LinkedList<String> packagesIdsToReInstall = new LinkedList<>();

                // Replace snapshots to install but already in cache (requested by name)
                if (CollectionUtils.containsAny(packageIdsToInstall, localSnapshotsToMaybeReplace)) {
                    for (Object pkgIdObj : CollectionUtils.intersection(packageIdsToInstall,
                            localSnapshotsToMaybeReplace)) {
                        String pkgId = (String) pkgIdObj;
                        LocalPackage addedPkg = pkgAdd(pkgId, ignoreMissing);
                        if (addedPkg == null) {
                            cmdOk = false;
                        }
                    }
                }

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
                    DependencyResolution uninstallResolution = getPackageManager().resolveDependencies(null,
                            packageIdsToRemove, null, requestPlatform, allowSNAPSHOT, keepExisting, true);
                    log.debug("Sub-resolution (uninstall) " + uninstallResolution);
                    if (uninstallResolution.isFailed()) {
                        return false;
                    }
                    LinkedList<String> newPackageIdsToRemove = new LinkedList<>(
                            uninstallResolution.getOrderedPackageIdsToRemove());
                    packagesIdsToReInstall = new LinkedList<>(newPackageIdsToRemove);
                    packagesIdsToReInstall.removeAll(packageIdsToRemove);
                    packagesIdsToReInstall.removeAll(packageIdsToUpgrade);
                    packageIdsToRemove = newPackageIdsToRemove;
                }
                log.debug("Uninstalling: " + packageIdsToRemove);
                while (!packageIdsToRemove.isEmpty()) {
                    String pkgId = packageIdsToRemove.poll();
                    if (pkgUninstall(pkgId) == null) {
                        log.error("Unable to uninstall " + pkgId);
                        return false;
                    }
                    if (isRestartRequired()) {
                        packageIdsToRemove.forEach(pkg -> persistCommand(CommandInfo.CMD_UNINSTALL + " " + pkg));
                        packageIdsToInstall.forEach(pkg -> persistCommand(CommandInfo.CMD_INSTALL + " " + pkg));
                        throw new LauncherRestartException();
                    }
                }

                // Install
                if (!packagesIdsToReInstall.isEmpty()) {
                    // Add list of packages uninstalled because of upgrade
                    packageIdsToInstall.addAll(packagesIdsToReInstall);
                    DependencyResolution installResolution = getPackageManager().resolveDependencies(
                            packageIdsToInstall, null, null, requestPlatform, allowSNAPSHOT, keepExisting, true);
                    log.debug("Sub-resolution (install) " + installResolution);
                    if (installResolution.isFailed()) {
                        return false;
                    }
                    packageIdsToInstall = new LinkedList<>(installResolution.getOrderedPackageIdsToInstall());
                }
                if (!pkgInstall(packageIdsToInstall, ignoreMissing)) {
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

    private boolean checkLocalPackagesAndAddLocalFiles(List<String> pkgsToInstall, boolean upgradeMode,
            boolean ignoreMissing, List<String> namesOrIdsToInstall, Set<String> localSnapshotsToUninstall,
            Set<String> localSnapshotsToReplace, Set<String> localSnapshotsToMaybeReplace) throws PackageException {
        boolean cmdOk = true;
        for (String pkgToInstall : pkgsToInstall) {
            String nameOrIdToInstall = pkgToInstall;
            if (!upgradeMode) {
                boolean isLocalPackageFile = isLocalPackageFile(pkgToInstall);
                if (isLocalPackageFile) {
                    // If install request is a file name, get the id
                    nameOrIdToInstall = getLocalPackageFileId(getLocalPackageFile(pkgToInstall));
                }
                // get corresponding local package if present.
                // if request is a name, prefer installed package
                LocalPackage localPackage = getInstalledPackageByName(nameOrIdToInstall);
                if (localPackage != null) {
                    // as not in upgrade mode, replace the package name by the installed package id
                    nameOrIdToInstall = localPackage.getId();
                } else {
                    if (isLocalPackageId(nameOrIdToInstall)) {
                        // if request is an id, get potential package in local cache
                        localPackage = getLocalPackage(nameOrIdToInstall);
                    } else {
                        // if request is a name but there is no installed package matching, get the best version
                        // in local cache to replace it if it is a snapshot and it happens to be the actual
                        // version to install afterward
                        LocalPackage potentialMatchingPackage = getLocalPackage(nameOrIdToInstall);
                        if (potentialMatchingPackage != null && potentialMatchingPackage.getVersion().isSnapshot()) {
                            localSnapshotsToMaybeReplace.add(potentialMatchingPackage.getId());
                        }
                    }
                }
                // first install of local file or directory
                if (localPackage == null && isLocalPackageFile) {
                    LocalPackage addedPkg = pkgAdd(pkgToInstall, ignoreMissing);
                    if (addedPkg == null) {
                        cmdOk = false;
                    }
                }
                // if a requested SNAPSHOT package is present, mark it for replacement in local cache
                if (localPackage != null && localPackage.getVersion().isSnapshot()) {
                    if (localPackage.getPackageState().isInstalled()) {
                        // if it's already installed, uninstall it
                        localSnapshotsToUninstall.add(nameOrIdToInstall);
                    }
                    // use the local file name if given and ensure we replace the right version, in case
                    // nameOrIdToInstall is a name
                    String pkgToAdd = isLocalPackageFile ? pkgToInstall : localPackage.getId();
                    localSnapshotsToReplace.add(pkgToAdd);
                }
            }
            namesOrIdsToInstall.add(nameOrIdToInstall);
        }
        return cmdOk;
    }

    /**
     * Installs a list of packages and uninstalls the rest (no dependency check)
     *
     * @since 5.9.2
     * @deprecated Since 7.10. Use #pkgSet(List, boolean) instead.
     */
    @Deprecated
    public boolean pkgSet(List<String> pkgList) {
        return pkgSet(pkgList, false);
    }

    /**
     * Installs a list of packages and uninstalls the rest (no dependency check)
     *
     * @since 6.0
     */
    public boolean pkgSet(List<String> pkgList, boolean ignoreMissing) {
        boolean cmdOK = true;
        cmdOK = cmdOK && pkgInstall(pkgList, ignoreMissing);
        List<DownloadablePackage> installedPkgs = getPackageManager().listInstalledPackages();
        List<String> pkgsToUninstall = new ArrayList<>();
        for (DownloadablePackage pkg : installedPkgs) {
            if ((!pkgList.contains(pkg.getName())) && (!pkgList.contains(pkg.getId()))) {
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
     * @param defaultValue The default answer if there's no console or if "Enter" key is pressed.
     * @param objects Parameters to use in the message (like in {@link String#format(String, Object...)})
     * @return {@code "true"} if answer is in {@link #POSITIVE_ANSWERS}, else return {@code "false"}
     */
    protected String readConsole(String message, String defaultValue, Object... objects) {
        String answer;
        Console console = System.console();
        if (console == null || StringUtils.isEmpty(answer = console.readLine(message, objects))) {
            answer = defaultValue;
        }
        answer = answer.trim().toLowerCase();
        return parseAnswer(answer);
    }

    /**
     * @return {@code "true"} if answer is in {@link #POSITIVE_ANSWERS}, and {@code "ask"} if answer values
     *         {@code "ask"}, else return {@code "false"}
     * @since 6.0
     */
    public static String parseAnswer(String answer) {
        if ("ask".equalsIgnoreCase(answer)) {
            return "ask";
        }
        if ("false".equalsIgnoreCase(answer)) {
            return "false";
        }
        for (String positive : POSITIVE_ANSWERS) {
            if (positive.equalsIgnoreCase(answer)) {
                return "true";
            }
        }
        return "false";
    }

    public boolean pkgHotfix() {
        List<String> lastHotfixes = getPackageManager().listLastHotfixes(targetPlatform, allowSNAPSHOT);
        return pkgRequest(null, lastHotfixes, null, null, true, false);
    }

    public boolean pkgUpgrade() {
        List<String> upgradeNames = getPackageManager().listInstalledPackagesNames(null);
        // use upgrade mode
        return pkgRequest(null, upgradeNames, null, null, true, false, true);
    }

    /**
     * Must be called after {@link #setAccept(String)} which overwrites its value.
     *
     * @param relaxValue true, false or ask; ignored if null
     */
    public void setRelax(String relaxValue) {
        if (relaxValue != null) {
            relax = parseAnswer(relaxValue);
        }
    }

    /**
     * @param acceptValue true, false or ask; if true or ask, then calls {@link #setRelax(String)} with the same value;
     *            ignored if null
     */
    public void setAccept(String acceptValue) {
        if (acceptValue != null) {
            accept = parseAnswer(acceptValue);
            if ("ask".equals(accept) || "true".equals(accept)) {
                setRelax(acceptValue);
            }
        }
    }

    /*
     * Helper for adding a new PackageInfo initialized with informations gathered from the given package. It is not put
     * into CommandInfo to avoid adding a dependency on Connect Client
     */
    private PackageInfo newPackageInfo(CommandInfo cmdInfo, Package pkg) {
        PackageInfo packageInfo = new PackageInfo(pkg);
        cmdInfo.packages.add(packageInfo);
        return packageInfo;
    }

    /**
     * @param packages List of packages identified by their ID, name or local filename.
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
                PackageInfo packageInfo = newPackageInfo(cmdInfo, findPackage(pkg));
                sb.append("\nPackage: ").append(packageInfo.id);
                sb.append("\nState: ").append(packageInfo.state);
                sb.append("\nVersion: ").append(packageInfo.version);
                sb.append("\nName: ").append(packageInfo.name);
                sb.append("\nType: ").append(packageInfo.type);
                sb.append("\nVisibility: ").append(packageInfo.visibility);
                if (packageInfo.state == PackageState.REMOTE && packageInfo.type != PackageType.STUDIO
                        && packageInfo.visibility != PackageVisibility.PUBLIC
                        && !LogicalInstanceIdentifier.isRegistered()) {
                    sb.append(" (registration required)");
                }
                sb.append("\nTarget platforms: ").append(ArrayUtils.toString(packageInfo.targetPlatforms));
                appendIfNotEmpty(sb, "\nVendor: ", packageInfo.vendor);
                sb.append("\nSupports hot-reload: ").append(packageInfo.supportsHotReload);
                sb.append("\nSupported: ").append(packageInfo.supported);
                sb.append("\nProduction state: ").append(packageInfo.productionState);
                sb.append("\nValidation state: ").append(packageInfo.validationState);
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
            sb.append(label).append(ArrayUtils.toString(array));
        }
    }

    private void appendIfNotEmpty(StringBuilder sb, String label, String value) {
        if (StringUtils.isNotEmpty(value)) {
            sb.append(label).append(value);
        }
    }

    /**
     * Looks for a package. First look if it's a local ZIP file, second if it's a local package and finally if it's a
     * remote package.
     *
     * @param pkg A ZIP filename or file path, or package ID or a package name.
     * @return The first package found matching the given string.
     * @throws PackageException If no package is found or if an issue occurred while searching.
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

        throw new PackageException("Could not find a remote or local (relative to "
                + "current directory or to NUXEO_HOME) " + "package with name or ID " + pkg);
    }

    /**
     * @since 5.9.1
     */
    public void setAllowSNAPSHOT(boolean allow) {
        CUDFHelper.defaultAllowSNAPSHOT = allow;
        allowSNAPSHOT = allow;
    }

}
