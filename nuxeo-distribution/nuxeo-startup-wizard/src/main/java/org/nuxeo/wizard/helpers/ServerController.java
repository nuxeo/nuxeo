/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */
package org.nuxeo.wizard.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;

/**
 * Manages execution of NuxeoCtl
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.1
 */
public class ServerController {

    protected static final String CMD_POSIX = "nuxeoctl";

    protected static final String CMD_WIN = "nuxeoctl.bat";

    protected static Log log = LogFactory.getLog(ServerController.class);

    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    protected static boolean doExec(String path) {

        long t0 = System.currentTimeMillis();
        List<String> output = new ArrayList<String>();

        String cmdName = CMD_POSIX;
        String paramString = "restart";

        if (!isWindows()) {
            // POXIX
            paramString += " 2>&1";
        } else {
            // WIN
            cmdName = CMD_WIN;
        }
        paramString += System.getProperty(
                ConfigurationGenerator.PARAM_WIZARD_RESTART_PARAMS, "");

        String[] cmd = { "/bin/sh", "-c",
                new File(path, cmdName).getPath() + " " + paramString };

        if (isWindows()) {
            cmd[0] = "cmd";
            cmd[1] = "/C";
        }

        Process p1;
        try {
            log.debug("Restart command: " + cmd);
            p1 = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            log.error("Unable to restart server", e);
            return false;
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                p1.getInputStream()));
        try {
            String strLine;
            while ((strLine = stdInput.readLine()) != null) {
                output.add(strLine);
            }
        } catch (IOException e) {
            log.error("Error while reading output", e);
            return false;
        }

        return true;
    }

    public static boolean restart(Context context) {
        ParamCollector collector = context.getCollector();
        File nuxeoHome = collector.getConfigurationGenerator().getNuxeoHome();
        final String binPath = new File(nuxeoHome, "bin").getPath();
        new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    doExec(binPath);
                } catch (InterruptedException e) {
                    log.error("Restart failed", e);
                }
            }
        }.start();
        return true;
    }
}
