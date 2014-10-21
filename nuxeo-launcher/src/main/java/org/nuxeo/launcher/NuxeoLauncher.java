/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.launcher;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.regex.Matcher;
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
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.text.StrSubstitutor;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.Version;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.connect.ConnectBroker;
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

/**
 * @author jcarsique
 * @since 5.4.2
 */
public abstract class NuxeoLauncher {

    /**
     * @since 5.6
     */
    protected static final String OPTION_NODEPS = "nodeps";

    private static final String OPTION_NODEPS_DESC = "Ignore package dependencies and constraints.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_GUI = "gui";

    private static final String OPTION_GUI_DESC = "Use graphical user interface (default depends on OS).";

    /**
     * @since 5.6
     */
    protected static final String OPTION_JSON = "json";

    private static final String OPTION_JSON_DESC = "Output JSON for mp-commands.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_XML = "xml";

    private static final String OPTION_XML_DESC = "Output XML for mp-commands.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_DEBUG = "debug";

    private static final String OPTION_DEBUG_DESC = "Activate debug messages. "
            + "See 'category' option.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_DEBUG_CATEGORY = "dc";

    private static final String OPTION_DEBUG_CATEGORY_DESC = "Comma separated root categories for 'debug' option (default: \"org.nuxeo.launcher\").";

    /**
     * @since 5.6
     */
    protected static final String OPTION_QUIET = "quiet";

    private static final String OPTION_QUIET_DESC = "Suppress information messages.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_HELP = "help";

    private static final String OPTION_HELP_DESC = "Show detailed help.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_RELAX = "relax";

    private static final String OPTION_RELAX_DESC = "Allow relax constraint on current platform (default: "
            + ConnectBroker.OPTION_RELAX_DEFAULT + ").";

    /**
     * @since 5.6
     */
    protected static final String OPTION_ACCEPT = "accept";

    private static final String OPTION_ACCEPT_DESC = "Accept, refuse or ask confirmation for all changes (default: "
            + ConnectBroker.OPTION_ACCEPT_DEFAULT + ").";

    /**
     * @since 5.9.1
     */
    protected static final String OPTION_SNAPSHOT = "snapshot";

    private static final String OPTION_SNAPSHOT_DESC = "Allow use of SNAPSHOT Marketplace packages.";

    /**
     * @since 5.9.1
     */
    protected static final String OPTION_FORCE = "force";

    private static final String OPTION_FORCE_DESC = "Force option can be used on start command to return an error if a server is already running.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_HIDE_DEPRECATION = "hide-deprecation-warnings";

    protected static final String OPTION_HIDE_DEPRECATION_DESC = "Hide deprecation warnings. Not advised on production platforms.";

    /**
     * @since 5.9.6
     */
    protected static final String OPTION_IGNORE_MISSING = "ignore-missing";

    protected static final String OPTION_IGNORE_MISSING_DESC = "Ignore unknown packages on mp-add/install/set commands.";

    // Fallback to avoid an error when the log dir is not initialized
    static {
        if (System.getProperty(Environment.NUXEO_LOG_DIR) == null) {
            System.setProperty(Environment.NUXEO_LOG_DIR, ".");
        }
    }

    /**
     * @since 5.6
     */
    private static final String DEFAULT_NUXEO_CONTEXT_PATH = "/nuxeo";

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

    private static final long STREAM_MAX_WAIT = 3000;

    private static final String PACK_JBOSS_CLASS = "org.nuxeo.runtime.deployment.preprocessor.PackZip";

    private static final String PACK_TOMCAT_CLASS = "org.nuxeo.runtime.deployment.preprocessor.PackWar";

    private static final String PARAM_UPDATECENTER_DISABLED = "nuxeo.updatecenter.disabled";

    private static final String[] COMMANDS_NO_GUI = { "configure", "mp-init",
            "mp-purge", "mp-add", "mp-install", "mp-uninstall", "mp-request",
            "mp-remove", "mp-hotfix", "mp-upgrade", "mp-reset", "mp-list",
            "mp-listall", "mp-update", "status", "showconf", "mp-show",
            "mp-set" };

    private static final String[] COMMANDS_NO_RUNNING_SERVER = { "mp-init",
            "mp-purge", "mp-add", "mp-install", "mp-uninstall", "mp-request",
            "mp-remove", "mp-hotfix", "mp-upgrade", "mp-reset", "mp-update",
            "mp-set" };

    /**
     * Program is running or service is OK.
     *
     * @since 5.7
     */
    public static final int STATUS_CODE_ON = 0;

    /**
     * Program is not running.
     *
     * @since 5.7
     */
    public static final int STATUS_CODE_OFF = 3;

    /**
     * Program or service status is unknown.
     *
     * @since 5.7
     */
    public static final int STATUS_CODE_UNKNOWN = 4;

    /**
     * @since 5.7
     */
    public static final int EXIT_CODE_OK = 0;

