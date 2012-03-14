/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique, mguillaume
 */
package org.nuxeo.ecm.admin.offline.update;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.MessageInfo;
import org.nuxeo.launcher.info.MessageInfoLogger;
import org.nuxeo.launcher.info.PackageInfo;
import org.nuxeo.osgi.application.loader.FrameworkLoader;
import org.nuxeo.runtime.api.Framework;

/**
 * Offline Marketplace packages manager.
 *
 * See {@link #printHelp()} for the usage.
 *
 * The target directory is set from System property "nuxeo.runtime.home".
 *
 * <p>
 * The environment used by Nuxeo runtime can be specified as System properties.
 * <p>
 * All the bundles and the third parties must be on the boot classpath. You
 * should have at least these bundles:
 * <ul>
 * <li>nuxeo-common
 * <li>nuxeo-connect-client
 * <li>nuxeo-connect-client-wrapper
 * <li>nuxeo-connect-update
 * <li>nuxeo-connect-offline-update
 * <li>nuxeo-runtime
 * <li>nuxeo-runtime-osgi
 * <li>nuxeo-runtime-reload
 * </ul>
 * and these libraries:
 * <ul>
 * <li>commons-io
 * <li>groovy-all
 * <li>osgi-core
 * <li>xercesImpl
 * <li>commons-logging
 * <li>log4j
 * </ul>
 *
 */
public class LocalPackageManager {

    static final MessageInfoLogger log = new MessageInfoLogger();

    protected File home;

    protected File wd;

    protected File bundlesDir;

    protected List<File> bundles;

    protected Map<String, Object> env;

    protected Environment targetEnv;

    protected PackageUpdateService pus;

    private static CommandInfo cmdInfo = new CommandInfo();

    private String command;

    private String param;

    private int errorValue = 0;

    private static Options launcherOptions = null;

    private static final Map<String, Integer> cmdNumArgs;
    static {
        cmdNumArgs = new HashMap<String, Integer>();
        cmdNumArgs.put(CommandInfo.CMD_LIST, 0);
        cmdNumArgs.put(CommandInfo.CMD_RESET, 0);
        cmdNumArgs.put(CommandInfo.CMD_ADD, 1);
        cmdNumArgs.put(CommandInfo.CMD_INSTALL, 1);
        cmdNumArgs.put(CommandInfo.CMD_UNINSTALL, 1);
        cmdNumArgs.put(CommandInfo.CMD_REMOVE, 1);
    }

    protected static void initParserOptions() {
        if (launcherOptions == null) {
            launcherOptions = new Options();
            OptionBuilder.withLongOpt("help");
            OptionBuilder.withDescription("Show detailed help");
            launcherOptions.addOption(OptionBuilder.create("h"));
            OptionBuilder.withLongOpt("workdir");
            OptionBuilder.hasArg();
            OptionBuilder.withArgName("wd");
            OptionBuilder.isRequired();
            OptionBuilder.withDescription("Working directory (framework home)");
            launcherOptions.addOption(OptionBuilder.create());
        }
    }

    protected static CommandLine parseOptions(String[] args)
            throws ParseException {
        initParserOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = null;
        Boolean stopAfterParsing = false;
        try {
            cmdLine = parser.parse(launcherOptions, args);
            if (cmdLine.hasOption("h")) {
                printHelp();
                stopAfterParsing = true;
            } else if (cmdLine.getArgs().length == 0) {
                printHelp();
                stopAfterParsing = true;
            } else {
                String arg0 = cmdLine.getArgs()[0];
                Integer nParams = cmdLine.getArgs().length - 1;
                if (!cmdNumArgs.containsKey(arg0)) {
                    log.error("Unknown command: " + arg0);
                    printHelp();
                    stopAfterParsing = true;
                } else if (nParams != cmdNumArgs.get(arg0)) {
                    log.error("Wrong number of arguments for command: " + arg0);
                    printHelp();
                    stopAfterParsing = true;
                }
            }
        } catch (UnrecognizedOptionException e) {
            log.error(e.getMessage());
            printHelp();
            stopAfterParsing = true;
        } catch (MissingArgumentException e) {
            log.error(e.getMessage());
            printHelp();
            stopAfterParsing = true;
        } catch (ParseException e) {
            log.error("Error while parsing command line: " + e.getMessage());
            printHelp();
            stopAfterParsing = true;
        } finally {
            if (stopAfterParsing) {
                throw new ParseException("Invalid command line");
            }
        }
        return cmdLine;
    }

