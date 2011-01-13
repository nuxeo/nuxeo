/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

/**
 * @author jcarsique
 * @since 5.4.1
 */
public abstract class NuxeoLauncher {
    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    public static final long DEFAULT_RETRY_TIMEOUT = 120000L;

    public static final long DEFAULT_RETRY_INTERVAL = 250L;

    private static final String JAVA_OPTS_PROPERTY = "launcher.java.opts";

    private static final String JAVA_OPTS_DEFAULT = "-Xms512m -Xmx1024m -XX:MaxPermSize=512m";

    private static final String START_MAX_WAIT_PARAM = "launcher.start.max.wait";

    /**
     * Default maximum time to wait for server startup summary in logs (in
     * seconds).
     */
    private static final String START_MAX_WAIT_DEFAULT = "300";

    /**
     * Max time to wait for effective stop (in seconds)
     */
    private static final int STOP_MAX_WAIT = 10;

    /**
     * Number of try to cleanly stop server before killing process
     */
    private static final int STOP_NB_TRY = 5;

    private static final int STOP_SECONDS_BEFORE_NEXT_TRY = 5;

    private int startMaxWait;

    protected ConfigurationGenerator configurationGenerator;

    protected ProcessManager processManager;

    protected Process nuxeoProcess;

    private String processRegex;

    protected String pid;