    /**
     * Generic or unspecified error.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_ERROR = 1;

    /**
     * Invalid or excess argument(s).
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_INVALID = 2;

    /**
     * Unimplemented feature.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_UNIMPLEMENTED = 3;

    /**
     * User had insufficient privilege.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_UNAUTHORIZED = 4;

    /**
     * Program is not installed.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_NOT_INSTALLED = 5;

    /**
     * Program is not configured.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_NOT_CONFIGURED = 6;

    /**
     * Program is not running.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_NOT_RUNNING = 7;

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

    private int status = STATUS_CODE_UNKNOWN;

    private int errorValue = EXIT_CODE_OK;

    private StatusServletClient statusServletClient;

    private static boolean quiet = false;

    private static boolean debug = false;

    private static boolean force = false;

    private boolean xmlOutput = false;

    private boolean jsonOutput = false;

    private ConnectBroker connectBroker = null;

    private CommandLine cmdLine;

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
        init();
    }

    /**
     * @since 5.6
     */
    public void init() {
        if (!configurationGenerator.init(true)) {
            throw new IllegalStateException("Initialization failed");
        }
        statusServletClient = new StatusServletClient(configurationGenerator);
        statusServletClient.setKey(configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_STATUS_KEY));
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
        if (PlatformUtils.isLinux() || isAix()) {
            UnixProcessManager unixProcessManager = new UnixProcessManager();
            return unixProcessManager;
        } else if (PlatformUtils.isMac()) {
            return new MacProcessManager();
        } else if (isSolaris()) {
            return new SolarisProcessManager();
        } else if (PlatformUtils.isWindows()) {
            WindowsProcessManager windowsProcessManager = new WindowsProcessManager();
            return windowsProcessManager.isUsable() ? windowsProcessManager
                    : new PureJavaProcessManager();
        } else {
            return new PureJavaProcessManager();
        }
    }

    // code similar to PlatformUtils
    private boolean isAix() {
        return System.getProperty("os.name").toLowerCase().startsWith("aix");
    }

    private boolean isSolaris() {
        return System.getProperty("os.name").toLowerCase().startsWith("sunos");
    }

    public static class SolarisProcessManager extends UnixProcessManager {

        protected static final String SOLARIS_11 = "5.11";

        protected static final String SOLARIS_10 = "5.10";

        protected static final String[] SOLARIS_11_PS = { "/usr/bin/ps",
                "auxww" };

        protected static final String[] SOLARIS_10_PS = { "/usr/ucb/ps",
                "auxww" };

        protected static final Pattern PS_OUTPUT_LINE = Pattern.compile("^"
                + "[^\\s]+\\s+" // USER
                + "([0-9]+)\\s+" // PID
                + "[0-9.\\s]+" // %CPU %MEM SZ RSS (may be collapsed)
                + "[^\\s]+\\s+" // TT (no starting digit)
                + "[^\\s]+\\s+" // S
                + "[^\\s]+\\s+" // START
                + "[^\\s]+\\s+" // TIME
                + "(.*)$" // COMMAND
        );

        protected String solarisVersion;

        protected String getSolarisVersion() {
            if (solarisVersion == null) {
                List<String> lines;
                try {
                    lines = execute(new String[] { "/usr/bin/uname", "-r" });
                } catch (IOException e) {
                    log.debug(e.getMessage(), e);
                    lines = Collections.emptyList();
                }
                if (lines.isEmpty()) {
                    solarisVersion = "?";
                } else {
                    solarisVersion = lines.get(0).trim();
                }
            }
            return solarisVersion;
        }

        @Override
        protected String[] psCommand() {
            if (SOLARIS_11.equals(getSolarisVersion())) {
                return SOLARIS_11_PS;
            }
            return null;
        }

        protected Matcher getLineMatcher(String line) {
            return PS_OUTPUT_LINE.matcher(line);
        }

        @Override
        public String findPid(String regex) throws IOException {
            if (SOLARIS_11.equals(getSolarisVersion())) {
                Pattern commandPattern = Pattern.compile(regex);
                for (String line : execute(psCommand())) {
                    Matcher lineMatcher = getLineMatcher(line);
                    if (lineMatcher.matches()) {
                        String pid = lineMatcher.group(1);
                        String command = lineMatcher.group(2);
                        Matcher commandMatcher = commandPattern.matcher(command);
                        if (commandMatcher.find()) {
                            return pid;
                        }
                    }
                }
            } else {
                log.debug("Unsupported Solaris version: " + solarisVersion);
            }
            return null;
        }

        protected List<String> execute(String... command) throws IOException {
            Process process = new ProcessBuilder(command).start();
            List<String> lines = IOUtils.readLines(process.getInputStream());
            return lines;
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
        Thread.sleep(1000);
        boolean processExited = false;
        // Check if process exited early
        try {
            int exitValue = nuxeoProcess.exitValue();
            if (exitValue != 0) {
                log.error(String.format("Server start failed (%d).", exitValue));
            }
            processExited = true;
        } catch (IllegalThreadStateException e) {
            // Normal case
        }
        logProcessStreams(nuxeoProcess, processExited || logProcessOutput);
        if (!processExited) {
            if (getPid() != null) {
                log.warn("Server started with process ID " + pid + ".");
            } else {
                log.warn("Sent server start command but could not get process ID.");
            }
        }
    }

    /**
     * Gets the Java options with 'nuxeo.*' properties substituted.
     *
     * It enables usage of property like ${nuxeo.log.dir} inside JAVA_OPTS.
     *
     * @return the java options string.
     */
    protected String getJavaOptsProperty() {
        String ret = System.getProperty(JAVA_OPTS_PROPERTY, JAVA_OPTS_DEFAULT);
        ret = StrSubstitutor.replace(ret,
                configurationGenerator.getUserConfig());
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
                errorValue = EXIT_CODE_OK;
                throw new IllegalStateException(
                        "A server is running with process ID " + existingPid);
            }
        } catch (IOException e) {
            log.warn("Could not check existing process: " + e.getMessage());
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
        nuxeoProperties.add(String.format("-D%s=%s", Environment.NUXEO_HOME,
                configurationGenerator.getNuxeoHome().getPath()));
        nuxeoProperties.add(String.format("-D%s=%s",
                ConfigurationGenerator.NUXEO_CONF,
                configurationGenerator.getNuxeoConf().getPath()));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_LOG_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_DATA_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_TMP_DIR));
        if (!DEFAULT_NUXEO_CONTEXT_PATH.equals(configurationGenerator.getUserConfig().getProperty(
                Environment.NUXEO_CONTEXT_PATH))) {
            nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_CONTEXT_PATH));
        }
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
            OptionBuilder.withLongOpt(OPTION_HELP);
            OptionBuilder.withDescription(OPTION_HELP_DESC);
            launcherOptions.addOption(OptionBuilder.create("h"));
            // Quiet option
            OptionBuilder.withLongOpt(OPTION_QUIET);
            OptionBuilder.withDescription(OPTION_QUIET_DESC);
            launcherOptions.addOption(OptionBuilder.create("q"));
            // Debug option
            OptionBuilder.withLongOpt(OPTION_DEBUG);
            OptionBuilder.withDescription(OPTION_DEBUG_DESC);
            launcherOptions.addOption(OptionBuilder.create("d"));
            // Debug category option
            OptionBuilder.withDescription(OPTION_DEBUG_CATEGORY_DESC);
            OptionBuilder.hasArg();
            launcherOptions.addOption(OptionBuilder.create(OPTION_DEBUG_CATEGORY));
            OptionGroup outputOptions = new OptionGroup();
            // Hide deprecation warnings option
            OptionBuilder.withLongOpt(OPTION_HIDE_DEPRECATION);
            OptionBuilder.withDescription(OPTION_HIDE_DEPRECATION_DESC);
            outputOptions.addOption(OptionBuilder.create());
            // XML option
            OptionBuilder.withLongOpt(OPTION_XML);
            OptionBuilder.withDescription(OPTION_XML_DESC);
            outputOptions.addOption(OptionBuilder.create());
            // JSON option
            OptionBuilder.withLongOpt(OPTION_JSON);
            OptionBuilder.withDescription(OPTION_JSON_DESC);
            outputOptions.addOption(OptionBuilder.create());
            launcherOptions.addOptionGroup(outputOptions);
            // GUI option
            OptionBuilder.withLongOpt(OPTION_GUI);
            OptionBuilder.hasArg();
            OptionBuilder.withArgName("true|false");
            OptionBuilder.withDescription(OPTION_GUI_DESC);
            launcherOptions.addOption(OptionBuilder.create());
            // Package management option
            OptionBuilder.withLongOpt(OPTION_NODEPS);
            OptionBuilder.withDescription(OPTION_NODEPS_DESC);
            launcherOptions.addOption(OptionBuilder.create());
            // Relax on target platform option
            OptionBuilder.withLongOpt(OPTION_RELAX);
            OptionBuilder.hasArg();
            OptionBuilder.withArgName("true|false|ask");
            OptionBuilder.withDescription(OPTION_RELAX_DESC);
            launcherOptions.addOption(OptionBuilder.create());
            // Accept option
            OptionBuilder.withLongOpt(OPTION_ACCEPT);
            OptionBuilder.hasArg();
            OptionBuilder.withArgName("true|false|ask");
            OptionBuilder.withDescription(OPTION_ACCEPT_DESC);
            launcherOptions.addOption(OptionBuilder.create());
            // Allow SNAPSHOT option
            OptionBuilder.withLongOpt(OPTION_SNAPSHOT);
            OptionBuilder.withDescription(OPTION_SNAPSHOT_DESC);
            launcherOptions.addOption(OptionBuilder.create("s"));
            // Force option
            OptionBuilder.withLongOpt(OPTION_FORCE);
            OptionBuilder.withDescription(OPTION_FORCE_DESC);
            launcherOptions.addOption(OptionBuilder.create("f"));
            // Ignore missing option
            OptionBuilder.withLongOpt(OPTION_IGNORE_MISSING);
            OptionBuilder.withDescription(OPTION_IGNORE_MISSING_DESC);
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
        Boolean stopAfterParsing = true;
        try {
            cmdLine = parser.parse(launcherOptions, args);
            if (cmdLine.hasOption(OPTION_HELP)
                    || cmdLine.getArgList().contains(OPTION_HELP)) {
                printLongHelp();
            } else if (cmdLine.getArgList().isEmpty()) {
                printShortHelp();
            } else {
                stopAfterParsing = false;
            }
        } catch (UnrecognizedOptionException e) {
            log.error(e.getMessage());
            printShortHelp();
        } catch (MissingArgumentException e) {
            log.error(e.getMessage());
            printShortHelp();
        } catch (ParseException e) {
            log.error("Error while parsing command line: " + e.getMessage());
            printShortHelp();
        } finally {
            if (stopAfterParsing) {
                throw new ParseException("Invalid command line");
            }
        }
        return cmdLine;
    }

    public static void main(String[] args) {
        try {
            final NuxeoLauncher launcher = createLauncher(args);
            if (Arrays.asList(COMMANDS_NO_GUI).contains(launcher.command)) {
                launcher.useGui = false;
            }
            if (launcher.useGui && launcher.getGUI() == null) {
                launcher.setGUI(new NuxeoLauncherGUI(launcher));
            }
            launch(launcher);
        } catch (ParseException e) {
            System.exit(1);
        } catch (Exception e) {
            log.error("Cannot execute command. " + e.getMessage());
            log.debug(e, e);
            System.exit(1);
        }
    }

    /**
     * @since 5.5
     * @param launcher
     * @param launcherGUI
     * @param command
     * @throws PackageException
     * @throws IOException
     */
    public static void launch(final NuxeoLauncher launcher) throws IOException,
            PackageException {
        int exitStatus = EXIT_CODE_OK;
        boolean commandSucceeded = true;
        if (launcher.command == null) {
            return;
        }
        if (Arrays.asList(COMMANDS_NO_RUNNING_SERVER).contains(launcher.command)) {
            launcher.checkNoRunningServer();
        }
        if ("status".equalsIgnoreCase(launcher.command)) {
            String statusMsg = launcher.status();
            if (!quiet) {
                log.warn(statusMsg);
                if (launcher.isStarted()) {
                    log.info("Go to " + launcher.getURL());
                    log.info(launcher.getStartupSummary());
                }
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
                    try {
                        if (!launcher.doStart(true)) {
                            launcher.removeShutdownHook();
                            System.exit(1);
                        } else if (!quiet) {
                            log.info("Go to " + launcher.getURL());
                        }
                    } catch (PackageException e) {
                        log.error(
                                "Could not initialize the packaging subsystem",
                                e);
                        launcher.removeShutdownHook();
                        System.exit(EXIT_CODE_ERROR);
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
                commandSucceeded = false;
                launcher.errorValue = EXIT_CODE_NOT_CONFIGURED;
                log.error("Could not run configuration: " + e.getMessage());
                log.debug(e, e);
            }
        } else if ("pack".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pack();
        } else if ("mp-list".equalsIgnoreCase(launcher.command)) {
            launcher.pkgList();
        } else if ("mp-listall".equalsIgnoreCase(launcher.command)) {
            launcher.pkgListAll();
        } else if ("mp-init".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgInit();
        } else if ("mp-purge".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgPurge();
        } else if ("mp-add".equalsIgnoreCase(launcher.command)) {
            if (launcher.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgAdd(params);
            } else {
                commandSucceeded = launcher.pkgRequest(Arrays.asList(params),
                        null, null, null);
            }
        } else if ("mp-install".equalsIgnoreCase(launcher.command)) {
            if (launcher.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgInstall(params);
            } else {
                commandSucceeded = launcher.pkgRequest(null,
                        Arrays.asList(params), null, null);
            }
        } else if ("mp-uninstall".equalsIgnoreCase(launcher.command)) {
            if (launcher.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgUninstall(params);
            } else {
                commandSucceeded = launcher.pkgRequest(null, null,
                        Arrays.asList(params), null);
            }
        } else if ("mp-remove".equalsIgnoreCase(launcher.command)) {
            if (launcher.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgRemove(params);
            } else {
                commandSucceeded = launcher.pkgRequest(null, null, null,
                        Arrays.asList(params));
            }
        } else if ("mp-request".equalsIgnoreCase(launcher.command)) {
            if (launcher.hasOption(OPTION_NODEPS)) {
                log.error("This command is not available with the --nodeps option");
                commandSucceeded = false;
            } else {
                commandSucceeded = launcher.pkgCompoundRequest(Arrays.asList(params));
            }
        } else if ("mp-set".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgSetRequest(Arrays.asList(params),
                    launcher.hasOption(OPTION_NODEPS));
        } else if ("mp-hotfix".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgHotfix();
        } else if ("mp-upgrade".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgUpgrade();
        } else if ("mp-reset".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgReset();
        } else if ("mp-update".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgRefreshCache();
        } else if ("showconf".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.showConfig();
        } else if ("mp-show".equalsIgnoreCase(launcher.command)) {
            commandSucceeded = launcher.pkgShow(params);
        } else {
            printLongHelp();
            commandSucceeded = false;
        }
        if (launcher.command.startsWith("mp-")) {
            launcher.printXMLOutput();
        }
        if (!commandSucceeded) {
            if (!quiet && !debug) {
                log.error("\nSome commands failed:");
                launcher.cset.log();
            }
            exitStatus = launcher.errorValue;
        }
        if (debug) {
            log.debug("\nCommands debug dump:");
            launcher.cset.log(true);
        }
        if (exitStatus != EXIT_CODE_OK) {
            System.exit(exitStatus);
        }
    }

    private boolean hasOption(String option) {
        return cmdLine.hasOption(option);
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
            if (configurationGenerator.isTomcat) {
                startCommand.add(PACK_TOMCAT_CLASS);
            } else {
                errorValue = EXIT_CODE_ERROR;
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
            errorValue = EXIT_CODE_ERROR;
            log.error("Could not start process", e);
        } catch (InterruptedException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error("Could not start process", e);
        } catch (IllegalStateException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error(
                    "The server must not be running while running pack command",
                    e);
        } catch (ConfigurationException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error(e);
        }
        return errorValue == 0;
    }

    private boolean startWizard() throws PackageException {
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
     * @throws PackageException
     * @see #doStartAndWait(boolean)
     */
    public boolean doStartAndWait() throws PackageException {
        boolean started = doStartAndWait(false);
        if (started && !quiet) {
            log.info("Go to " + getURL());
        }
        return started;
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
     * @throws PackageException
     */
    public boolean doStart() throws PackageException {
        boolean started = doStart(false);
        if (started && !quiet) {
            log.info("Go to " + getURL());
        }
        return started;
    }

    /**
     * Whereas {@link #doStart()} considers the server as started when the
     * process is running, {@link #doStartAndWait()} waits for effective start
     * by watching the logs
     *
     * @param logProcessOutput Must process output stream must be logged or not.
     *
     * @return true if the server started successfully
     * @throws PackageException
     */
    public boolean doStartAndWait(boolean logProcessOutput)
            throws PackageException {
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
        errorValue = EXIT_CODE_ERROR;
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
     * @throws PackageException
     */
    public boolean doStart(boolean logProcessOutput) throws PackageException {
        errorValue = EXIT_CODE_OK;
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
            configure();
            configurationGenerator.verifyInstallation();

            if (configurationGenerator.isWizardRequired()) {
                if (!configurationGenerator.isForceGeneration()) {
                    log.error("Cannot start setup wizard with "
                            + ConfigurationGenerator.PARAM_FORCE_GENERATION
                            + "=false. Either set it to true or once, either set "
                            + ConfigurationGenerator.PARAM_WIZARD_DONE
                            + "=true to skip the wizard.");
                    errorValue = EXIT_CODE_NOT_CONFIGURED;
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
                getConnectBroker().executePending(
                        configurationGenerator.getInstallFile(), true, true);
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
            errorValue = EXIT_CODE_NOT_CONFIGURED;
            log.error("Could not run configuration: " + e.getMessage());
            log.debug(e, e);
        } catch (IOException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error("Could not start process: " + e.getMessage());
            log.debug(e, e);
        } catch (InterruptedException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error("Could not start process: " + e.getMessage());
            log.debug(e, e);
        } catch (IllegalStateException e) {
            if (force) {
                // assume program is not configured because of http port binding
                // conflict
                errorValue = EXIT_CODE_NOT_CONFIGURED;
            }
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
            log.error("Output serialization failed: " + e.getMessage(), e);
            errorValue = EXIT_CODE_NOT_RUNNING;
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
            log.error("Output serialization failed: " + e.getMessage(), e);
            errorValue = EXIT_CODE_NOT_RUNNING;
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

        @Override
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
                    if (!processManager.canFindPid()) {
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
        checkNoRunningServer();
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
        return START_MAX_WAIT_DEFAULT;
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
            status = STATUS_CODE_UNKNOWN;
            return "Can't check server status on your OS.";
        }
        try {
            if (getPid() == null) {
                status = STATUS_CODE_OFF;
                return "Server is not running.";
            } else {
                status = STATUS_CODE_ON;
                return "Server is running with process ID " + getPid() + ".";
            }
        } catch (IOException e) {
            status = STATUS_CODE_UNKNOWN;
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
     * Last error value set by any method. Exit code values are following the
     * Linux Standard Base Core Specification 4.1.
     */
    public int getErrorValue() {
        return errorValue;
    }

    /**
     * @throws ParseException
     * @return a NuxeoLauncher instance specific to current server (
     *         Tomcat or Jetty).
     * @throws ConfigurationException If server cannot be identified
     * @since 5.5
     */
    public static NuxeoLauncher createLauncher(String[] args)
            throws ConfigurationException, ParseException {
        CommandLine cmdLine = parseOptions(args);
        // Common options to the Launcher and the ConfigurationGenerator
        if (cmdLine.hasOption(OPTION_QUIET) || cmdLine.hasOption(OPTION_XML)
                || cmdLine.hasOption(OPTION_JSON)) {
            setQuiet();
        }
        if (cmdLine.hasOption(OPTION_DEBUG)
                || cmdLine.hasOption(OPTION_DEBUG_CATEGORY)) {
            setDebug(cmdLine.getOptionValue(OPTION_DEBUG_CATEGORY,
                    "org.nuxeo.launcher"));
        }
        if (cmdLine.hasOption(OPTION_FORCE)) {
            setForce(true);
        }
        NuxeoLauncher launcher;
        ConfigurationGenerator cg = new ConfigurationGenerator(quiet, debug);
        if (cmdLine.hasOption(OPTION_HIDE_DEPRECATION)) {
            cg.hideDeprecationWarnings(true);
        }
        if (cg.isJetty) {
            launcher = new NuxeoJettyLauncher(cg);
        } else if (cg.isTomcat) {
            launcher = new NuxeoTomcatLauncher(cg);
        } else {
            throw new ConfigurationException("Unknown server!");
        }
        launcher.setArgs(cmdLine);
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
        this.cmdLine = cmdLine;
        extractCommandAndParams(cmdLine.getArgs());
        // Use GUI?
        if (cmdLine.hasOption(OPTION_GUI)) {
            useGui = Boolean.valueOf(cmdLine.getOptionValue(OPTION_GUI));
            log.debug("GUI: " + cmdLine.getOptionValue(OPTION_GUI) + " -> "
                    + new Boolean(useGui).toString());
        } else if (OPTION_GUI.equalsIgnoreCase(command)) {
            useGui = true;
            // Shift params and extract command if there is one
            extractCommandAndParams(params);
        } else {
            if (PlatformUtils.isWindows()) {
                useGui = true;
                log.debug("GUI: option not set - platform is Windows -> start GUI");
            } else {
                useGui = false;
                log.debug("GUI: option not set - platform is not Windows -> do not start GUI");
            }
        }
        // Output format
        if (cmdLine.hasOption(OPTION_XML)) {
            setXMLOutput();
        }
        if (cmdLine.hasOption(OPTION_JSON)) {
            setJSONOutput();
        }
    }

    private void extractCommandAndParams(String[] args) {
        if (args.length > 0) {
            command = args[0];
            log.debug("Launcher command: " + command);
            // Command parameters
            if (args.length > 1) {
                params = Arrays.copyOfRange(args, 1, args.length);
                if (log.isDebugEnabled()) {
                    log.debug("Command parameters: "
                            + ArrayUtils.toString(params));
                }
            } else {
                params = new String[0];
            }
        } else {
            command = null;
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
     * @param categories Root categories to switch DEBUG on.
     * @since 5.6
     */
    protected static void setDebug(String categories) {
        setDebug(categories, true);
    }

    /**
     * @param categories Root categories to switch DEBUG on or off
     * @param activateDebug Set DEBUG on or off.
     * @since 5.6
     */
    protected static void setDebug(String categories, boolean activateDebug) {
        debug = activateDebug;
        Log4JHelper.setDebug(categories, activateDebug, true, new String[] {
                Log4JHelper.CONSOLE_APPENDER_NAME, "FILE" });
    }

    /**
     * @param activateDebug if true, will activate the DEBUG logs
     * @since 5.5
     */
    protected static void setDebug(boolean activateDebug) {
        setDebug("org.nuxeo", activateDebug);
    }

    /**
     * @param set a launcher force option
     * @since 5.9
     */
    protected static void setForce(boolean value) {
        force = value;
    }

    protected void setXMLOutput() {
        xmlOutput = true;
    }

    protected void setJSONOutput() {
        jsonOutput = true;
        setXMLOutput();
    }

    public static void printShortHelp() {
        HelpFormatter help = new HelpFormatter();
        help.setSyntaxPrefix("Usage: ");
        help.printHelp("nuxeoctl [options] <command> [command parameters]",
                launcherOptions);
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
        log.error("\tgui\t\t\tStart the user graphical interface.");
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
        log.error("\tpack <target>\t\tBuild a static archive (the \"pack\" Shell script is deprecated).");
        log.error("\tshowconf\t\tDisplay the instance configuration.");
        log.error("\tmp-list\t\t\tList local Marketplace packages.");
        log.error("\tmp-listall\t\tList all Marketplace packages (requires a registered instance).");
        log.error("\tmp-init\t\t\tPre-cache Marketplace packages locally available in the distribution.");
        log.error("\tmp-update\t\tUpdate cache of marketplace packages list.");
        log.error("\tmp-add\t\t\tAdd Marketplace package(s) to local cache. You must provide the package file(s), name(s) or ID(s) as parameter.");
        log.error("\tmp-install\t\tRun Marketplace package installation. "
                + "It is automatically called at startup if {{installAfterRestart.log}} file exists in data directory. "
                + "Else you must provide the package file(s), name(s) or ID(s) as parameter.");
        log.error("\tmp-uninstall\t\tUninstall Marketplace package(s). "
                + "You must provide the package name(s) or ID(s) as parameter (see \"mp-list\" command).");
        log.error("\tmp-set\t\t\tInstalls a list of Marketplace packages and removes those not in the list.");
        log.error("\tmp-request\t\tInstall + uninstall Marketplace package(s) in one command. "
                + "You must provide a *quoted* list of package names or IDs prefixed with + (install) or - (uninstall).");
        log.error("\tmp-remove\t\tRemove Marketplace package(s) from the local cache. "
                + "You must provide the package name(s) or ID(s) as parameter (see \"mp-list\" command).");
        log.error("\tmp-reset\t\tReset all packages to DOWNLOADED state. May be useful after a manual server upgrade.");
        log.error("\tmp-purge\t\tUninstall and remove all packages from the local cache.");
        log.error("\tmp-hotfix\t\tInstall all the available hotfixes for the current platform (requires a registered instance).");
        log.error("\tmp-upgrade\t\tGet all the available upgrades for the Marketplace packages currently installed (requires a registered instance).");
        log.error("\tmp-show\t\t\tShow Marketplace package(s) information. You must provide the package file(s), name(s) or ID(s) as parameter.");
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
                ConfigurationGenerator.PARAM_NUXEO_URL);
    }

    protected ConnectBroker getConnectBroker() throws IOException,
            PackageException {
        if (connectBroker == null) {
            connectBroker = new ConnectBroker(configurationGenerator.getEnv());
            if (hasOption(OPTION_ACCEPT)) {
                connectBroker.setAccept(cmdLine.getOptionValue(OPTION_ACCEPT,
                        ConnectBroker.OPTION_ACCEPT_DEFAULT));
            }
            if (hasOption(OPTION_RELAX)) {
                connectBroker.setRelax(cmdLine.getOptionValue(OPTION_RELAX));
            }
            if (hasOption(OPTION_SNAPSHOT) || isSNAPSHOTDistribution()) {
                connectBroker.setAllowSNAPSHOT(true);
            }
            cset = connectBroker.getCommandSet();
        }
        return connectBroker;
    }

    /**
     * @since 5.9.1
     */
    private boolean isSNAPSHOTDistribution() {
        return new Version(getDistributionInfo().version).isSnapshot();
    }

    /**
     * List all local packages.
     *
     * @throws IOException
     * @throws PackageException
     */
    protected void pkgList() throws IOException, PackageException {
        getConnectBroker().listPending(configurationGenerator.getInstallFile());
        getConnectBroker().pkgList();
    }

    /**
     * List all packages including remote ones.
     *
     * @since 5.6
     * @throws IOException
     * @throws PackageException
     */
    protected void pkgListAll() throws IOException, PackageException {
        getConnectBroker().listPending(configurationGenerator.getInstallFile());
        getConnectBroker().pkgListAll();
    }

    protected boolean pkgAdd(String[] pkgNames) throws IOException,
            PackageException {
        boolean cmdOK = getConnectBroker().pkgAdd(Arrays.asList(pkgNames), hasOption(OPTION_IGNORE_MISSING));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgInstall(String[] pkgIDs) throws IOException,
            PackageException {
        boolean cmdOK = true;
        if (configurationGenerator.isInstallInProgress()) {
            cmdOK = getConnectBroker().executePending(
                    configurationGenerator.getInstallFile(), true,
                    !hasOption(OPTION_NODEPS));
        }
        cmdOK = cmdOK && getConnectBroker().pkgInstall(Arrays.asList(pkgIDs), hasOption(OPTION_IGNORE_MISSING));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgUninstall(String[] pkgIDs) throws IOException,
            PackageException {
        boolean cmdOK = getConnectBroker().pkgUninstall(Arrays.asList(pkgIDs));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgRemove(String[] pkgIDs) throws IOException,
            PackageException {
        boolean cmdOK = getConnectBroker().pkgRemove(Arrays.asList(pkgIDs));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgReset() throws IOException, PackageException {
        boolean cmdOK = getConnectBroker().pkgReset();
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
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
            log.error("Output serialization failed: " + e.getMessage());
            log.debug(e, e);
            errorValue = EXIT_CODE_NOT_RUNNING;
        }
    }

    /**
     * @throws PackageException
     * @throws IOException
     * @since 5.6
     */
    protected boolean showConfig() throws IOException, PackageException {
        InstanceInfo nxInstance = new InstanceInfo();
        log.info("***** Nuxeo instance configuration *****");
        nxInstance.NUXEO_CONF = configurationGenerator.getNuxeoConf().getPath();
        log.info("NUXEO_CONF: " + nxInstance.NUXEO_CONF);
        nxInstance.NUXEO_HOME = configurationGenerator.getNuxeoHome().getPath();
        log.info("NUXEO_HOME: " + nxInstance.NUXEO_HOME);
        // CLID
        try {
            nxInstance.clid = getConnectBroker().getCLID();
            log.info("Instance CLID: " + nxInstance.clid);
        } catch (NoCLID e) {
            // leave nxInstance.clid unset
        } catch (PackageException e) {
            // something went wrong in the NuxeoConnectClient initialization
            errorValue = EXIT_CODE_UNAUTHORIZED;
            log.error("Could not initialize NuxeoConnectClient", e);
            return false;
        }
        // distribution.properties
        DistributionInfo nxDistrib = getDistributionInfo();
        nxInstance.distribution = nxDistrib;
        log.info("** Distribution");
        log.info("- name: " + nxDistrib.name);
        log.info("- server: " + nxDistrib.server);
        log.info("- version: " + nxDistrib.version);
        log.info("- date: " + nxDistrib.date);
        log.info("- packaging: " + nxDistrib.packaging);
        // packages
        List<LocalPackage> pkgs = getConnectBroker().getPkgList();
        log.info("** Packages:");
        List<String> pkgTemplates = new ArrayList<String>();
        for (LocalPackage pkg : pkgs) {
            nxInstance.packages.add(new PackageInfo(pkg));
            log.info(String.format("- %s (version: %s - id: %s - state: %s)",
                    pkg.getName(), pkg.getVersion(), pkg.getId(),
                    pkg.getPackageState().getLabel()));
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
        log.info("** Settings from nuxeo.conf:");
        Properties userConfig = configurationGenerator.getUserConfig();
        @SuppressWarnings("rawtypes")
        Enumeration nxConfEnum = userConfig.keys();
        while (nxConfEnum.hasMoreElements()) {
            String key = (String) nxConfEnum.nextElement();
            String value = userConfig.getProperty(key);
            if (key.equals("JAVA_OPTS")) {
                value = getJavaOptsProperty();
            }
            KeyValueInfo kv = new KeyValueInfo(key, value);
            nxConfig.keyvals.add(kv);
            if (!key.contains("password")
                    && !key.equals(ConfigurationGenerator.PARAM_STATUS_KEY)) {
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

    /**
     * @since 5.9.1
     */
    protected DistributionInfo getDistributionInfo() {
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
        DistributionInfo nxDistrib;
        try {
            nxDistrib = new DistributionInfo(distFile);
        } catch (IOException e) {
            nxDistrib = new DistributionInfo();
        }
        return nxDistrib;
    }

    /**
     * @since 5.6
     * @param pkgsToAdd
     * @param pkgsToInstall
     * @param pkgsToUninstall
     * @param pkgsToRemove
     * @return true if request execution was fine
     * @throws IOException
     * @throws PackageException
     */
    protected boolean pkgRequest(List<String> pkgsToAdd,
            List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) throws IOException, PackageException {
        boolean cmdOK = true;
        if (configurationGenerator.isInstallInProgress()) {
            cmdOK = getConnectBroker().executePending(
                    configurationGenerator.getInstallFile(), true, true);
        }
        cmdOK = cmdOK
                && getConnectBroker().pkgRequest(pkgsToAdd, pkgsToInstall,
                        pkgsToUninstall, pkgsToRemove, true, hasOption(OPTION_IGNORE_MISSING));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    /**
     * Update the cached list of remote packages
     *
     * @since 5.6
     * @return true
     * @throws IOException
     * @throws PackageException
     */
    protected boolean pkgRefreshCache() throws IOException, PackageException {
        getConnectBroker().refreshCache();
        return true;
    }

    /**
     * Add packages from the distribution to the local cache
     *
     * @throws PackageException
     * @throws IOException
     * @since 5.6
     *
     */
    protected boolean pkgInit() throws IOException, PackageException {
        return getConnectBroker().addDistributionPackages();
    }

    /**
     * Uninstall and remove all packages from the local cache
     *
     * @return
     *
     * @throws PackageException
     * @throws IOException
     * @since 5.6
     *
     */
    protected boolean pkgPurge() throws PackageException, IOException {
        return getConnectBroker().pkgPurge();
    }

    /**
     * Install the hotfixes available for the instance
     *
     * @return
     *
     * @throws PackageException
     * @throws IOException
     * @since 5.6
     */
    protected boolean pkgHotfix() throws IOException, PackageException {
        return getConnectBroker().pkgHotfix();
    }

    /**
     * Upgrade the marketplace packages (addons) available for the instance
     *
     * @return
     *
     * @throws PackageException
     * @throws IOException
     * @since 5.6
     */
    protected boolean pkgUpgrade() throws IOException, PackageException {
        return getConnectBroker().pkgUpgrade();
    }

    /**
     * Combined install/uninstall request
     *
     * @param request Space separated list of package names or IDs prefixed with
     *            + (install) or - (uninstall)
     * @throws IOException
     * @throws PackageException
     * @since 5.6
     */
    protected boolean pkgCompoundRequest(List<String> request)
            throws IOException, PackageException {
        List<String> add = new ArrayList<String>();
        List<String> install = new ArrayList<String>();
        List<String> uninstall = new ArrayList<String>();
        for (String param : request) {
            for (String subparam : param.split("[ ,]")) {
                if (subparam.charAt(0) == '-') {
                    uninstall.add(subparam.substring(1));
                } else if (subparam.charAt(0) == '+') {
                    install.add(subparam.substring(1));
                } else {
                    add.add(subparam);
                }
            }
        }
        return pkgRequest(add, install, uninstall, null);
    }

    protected boolean pkgSetRequest(List<String> request, boolean nodeps)
            throws IOException, PackageException {
        boolean cmdOK;
        if (nodeps) {
            cmdOK = getConnectBroker().pkgSet(request, hasOption(OPTION_IGNORE_MISSING));
        } else {
            cmdOK = getConnectBroker().pkgRequest(null, request, null, null,
                    false, hasOption(OPTION_IGNORE_MISSING));
        }
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    /**
     * dpkg-like command which returns package location, version, dependencies,
     * conflicts, ...
     *
     * @param packages List of packages identified by their ID, name or local
     *            filename.
     *
     * @return false if unable to show package information.
     * @throws PackageException
     * @throws IOException
     * @since 5.7
     */
    protected boolean pkgShow(String[] packages) throws IOException,
            PackageException {
        boolean cmdOK = getConnectBroker().pkgShow(Arrays.asList(packages));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

}