    protected static void printCommandInfo() {
        try {
            Writer xml = new StringWriter();
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    CommandInfo.class, PackageInfo.class, MessageInfo.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(cmdInfo, xml);
            System.out.println(xml.toString());
        } catch (JAXBException e) {
            System.out.println("Output serialization failed");
            System.out.println("LocalPackageManager messages:");
            log.printMessages();
            System.exit(4);
        }
    }

    protected static void errorExit(int exitCode) {
        cmdInfo.exitCode = exitCode;
        cmdInfo.messages = log.getMessages();
        printCommandInfo();
        System.exit(exitCode);
    }

    public static void main(String[] args) throws Exception {
        cmdInfo.name = CommandInfo.CMD_UNKNOWN;
        CommandLine cmdLine = null;
        try {
            cmdLine = parseOptions(args);
        } catch (ParseException e) {
            errorExit(1);
        }
        LocalPackageManager main = null;
        try {
            main = new LocalPackageManager(cmdLine);
            main.initializeFramework();
            main.startFramework();
            main.run();
        } catch (Throwable e) {
            log.error(e);
            if (main != null) {
                main.errorValue = 2;
            } else {
                errorExit(2);
            }
        } finally {
            if (main != null) {
                main.stopFramework();
            }
        }
        errorExit(main.errorValue);
    }

    public LocalPackageManager(CommandLine cmdLine)
            throws FileNotFoundException {
        wd = new File(cmdLine.getOptionValue("workdir"));
        if (!wd.isDirectory()) {
            throw new IllegalStateException(wd + " is not a directory!");
        }
        String[] argList = cmdLine.getArgs();
        command = argList[0];
        if (argList.length == 1) {
            param = null;
        } else if (argList.length == 2) {
            param = argList[1];
        } else {
            throw new IllegalStateException("Multiple parameters not supported");
            // params = Arrays.copyOfRange(argList, 1, argList.length);
        }

        home = new File(System.getProperty("nuxeo.runtime.home"));
        if (home == null) {
            throw new IllegalStateException(
                    "Syntax Error: You must provide the runtime home "
                            + "as a System property (\""
                            + Environment.NUXEO_RUNTIME_HOME + "\").");
        }
        bundlesDir = new File(wd, "bundles");
        initBundleFiles();
        initEnvironment();
        targetEnv = createTargetEnvironment();
    }

