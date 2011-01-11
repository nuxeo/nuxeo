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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.Environment;

/**
 * @author jcarsique
 *
 */
public abstract class NuxeoLauncher {
    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    protected ConfigurationGenerator configurationGenerator;

    protected Process nuxeoProcess;

    @SuppressWarnings("rawtypes")
    protected Class startupClass;

    public NuxeoLauncher(ConfigurationGenerator configurationGenerator) {
        // super("Nuxeo");
        this.configurationGenerator = configurationGenerator;
    }

    // @Override
    public void run() {
        List<String> command = new ArrayList<String>();
        command.add(getJavaExecutable().getPath());
        command.add("-cp");
        command.add(getClassPath());
        command.addAll(getNuxeoProperties());
        command.addAll(getServerProperties());
        setServerStartCommand(command);
        // if (daemon) {
        command.add("&");
        // }
        ProcessBuilder pb = new ProcessBuilder(command);
        // Map<String, String> env = pb.environment();
        // setServerProperties(env);
        // setNuxeoProperties(env);
        pb.directory(configurationGenerator.getNuxeoHome());
        log.debug("Command: " + command);
        // log.debug("Env: " + env);
        // configurationGenerator.initLogs();
        try {
            nuxeoProcess = pb.start();
        } catch (IOException e) {
            log.error(e);
        }
        // pb = pb.redirectErrorStream(true);
        // InputStream is = nuxeoProcess.getErrorStream();
        // InputStreamReader isr = new InputStreamReader(is);
        // BufferedReader br = new BufferedReader(isr);
        // String line;
        //
        // try {
        // while ((line = br.readLine()) != null) {
        // System.out.println(line);
        // log.info(line);
        // }
        // } catch (IOException e) {
        // log.error(e);
        // }
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

    private void redirectConsoleToFile() {
        try {
            configurationGenerator.getLogDir().mkdirs();
            PrintStream fileStream = new PrintStream(
                    new FileOutputStream(new File(
                            configurationGenerator.getLogDir(), "console.log")));
            System.setOut(fileStream);
            System.setErr(fileStream);
        } catch (FileNotFoundException e) {
            log.error("Error in IO Redirection", e);
        }
    }

    public static void main(String[] args) throws ConfigurationException {
        NuxeoLauncher nuxeoThread;
        ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();
        if (configurationGenerator.isJBoss) {
            nuxeoThread = new NuxeoJBossLauncher(configurationGenerator);
        } else if (configurationGenerator.isJetty) {
            nuxeoThread = new NuxeoJettyLauncher(configurationGenerator);
        } else if (configurationGenerator.isTomcat) {
            nuxeoThread = new NuxeoTomcatLauncher(configurationGenerator);
        } else {
            throw new ConfigurationException("Unknown server !");
        }
        if (configurationGenerator.init()) {
            nuxeoThread.run();
            try {
                int exitValue = nuxeoThread.nuxeoProcess.waitFor();
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
    }

}