    private ExecutorService executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory(
            "NuxeoProcessThread"));

    private boolean consoleLogs = false;

    private ShutdownThread shutdownHook;

    public NuxeoLauncher(ConfigurationGenerator configurationGenerator) {
        // super("Nuxeo");
        this.configurationGenerator = configurationGenerator;
        processManager = getOSProcessManager();
        processRegex = Pattern.quote(configurationGenerator.getNuxeoConf().getPath())
                + ".*" + Pattern.quote(getServerPrint());
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
    protected void start() throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(getJavaExecutable().getPath());
        command.addAll(Arrays.asList(System.getProperty(JAVA_OPTS_PROPERTY,
                JAVA_OPTS_DEFAULT).split(" ")));
        command.add("-cp");
        command.add(getClassPath());
        command.addAll(getNuxeoProperties());
        command.addAll(getServerProperties());
        setServerStartCommand(command);
        ProcessBuilder pb = new ProcessBuilder(getOSCommand(command));
        pb.directory(configurationGenerator.getNuxeoHome());
        log.debug("Server command: " + pb.command());
        nuxeoProcess = pb.start();
        if (consoleLogs) {
            Thread streamHandler = logProcessStreams(pb);
            streamHandler.start();
        }
        Thread.sleep(1000);
        log.info("Server started"
                + (getPid() != null ? " with process ID " + pid : "") + ".");
    }

    /**
     * Check if some server is already running (from another thread) and throw a
     * Runtime exception if it finds one. That method will work where
     * {@link #isRunning()} won't.
     *
     * @throws IllegalThreadStateException Thrown if a server is already
     *             running.
     */
    public void checkNoRunningServer() throws IllegalThreadStateException {
        try {
            String existingPid = getPid();
            if (existingPid != null) {
                throw new IllegalStateException(
                        "A server is already running with process ID "
                                + existingPid);
            }
        } catch (IOException e) {
            log.warn("Could not check existing process" + e.getMessage());
        }
    }

    protected class ThreadedStreamHandler extends Thread {
        private InputStream inputStream;

        ThreadedStreamHandler(InputStream is) {
            this.inputStream = is;
        }

        @Override
        public void run() {
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    log.info(line);
                }
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public Thread logProcessStreams(ProcessBuilder pb) {
        pb = pb.redirectErrorStream(true);
        ThreadedStreamHandler streamHandler = new ThreadedStreamHandler(
                nuxeoProcess.getInputStream());
        return streamHandler;
    }

    protected abstract String getServerPrint();

    /**
     * Will wrap, if necessary, the command within a Shell command
     *
     * @param command Java command which will be ran
     * @return wrapped command depending on the OS
     */
    private List<String> getOSCommand(List<String> command) {
        String linearizedCommand = new String();
        for (Iterator<String> iterator = command.iterator(); iterator.hasNext();) {
            linearizedCommand += " " + iterator.next();
        }
        ArrayList<String> osCommand = new ArrayList<String>();
        if (PlatformUtils.isLinux() || PlatformUtils.isMac()) {
            osCommand.add("/bin/sh");
            osCommand.add("-c");
            osCommand.add(linearizedCommand);
            // Useless ?!
            // if (background) {
            // osCommand.add("&");
            // }
            return osCommand;
        } else if (PlatformUtils.isWindows()) {
            osCommand.add("cmd");
            osCommand.add("/C");
            osCommand.add(linearizedCommand);
            return command;
        } else {
            return command;
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
                    "Tried to add inexistant classpath entry: "
                            + classPathEntry);
        }
        cp += ":" + classPathEntry.getPath();
        return cp;
    }

    public static void main(String[] args) throws ConfigurationException,
            URISyntaxException {
        if (args.length == 0) {
            printHelp();
            return;
        }
        String command = args[0];
        final NuxeoLauncher launcher = createLauncher();
        boolean commandSucceeded = true;
        if ("status".equalsIgnoreCase(command)) {
            launcher.status();
        } else if ("startbg".equalsIgnoreCase(command)) {
            commandSucceeded = launcher.doStart();
        } else if ("start".equalsIgnoreCase(command)) {
            commandSucceeded = launcher.doStartAndWait();
        } else if ("console".equalsIgnoreCase(command)) {
            launcher.setConsoleLogs(true);
            launcher.executor.execute(new Runnable() {
                public void run() {
                    launcher.addShutdownHook();
                    if (!launcher.doStart()) {
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
        } else if ("configure".equalsIgnoreCase(command)) {
            launcher.configure();
        } else if ("pack".equalsIgnoreCase(command)) {
            // PackZip.main(Arrays.copyOfRange(params, 1, params.length));
            throw new UnsupportedOperationException();
        }
        if (!commandSucceeded) {
            System.exit(1);
        }
    }

    /**
     * Whereas {@link #doStart()} considers the server as started when the
     * process is running, {@link #doStartAndWait()} waits for effective start
     * by watching the logs
     *
     * @return true if the server started successfully
     */
    protected boolean doStartAndWait() {
        boolean commandSucceeded = true;
        if (doStart()) {
            addShutdownHook();
            if (!waitForEffectiveStart()) {
                commandSucceeded = false;
                stop();
            } else {
                removeShutdownHook();
            }
        } else {
            commandSucceeded = false;
        }
        return commandSucceeded;
    }

    protected void removeShutdownHook() {
        log.debug("Remove shutdown hook");
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
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
            in = new BufferedReader(new FileReader(logFile));
            int count = 0;
            int countStatus = 0;
            boolean countActive = false;
            String line;
            // Go to end of file
            while (in.readLine() != null)
                ;
            do {
                // Wait for something to read
                while (!in.ready() && count < startMaxWait && isRunning()) {
                    System.out.print(".");
                    count++;
                    synchronized (nuxeoProcess) {
                        nuxeoProcess.wait(1000);
                    }
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
     * @return true if server successfully started
     */
    protected boolean doStart() {
        boolean serverStarted = false;
        try {
            checkNoRunningServer();
            configure();
            start();
            serverStarted = isRunning();
            if (pid != null) {
                File pidFile = new File(configurationGenerator.getPidDir(),
                        "nuxeo.pid");
                FileWriter writer = new FileWriter(pidFile);
                writer.write(pid);
                writer.close();
            }
        } catch (ConfigurationException e) {
            log.error("Could not run configuration", e);
        } catch (IOException e) {
            log.error("Could not start process", e);
        } catch (InterruptedException e) {
            log.error("Could not start process", e);
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
        } catch (IllegalThreadStateException e) {
            // Already running server
        }
        return serverStarted;
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

    private void setConsoleLogs(boolean consoleLogs) {
        this.consoleLogs = consoleLogs;
    }

    public void stop() {
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
                List<String> command = new ArrayList<String>();
                command.add(getJavaExecutable().getPath());
                command.add("-cp");
                command.add(getClassPath());
                command.addAll(getNuxeoProperties());
                command.addAll(getServerProperties());
                setServerStopCommand(command);
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(configurationGenerator.getNuxeoHome());
                log.debug("Server command: " + pb.command());
                try {
                    nuxeoProcess = pb.start();
                    Thread streamHandler = logProcessStreams(pb);
                    streamHandler.start();
                    nuxeoProcess.waitFor();
                    streamHandler.interrupt();
                    streamHandler.join();
                    boolean wait = true;
                    while (wait) {
                        try {
                            if (nuxeoProcess.exitValue() == 0) {
                                // Successful call for server stop
                                retry = false;
                            } else {
                                // Failed to call for server stop
                                retry = ++nbTry < STOP_NB_TRY;
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
                log.info("No answer from server, try to kill it...");
                if (pid != null) {
                    processManager.kill(nuxeoProcess, pid);
                    if (getPid() == null) {
                        log.info("Server forcibly stopped.");
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not manage process!", e);
        }
    }

    protected abstract void setServerStopCommand(List<String> command);

    private String getPid() throws IOException {
        pid = processManager.findPid(processRegex);
        return pid;
    }

    protected void configure() throws ConfigurationException {
        configurationGenerator.verifyInstallation();
        configurationGenerator.run();
        startMaxWait = Integer.parseInt(configurationGenerator.getUserConfig().getProperty(
                START_MAX_WAIT_PARAM, START_MAX_WAIT_DEFAULT));
    }

    public void status() {
        if (processManager instanceof PureJavaProcessManager) {
            log.info("Can't check server status on your OS.");
            return;
        }
        try {
            if (getPid() == null) {
                log.info("Server is not running.");
            } else {
                log.info("Server is running with process ID " + getPid() + ".");
            }
        } catch (IOException e) {
            log.warn("Could not check existing process (" + e.getMessage()
                    + ").");
        }
    }

    /**
     * @return a NuxeoLauncher instance specific to current server (JBoss,
     *         Tomcat or Jetty).
     * @throws ConfigurationException If server cannot be identified
     */
    private static NuxeoLauncher createLauncher() throws ConfigurationException {
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
        configurationGenerator.init();
        return launcher;
    }

    /**
     * Print class usage on standard system output.
     *
     * @throws URISyntaxException
     */
    public static void printHelp() throws URISyntaxException {
        System.err.println("Usage: java -jar "
                + new File(
                        NuxeoLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + " (help|start|stop|restart|configure|console|status|startbg|restartbg|pack)");
    }

    /**
     * Works only on current nuxeoProcess
     *
     * @return true if current process is running
     */
    protected boolean isRunning() {
        if (nuxeoProcess == null) {
            return false;
        }
        try {
            nuxeoProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException exception) {
            return true;
        }
    }

}