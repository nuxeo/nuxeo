/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     tdelprat, jcarsique
 */

package org.nuxeo.ecm.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.nuxeo.common.Environment;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.log4j.ThreadedStreamGobbler;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to call NuxeoCtl restart.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class NuxeoCtlManager {

    protected static final String CMD_POSIX = "nuxeoctl";

    protected static final String CMD_WIN = "nuxeoctl.bat";

    protected static final Log log = LogFactory.getLog(NuxeoCtlManager.class);

    private ConfigurationGenerator cg;

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private static String winEscape(String command) {
        return command.replaceAll("([ ()<>&])", "^$1");
    }

    protected static boolean doExec(String path, String logPath) {
        String[] cmd;
        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = new String[] { "cmd", "/C", winEscape(new File(path, CMD_WIN).getPath()), "--gui=false", "restartbg" };
        } else {
            cmd = new String[] { "/bin/sh", "-c", "\"" + new File(path, CMD_POSIX).getPath() + "\"" + " restartbg" };
        }

        Process p1;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Restart command: " + StringUtils.join(cmd, " "));
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            p1 = pb.start();
        } catch (IOException e) {
            log.error("Unable to restart server", e);
            return false;
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            File logPathDir = new File(logPath);
            File out = new File(logPathDir, "restart-" + System.currentTimeMillis() + ".log");
            File err = new File(logPathDir, "restart-err-" + System.currentTimeMillis() + ".log");
            OutputStream fout;
            OutputStream ferr;
            try {
                fout = new FileOutputStream(out);
                ferr = new FileOutputStream(err);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            new ThreadedStreamGobbler(p1.getInputStream(), fout).start();
            new ThreadedStreamGobbler(p1.getErrorStream(), ferr).start();
        } else {
            new ThreadedStreamGobbler(p1.getInputStream(), SimpleLog.LOG_LEVEL_OFF).start();
            new ThreadedStreamGobbler(p1.getErrorStream(), SimpleLog.LOG_LEVEL_ERROR).start();
        }
        return true;
    }

    private static boolean restartInProgress = false;

    public static synchronized boolean restart() {
        if (restartInProgress) {
            return false;
        }
        restartInProgress = true;
        String nuxeoHome = Framework.getProperty(Environment.NUXEO_HOME);
        final String binPath = new File(nuxeoHome, "bin").getPath();
        final String logDir = Framework.getProperty(Environment.NUXEO_LOG_DIR, nuxeoHome);
        new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    log.info("Restarting Nuxeo server");
                    Thread.sleep(3000);
                    doExec(binPath, logDir);
                } catch (InterruptedException e) {
                    log.error("Restart failed", e);
                }
            }
        }.start();
        return true;
    }

    public String restartServer() {
        restart();
        return "Nuxeo server is restarting";
    }

    /**
     * @since 5.6
     * @return Configured server URL (may differ from current URL)
     */
    public String getServerURL() {
        if (cg == null) {
            cg = new ConfigurationGenerator();
            cg.init();
        }
        return cg.getUserConfig().getProperty(ConfigurationGenerator.PARAM_NUXEO_URL);
    }

}
