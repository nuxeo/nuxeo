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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Nuxeo server launcher
 *
 * @author jcarsique
 * @since 5.4.1
 */
public class Launcher {
    private static final Log log = LogFactory.getLog(Launcher.class);

    private ConfigurationGenerator configurationGenerator;

    private String[] params;

    /**
     * @param args
     * @throws ConfigurationException
     */
    public Launcher(String[] params) throws ConfigurationException {
        this.params = params;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.run();
    }

    /**
     * @throws URISyntaxException
     * @throws ConfigurationException
     * @throws IOException
     * @throws InterruptedException
     *
     */
    private void run() throws URISyntaxException, ConfigurationException,
            IOException, InterruptedException {
        if (params.length == 0) {
            printHelp();
            return;
        }
        String command = params[0];

        // if JBoss, setup jboss.server.log.dir ?

        // Setup nuxeo.pid.dir
        // Setup nuxeo.tmp.dir
        // Setup nuxeo.bind.address

        // Detect OS

        // If cygwin, update paths

        // Setup Java Home

        // Layout setup (log, ...)

        // Check file descriptors limit

        // Complete JAVA_OPTS

        // Read or set bind address

        // Setup server parameters

        // Check old paths

        // Method checkalive

        if ("status".equalsIgnoreCase(command)) {
            status();
        } else if ("startbg".equalsIgnoreCase(command)
                || "start".equalsIgnoreCase(command)) {
            configure();
            // redirectConsoleToFile();
            startbg();
        } else if ("console".equalsIgnoreCase(command)) {
            configure();
            start();
        } else if ("stop".equalsIgnoreCase(command)) {
            stop();
        } else if ("restart".equalsIgnoreCase(command)) {
            stop();
            configure();
            startbg();
        } else if ("configure".equalsIgnoreCase(command)) {
            configure();
        } else if ("pack".equalsIgnoreCase(command)) {
            // PackZip.main(Arrays.copyOfRange(params, 1, params.length));
            throw new UnsupportedOperationException();
        }
    }

    private Process startbg() throws IOException {
        return start(true);
    }

    public Process start(boolean daemon) throws IOException {
        File javaExec = new File(System.getProperty("java.home"), "bin"
                + File.separator + "java");
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
        List<String> command = new ArrayList<String>();
        command.add(javaExec.getPath());
        command.add("-cp");
        command.add(jarLauncher.getPath());
        command.add(NuxeoLauncher.class.getName());
        command.addAll(Arrays.asList(params));
        // if (daemon) {
        command.add("&");
        // }
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put(ConfigurationGenerator.NUXEO_HOME,
                configurationGenerator.getNuxeoHome().getPath());
        env.put(ConfigurationGenerator.NUXEO_CONF,
                configurationGenerator.getNuxeoConf().getPath());
        pb.directory(configurationGenerator.getNuxeoHome());
        log.debug("Command: " + command);
        log.debug("Env: " + env);
        Process nuxeoProcess = pb.start();
        // log.info("Process ID: "+((UNI)nuxeoProcess));
        return nuxeoProcess;
    }

    private void redirectConsoleToFile() {
        // Logger.getRootLogger().removeAppender("CONSOLE");
        try {
            configurationGenerator.getLogDir().mkdirs();
            PrintStream fileStream = new PrintStream(
                    new FileOutputStream(new File(
                            configurationGenerator.getLogDir(), "console.log")));
            // Redirect stdout and stderr to file
            System.setOut(fileStream);
            System.setErr(fileStream);
        } catch (FileNotFoundException e) {
            log.error("Error in IO Redirection", e);
        }
    }

    /**
     * Print class usage on standard system output.
     *
     * @throws URISyntaxException
     */
    public void printHelp() throws URISyntaxException {
        System.err.println("Usage: java -jar "
                + new File(
                        getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                + " (help|start|stop|restart|configure|console|status|startbg|pack)");
    }

    private void stop() {
        throw new UnsupportedOperationException();
    }

    private void status() {
        throw new UnsupportedOperationException();
    }

    private void start() throws IOException, ConfigurationException {
        NuxeoLauncher.main(params);
        // Process nuxeoProcess = start(false);
        // log.debug("after start, before waitfor()");
        // try {
        // nuxeoProcess.waitFor();
        // } catch (InterruptedException e) {
        // throw new RuntimeException(e);
        // }
    }

    private void configure() throws ConfigurationException {
        // configurationGenerator.verifyInstallation();
        configurationGenerator.run();
    }

}
