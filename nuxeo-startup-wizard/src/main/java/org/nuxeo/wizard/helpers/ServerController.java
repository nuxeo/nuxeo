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

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.nuxeo.log4j.ThreadedStreamGobbler;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;

/**
 * Manages execution of NuxeoCtl
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
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
        // paramString += System.getProperty(
        // ConfigurationGenerator.PARAM_WIZARD_RESTART_PARAMS, "");

        String[] cmd;
        if (isWindows()) {
            cmd = new String[] { "cmd", "/C",
                    new File(path, CMD_WIN).getPath(), "nogui", "restartbg" };
            log.debug("Restart command: " + cmd[0] + " " + cmd[1] + " "
                    + cmd[2] + " " + cmd[3] + " " + cmd[4]);
        } else {
            cmd = new String[] { "/bin/sh", "-c",
                    new File(path, CMD_POSIX).getPath() + " restartbg" };
            log.debug("Restart command: " + cmd[0] + " " + cmd[1] + " "
                    + cmd[2]);
        }

        Process p1;
        try {
            p1 = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            log.error("Unable to restart server", e);
            return false;
        }

        new ThreadedStreamGobbler(p1.getInputStream(), SimpleLog.LOG_LEVEL_OFF).start();
        new ThreadedStreamGobbler(p1.getErrorStream(),
                SimpleLog.LOG_LEVEL_ERROR).start();
        return true;
    }

    private static boolean restartInProgress = false;

    public static synchronized boolean restart(Context context) {
        if (restartInProgress) {
            return false;
        }
        ParamCollector collector = context.getCollector();
        File nuxeoHome = collector.getConfigurationGenerator().getNuxeoHome();
        final String binPath = new File(nuxeoHome, "bin").getPath();
        new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    doExec(binPath);
                } catch (InterruptedException e) {
                    log.error("Restart failed", e);
                }
            }
        }.start();
        return true;
    }
}
