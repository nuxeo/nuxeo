/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.launcher;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.artofsolving.jodconverter.process.MacProcessManager;
import org.artofsolving.jodconverter.process.ProcessManager;
import org.artofsolving.jodconverter.process.PureJavaProcessManager;
import org.artofsolving.jodconverter.process.UnixProcessManager;
import org.artofsolving.jodconverter.process.WindowsProcessManager;
import org.artofsolving.jodconverter.util.PlatformUtils;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.Environment;
import org.nuxeo.launcher.daemon.DaemonThreadFactory;
import org.nuxeo.launcher.gui.NuxeoLauncherGUI;
import org.nuxeo.launcher.monitoring.StatusServletClient;
import org.nuxeo.log4j.Log4JHelper;
import org.nuxeo.log4j.ThreadedStreamGobbler;

/**
 * @author jcarsique
 * @since 5.4.2
 */
public abstract class NuxeoLauncher {
    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    private static final String JAVA_OPTS_PROPERTY = "launcher.java.opts";

    private static final String JAVA_OPTS_DEFAULT = "-Xms512m -Xmx1024m -XX:MaxPermSize=512m";

    private static final String OVERRIDE_JAVA_TMPDIR_PARAM = "launcher.override.java.tmpdir";

    protected boolean overrideJavaTmpDir;

    private static final String START_MAX_WAIT_PARAM = "launcher.start.max.wait";

    private static final String STOP_MAX_WAIT_PARAM = "launcher.stop.max.wait";

    /**
     * Default maximum time to wait for server startup summary in logs (in
     * seconds).
     */
    private static final String START_MAX_WAIT_DEFAULT = "300";

    private static final String START_MAX_WAIT_JBOSS_DEFAULT = "900";

    /**
     * Default maximum time to wait for effective stop (in seconds)
     */
    private static final String STOP_MAX_WAIT_DEFAULT = "60";

    /**
     * Number of try to cleanly stop server before killing process
     */
    private static final int STOP_NB_TRY = 5;

    private static final int STOP_SECONDS_BEFORE_NEXT_TRY = 2;

    private static final String PARAM_NUXEO_URL = "nuxeo.url";

    private static final String PKG_MANAGER_CLASS = "org.nuxeo.ecm.admin.offline.update.LocalPackageManager";

    private static final long STREAM_MAX_WAIT = 3000;

    private static final String PACK_JBOSS_CLASS = "org.nuxeo.runtime.deployment.preprocessor.PackZip";

    private static final String PACK_TOMCAT_CLASS = "org.nuxeo.runtime.deployment.preprocessor.PackWar";

    private static final String PARAM_UPDATECENTER_DISABLED = "nuxeo.updatecenter.disabled";

    protected ConfigurationGenerator configurationGenerator;

    public final ConfigurationGenerator getConfigurationGenerator() {
        return configurationGenerator;
    }

    protected ProcessManager processManager;

    protected Process nuxeoProcess;

    private String processRegex;

    protected String pid;

