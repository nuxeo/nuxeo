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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.artofsolving.jodconverter.process.MacProcessManager;
import org.artofsolving.jodconverter.process.ProcessManager;
import org.artofsolving.jodconverter.process.PureJavaProcessManager;
import org.artofsolving.jodconverter.process.UnixProcessManager;
import org.artofsolving.jodconverter.process.WindowsProcessManager;
import org.artofsolving.jodconverter.util.PlatformUtils;
import org.json.JSONException;
import org.json.XML;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.connect.StandaloneCallbackHolder;
import org.nuxeo.launcher.connect.StandalonePackageManager;
import org.nuxeo.launcher.daemon.DaemonThreadFactory;
import org.nuxeo.launcher.gui.NuxeoLauncherGUI;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.CommandSetInfo;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.MessageInfo;
import org.nuxeo.launcher.info.PackageInfo;
import org.nuxeo.launcher.monitoring.StatusServletClient;
import org.nuxeo.log4j.Log4JHelper;
import org.nuxeo.log4j.ThreadedStreamGobbler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author jcarsique
 * @since 5.4.2
 */
public abstract class NuxeoLauncher {

    // Fallback to avoid an error when the log dir is not initialized
    static {
        if (System.getProperty(Environment.NUXEO_LOG_DIR) == null) {
            System.setProperty(Environment.NUXEO_LOG_DIR, ".");
        }
    }

    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    private static Options launcherOptions = null;

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

    protected static String[] params;

    protected String command;

    public String getCommand() {
        return command;
    }

    public CommandSetInfo cset = new CommandSetInfo();

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

    private static boolean quiet = false;

    private static boolean debug = false;

    private static boolean xmlOutput = false;

    private static boolean jsonOutput = false;

