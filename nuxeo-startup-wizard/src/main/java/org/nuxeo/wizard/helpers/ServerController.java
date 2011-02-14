/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 * $Id$
 */
package org.nuxeo.wizard.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages execution of NuxeoCtl
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ServerController {

    protected static final String CMD_POSIX ="nuxeoctl";
    protected static final String CMD_WIN ="nuxeoctl.exe";

    protected static Log log = LogFactory.getLog(ServerController.class);

    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    protected static boolean doExec(String path) {

        long t0 = System.currentTimeMillis();
        List<String> output = new ArrayList<String>();

        String cmdName = CMD_POSIX;
        String paramString = " restart";

        if (!isWindows()) {
            // POXIX
            paramString += " 2>&1";
        } else {
            // WIN
            cmdName = CMD_WIN;
        }

        String[] cmd = { "/bin/sh", "-c",
                cmdName + " " + paramString };

        if (isWindows()) {
            cmd[0] = "cmd";
            cmd[1] = "/C";
        }

        Process p1;
        try {
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
            log.error("Error while reading output",e);
            return false;
        }

        return true;
    }


    public static boolean restart(ServletContext servletContext) {

        // /opt/dm/tomcat-bare/webapps/nuxeo-startup-wizard-1.0-SNAPSHOT/

        String basePath = servletContext.getRealPath("/");
        basePath = basePath.split("/webapps/")[0];

        basePath = basePath + "/bin";

        return doExec(basePath);
    }
}