    private ExecutorService executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory(
            "NuxeoProcessThread", false));

    private ShutdownThread shutdownHook;

    protected String[] params;

    protected String command;

    public String getCommand() {
        return command;
    }

    private boolean useGui = false;

    /**
     * @since 5.5
     */
    public boolean isUsingGui() {
        return useGui;
    }

    private boolean reloadConfiguration = false;

    private int status = 4;

    private int errorValue = 0;

    private StatusServletClient statusServletClient;

    private boolean quiet = false;

    private boolean debug = false;

    /**
     * @since 5.5
     * @return true if quiet mode is active
     */
    public boolean isQuiet() {
        return quiet;
    }

    private static Map<String, NuxeoLauncherGUI> guis;

    /**
     * @since 5.5
     */
    public NuxeoLauncherGUI getGUI() {
        if (guis == null) {
            return null;
        }
        return guis.get(configurationGenerator.getNuxeoConf().toString());
    }

    /**
     * @since 5.5
     */
    public void setGUI(NuxeoLauncherGUI gui) {
        if (guis == null) {
            guis = new HashMap<String, NuxeoLauncherGUI>();
        }
        guis.put(configurationGenerator.getNuxeoConf().toString(), gui);
    }

    public NuxeoLauncher(ConfigurationGenerator configurationGenerator) {
        this.configurationGenerator = configurationGenerator;
        processManager = getOSProcessManager();
        processRegex = "^(?!/bin/sh).*"
                + Pattern.quote(configurationGenerator.getNuxeoConf().getPath())
                + ".*" + Pattern.quote(getServerPrint()) + ".*$";
        // Set OS-specific decorations
        if (PlatformUtils.isMac()) {
            System.setProperty(
                    "com.apple.mrj.application.apple.menu.about.name",
                    "NuxeoCtl");
        }
    }

    private ProcessManager getOSProcessManager() {
        if (PlatformUtils.isLinux()) {
            UnixProcessManager unixProcessManager = new UnixProcessManager();
            return unixProcessManager;
        } else if (PlatformUtils.isMac()) {
            return new MacProcessManager();
        } else if (PlatformUtils.isWindows()) {
            WindowsProcessManager windowsProcessManager = new WindowsProcessManager();
            return windowsProcessManager.isUsable() ? windowsProcessManager
                    : new PureJavaProcessManager();
        } else {
            // NOTE: UnixProcessManager can't be trusted to work on Solaris
            // because of the 80-char limit on ps output there
            return new PureJavaProcessManager();
        }
    }

    /**
     * Do not directly call this method without a call to
     * {@link #checkNoRunningServer()}
     *
     * @see #doStart()
     * @throws IOException In case of issue with process.
     * @throws InterruptedException If any thread has interrupted the current
     *             thread.
     */
    protected void start(boolean logProcessOutput) throws IOException,
            InterruptedException {
        List<String> startCommand = new ArrayList<String>();
        startCommand.add(getJavaExecutable().getPath());
        startCommand.addAll(Arrays.asList(getJavaOptsProperty().split(" ")));
        startCommand.add("-cp");
        startCommand.add(getClassPath());
        startCommand.addAll(getNuxeoProperties());
        startCommand.addAll(getServerProperties());
        setServerStartCommand(startCommand);
        for (String param : params) {
            startCommand.add(param);
        }
        ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
        pb.directory(configurationGenerator.getNuxeoHome());
        // pb = pb.redirectErrorStream(true);
        log.debug("Server command: " + pb.command());
        nuxeoProcess = pb.start();
        logProcessStreams(nuxeoProcess, logProcessOutput);
        Thread.sleep(1000);
        if (getPid() != null) {
            log.warn("Server started with process ID " + pid + ".");
        } else {
            log.warn("Sent server start command but could not get process ID.");
        }
    }

    /**
     * Gets the java options with Nuxeo properties substituted.
     *
     * It enables usage of property like ${nuxeo.log.dir} inside JAVA_OPTS.
     *
     * @return the java options string.
     */
    protected String getJavaOptsProperty() {
        String ret = System.getProperty(JAVA_OPTS_PROPERTY, JAVA_OPTS_DEFAULT);
        String properties[] = { Environment.NUXEO_HOME_DIR,
                Environment.NUXEO_LOG_DIR, Environment.NUXEO_DATA_DIR,
                Environment.NUXEO_TMP_DIR };

        for (String property : properties) {
            String value = configurationGenerator.getUserConfig().getProperty(
                    property);
            if (value != null && !value.isEmpty()) {
                ret = ret.replace("${" + property + "}", value);
            }
        }
        return ret;
    }

    /**
     * Check if some server is already running (from another thread) and throw a
     * Runtime exception if it finds one. That method will work where
     * {@link #isRunning()} won't.
     *
     * @throws IllegalThreadStateException Thrown if a server is already
     *             running.
     */
    public void checkNoRunningServer() throws IllegalStateException {
        try {
            String existingPid = getPid();
            if (existingPid != null) {
                errorValue = 0;
                throw new IllegalStateException(
                        "A server is already running with process ID "
                                + existingPid);
            }
        } catch (IOException e) {
            log.warn("Could not check existing process" + e.getMessage());
        }
    }

    /**
     * @return (since 5.5) Array list with created stream gobbler threads.
     */
    public ArrayList<ThreadedStreamGobbler> logProcessStreams(Process process,
            boolean logProcessOutput) {
        ArrayList<ThreadedStreamGobbler> sgArray = new ArrayList<ThreadedStreamGobbler>();
        ThreadedStreamGobbler inputSG, errorSG;
        if (logProcessOutput) {
            inputSG = new ThreadedStreamGobbler(process.getInputStream(),
                    System.out);
            errorSG = new ThreadedStreamGobbler(process.getErrorStream(),
                    System.err);
        } else {
            inputSG = new ThreadedStreamGobbler(process.getInputStream(),
                    SimpleLog.LOG_LEVEL_OFF);
            errorSG = new ThreadedStreamGobbler(process.getErrorStream(),
                    SimpleLog.LOG_LEVEL_OFF);
        }
        inputSG.start();
        errorSG.start();
        sgArray.add(inputSG);
        sgArray.add(errorSG);
        return sgArray;
    }

    protected abstract String getServerPrint();

    /**
     * Will wrap, if necessary, the command within a Shell command
     *
     * @param roughCommand Java command which will be run
     * @return wrapped command depending on the OS
     */
    private List<String> getOSCommand(List<String> roughCommand) {
        String linearizedCommand = new String();
        ArrayList<String> osCommand = new ArrayList<String>();
        if (PlatformUtils.isLinux() || PlatformUtils.isMac()) {
            for (Iterator<String> iterator = roughCommand.iterator(); iterator.hasNext();) {
                String commandToken = iterator.next();
                if (commandToken.contains(" ")) {
                    commandToken = commandToken.replaceAll(" ", "\\\\ ");
                }
                linearizedCommand += " " + commandToken;
            }
            osCommand.add("/bin/sh");
            osCommand.add("-c");
            osCommand.add(linearizedCommand);
            // osCommand.add("&");
            return osCommand;
            // return roughCommand;
        } else if (PlatformUtils.isWindows()) {
            // for (Iterator<String> iterator = roughCommand.iterator();
            // iterator.hasNext();) {
            // String commandToken = iterator.next();
            // if (commandToken.endsWith("java")) {
            // commandToken = "^\"" + commandToken + "^\"";
            // } else if (commandToken.contains(" ")) {
            // commandToken = commandToken.replaceAll(" ", "^ ");
            // }
            // linearizedCommand += " " + commandToken;
            // }
            // osCommand.add("cmd");
            // osCommand.add("/C");
            // osCommand.add(linearizedCommand);
            // return osCommand;
            return roughCommand;
        } else {
            return roughCommand;
        }
    }

    protected abstract Collection<? extends String> getServerProperties();

    protected abstract void setServerStartCommand(List<String> command);

    private File getJavaExecutable() {
        File javaExec = new File(System.getProperty("java.home"), "bin"
                + File.separator + "java");
        return javaExec;
    }

    protected abstract String getClassPath();

    protected Collection<? extends String> getNuxeoProperties() {
        ArrayList<String> nuxeoProperties = new ArrayList<String>();
        nuxeoProperties.add("-Dnuxeo.home="
                + configurationGenerator.getNuxeoHome().getPath());
        nuxeoProperties.add("-Dnuxeo.conf="
                + configurationGenerator.getNuxeoConf().getPath());
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_LOG_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_DATA_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_TMP_DIR));
        if (overrideJavaTmpDir) {
            nuxeoProperties.add("-Djava.io.tmpdir="
                    + configurationGenerator.getUserConfig().getProperty(
                            Environment.NUXEO_TMP_DIR));
        }
        return nuxeoProperties;
    }

    private String getNuxeoProperty(String property) {
        return "-D" + property + "="
                + configurationGenerator.getUserConfig().getProperty(property);
    }

    protected String addToClassPath(String cp, String filename) {
        File classPathEntry = new File(configurationGenerator.getNuxeoHome(),
                filename);
        if (!classPathEntry.exists()) {
            classPathEntry = new File(filename);
        }
        if (!classPathEntry.exists()) {
            throw new RuntimeException(
                    "Tried to add inexistent classpath entry: " + filename);
        }
        cp += System.getProperty("path.separator") + classPathEntry.getPath();
        return cp;
    }

    public static void main(String[] args) throws ConfigurationException {
        final NuxeoLauncher launcher = createLauncher(args);
        if (launcher.useGui && launcher.getGUI() == null) {
            launcher.setGUI(new NuxeoLauncherGUI(launcher));
        }
        launch(launcher);
    }

    /**
     * @since 5.5
     * @param launcher
     * @param launcherGUI
     * @param command
     */
    public static void launch(final NuxeoLauncher launcher) {
        int exitStatus = 0;
        boolean commandSucceeded = true;
        if (launcher.command == null) {
            // Nothing to do
        } else if ("help".equalsIgnoreCase(launcher.command)) {
            printHelp();
        } else if ("status".equalsIgnoreCase(launcher.command)) {
            log.warn(launcher.status());
            if (launcher.isStarted()) {
                log.info(launcher.getStartupSummary());
            }
            exitStatus = launcher.status;
        } else if ("startbg".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.doStart();
        } else if ("start".equalsIgnoreCase(launcher.command)) {
            if (launcher.useGui) {
                launcher.getGUI().start();
            } else {
                commandSucceeded = launcher.doStartAndWait();
            }
        } else if ("console".equalsIgnoreCase(launcher.command)) {
            launcher.executor.execute(new Runnable() {
                @Override
                public void run() {
                    launcher.addShutdownHook();
                    if (!launcher.doStart(true)) {
                        launcher.removeShutdownHook();
                        System.exit(1);
                    }
                }
            });
        } else if ("stop".equalsIgnoreCase(launcher.command)) {
            if (launcher.useGui) {
                launcher.getGUI().stop();
            } else {
                launcher.stop();
            }
        } else if ("restartbg".equalsIgnoreCase(launcher.command)) {
            launcher.stop();
            commandSucceeded = launcher.doStart();
        } else if ("restart".equalsIgnoreCase(launcher.command)) {
            launcher.stop();
            commandSucceeded = launcher.doStartAndWait();
        } else if ("wizard".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.startWizard();
        } else if ("configure".equalsIgnoreCase(launcher.command)) {
            try {
                launcher.configure();
            } catch (ConfigurationException e) {
                log.error(e);
                commandSucceeded = false;
            }
        } else if ("pack".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pack();
            // log.error("Not implemented. Use \"pack\" Shell script.");
            // exitStatus = 3;
        } else if ("mp-list".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgList();
        } else if ("mp-add".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgAdd();
        } else if ("mp-install".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgInstall();
        } else if ("mp-uninstall".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgUninstall();
        } else if ("mp-remove".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgRemove();
        } else if ("mp-reset".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgReset();
        } else {
            printHelp();
            commandSucceeded = false;
        }
        if (!commandSucceeded) {
            exitStatus = launcher.errorValue;
        }
        if (exitStatus != 0) {
            System.exit(exitStatus);
        }
    }

    /**
     * Since 5.5
     */
    private boolean pack() {
        try {
            checkNoRunningServer();
            configurationGenerator.setProperty(PARAM_UPDATECENTER_DISABLED,
                    "true");
            List<String> startCommand = new ArrayList<String>();
            startCommand.add(getJavaExecutable().getPath());
            startCommand.addAll(Arrays.asList(getJavaOptsProperty().split(" ")));
            startCommand.add("-cp");
            String classpath = getClassPath();
            classpath = addToClassPath(classpath, "bin" + File.separator
                    + "nuxeo-launcher.jar");
            classpath = getClassPath(
                    classpath,
                    configurationGenerator.getServerConfigurator().getServerLibDir());
            classpath = getClassPath(
                    classpath,
                    configurationGenerator.getServerConfigurator().getNuxeoLibDir());
            classpath = getClassPath(
                    classpath,
                    new File(configurationGenerator.getRuntimeHome(), "bundles"));
            startCommand.add(classpath);
            startCommand.addAll(getNuxeoProperties());
            if (configurationGenerator.isJBoss) {
                startCommand.add(PACK_JBOSS_CLASS);
            } else if (configurationGenerator.isTomcat) {
                startCommand.add(PACK_TOMCAT_CLASS);
            } else {
                errorValue = 1;
                return false;
            }
            startCommand.add(configurationGenerator.getRuntimeHome().getPath());
            for (String param : params) {
                startCommand.add(param);
            }
            ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
            pb.directory(configurationGenerator.getNuxeoHome());
            log.debug("Pack command: " + pb.command());
            Process process = pb.start();
            ArrayList<ThreadedStreamGobbler> sgArray = logProcessStreams(
                    process, true);
            Thread.sleep(100);
            process.waitFor();
            waitForProcessStreams(sgArray);
        } catch (IOException e) {
            errorValue = 1;
            log.error("Could not start process", e);
        } catch (InterruptedException e) {
            errorValue = 1;
            log.error("Could not start process", e);
        } catch (IllegalStateException e) {
            errorValue = 1;
            log.error(
                    "The server must not be running while running pack command",
                    e);
        } catch (ConfigurationException e) {
            errorValue = 1;
            log.error(e);
        }
        return errorValue == 0;
    }

    private boolean startWizard() {
        if (!configurationGenerator.getServerConfigurator().isWizardAvailable()) {
            log.error("Sorry, the wizard is not available within that server.");
            return false;
        }
        if (isRunning()) {
            log.error("Server already running. "
                    + "Please stop it before calling \"wizard\" command "
                    + "or use the Admin Center instead of the wizard.");
            return false;
        }
        if (reloadConfiguration) {
            configurationGenerator = new ConfigurationGenerator();
            configurationGenerator.init();
            reloadConfiguration = false;
        }
        configurationGenerator.getUserConfig().setProperty(
                ConfigurationGenerator.PARAM_WIZARD_DONE, "false");
        return doStart();
    }

    /**
     * @see #doStartAndWait(boolean)
     */
    public boolean doStartAndWait() {
        return doStartAndWait(false);
    }

    /**
     * @see #stop(boolean)
     */
    public void stop() {
        stop(false);
    }

    /**
     * Call {@link #doStart(boolean)} with false as parameter.
     *
     * @see #doStart(boolean)
     * @return true if the server started successfully
     */
    public boolean doStart() {
        return doStart(false);
    }

    /**
     * Whereas {@link #doStart()} considers the server as started when the
     * process is running, {@link #doStartAndWait()} waits for effective start
     * by watching the logs
     *
     * @param logProcessOutput Must process output stream must be logged or not.
     *
     * @return true if the server started successfully
     */
    public boolean doStartAndWait(boolean logProcessOutput) {
        boolean commandSucceeded = false;
        if (doStart(logProcessOutput)) {
            addShutdownHook();
            try {
                if (configurationGenerator.isWizardRequired()
                        || waitForEffectiveStart()) {
                    commandSucceeded = true;
                }
                removeShutdownHook();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        return commandSucceeded;
    }

    protected void removeShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            log.debug("Removed shutdown hook");
        } catch (IllegalStateException e) {
            // the virtual machine is already in the process of shutting down
        }
    }

    /**
     * @return true if Nuxeo is ready
     * @throws InterruptedException
     */
    protected boolean waitForEffectiveStart() throws InterruptedException {
        long startTime = new Date().getTime();
        int startMaxWait = Integer.parseInt(configurationGenerator.getUserConfig().getProperty(
                START_MAX_WAIT_PARAM, getDefaultMaxWait()));
        log.debug("Will wait for effective start during " + startMaxWait
                + " seconds.");
        final StringBuilder startSummary = new StringBuilder();
        final String newLine = System.getProperty("line.separator");
        boolean isReady = false;
        long deltaTime = 0;
        // Wait for status servlet ready
        do {
            try {
                isReady = statusServletClient.init();
            } catch (SocketTimeoutException e) {
                if (!quiet) {
                    System.out.print(".");
                }
            }
            deltaTime = (new Date().getTime() - startTime) / 1000;
        } while (!isReady && deltaTime < startMaxWait && isRunning());
        isReady = false;
        // Wait for effective start reported from status servlet
        do {
            isReady = isStarted();
            if (!isReady) {
                if (!quiet) {
                    System.out.print(".");
                }
                Thread.sleep(1000);
            }
            deltaTime = (new Date().getTime() - startTime) / 1000;
        } while (!isReady && deltaTime < startMaxWait && isRunning());
        if (isReady) {
            startSummary.append(newLine + getStartupSummary());
            long duration = (new Date().getTime() - startTime) / 1000;
            startSummary.append("Started in "
                    + String.format("%dmin%02ds", new Long(duration / 60),
                            new Long(duration % 60)));
            if (wasStartupFine()) {
                if (!quiet) {
                    System.out.println(startSummary);
                }
            } else {
                System.err.println(startSummary);
            }
            return true;
        } else if (deltaTime >= startMaxWait) {
            if (!quiet) {
                System.out.println();
            }
            log.error("Starting process is taking too long - giving up.");
        }
        errorValue = 1;
        return false;
    }

    /**
     * Must be called after {@link #getStartupSummary()}
     *
     * @since 5.5
     * @return last detected status of running Nuxeo server
     */
    public boolean wasStartupFine() {
        return statusServletClient.isStartupFine();
    }

    /**
     * @since 5.5
     * @return Nuxeo startup summary
     * @throws SocketTimeoutException if Nuxeo server is not responding
     */
    public String getStartupSummary() {
        try {
            return statusServletClient.getStartupSummary();
        } catch (SocketTimeoutException e) {
            log.warn("Failed to contact Nuxeo for getting startup summary", e);
            return "";
        }
    }

    /**
     * Starts the server in background.
     *
     * @return true if server successfully started
     */
    public boolean doStart(boolean logProcessOutput) {
        errorValue = 0;
        boolean serverStarted = false;
        try {
            if (reloadConfiguration) {
                configurationGenerator = new ConfigurationGenerator();
                configurationGenerator.init();
            } else {
                // Ensure reload on next start
                reloadConfiguration = true;
            }
            checkNoRunningServer();
            configure();

            if (configurationGenerator.isWizardRequired()) {
                if (!configurationGenerator.isForceGeneration()) {
                    log.error("Cannot start setup wizard with "
                            + ConfigurationGenerator.PARAM_FORCE_GENERATION
                            + "=false. Either set it to true or once, either set "
                            + ConfigurationGenerator.PARAM_WIZARD_DONE
                            + "=true to skip the wizard.");
                    errorValue = 6;
                    return false;
                }
                String paramsStr = "";
                for (String param : params) {
                    paramsStr += " " + param;
                }
                System.setProperty(
                        ConfigurationGenerator.PARAM_WIZARD_RESTART_PARAMS,
                        paramsStr);
                configurationGenerator.prepareWizardStart();
            } else {
                configurationGenerator.cleanupPostWizard();
            }

            log.debug("Check if install in progress...");
            if (configurationGenerator.isInstallInProgress()) {
                pkgInstall();
                // configuration will be reloaded, keep wizard value
                System.setProperty(
                        ConfigurationGenerator.PARAM_WIZARD_DONE,
                        configurationGenerator.getUserConfig().getProperty(
                                ConfigurationGenerator.PARAM_WIZARD_DONE,
                                "true"));
                return doStart(logProcessOutput);
            }

            start(logProcessOutput);
            serverStarted = isRunning();
            if (pid != null) {
                File pidFile = new File(configurationGenerator.getPidDir(),
                        "nuxeo.pid");
                FileWriter writer = new FileWriter(pidFile);
                writer.write(pid);
                writer.close();
            }
        } catch (ConfigurationException e) {
            errorValue = 6;
            log.error("Could not run configuration", e);
        } catch (IOException e) {
            errorValue = 1;
            log.error("Could not start process", e);
        } catch (InterruptedException e) {
            errorValue = 1;
            log.error("Could not start process", e);
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
        }
        return serverStarted;
    }

    private boolean pkgList() {
        callPackageManager("list", null);
        return errorValue == 0;
    }

    private boolean pkgReset() {
        callPackageManager("reset", null);
        return errorValue == 0;
    }

    private boolean pkgUninstall() {
        log.info("Package(s) uninstall in progress...");
        callPackageManager("uninstall", params);
        return errorValue == 0;
    }

    private boolean pkgRemove() {
        log.info("Package(s) removal in progress...");
        callPackageManager("remove", params);
        return errorValue == 0;
    }

    private boolean pkgAdd() {
        log.info("Package(s) add in progress...");
        callPackageManager("add", params);
        return errorValue == 0;
    }

    private boolean pkgInstall() {
        log.info("Package(s) install in progress...");
        if (params.length > 0) {
            callPackageManager("installpkg", params);
        } else if (!configurationGenerator.isInstallInProgress()) {
            log.error("No package to install.");
            errorValue = 1;
        } else {
            callPackageManager("install", null);
        }
        return errorValue == 0;
    }

    /**
     * @since 5.5
     * @param pkgParams Parameters passed to the package manager
     * @throws IOException
     * @throws InterruptedException
     */
    protected void callPackageManager(String pkgCommand, String[] pkgParams) {
        try {
            if (!"list".equals(pkgCommand)) {
                checkNoRunningServer();
            }
            List<String> startCommand = new ArrayList<String>();
            startCommand.add(getJavaExecutable().getPath());
            startCommand.addAll(Arrays.asList(getJavaOptsProperty().split(" ")));
            startCommand.add("-cp");
            File tmpDir = File.createTempFile("install", null);
            startCommand.add(getInstallClassPath(tmpDir));
            startCommand.addAll(getNuxeoProperties());
            startCommand.add("-Dnuxeo.runtime.home="
                    + configurationGenerator.getRuntimeHome().getPath());
            startCommand.add(PKG_MANAGER_CLASS);
            if (quiet) {
                startCommand.add("-q");
            }
            if (debug) {
                startCommand.add("-d");
            }
            startCommand.add(tmpDir.getPath());
            startCommand.add(pkgCommand);
            startCommand.add(configurationGenerator.getInstallFile().getPath());
            if (pkgParams != null) {
                for (String param : pkgParams) {
                    startCommand.add(param);
                }
            }
            ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
            pb.directory(configurationGenerator.getNuxeoHome());
            log.debug("Package manager command: " + pb.command());
            Process process = pb.start();
            ArrayList<ThreadedStreamGobbler> sgArray = logProcessStreams(
                    process, true);
            Thread.sleep(100);
            process.waitFor();
            waitForProcessStreams(sgArray);
        } catch (IOException e) {
            errorValue = 1;
            log.error("Could not start process", e);
        } catch (InterruptedException e) {
            errorValue = 1;
            log.error("Process interrupted", e);
        } catch (IllegalStateException e) {
            errorValue = 1;
            log.error(
                    "The server must not be running while managing marketplace packages",
                    e);
        }
    }

    /**
     * Stop stream gobblers contained in the given ArrayList
     *
     * @throws InterruptedException
     *
     * @since 5.5
     * @see #logProcessStreams(Process, boolean)
     */
    public void waitForProcessStreams(ArrayList<ThreadedStreamGobbler> sgArray) {
        for (ThreadedStreamGobbler streamGobbler : sgArray) {
            try {
                streamGobbler.join(STREAM_MAX_WAIT);
            } catch (InterruptedException e) {
                streamGobbler.interrupt();
            }
        }
    }

    /**
     * Copy required JARs into temporary directory and return it as classpath
     *
     * @param tmpDir temporary directory hosting classpath
     * @throws IOException If temporary directory could not be created.
     */
    protected String getInstallClassPath(File tmpDir) throws IOException {
        String cp = ".";
        tmpDir.delete();
        tmpDir.mkdirs();
        File baseDir = new File(configurationGenerator.getRuntimeHome(),
                "bundles");
        String[] filenames = new String[] { "nuxeo-runtime-osgi",
                "nuxeo-runtime", "nuxeo-common", "nuxeo-connect-update",
                "nuxeo-connect-client", "nuxeo-connect-offline-update",
                "nuxeo-connect-client-wrapper", "nuxeo-runtime-reload",
                "nuxeo-launcher-commons" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = configurationGenerator.getServerConfigurator().getNuxeoLibDir();
        filenames = new String[] { "commons-io", "commons-jexl", "groovy-all",
                "osgi-core", "xercesImpl", "commons-collections" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = configurationGenerator.getServerConfigurator().getServerLibDir();
        filenames = new String[] { "commons-lang", "commons-logging", "log4j" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = new File(configurationGenerator.getNuxeoHome(), "bin");
        filenames = new String[] { "nuxeo-launcher" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        return cp;
    }

    /**
     * Build a temporary classpath directory, copying inside filenames from
     * baseDir
     *
     * @param tmpDir temporary target directory
     * @param classpath classpath including filenames with their new location in
     *            tmpDir
     * @param baseDir base directory where to look for filenames
     * @param filenames filenames' patterns (must end with "-[0-9].*\\.jar)
     * @return completed classpath
     * @throws IOException in case of copy error
     */
    protected String getTempClassPath(File tmpDir, String classpath,
            File baseDir, String[] filenames) throws IOException {
        File targetDir = new File(tmpDir, baseDir.getName());
        targetDir.mkdirs();
        for (final String filePattern : filenames) {
            File[] files = getFilename(baseDir, filePattern);
            for (File file : files) {
                FileUtils.copyFileToDirectory(file, targetDir);
                File classPathEntry = new File(targetDir, file.getName());
                classpath += System.getProperty("path.separator")
                        + classPathEntry.getPath();
            }
        }
        return classpath;
    }

    /**
     * @since 5.5
     * @param classpath
     * @param baseDir
     * @return classpath with all jar files in baseDir
     * @throws IOException
     */
    protected String getClassPath(String classpath, File baseDir)
            throws IOException {
        File[] files = getFilename(baseDir, ".*");
        for (File file : files) {
            classpath += System.getProperty("path.separator") + file.getPath();
        }
        return classpath;
    }

    /**
     * @since 5.5
     * @param baseDir
     * @param filePattern
     * @return filename matching filePattern in baseDir
     */
    protected File[] getFilename(File baseDir, final String filePattern) {
        File[] files = baseDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File basedir, String filename) {
                return filename.matches(filePattern + "(-[0-9].*)?\\.jar");
            }
        });
        return files;
    }

    protected class ShutdownThread extends Thread {

        private NuxeoLauncher launcher;

        public ShutdownThread(NuxeoLauncher launcher) {
            super();
            this.launcher = launcher;
        }

        public void run() {
            log.debug("Shutting down...");
            if (launcher.isRunning()) {
                launcher.stop();
            }
            log.debug("Shutdown complete.");
        }
    }

    protected void addShutdownHook() {
        log.debug("Add shutdown hook");
        shutdownHook = new ShutdownThread(this);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Stops the server.
     *
     * Will try to call specific class for a clean stop, retry
     * {@link #STOP_NB_TRY}, waiting {@link #STOP_SECONDS_BEFORE_NEXT_TRY}
     * between each try, then kill the process if still running.
     */
    public void stop(boolean logProcessOutput) {
        long startTime = new Date().getTime();
        long deltaTime;
        try {
            if (!isRunning()) {
                log.warn("Server is not running.");
                return;
            }
            if (!quiet) {
                System.out.print("\nStopping server...");
            }
            int nbTry = 0;
            boolean retry = false;
            int stopMaxWait = Integer.parseInt(configurationGenerator.getUserConfig().getProperty(
                    STOP_MAX_WAIT_PARAM, STOP_MAX_WAIT_DEFAULT));
            do {
                List<String> stopCommand = new ArrayList<String>();
                stopCommand.add(getJavaExecutable().getPath());
                stopCommand.add("-cp");
                stopCommand.add(getClassPath());
                stopCommand.addAll(getNuxeoProperties());
                stopCommand.addAll(getServerProperties());
                setServerStopCommand(stopCommand);
                for (String param : params) {
                    stopCommand.add(param);
                }
                ProcessBuilder pb = new ProcessBuilder(
                        getOSCommand(stopCommand));
                pb.directory(configurationGenerator.getNuxeoHome());
                // pb = pb.redirectErrorStream(true);
                log.debug("Server command: " + pb.command());
                try {
                    Process stopProcess = pb.start();
                    ArrayList<ThreadedStreamGobbler> sgArray = logProcessStreams(
                            stopProcess, logProcessOutput);
                    stopProcess.waitFor();
                    waitForProcessStreams(sgArray);
                    boolean wait = true;
                    while (wait) {
                        try {
                            if (stopProcess.exitValue() == 0) {
                                // Successful call for server stop
                                retry = false;
                            } else {
                                // Failed to call for server stop
                                retry = ++nbTry < STOP_NB_TRY;
                                if (!quiet) {
                                    System.out.print(".");
                                }
                                Thread.sleep(STOP_SECONDS_BEFORE_NEXT_TRY * 1000);
                            }
                            wait = false;
                        } catch (IllegalThreadStateException e) {
                            // Stop call is still running
                            wait = true;
                            if (!quiet) {
                                System.out.print(".");
                            }
                            Thread.sleep(1000);
                        }
                    }
                    // Exit if there's no way to check for server stop
                    if (processManager instanceof PureJavaProcessManager) {
                        log.warn("Can't check server status on your OS.");
                        return;
                    }
                    // Wait a few seconds for effective stop
                    deltaTime = 0;
                    do {
                        if (!quiet) {
                            System.out.print(".");
                        }
                        Thread.sleep(1000);
                        deltaTime = (new Date().getTime() - startTime) / 1000;
                    } while (!retry && getPid() != null
                            && deltaTime < stopMaxWait);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            } while (retry);
            if (getPid() == null) {
                log.warn("Server stopped.");
            } else {
                log.info("No answer from server, try to kill process " + pid
                        + "...");
                processManager.kill(nuxeoProcess, pid);
                if (getPid() == null) {
                    log.warn("Server forcibly stopped.");
                }
            }
        } catch (IOException e) {
            log.error("Could not manage process!", e);
        }
    }

    protected abstract void setServerStopCommand(List<String> command);

    private String getPid() throws IOException {
        pid = processManager.findPid(processRegex);
        log.debug("regexp: " + processRegex + " pid:" + pid);
        return pid;
    }

    /**
     * Configure the server after checking installation
     *
     * @throws ConfigurationException If an installation error is detected or if
     *             configuration fails
     */
    public void configure() throws ConfigurationException {
        configurationGenerator.verifyInstallation();
        configurationGenerator.run();
        overrideJavaTmpDir = Boolean.parseBoolean(configurationGenerator.getUserConfig().getProperty(
                OVERRIDE_JAVA_TMPDIR_PARAM, "true"));
    }

    /**
     * @return Default max wait depending on server (ie JBoss takes much more
     *         time than Tomcat)
     */
    private String getDefaultMaxWait() {
        return configurationGenerator.isJBoss ? START_MAX_WAIT_JBOSS_DEFAULT
                : START_MAX_WAIT_DEFAULT;
    }

    /**
     * Return process status (running or not) as String, depending on OS
     * capability to manage processes. Set status value following
     * "http://refspecs.freestandards.org/LSB_4.1.0/LSB-Core-generic/LSB-Core-generic/iniscrptact.html"
     *
     * @see #status
     */
    public String status() {
        if (processManager instanceof PureJavaProcessManager) {
            status = 4;
            return "Can't check server status on your OS.";
        }
        try {
            if (getPid() == null) {
                status = 3;
                return "Server is not running.";
            } else {
                status = 0;
                return "Server is running with process ID " + getPid() + ".";
            }
        } catch (IOException e) {
            status = 4;
            return "Could not check existing process (" + e.getMessage() + ").";
        }
    }

    /**
     * Last status value set by {@link #status()}.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Last error value set by any method.
     */
    public int getErrorValue() {
        return errorValue;
    }

    /**
     * @param args Program arguments
     * @return a NuxeoLauncher instance specific to current server (JBoss,
     *         Tomcat or Jetty).
     * @throws ConfigurationException If server cannot be identified
     * @since 5.5
     */
    public static NuxeoLauncher createLauncher(String[] args)
            throws ConfigurationException {
        NuxeoLauncher launcher;
        ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();
        if (configurationGenerator.isJBoss) {
            launcher = new NuxeoJBossLauncher(configurationGenerator);
        } else if (configurationGenerator.isJetty) {
            launcher = new NuxeoJettyLauncher(configurationGenerator);
        } else if (configurationGenerator.isTomcat) {
            launcher = new NuxeoTomcatLauncher(configurationGenerator);
        } else {
            throw new ConfigurationException("Unknown server !");
        }
        launcher.setArgs(args);
        configurationGenerator.init();
        launcher.statusServletClient = new StatusServletClient(
                configurationGenerator);
        launcher.statusServletClient.setKey(configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_STATUS_KEY));
        return launcher;
    }

    /**
     * Sets from program arguments the launcher command and additional
     * parameters.
     *
     * @param args Program arguments; may be used by launcher implementation.
     *            Must not be null or empty.
     */
    private void setArgs(String[] args) {
        args = readOptions(args);
        if (args.length == 0) {
            printHelp();
            System.exit(2);
        }
        command = args[0];
        int firstParamToKeep = 1;
        if ("gui".equalsIgnoreCase(command)
                || "nogui".equalsIgnoreCase(command)) {
            useGui = "gui".equalsIgnoreCase(command);
            command = args.length > 1 ? args[1] : null;
            firstParamToKeep = 2;
        }
        params = firstParamToKeep > args.length ? new String[0]
                : Arrays.copyOfRange(args, firstParamToKeep, args.length);
    }

    /**
     * Read options (i.e. parameters starting with one or two dashes)
     *
     * @param args arguments to read
     * @return the passed arguments without the options
     * @since 5.5
     */
    protected String[] readOptions(String[] args) {
        int nbOptions = 0;
        for (String arg : args) {
            if (arg.startsWith("-") || arg.startsWith("--")) {
                nbOptions++;
                if ("-d".equalsIgnoreCase(arg)
                        || "--debug".equalsIgnoreCase(arg)) {
                    setDebug();
                } else if ("-q".equalsIgnoreCase(arg)
                        || "--quiet".equalsIgnoreCase(arg)) {
                    setQuiet();
                } else {
                    log.error("Unknown option " + arg);
                }
            } else {
                break;
            }
        }
        return Arrays.copyOfRange(args, nbOptions, args.length);
    }

    /**
     * @param beQuiet if true, launcher will be in quiet mode
     * @since 5.5
     */
    protected void setQuiet() {
        quiet = true;
        Log4JHelper.setQuiet(Log4JHelper.CONSOLE_APPENDER_NAME);
    }

    /**
     * @param activateDebug if true, will activate the DEBUG logs
     * @since 5.5
     */
    protected void setDebug() {
        debug = true;
        Log4JHelper.setDebug("org.nuxeo.launcher", true, true, "FILE");
    }

    /**
     * Print class usage on standard system output.
     *
     * @throws URISyntaxException
     */
    public static void printHelp() {
        log.error("\nnuxeoctl usage:\n\tnuxeoctl [options] [gui|nogui] "
                + "[help|start|stop|restart|configure|wizard|console|status|startbg|restartbg|pack] [additional parameters]");
        log.error("\njava usage:\n\tjava [-D"
                + JAVA_OPTS_PROPERTY
                + "=\"JVM options\"] [-D"
                + ConfigurationGenerator.NUXEO_HOME
                + "=\"/path/to/nuxeo\"] [-D"
                + ConfigurationGenerator.NUXEO_CONF
                + "=\"/path/to/nuxeo.conf\"] [-Djvmcheck=nofail] -jar \"path/to/nuxeo-launcher.jar\""
                + " \\ \n\t\t[options] [gui] [help|start|stop|restart|configure|wizard|console|status|startbg|restartbg|pack] [additional parameters]");
        log.error("\n\t Java parameters:");
        log.error("\t\t " + JAVA_OPTS_PROPERTY
                + "\tParameters for the server JVM (default are "
                + JAVA_OPTS_DEFAULT + ").");
        log.error("\t\t "
                + ConfigurationGenerator.NUXEO_HOME
                + "\t\tNuxeo server root path (default is parent of called script).");
        log.error("\t\t "
                + ConfigurationGenerator.NUXEO_CONF
                + "\t\tPath to nuxeo.conf file (default is $NUXEO_HOME/bin/nuxeo.conf).");
        log.error("\t\t jvmcheck\t\tWill continue execution if equals to \"nofail\", else will exit.");
        log.error("\n\t Options:");
        log.error("\t\t -d, --debug\t\tActivate Launcher DEBUG logs.");
        log.error("\t\t -q, --quiet\t\tActivate quiet mode.");
        log.error("\n\t GUI options:");
        log.error("\t\t gui\t\t\tLauncher with a graphical user interface (default is headless/console mode except under Windows).");
        log.error("\t\t nogui\t\t\tDeactivate gui option which is set by default under Windows.");
        log.error("\n\t Commands:");
        log.error("\t\t help\t\t\tPrint this message.");
        log.error("\t\t start\t\t\tStart Nuxeo server in background, waiting for effective start. "
                + "Useful for batch executions requiring the server being immediately available after the script returned.");
        log.error("\t\t stop\t\t\tStop any Nuxeo server started with the same nuxeo.conf file.");
        log.error("\t\t restart\t\tRestart Nuxeo server.");
        log.error("\t\t configure\t\tConfigure Nuxeo server with parameters from nuxeo.conf.");
        log.error("\t\t wizard\t\t\tEnable the wizard (force the wizard to be played again in case the wizard configuration has already been done).");
        log.error("\t\t console\t\tStart Nuxeo server in a console mode. Ctrl-C will stop it.");
        log.error("\t\t status\t\t\tPrint server status (running or not).");
        log.error("\t\t startbg\t\tStart Nuxeo server in background, without waiting for effective start. Useful for starting Nuxeo as a service.");
        log.error("\t\t restartbg\t\tRestart Nuxeo server with a call to \"startbg\" after \"stop\".");
        log.error("\t\t pack <target>\t\tBuild a static archive. Same as the \"pack\" Shell script.");
        log.error("\t\t mp-list\t\tList marketplace packages.");
        log.error("\t\t mp-add\t\t\tAdd marketplace package(s) to local cache. You must provide the package file(s) as parameter.");
        log.error("\t\t mp-install\t\tRun marketplace package installation. "
                + "It is automatically called at startup if installAfterRestart.log exists. "
                + "Else you must provide the package file(s) or ID(s) as parameter.");
        log.error("\t\t mp-uninstall\t\tUninstall marketplace package(s). You must provide the package ID(s) as parameter (see \"mp-list\" command).");
        log.error("\t\t mp-remove\t\tRemove marketplace package(s). You must provide the package ID(s) as parameter (see \"mp-list\" command).");
        log.error("\t\t mp-reset\t\tReset all packages to DOWNLOADED state. May be useful after a manual server upgrade.");
        log.error("\n\t Additional parameters: All parameters following a command are passed to the java process when executing the command.");
    }

    /**
     * Work best with current nuxeoProcess. If nuxeoProcess is null or has
     * exited, then will try to get process ID (so, result in that case depends
     * on OS capabilities).
     *
     * @return true if current process is running or if a running PID is found
     */
    public boolean isRunning() {
        if (nuxeoProcess != null) {
            try {
                nuxeoProcess.exitValue();
                // Previous process has exited
                nuxeoProcess = null;
            } catch (IllegalThreadStateException exception) {
                return true;
            }
        }
        try {
            return (getPid() != null);
        } catch (IOException e) {
            log.error(e);
            return false;
        }
    }

    /**
     * @since 5.5
     * @return true if Nuxeo finished starting
     */
    public boolean isStarted() {
        boolean isStarted;
        if (configurationGenerator.isWizardRequired()) {
            isStarted = isRunning();
        } else {
            try {
                isStarted = isRunning() && statusServletClient.isStarted();
            } catch (SocketTimeoutException e) {
                isStarted = false;
            }
        }
        return isStarted;
    }

    /**
     * @return Server log file
     */
    public File getLogFile() {
        return new File(configurationGenerator.getLogDir(), "server.log");
    }

    /**
     * @return Server URL
     */
    public String getURL() {
        return configurationGenerator.getUserConfig().getProperty(
                PARAM_NUXEO_URL);
    }

}
