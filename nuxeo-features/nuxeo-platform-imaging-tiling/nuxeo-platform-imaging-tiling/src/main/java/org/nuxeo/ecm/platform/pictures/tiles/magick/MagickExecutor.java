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
package org.nuxeo.ecm.platform.pictures.tiles.magick;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent;

/**
 *
 * Helper class to execute an ImageMagic command
 *
 * @author tiry
 *
 */
public class MagickExecutor {

    private static final Log log = LogFactory.getLog(MagickExecutor.class);

    protected static String convertCmd() {
        return PictureTilingComponent.getEnvValue("IMConvert", "convert");
    }

    protected static String identifyCmd() {
        return PictureTilingComponent.getEnvValue("IMIdentify", "identify");
    }

    protected static String streamCmd() {
        return PictureTilingComponent.getEnvValue("IMStream", "stream");
    }

    protected static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    protected static ExecResult execCmd(String cmdStr) throws Exception {

        // init command script
        String[] cmd = { "/bin/sh", "-c", cmdStr + " 2>&1" };

        if (isWindows()) {
            cmd[0] = "cmd";
            cmd[1] = "/C";
        }

        log.debug("MagicExecutor command=" + cmd[0]);
        log.debug("     " + cmd[1]);
        log.debug("     " + cmd[2]);

        long t0 = System.currentTimeMillis();
        Process p1 = Runtime.getRuntime().exec(cmd);
        int exitValue = p1.waitFor();

        List<String> output = new ArrayList<String>();

        if (exitValue == 0) {
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String strLine;

            while ((strLine = stdInput.readLine()) != null) {
                output.add(strLine.trim());
            }
        } else {
            log.error("Error during MagicExec");
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String strLine;
            while ((strLine = stdInput.readLine()) != null) {
                log.error("ExecOutput=" + strLine.trim());
            }
            throw new Exception("Execution failed on cmd " + cmdStr);
        }

        long t1 = System.currentTimeMillis();
        return new ExecResult(output, t1 - t0);
    }

    protected static String formatFilePath(String filePath) {
        return String.format("\"%s\"", filePath);
    }

}
