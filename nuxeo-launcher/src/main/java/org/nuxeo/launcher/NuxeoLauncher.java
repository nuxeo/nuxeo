/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.nuxeo.log4j.ThreadedStreamGobbler;

/**
 * @author jcarsique
 * @since 5.4.2
 */
public abstract class NuxeoLauncher {
    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    public static final long DEFAULT_RETRY_TIMEOUT = 120000L;

    public static final long DEFAULT_RETRY_INTERVAL = 250L;

    private static final String JAVA_OPTS_PROPERTY = "launcher.java.opts";

    private static final String JAVA_OPTS_DEFAULT = "-Xms512m -Xmx1024m -XX:MaxPermSize=512m";

    private static final String OVERRIDE_JAVA_TMPDIR_PARAM = "launcher.override.java.tmpdir";

    protected boolean overrideJavaTmpDir;

    private static final String START_MAX_WAIT_PARAM = "launcher.start.max.wait";

    /**
     * Default maximum time to wait for server startup summary in logs (in
     * seconds).
     */
    private static final String START_MAX_WAIT_DEFAULT = "300";

    private static final String START_MAX_WAIT_JBOSS_DEFAULT = "900";

    /**
     * Max time to wait for effective stop (in seconds)
     */
    private static final int STOP_MAX_WAIT = 10;

    /**
     * Number of try to cleanly stop server before killing process
     */
    private static final int STOP_NB_TRY = 5;

    private static final int STOP_SECONDS_BEFORE_NEXT_TRY = 2;

    private static final int MAX_WAIT_LOGFILE = 20;

    private static final String PARAM_NUXEO_URL = "nuxeo.url";

    private static final String INSTALLER_CLASS = "org.nuxeo.ecm.admin.offline.update.Main";

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

    private boolean reloadConfiguration = false;

    private int status = 4;

    private int errorValue = 0;

    public NuxeoLauncher(ConfigurationGenerator configurationGenerator) {
        this.configurationGenerator = configurationGenerator;
        processManager = getOSProcessManager();
        processRegex = Pattern.quote(configurationGenerator.getNuxeoConf().getPath())
                + ".*" + Pattern.quote(getServerPrint());
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
            log.info("Server started with process ID " + pid + ".");
        } else {
            log.info("Sent server start command but could not get process ID.");
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

    public void logProcessStreams(Process process, boolean logProcessOutput) {
        new ThreadedStreamGobbler(process.getInputStream(),
                logProcessOutput ? SimpleLog.LOG_LEVEL_INFO
                        : SimpleLog.LOG_LEVEL_OFF).start();
        new ThreadedStreamGobbler(process.getErrorStream(),
                logProcessOutput ? SimpleLog.LOG_LEVEL_ERROR
                        : SimpleLog.LOG_LEVEL_OFF).start();
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
            String shellPath;
            if (new File("/bin/bash").exists()) {
                shellPath = "/bin/bash";
            } else {
                shellPath = "/bin/sh";
            }
            osCommand.add(shellPath);
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
            throw new RuntimeException(
                    "Tried to add inexistent classpath entry: "
                            + classPathEntry);
        }
        cp += System.getProperty("path.separator") + classPathEntry.getPath();
        return cp;
    }