    public void run() {
        Environment defaultEnv = Environment.getDefault();
        try {
            Environment.setDefault(targetEnv);
            if (CommandInfo.CMD_INSTALL.equalsIgnoreCase(command)) {
                cmdInfo.name = CommandInfo.CMD_INSTALL;
                cmdInfo.param = param;
                LocalPackage installed = install(param);
                if (installed != null) {
                    PackageInfo info = new PackageInfo(installed);
                    // If the package requires a restart, state will be
                    // INSTALLED
                    // However, we are offline, show as STARTED
                    if (info.state == PackageState.INSTALLED) {
                        info.state = PackageState.STARTED;
                    }
                    cmdInfo.packages.add(info);
                }
            } else if (CommandInfo.CMD_UNINSTALL.equalsIgnoreCase(command)) {
                cmdInfo.name = CommandInfo.CMD_UNINSTALL;
                cmdInfo.param = param;
                LocalPackage uninstalled = uninstall(param);
                if (uninstalled != null) {
                    PackageInfo info = new PackageInfo(uninstalled);
                    cmdInfo.packages.add(info);
                }
            } else if (CommandInfo.CMD_ADD.equalsIgnoreCase(command)) {
                cmdInfo.name = CommandInfo.CMD_ADD;
                cmdInfo.param = param;
                LocalPackage added = add(param);
                if (added != null) {
                    PackageInfo info = new PackageInfo(added);
                    cmdInfo.packages.add(info);
                }
            } else if (CommandInfo.CMD_REMOVE.equalsIgnoreCase(command)) {
                cmdInfo.name = CommandInfo.CMD_REMOVE;
                cmdInfo.param = param;
                LocalPackage removed = remove(param);
                if (removed != null) {
                    PackageInfo info = new PackageInfo(removed);
                    info.state = PackageState.REMOTE;
                    cmdInfo.packages.add(info);
                }
            } else if (CommandInfo.CMD_LIST.equalsIgnoreCase(command)) {
                cmdInfo.name = CommandInfo.CMD_LIST;
                List<LocalPackage> listed = listPackages();
                for (LocalPackage l : listed) {
                    PackageInfo info = new PackageInfo(l);
                    cmdInfo.packages.add(info);
                }
            } else if (CommandInfo.CMD_RESET.equalsIgnoreCase(command)) {
                cmdInfo.name = CommandInfo.CMD_RESET;
                reset();
            } else {
                printHelp();
                return;
            }
        } catch (PackageException e) {
            log.error(e);
            errorValue = 1;
        } finally {
            Environment.setDefault(defaultEnv);
        }
    }

