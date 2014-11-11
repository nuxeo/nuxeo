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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.gimp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent;

/**
 *
 * Helper class to execute a Gimp procedure
 *
 * @author tiry
 *
 */
public class GimpExecutor {

    private static String gimpOpts = "--no-interface --batch ";

    private static String gimpQuit = " --batch '(gimp-quit 1)'";

    private static String GIMPLOG_PREFIX = "*NXGIMPLOG*";

    protected static boolean useQuickExec = false;

    private static String gimpPath;

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    public static void setUseQuickExec(boolean quickExec) {
        useQuickExec = quickExec;
    }

    protected static Map<String, String> execCmd(String[] cmd) throws Exception {
        long t0 = System.currentTimeMillis();
        Process p1 = Runtime.getRuntime().exec(cmd);
        int exitValue = p1.waitFor();

        Map<String, String> result = new HashMap<String, String>();

        if (exitValue == 0) {
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String strLine;

            while ((strLine = stdInput.readLine()) != null) {

                if (strLine.startsWith(GIMPLOG_PREFIX)) {
                    String resLine = strLine.substring(GIMPLOG_PREFIX.length());
                    String[] res = resLine.split(":");
                    result.put(res[0].trim(), res[1].trim());
                }
            }
        }
        long t1 = System.currentTimeMillis();
        result.put("JavaProcessExecTime", t1 - t0 + "ms");

        return result;
    }

    protected static Map<String, String> quickExecCmd(String[] cmd)
            throws Exception {
        long t0 = System.currentTimeMillis();
        Process p1 = Runtime.getRuntime().exec(cmd);

        boolean execTerminated = false;
        Map<String, String> result = new HashMap<String, String>();

        while (!execTerminated) {
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String strLine;

            while ((strLine = stdInput.readLine()) != null) {

                if (strLine.startsWith(GIMPLOG_PREFIX)) {
                    String resLine = strLine.substring(GIMPLOG_PREFIX.length());
                    String[] res = resLine.split(":");
                    result.put(res[0].trim(), res[1].trim());
                }
            }

            // check if we can exit
            if (result.containsKey("ReturnCode")) {
                execTerminated = true;
            }

            try {
                int exitCode = p1.exitValue();
                execTerminated = true;
            } catch (IllegalThreadStateException e) {
                // process not yet terminated
            }
        }
        long t1 = System.currentTimeMillis();
        result.put("JavaProcessExecTime", t1 - t0 + "ms");
        return result;
    }

    public static Map<String, String> exec(String procName, List<Object> params)
            throws Exception {

        StringBuffer procStringBuf = new StringBuffer();

        procStringBuf.append("'(");
        procStringBuf.append(procName);
        procStringBuf.append(" RUN-NONINTERACTIVE ");

        for (Object p : params) {
            if (p instanceof String) {
                String pStr = (String) p;
                procStringBuf.append(" \"");
                procStringBuf.append(pStr);
                procStringBuf.append("\"");
            } else if (p instanceof Integer) {
                Integer pInt = (Integer) p;
                procStringBuf.append(" ");
                procStringBuf.append(pInt.toString());
            }
        }

        procStringBuf.append(")'");
        String procString = procStringBuf.toString();

        // init command script
        String[] cmd = {
                "/bin/sh",
                "-c",
                getGimpPath() + " " + gimpOpts + procString + gimpQuit
                        + " 2>&1" };

        if (isWindows()) {
            cmd[0] = "cmd";
            cmd[1] = "/C";
        }

        if (useQuickExec) {
            return quickExecCmd(cmd);
        } else {
            return execCmd(cmd);
        }
    }

    protected static String getGimpPath() {
        if ((gimpPath == null) || ("".equals(gimpPath))) {
            if (isWindows()) {
                gimpPath = PictureTilingComponent.getEnvValue("GimpExecutable",
                        "gimp.exe");
                // gimpPath="gimp.exe";
            } else {
                gimpPath = PictureTilingComponent.getEnvValue("GimpExecutable",
                        "gimp");
                // gimpPath="gimp";
            }
        }
        return gimpPath;
    }

}