    public static void main(String[] args) throws ConfigurationException {
        int exitStatus = 0;
        if (args.length == 0) {
            printHelp();
            return;
        }
        final NuxeoLauncher launcher = createLauncher(args);
        NuxeoLauncherGUI launcherGUI = null;
        String command = launcher.command;
        boolean commandSucceeded = true;
        if (launcher.useGui) {
            launcherGUI = new NuxeoLauncherGUI(launcher);
            command = launcherGUI.execute();
        }
        if (command == null) {
            // Nothing to do
        } else if ("help".equalsIgnoreCase(command)) {
            printHelp();
        } else if ("status".equalsIgnoreCase(command)) {
            log.info(launcher.status());
            exitStatus = launcher.status;
        } else if ("startbg".equalsIgnoreCase(command)) {
            commandSucceeded = launcher.doStart();
        } else if ("start".equalsIgnoreCase(command)) {
            commandSucceeded = launcher.doStartAndWait();
        } else if ("console".equalsIgnoreCase(command)) {
            launcher.executor.execute(new Runnable() {
                public void run() {
                    launcher.addShutdownHook();
                    if (!launcher.doStart(true)) {
                        launcher.removeShutdownHook();
                        System.exit(1);
                    }
                }
            });
        } else if ("stop".equalsIgnoreCase(command)) {
            launcher.stop();
        } else if ("restartbg".equalsIgnoreCase(command)) {
            launcher.stop();
            commandSucceeded = launcher.doStart();
        } else if ("restart".equalsIgnoreCase(command)) {
            launcher.stop();
            commandSucceeded = launcher.doStartAndWait();
        } else if ("wizard".equalsIgnoreCase(command)) {
            commandSucceeded = launcher.startWizard();
        } else if ("configure".equalsIgnoreCase(command)) {
            try {
                launcher.configure();
            } catch (ConfigurationException e) {
                log.error(e);
                commandSucceeded = false;
            }
        } else if ("pack".equalsIgnoreCase(command)) {
            exitStatus = 3;
        } else {
            printHelp();
            commandSucceeded = false;
        }
        if (launcher.useGui) {
            launcherGUI.updateServerStatus();
        }
        if (!commandSucceeded) {
            exitStatus = launcher.errorValue;
        }
        if (exitStatus != 0) {
            System.exit(exitStatus);
        }
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
            if (!configurationGenerator.isWizardRequired()
                    && !waitForEffectiveStart()) {
                removeShutdownHook();
                stop(logProcessOutput);
            } else {
                removeShutdownHook();
                commandSucceeded = true;
            }
        }
        return commandSucceeded;
    }

    protected void removeShutdownHook() {
        log.debug("Remove shutdown hook");
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // the virtual machine is already in the process of shutting down
        }
    }

    protected boolean waitForEffectiveStart() {
        long startTime = new Date().getTime();
        Pattern nuxeoStartedPattern = Pattern.compile(".*OSGiRuntimeService.*Nuxeo EP Started");
        Pattern separatorPattern = Pattern.compile("======================================================================");
        File logFile = new File(configurationGenerator.getLogDir(),
                "server.log");
        final StringBuilder startSummary = new StringBuilder();
        final String newLine = System.getProperty("line.separator");
        BufferedReader in = null;
        try {
            // Wait for logfile creation
            int notfound = 0;
            while (notfound++ < MAX_WAIT_LOGFILE && !logFile.exists()) {
                System.out.print(".");
                Thread.sleep(1000);
            }
            try {
                in = new BufferedReader(new FileReader(logFile));
            } catch (FileNotFoundException e) {
                log.error("Waited for " + MAX_WAIT_LOGFILE + "s but "
                        + e.getMessage());
                return false;
            }
            int count = 0;
            int countStatus = 0;
            boolean countActive = false;
            String line;
            // Go to end of file
            while (in.readLine() != null)
                ;
            int startMaxWait = Integer.parseInt(configurationGenerator.getUserConfig().getProperty(
                    START_MAX_WAIT_PARAM, getDefaultMaxWait()));
            log.debug("Will wait for effective start during " + startMaxWait
                    + " seconds.");
            do {
                // Wait for something to read
                while (!in.ready() && count < startMaxWait && isRunning()) {
                    System.out.print(".");
                    count++;
                    Thread.sleep(1000);
                }
                line = in.readLine();
                if (line != null && nuxeoStartedPattern.matcher(line).matches()) {
                    countActive = true;
                }
                if (countActive) {
                    if (line != null
                            && separatorPattern.matcher(line).matches()) {
                        countStatus++;
                    }
                    if (countStatus > 0) {
                        startSummary.append(newLine + line);
                    }
                }
            } while (countStatus < 3 && count < startMaxWait && isRunning());
            if (countStatus == 3) {
                long duration = (new Date().getTime() - startTime) / 1000;
                startSummary.append(newLine
                        + "Started in "
                        + String.format("%dmin%02ds", new Long(duration / 60),
                                new Long(duration % 60)));
                System.out.println(startSummary);
                return true;
            } else {
                log.error("Starting process is taking too long - giving up.");
            }
        } catch (FileNotFoundException e) {
            log.error("Unable to open " + logFile.getPath(), e);
        } catch (IOException e) {
            log.error(e);
        } catch (InterruptedException e) {
            log.debug(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return false;
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
            if ((PlatformUtils.isWindows() || "true".equalsIgnoreCase(configurationGenerator.getUserConfig().getProperty(
                    ConfigurationGenerator.PARAM_FAKE_WINDOWS, "false")))
                    && configurationGenerator.isInstallInProgress()) {
                log.debug("Install in progress...");
                install();
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

    private void install() throws IOException, InterruptedException {
        List<String> startCommand = new ArrayList<String>();
        startCommand.add(getJavaExecutable().getPath());
        startCommand.addAll(Arrays.asList(getJavaOptsProperty().split(" ")));
        startCommand.add("-cp");
        File tmpDir = File.createTempFile("install", null);
        startCommand.add(getInstallClassPath(tmpDir));
        startCommand.addAll(getNuxeoProperties());
        startCommand.add("-Dnuxeo.runtime.home="
                + configurationGenerator.getRuntimeHome().getPath());
        startCommand.add(INSTALLER_CLASS);
        startCommand.add(tmpDir.getPath());
        startCommand.add(configurationGenerator.getInstallFile().getPath());
        ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
        pb.directory(configurationGenerator.getNuxeoHome());
        log.debug("Install command: " + pb.command());
        Process installProcess = pb.start();
        logProcessStreams(installProcess, false);
        Thread.sleep(1000);
        installProcess.waitFor();
    }

    /**
     * Copy required JARs into temporary directory and return it as classpath
     *
     * @param tmpDir temporary directory hosting classpath
     * @throws IOException If temporary directory could not be created.
     */
    private String getInstallClassPath(File tmpDir) throws IOException {
        String cp = ".";
        tmpDir.delete();
        tmpDir.mkdirs();
        File baseDir = new File(configurationGenerator.getRuntimeHome(),
                "bundles");
        String[] filenames = new String[] { "nuxeo-runtime-osgi",
                "nuxeo-runtime", "nuxeo-common", "nuxeo-connect-update",
                "nuxeo-connect-client", "nuxeo-connect-offline-update",
                "nuxeo-connect-client-wrapper", "nuxeo-runtime-reload" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = configurationGenerator.getServerConfigurator().getNuxeoLibDir();
        filenames = new String[] { "commons-io", "commons-jexl", "groovy-all",
                "osgi-core", "xercesImpl" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = configurationGenerator.getServerConfigurator().getServerLibDir();
        filenames = new String[] { "commons-logging", "log4j" };
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
    private String getTempClassPath(File tmpDir, String classpath,
            File baseDir, String[] filenames) throws IOException {
        File targetDir = new File(tmpDir, baseDir.getName());
        targetDir.mkdirs();
        for (final String filePattern : filenames) {
            File[] files = baseDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File basedir, String filename) {
                    return filename.matches(filePattern + "(-[0-9].*)?\\.jar");
                }
            });
            FileUtils.copyFileToDirectory(files[0], targetDir);
            File classPathEntry = new File(targetDir, files[0].getName());
            classpath += System.getProperty("path.separator")
                    + classPathEntry.getPath();
        }
        return classpath;
    }

    protected class ShutdownThread extends Thread {

        private NuxeoLauncher launcher;

        public ShutdownThread(NuxeoLauncher launcher) {
            super();
            this.launcher = launcher;
        }

        public void run() {
            log.debug("Shutting down...");
            launcher.stop();
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
        try {
            if (!(processManager instanceof PureJavaProcessManager)
                    && getPid() == null) {
                log.info("Server is not running.");
                return;
            }
            System.out.print("Stopping server...");
            int nbTry = 0;
            boolean retry = false;
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
                    logProcessStreams(stopProcess, logProcessOutput);
                    stopProcess.waitFor();
                    boolean wait = true;
                    while (wait) {
                        try {
                            if (stopProcess.exitValue() == 0) {
                                // Successful call for server stop
                                retry = false;
                            } else {
                                // Failed to call for server stop
                                retry = ++nbTry < STOP_NB_TRY;
                                System.out.print(".");
                                Thread.sleep(STOP_SECONDS_BEFORE_NEXT_TRY * 1000);
                            }
                            wait = false;
                        } catch (IllegalThreadStateException e) {
                            // Stop call is still running
                            wait = true;
                            System.out.print(".");
                            Thread.sleep(1000);
                        }
                    }
                    // Exit if there's no way to check for server stop
                    if (processManager instanceof PureJavaProcessManager) {
                        log.info("Can't check server status on your OS.");
                        return;
                    }
                    // Wait a few seconds for effective stop
                    for (int i = 0; !retry && getPid() != null
                            && i < STOP_MAX_WAIT; i++) {
                        System.out.print(".");
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    log.error(e);
                }
            } while (retry);
            if (getPid() == null) {
                log.info("Server stopped.");
            } else {
                log.info("No answer from server, try to kill process " + pid
                        + "...");
                processManager.kill(nuxeoProcess, pid);
                if (getPid() == null) {
                    log.info("Server forcibly stopped.");
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
     * capability to manage processes.
     * Set status value following
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
     */
    private static NuxeoLauncher createLauncher(String[] args)
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
     * Print class usage on standard system output.
     *
     * @throws URISyntaxException
     */
    public static void printHelp() {
        log.error("\nnuxeoctl usage:\n\tnuxeoctl [gui|nogui] (help|start|stop|restart|configure|wizard|console|status|startbg|restartbg|pack) [additional parameters]");
        log.error("\njava usage:\n\tjava [-D"
                + JAVA_OPTS_PROPERTY
                + "=\"JVM options\"] [-D"
                + ConfigurationGenerator.NUXEO_HOME
                + "=\"/path/to/nuxeo\"] [-D"
                + ConfigurationGenerator.NUXEO_CONF
                + "=\"/path/to/nuxeo.conf\"] [-Djvmcheck=nofail] -jar \"path/to/nuxeo-launcher.jar\""
                + " \\ \n\t\t[gui] (help|start|stop|restart|configure|wizard|console|status|startbg|restartbg|pack) [additional parameters]");
        log.error("\n\t Options:");
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
        log.error("\t\t gui\t\t\tLauncher with a graphical user interface (default is headless/console mode).");
        log.error("\t\t nogui\t\t\tDeactivate gui option which is set by default under Windows.");
        log.error("\n\t Commands:");
        log.error("\t\t help\t\tPrint this message.");
        log.error("\t\t start\t\tStart Nuxeo server in background, waiting for effective start. Useful for batch executions requiring the server being immediately available after the script returned.");
        log.error("\t\t stop\t\tStop any Nuxeo server started with the same nuxeo.conf file.");
        log.error("\t\t restart\tRestart Nuxeo server.");
        log.error("\t\t configure\tConfigure Nuxeo server with parameters from nuxeo.conf.");
        log.error("\t\t wizard\tEnable the wizard (force the wizard to be played again in case the wizard configuration has already been done).");
        log.error("\t\t console\tStart Nuxeo server in a console mode. Ctrl-C will stop it.");
        log.error("\t\t status\t\tPrint server status (running or not).");
        log.error("\t\t startbg\tStart Nuxeo server in background, without waiting for effective start. Useful for starting Nuxeo as a service.");
        log.error("\t\t restartbg\tRestart Nuxeo server with a call to \"startbg\" after \"stop\".");
        log.error("\t\t pack\t\tNot implemented. Use \"pack\" Shell script.");
        log.error("\n\t Additional parameters: All parameters following a command are passed to the java process when executing the command.");
    }

    /**
     * Work best with current nuxeoProcess. If nuxeoProcess is null, will try to
     * get process ID (so, result in that case depends on OS capabilities).
     *
     * @return true if current process is running or if a running PID is found
     */
    public boolean isRunning() {
        if (nuxeoProcess == null) {
            try {
                return (getPid() != null);
            } catch (IOException e) {
                log.error(e);
                return false;
            }
        }
        try {
            nuxeoProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException exception) {
            return true;
        }
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