    private StandalonePackageManager packageManager = null;

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
                        "A server is running with process ID " + existingPid);
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

    /**
     * @since 5.6
     */
    protected abstract String getShutdownClassPath();

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

    /**
     * @since 5.6
     */
    protected static void initParserOptions() {
        if (launcherOptions == null) {
            launcherOptions = new Options();
            // help option
            OptionBuilder.withLongOpt("help");
            OptionBuilder.withDescription("Show detailed help");
            launcherOptions.addOption(OptionBuilder.create("h"));
            // Quiet option
            OptionBuilder.withLongOpt("quiet");
            OptionBuilder.withDescription("Suppress information messages");
            launcherOptions.addOption(OptionBuilder.create("q"));
            // Debug option
            OptionBuilder.withLongOpt("debug");
            OptionBuilder.withDescription("Activate debug messages");
            launcherOptions.addOption(OptionBuilder.create("d"));
            // XML option
            OptionBuilder.withLongOpt("xml");
            OptionBuilder.withDescription("Output XML for mp-commands");
            launcherOptions.addOption(OptionBuilder.create());
            // JSON option
            OptionBuilder.withLongOpt("json");
            OptionBuilder.withDescription("Output JSON for mp-commands");
            launcherOptions.addOption(OptionBuilder.create());
            // GUI option
            OptionBuilder.withLongOpt("gui");
            OptionBuilder.hasArg();
            OptionBuilder.withArgName("true|false");
            OptionBuilder.withDescription("Use graphical interface");
            launcherOptions.addOption(OptionBuilder.create());
        }
    }

    /**
     * @since 5.6
     */
    protected static CommandLine parseOptions(String[] args)
            throws ParseException {
        initParserOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = null;
        Boolean stopAfterParsing = false;
        try {
            cmdLine = parser.parse(launcherOptions, args);
            if (cmdLine.hasOption("h")) {
                printLongHelp();
                stopAfterParsing = true;
            } else if (cmdLine.getArgs().length == 0) {
                printShortHelp();
                stopAfterParsing = true;
            } else if (cmdLine.getArgs()[0].equals("help")) {
                printLongHelp();
                stopAfterParsing = true;
            }
        } catch (UnrecognizedOptionException e) {
            log.error(e.getMessage());
            printShortHelp();
            stopAfterParsing = true;
        } catch (MissingArgumentException e) {
            log.error(e.getMessage());
            printShortHelp();
            stopAfterParsing = true;
        } catch (ParseException e) {
            log.error("Error while parsing command line: " + e.getMessage());
            printShortHelp();
            stopAfterParsing = true;
        } finally {
            if (stopAfterParsing) {
                throw new ParseException("Invalid command line");
            }
        }
        return cmdLine;
    }

    public static void main(String[] args) throws ConfigurationException {
        CommandLine cmdLine = null;
        try {
            cmdLine = parseOptions(args);
        } catch (ParseException e) {
            System.exit(1);
        }
        if (cmdLine.hasOption("q")) {
            setQuiet();
        }
        if (cmdLine.hasOption("d")) {
            setDebug();
        }
        if (cmdLine.hasOption("xml")) {
            setXMLOutput();
        }
        if (cmdLine.hasOption("json")) {
            setJSONOutput();
        }
        final NuxeoLauncher launcher = createLauncher(cmdLine);
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
        } else if ("mp-list".equalsIgnoreCase(launcher.command)) {
            launcher.pkgList();
            launcher.printXMLOutput();
        } else if ("mp-add".equalsIgnoreCase(launcher.command)) {
            launcher.checkNoRunningServer();
            commandSucceeded = commandSucceeded && launcher.pkgAdd(params);
            launcher.printXMLOutput();
        } else if ("mp-install".equalsIgnoreCase(launcher.command)) {
            launcher.checkNoRunningServer();
            commandSucceeded = commandSucceeded && launcher.pkgInstall(params);
            launcher.printXMLOutput();
        } else if ("mp-uninstall".equalsIgnoreCase(launcher.command)) {
            launcher.checkNoRunningServer();
            commandSucceeded = commandSucceeded
                    && launcher.pkgUninstall(params);
            launcher.printXMLOutput();
        } else if ("mp-remove".equalsIgnoreCase(launcher.command)) {
            launcher.checkNoRunningServer();
            commandSucceeded = commandSucceeded && launcher.pkgRemove(params);
            launcher.printXMLOutput();
        } else if ("mp-reset".equalsIgnoreCase(launcher.command)) {
            launcher.checkNoRunningServer();
            commandSucceeded = commandSucceeded && launcher.pkgReset();
            launcher.printXMLOutput();
        } else if ("showconf".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = commandSucceeded && launcher.showConfig();
        } else if ("testparser".equalsIgnoreCase(launcher.command)) {
            launcher.testparser();
        } else {
            printLongHelp();
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
            configurationGenerator = new ConfigurationGenerator(quiet, debug);
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
                configurationGenerator = new ConfigurationGenerator(quiet,
                        debug);
                configurationGenerator.init();
            } else {
                // Ensure reload on next start
                reloadConfiguration = true;
            }
            checkNoRunningServer();
            configure();
            configurationGenerator.verifyInstallation();

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
                getPackageManager().executePending(
                        configurationGenerator.getInstallFile(), true);
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

    /**
     * @since 5.6
     */
    protected void printXMLOutput() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    CommandSetInfo.class, CommandInfo.class, PackageInfo.class,
                    MessageInfo.class);
            printXMLOutput(jaxbContext, cset);
        } catch (JAXBException e) {
            log.error("Output serialization failed: " + e.getMessage());
            errorValue = 7;
        }
    }

    /**
     * @since 5.6
     */
    protected void printXMLOutput(JAXBContext jaxbContext, Object objectToOutput) {
        if (!xmlOutput) {
            return;
        }
        try {
            Writer xml = new StringWriter();
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(objectToOutput, xml);
            if (!jsonOutput) {
                System.out.println(xml.toString());
            } else {
                try {
                    System.out.println(XML.toJSONObject(xml.toString()).toString(
                            2));
                } catch (JSONException e) {
                    log.error(String.format(
                            "XML to JSON conversion failed: %s\nOutput was:\n%s",
                            e.getMessage(), xml.toString()));
                }
            }
        } catch (JAXBException e) {
            log.error("Output serialization failed: " + e.getMessage());
            e.printStackTrace();
            errorValue = 7;
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
                stopCommand.add(getShutdownClassPath());
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
        configurationGenerator.checkJavaVersion();
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
     * @throws ParseException
     * @throws ConfigurationException
     * @deprecated since 5.6; use {@link #createLauncher(CommandLine)} instead
     * @since 5.5
     */
    @Deprecated
    public static NuxeoLauncher createLauncher(String[] args)
            throws ConfigurationException, ParseException {
        return createLauncher(parseOptions(args));
    }

    /**
     * @param cmdLine Program arguments
     * @return a NuxeoLauncher instance specific to current server (JBoss,
     *         Tomcat or Jetty).
     * @throws ConfigurationException If server cannot be identified
     * @since 5.6
     */
    public static NuxeoLauncher createLauncher(CommandLine cmdLine)
            throws ConfigurationException {
        NuxeoLauncher launcher;
        ConfigurationGenerator cg = new ConfigurationGenerator(quiet, debug);
        if (cg.isJBoss) {
            launcher = new NuxeoJBossLauncher(cg);
        } else if (cg.isJetty) {
            launcher = new NuxeoJettyLauncher(cg);
        } else if (cg.isTomcat) {
            launcher = new NuxeoTomcatLauncher(cg);
        } else {
            throw new ConfigurationException("Unknown server!");
        }
        launcher.setArgs(cmdLine);
        cg.init();
        launcher.statusServletClient = new StatusServletClient(cg);
        launcher.statusServletClient.setKey(cg.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_STATUS_KEY));
        return launcher;
    }

    /**
     * Sets from program arguments the launcher command and additional
     * parameters.
     *
     * @param cmdLine Program arguments; may be used by launcher implementation.
     *            Must not be null or empty.
     */
    private void setArgs(CommandLine cmdLine) {
        String[] argList = cmdLine.getArgs();
        command = argList[0];
        log.debug("Launcher command: " + command);
        if (argList.length > 1) {
            params = Arrays.copyOfRange(argList, 1, argList.length);
        } else {
            params = new String[0];
        }
        for (String param : params) {
            log.debug("Command parameter: " + param);
        }
        if (cmdLine.hasOption("gui")) {
            useGui = Boolean.valueOf(cmdLine.getOptionValue("gui"));
            log.debug("GUI: " + cmdLine.getOptionValue("gui") + " -> "
                    + new Boolean(useGui).toString());
        } else {
            if (PlatformUtils.isWindows()) {
                useGui = true;
                log.debug("GUI: option not set - platform is Windows -> start GUI");
            } else {
                useGui = false;
                log.debug("GUI: option not set - platform is not Windows -> do not start GUI");
            }
        }
    }

    /**
     * @param beQuiet if true, launcher will be in quiet mode
     * @since 5.5
     */
    protected static void setQuiet() {
        quiet = true;
        Log4JHelper.setQuiet(Log4JHelper.CONSOLE_APPENDER_NAME);
    }

    /**
     * @param activateDebug if true, will activate the DEBUG logs
     * @since 5.5
     */
    protected static void setDebug() {
        debug = true;
        Log4JHelper.setDebug("org.nuxeo.launcher", true, true, "FILE");
    }

    protected static void setXMLOutput() {
        xmlOutput = true;
        setQuiet();
    }

    protected static void setJSONOutput() {
        jsonOutput = true;
        setXMLOutput();
    }

    public static void printShortHelp() {
        initParserOptions();
        String cmdLineSyntax = "nuxeoctl [options] <command> [command parameters]";
        HelpFormatter help = new HelpFormatter();
        help.setSyntaxPrefix("Usage: ");
        help.printHelp(cmdLineSyntax, launcherOptions);
    }

    public static void printLongHelp() {
        printShortHelp();
        log.error("\n\nJava usage:\n\tjava [-D"
                + JAVA_OPTS_PROPERTY
                + "=\"JVM options\"] [-D"
                + Environment.NUXEO_HOME
                + "=\"/path/to/nuxeo\"] [-D"
                + ConfigurationGenerator.NUXEO_CONF
                + "=\"/path/to/nuxeo.conf\"] [-Djvmcheck=nofail] -jar \"path/to/nuxeo-launcher.jar\""
                + " \\ \n\t\t[options] <command> [command parameters]");
        log.error("\n\t" + JAVA_OPTS_PROPERTY
                + "\tParameters for the server JVM (default are "
                + JAVA_OPTS_DEFAULT + ").");
        log.error("\t"
                + Environment.NUXEO_HOME
                + "\t\tNuxeo server root path (default is parent of called script).");
        log.error("\t"
                + ConfigurationGenerator.NUXEO_CONF
                + "\t\tPath to nuxeo.conf file (default is $NUXEO_HOME/bin/nuxeo.conf).");
        log.error("\tjvmcheck\t\tIf set to \"nofail\", ignore JVM version validation errors.");
        log.error("\n\nCommands list:");
        log.error("\thelp\t\t\tPrint this message.");
        log.error("\tstart\t\t\tStart Nuxeo server in background, waiting for effective start. "
                + "Useful for batch executions requiring the server being immediately available after the script returned.");
        log.error("\tstop\t\t\tStop any Nuxeo server started with the same nuxeo.conf file.");
        log.error("\trestart\t\t\tRestart Nuxeo server.");
        log.error("\tconfigure\t\tConfigure Nuxeo server with parameters from nuxeo.conf.");
        log.error("\twizard\t\t\tEnable the wizard (force the wizard to be played again in case the wizard configuration has already been done).");
        log.error("\tconsole\t\t\tStart Nuxeo server in a console mode. Ctrl-C will stop it.");
        log.error("\tstatus\t\t\tPrint server status (running or not).");
        log.error("\tstartbg\t\t\tStart Nuxeo server in background, without waiting for effective start. Useful for starting Nuxeo as a service.");
        log.error("\trestartbg\t\tRestart Nuxeo server with a call to \"startbg\" after \"stop\".");
        log.error("\tpack <target>\t\tBuild a static archive. Same as the \"pack\" Shell script.");
        log.error("\tshowconf\t\tDisplay the instance configuration.");
        log.error("\tmp-list\t\t\tList marketplace packages.");
        log.error("\tmp-add\t\t\tAdd marketplace package(s) to local cache. You must provide the package file(s) as parameter.");
        log.error("\tmp-install\t\tRun marketplace package installation. "
                + "It is automatically called at startup if installAfterRestart.log exists. "
                + "Else you must provide the package file(s) or ID(s) as parameter.");
        log.error("\tmp-uninstall\t\tUninstall marketplace package(s). You must provide the package ID(s) as parameter (see \"mp-list\" command).");
        log.error("\tmp-remove\t\tRemove marketplace package(s). You must provide the package ID(s) as parameter (see \"mp-list\" command).");
        log.error("\tmp-reset\t\tReset all packages to DOWNLOADED state. May be useful after a manual server upgrade.");
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

    protected Environment getDistributionEnvironment() {
        Environment env = new Environment(
                configurationGenerator.getRuntimeHome());
        env.init();
        env.setServerHome(configurationGenerator.getNuxeoHome());
        env.setData(new File(
                configurationGenerator.getUserConfig().getProperty(
                        Environment.NUXEO_DATA_DIR)));
        env.setLog(new File(configurationGenerator.getUserConfig().getProperty(
                Environment.NUXEO_LOG_DIR)));
        env.setTemp(new File(
                configurationGenerator.getUserConfig().getProperty(
                        Environment.NUXEO_TMP_DIR)));
        env.loadProperties(configurationGenerator.getUserConfig());
        return env;
    }

    protected StandalonePackageManager getPackageManager() {
        if (packageManager == null) {
            packageManager = new StandalonePackageManager(
                    getDistributionEnvironment());
        }
        return packageManager;
    }

    protected void pkgList() {
        StandalonePackageManager pkgman = getPackageManager();
        pkgman.listPending(configurationGenerator.getInstallFile());
        pkgman.pkgList();
        cset = pkgman.getCommandSet();
    }

    protected boolean pkgAdd(String[] params) {
        StandalonePackageManager pkgman = getPackageManager();
        boolean cmdOK = true;
        LocalPackage pkg;
        for (String param : params) {
            pkg = pkgman.pkgAdd(param);
            cmdOK = cmdOK && (pkg != null);
        }
        cset = pkgman.getCommandSet();
        if (!cmdOK) {
            errorValue = 3;
        }
        return cmdOK;
    }

    protected boolean pkgInstall(String[] params) {
        StandalonePackageManager pkgman = getPackageManager();
        boolean cmdOK = true;
        if (configurationGenerator.isInstallInProgress()) {
            cmdOK = pkgman.executePending(
                    configurationGenerator.getInstallFile(), true);
        }
        LocalPackage pkg;
        for (String param : params) {
            pkg = pkgman.pkgInstall(param);
            cmdOK = cmdOK && (pkg != null);
        }
        cset = pkgman.getCommandSet();
        if (!cmdOK) {
            errorValue = 3;
        }
        return cmdOK;
    }

    protected boolean pkgUninstall(String[] params) {
        StandalonePackageManager pkgman = getPackageManager();
        boolean cmdOK = true;
        LocalPackage pkg;
        for (String param : params) {
            pkg = pkgman.pkgUninstall(param);
            cmdOK = cmdOK && (pkg != null);
        }
        cset = pkgman.getCommandSet();
        if (!cmdOK) {
            errorValue = 3;
        }
        return cmdOK;
    }

    protected boolean pkgRemove(String[] params) {
        StandalonePackageManager pkgman = getPackageManager();
        boolean cmdOK = true;
        LocalPackage pkg;
        for (String param : params) {
            pkg = pkgman.pkgRemove(param);
            cmdOK = cmdOK && (pkg != null);
        }
        cset = pkgman.getCommandSet();
        if (!cmdOK) {
            errorValue = 3;
        }
        return cmdOK;
    }

    protected boolean pkgReset() {
        StandalonePackageManager pkgman = getPackageManager();
        boolean cmdOK = pkgman.pkgReset();
        cset = pkgman.getCommandSet();
        if (!cmdOK) {
            errorValue = 3;
        }
        return cmdOK;
    }

    /**
     * @since 5.6
     */
    protected void printInstanceXMLOutput(InstanceInfo instance) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    InstanceInfo.class, DistributionInfo.class,
                    PackageInfo.class, ConfigurationInfo.class,
                    KeyValueInfo.class);
            printXMLOutput(jaxbContext, instance);
        } catch (JAXBException e) {
            e.printStackTrace();
            log.error("Output serialization failed: " + e.getMessage());
            errorValue = 7;
        }
    }

    /**
     * @since 5.6
     */
    protected boolean showConfig() {
        InstanceInfo nxInstance = new InstanceInfo();
        log.info("***** Nuxeo instance configuration *****");
        nxInstance.NUXEO_CONF = configurationGenerator.getNuxeoConf().getPath();
        log.info("NUXEO_CONF: " + nxInstance.NUXEO_CONF);
        nxInstance.NUXEO_HOME = configurationGenerator.getNuxeoHome().getPath();
        log.info("NUXEO_HOME: " + nxInstance.NUXEO_HOME);
        // CLID
        try {
            CallbackHolder cbHolder = new StandaloneCallbackHolder(
                    getDistributionEnvironment(),
                    getPackageManager().getUpdateService());
            NuxeoConnectClient.setCallBackHolder(cbHolder);
            nxInstance.clid = LogicalInstanceIdentifier.instance().getCLID();
            log.info("Instance CLID: " + nxInstance.clid);
        } catch (NoCLID e) {
            // leave nxInstance.clid unset
        } catch (Exception e) {
            // something went wrong in the NuxeoConnectClient initialization
            errorValue = 4;
            log.error("Could not initialize NuxeoConnectClient", e);
            return false;
        }
        // distribution.properties
        DistributionInfo nxDistrib;
        File distFile = new File(configurationGenerator.getConfigDir(),
                "distribution.properties");
        if (!distFile.exists()) {
            // fallback in the file in templates
            distFile = new File(configurationGenerator.getNuxeoHome(),
                    "templates");
            distFile = new File(distFile, "common");
            distFile = new File(distFile, "config");
            distFile = new File(distFile, "distribution.properties");
        }
        try {
            nxDistrib = new DistributionInfo(distFile);
        } catch (IOException e) {
            nxDistrib = new DistributionInfo();
        }
        nxInstance.distribution = nxDistrib;
        log.info("** Distribution");
        log.info("- name: " + nxDistrib.name);
        log.info("- server: " + nxDistrib.server);
        log.info("- version: " + nxDistrib.version);
        log.info("- date: " + nxDistrib.date);
        log.info("- packaging: " + nxDistrib.packaging);
        // packages
        List<LocalPackage> pkgs = getPackageManager().getPkgList();
        log.info("** Packages:");
        List<String> pkgTemplates = new ArrayList<String>();
        for (LocalPackage pkg : pkgs) {
            nxInstance.packages.add(new PackageInfo(pkg));
            String packageDescription;
            switch (pkg.getState()) {
            case PackageState.DOWNLOADING:
                packageDescription = "downloading";
                break;
            case PackageState.DOWNLOADED:
                packageDescription = "downloaded";
                break;
            case PackageState.INSTALLING:
                packageDescription = "installing";
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
            log.info("- " + pkg.getName() + " (version: "
                    + pkg.getVersion().toString() + " - id: " + pkg.getId()
                    + " - state: " + packageDescription + ")");
            // store template(s) added by this package
            try {
                File installFile = pkg.getInstallFile();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(installFile);
                NodeList nodes = dom.getDocumentElement().getElementsByTagName(
                        "config");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element node = (Element) nodes.item(i);
                    if (node.hasAttribute("addtemplate")) {
                        pkgTemplates.add(node.getAttribute("addtemplate"));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse install file for " + pkg.getName(), e);
            }
        }
        // nuxeo.conf
        ConfigurationInfo nxConfig = new ConfigurationInfo();
        nxConfig.dbtemplate = configurationGenerator.extractDatabaseTemplateName();
        log.info("** Templates:");
        log.info("Database template: " + nxConfig.dbtemplate);
        String userTemplates = configurationGenerator.getUserTemplates();
        StringTokenizer st = new StringTokenizer(userTemplates, ",");
        while (st.hasMoreTokens()) {
            String template = st.nextToken();
            if (template.equals(nxConfig.dbtemplate)) {
                continue;
            }
            if (pkgTemplates.contains(template)) {
                nxConfig.pkgtemplates.add(template);
                log.info("Package template: " + template);
            } else {
                File testBase = new File(configurationGenerator.getNuxeoHome(),
                        ConfigurationGenerator.TEMPLATES + File.separator
                                + template);
                if (testBase.exists()) {
                    nxConfig.basetemplates.add(template);
                    log.info("Base template: " + template);
                } else {
                    nxConfig.usertemplates.add(template);
                    log.info("User template: " + template);
                }
            }
        }
        Properties nxConfProps = new Properties();
        try {
            nxConfProps.load(new FileInputStream(
                    configurationGenerator.getNuxeoConf()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read nuxeo.conf");
        }
        log.info("** Settings from nuxeo.conf:");
        @SuppressWarnings("rawtypes")
        Enumeration nxConfEnum = nxConfProps.propertyNames();
        while (nxConfEnum.hasMoreElements()) {
            String key = (String) nxConfEnum.nextElement();
            String value = nxConfProps.getProperty(key);
            if (key.equals("JAVA_OPTS")) {
                value = getJavaOptsProperty();
            }
            KeyValueInfo kv = new KeyValueInfo(key, value);
            nxConfig.keyvals.add(kv);
            if (!key.contains("password")) {
                log.info(key + "=" + value);
            } else {
                log.info(key + "=********");
            }
        }
        nxInstance.config = nxConfig;
        log.info("****************************************");
        printInstanceXMLOutput(nxInstance);
        return true;
    }

    protected void testparser() {
        try {
            CallbackHolder cbHolder = new StandaloneCallbackHolder(
                    getDistributionEnvironment(),
                    getPackageManager().getUpdateService());
            NuxeoConnectClient.setCallBackHolder(cbHolder);
        } catch (Exception e) {
            // something went wrong in the NuxeoConnectClient initialization
            errorValue = 4;
            log.error("Could not initialize NuxeoConnectClient", e);
            return;
        }
        PackageManager pm = NuxeoConnectClient.getPackageManager();
        pm.setResolver("p2cudf");
        pm.resolveDependencies("nuxeo-dm", null);
    }
}
