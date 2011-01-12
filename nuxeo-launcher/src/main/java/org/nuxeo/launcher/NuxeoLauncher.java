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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

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

    private static final String JAVA_OPTS_PROPERTY = "java.launcher.opts";

    private static final String JAVA_OPTS_DEFAULT = "-Xms512m -Xmx1024m -XX:MaxPermSize=512m";

    protected ConfigurationGenerator configurationGenerator;

    protected ProcessManager processManager;

    protected Process nuxeoProcess;

    private String processRegex;

    protected String pid;

    private ExecutorService executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory(
            "NuxeoProcessThread"));

    private boolean consoleLogs = false;

    // private boolean background = false;

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

    public void start() throws IOException, InterruptedException {
        // Check if already running
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

        // Prepare startup command
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
        pid = processManager.findPid(processRegex);
        log.info("Started process" + (pid != null ? "; pid = " + pid : ""));
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

    protected File getJarLauncher() {
        File jarLauncher = new File(configurationGenerator.getNuxeoHome(),
                "bin").listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("nuxeo-launcher")) {
                    return true;
                } else
                    return false;
            }
        })[0];
        return jarLauncher;
    }

    private File getJavaExecutable() {
        File javaExec = new File(System.getProperty("java.home"), "bin"
                + File.separator + "java");
        return javaExec;
    }

    protected abstract void setServerProperties(Map<String, String> env);

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

    protected void setNuxeoProperties(Map<String, String> env) {
        env.put("nuxeo.home", configurationGenerator.getNuxeoHome().getPath());
        env.put("nuxeo.conf", configurationGenerator.getNuxeoConf().getPath());
        setNuxeoProperty(env, Environment.NUXEO_LOG_DIR);
        setNuxeoProperty(env, Environment.NUXEO_DATA_DIR);
        // setNuxeoProperty(Environment.NUXEO_TMP_DIR);
    }

    private String getNuxeoProperty(String property) {
        return "-D" + property + "="
                + configurationGenerator.getUserConfig().getProperty(property);
    }

    private void setNuxeoProperty(Map<String, String> env, String property) {
        log.debug("Set property " + property + ": "
                + configurationGenerator.getUserConfig().getProperty(property));
        env.put(property,
                configurationGenerator.getUserConfig().getProperty(property));
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

        if ("status".equalsIgnoreCase(command)) {
            launcher.status();
        } else if ("startbg".equalsIgnoreCase(command)) {
            launcher.doStart();
        } else if ("start".equalsIgnoreCase(command)) {
            launcher.doStart();
            // TODO wait for effective start if returned true
        } else if ("console".equalsIgnoreCase(command)) {
            launcher.setConsoleLogs(true);
            launcher.executor.execute(new Runnable() {
                public void run() {
                    launcher.doStart();
                }
            });
        } else if ("stop".equalsIgnoreCase(command)) {
            launcher.stop();
        } else if ("restart".equalsIgnoreCase(command)) {
            launcher.stop();
            launcher.doStart();
        } else if ("configure".equalsIgnoreCase(command)) {
            launcher.configure();
        } else if ("pack".equalsIgnoreCase(command)) {
            // PackZip.main(Arrays.copyOfRange(params, 1, params.length));
            throw new UnsupportedOperationException();
        }

    }

    // private void setBackground(boolean background) {
    // this.background = background;
    // }

    /**
     * @return true if server successfully started
     */
    protected boolean doStart() {
        boolean serverStarted = false;
        try {
            configure();
            start();
            serverStarted = true;
        } catch (ConfigurationException e) {
            log.error("Could not run configuration", e);
        } catch (IOException e) {
            log.error("Could not start process", e);
        } catch (InterruptedException e) {
            log.error("Could not start process", e);
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
        }
        return serverStarted;
    }

    private void setConsoleLogs(boolean consoleLogs) {
        this.consoleLogs = consoleLogs;
    }

    public void stop() {
        try {
            if (getPid() == null) {
                log.info("Server is not running.");
                return;
            }
            int retry = 0;
            do {
                retry++;
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
                    // Thread streamHandler = logProcessStreams(pb);
                    // streamHandler.start();
                    nuxeoProcess.waitFor();
                    // streamHandler.interrupt();
                    // streamHandler.join();
                    try {
                        if (nuxeoProcess.exitValue() == 0) {
                            // Wait for server end
                            Thread.sleep(2000);
                        }
                    } catch (IllegalThreadStateException e) {
                        log.debug("Wait for end of stop");
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                    log.error(e);
                }

                if (processManager instanceof PureJavaProcessManager) {
                    log.info("Can't check server status as your OS doesn't allow to manage processes.");
                    return;
                }
                if (getPid() == null) {
                    log.info("Server stopped.");
                    return;
                }
            } while (pid != null && retry < 3);
            processManager.kill(nuxeoProcess, pid);
            if (getPid() == null) {
                log.info("Server forcibly stopped.");
            }
        } catch (IOException e) {
            log.error("Could not manage process!", e);
        }
    }

    protected abstract void setServerStopCommand(List<String> command);

    private String getPid() throws IOException {
        // if (pid == null) {
        pid = processManager.findPid(processRegex);
        // }
        return pid;
    }

    protected void configure() throws ConfigurationException {
        configurationGenerator.run();
    }

    public void status() {
        try {
            if (getPid() == null) {
                log.info("Server is not running.");
            } else {
                log.info("Server is running with process ID " + getPid());
            }
        } catch (IOException e) {
            log.warn("Could not check existing process" + e.getMessage());
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
                + " (help|start|stop|restart|configure|console|status|startbg|pack)");
    }

    public boolean isRunning() {
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