    public static void printHelp() {
        initParserOptions();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String cmdLineSyntax = "LocalPackageManager [options] <command> [parameter]";
        HelpFormatter help = new HelpFormatter();
        help.setSyntaxPrefix("Usage: ");
        help.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, cmdLineSyntax, "",
                launcherOptions, HelpFormatter.DEFAULT_LEFT_PAD,
                HelpFormatter.DEFAULT_DESC_PAD, "");
        log.error(sw.toString());
        log.error("Commands:");
        log.error("\tlist\t\t\t\t\t\tList local packages and their status.");
        log.error("\tadd </path/to/package>\t\t\tAdd the package to the local cache.");
        log.error("\tinstall </path/to/package|packgeId>\t\t\tInstall the package.");
        log.error("\tuninstall <packageId>\t\t\t\tUninstall the package.");
        log.error("\tremove <packageId>\t\t\t\tRemove the package from the local cache.");
        log.error("\treset\t\t\t\t\t\tReset all package states to DOWNLOADED. "
                + "This may be useful after a manual upgrade of the server.");
    }

    protected void initEnvironment() {
        env = new HashMap<String, Object>();
    }

    protected Environment createTargetEnvironment() {
        Environment environment = new Environment(home);
        environment.init();
        return environment;
    }

    protected void initBundleFiles() throws FileNotFoundException {
        bundles = new ArrayList<File>();
        if (!bundlesDir.isDirectory()) {
            throw new FileNotFoundException("File " + bundlesDir
                    + " is not a directory");
        }
        File[] list = bundlesDir.listFiles();
        if (list == null) {
            throw new FileNotFoundException("No bundles found in " + bundlesDir);
        }
        for (File file : list) {
            String name = file.getName();
            if (name.endsWith(".jar") && name.contains("nuxeo-")) { // a bundle
                if (!name.contains("osgi")) { // avoid loading the system bundle
                    bundles.add(file);
                }
            }
        }
    }

    public void initializeFramework() {
        System.setProperty("org.nuxeo.connect.update.dataDir",
                targetEnv.getData().getAbsolutePath());
        FrameworkLoader.initialize(LocalPackageManager.class.getClassLoader(),
                wd, bundles, env);
    }

    public void startFramework() throws Exception {
        FrameworkLoader.start();
        pus = Framework.getLocalService(PackageUpdateService.class);
        if (pus == null) {
            throw new IllegalStateException("PackagUpdateService not found");
        }
    }

    public void stopFramework() throws Exception {
        try {
            FrameworkLoader.stop();
        } finally {
            if (wd != null) {
                FileUtils.deleteTree(wd);
            }
        }
    }

    /**
     * Validate and run given task
     *
     * @since 5.5
     * @param task
     * @throws PackageException
     */
    public void performTask(Task task) throws PackageException {
        ValidationStatus status = task.validate();
        if (status.hasErrors()) {
            errorValue = 3;
            throw new PackageException("Failed to validate package "
                    + task.getPackage().getId() + " -> " + status.getErrors());
        }
        if (status.hasWarnings()) {
            log.warn("Got warnings on package validation "
                    + task.getPackage().getId() + " -> " + status.getWarnings());
        }
        task.run(null);
    }

    /**
     * @param pkgId Marketplace package id
     * @throws PackageException
     * @since 5.6
     */
    private LocalPackage add(String packageFileName) throws PackageException {
        if (packageFileName.startsWith("file:")) {
            packageFileName = packageFileName.substring(5);
        }
        log.info("Adding " + packageFileName);
        try {
            LocalPackage pkg = pus.addPackage(new File(packageFileName));
            return pkg;
        } catch (Throwable e) {
            log.error("Failed to add package: " + packageFileName, e);
            return null;
        }
    }

    /**
     * @param pkgId Marketplace package id
     * @throws PackageException
     * @since 5.6
     */
    private LocalPackage install(String pkgIdOrFileName)
            throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgIdOrFileName);
        // Unknown ID : assume it's a filename
        if (pkg == null) {
            pkg = add(pkgIdOrFileName);
            // Validate install went OK
            if (pkg == null) {
                errorValue = 1;
                throw new IllegalStateException("Package not found: "
                        + pkgIdOrFileName);
            }
        }
        log.info("Updating " + pkg.getId());
        Task installTask = pkg.getInstallTask();
        try {
            performTask(installTask);
            return pus.getPackage(pkg.getId());
        } catch (Throwable e) {
            installTask.rollback();
            errorValue = 1;
            log.error("Failed to install package: " + pkg.getId(), e);
            return null;
        }

    }

    /**
     * @param pkgId Marketplace package id
     * @throws PackageException
     * @since 5.6
     */
    private LocalPackage uninstall(String pkgId) throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgId);
        if (pkg == null) {
            throw new IllegalStateException("No package found: " + pkgId);
        }
        log.info("Uninstalling " + pkgId);
        Task uninstallTask = pkg.getUninstallTask();
        try {
            performTask(uninstallTask);
            return pkg;
        } catch (Throwable e) {
            uninstallTask.rollback();
            errorValue = 1;
            log.error("Failed to uninstall package: " + pkgId, e);
            return null;
        }
    }

    /**
     * @param pkgId Marketplace package id
     * @throws PackageException
     * @since 5.6
     */
    private LocalPackage remove(String pkgId) throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgId);
        if (pkg == null) {
            throw new IllegalStateException("No package found: " + pkgId);
        }

        if ((pkg.getState() == PackageState.STARTED)
                || (pkg.getState() == PackageState.INSTALLED)) {
            uninstall(pkgId);
            // Refresh state
            pkg = pus.getPackage(pkgId);
        }
        if (pkg.getState() != PackageState.DOWNLOADED) {
            throw new IllegalStateException(
                    "Can only remove packages in DOWNLOADED, INSTALLED or STARTED state");
        }
        log.info("Removing " + pkgId);
        try {
            pus.removePackage(pkgId);
            return pkg;
        } catch (Throwable e) {
            log.error("Failed to remove package: " + pkgId, e);
            return pkg;
        }
    }

    /**
     * @throws PackageException
     * @since 5.6
     */
    private List<LocalPackage> listPackages() throws PackageException {
        List<LocalPackage> localPackages = pus.getPackages();
        if (localPackages.isEmpty()) {
            log.info("No local package.");
        } else {
            log.info("Local packages:");
            for (LocalPackage localPackage : localPackages) {
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
                packageDescription += "\t" + localPackage.getName() + " (id: "
                        + localPackage.getId() + ")";
                log.info(packageDescription);
            }
        }
        return localPackages;
    }

    private void reset() throws PackageException {
        pus.reset();
        log.info("Packages reset done: All packages were marked as DOWNLOADED");
    }

}
