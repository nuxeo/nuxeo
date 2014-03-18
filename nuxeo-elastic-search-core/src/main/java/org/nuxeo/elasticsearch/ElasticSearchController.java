/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.elasticsearch.client.Client;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.elasticsearch.config.NuxeoElasticSearchConfig;
import org.nuxeo.log4j.ThreadedStreamGobbler;

/**
 * Controller to start an ElasticSearch process outside of Nuxeo JVM.
 *
 * There is only a start method, since the stop is handled via the
 * {@link Client}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class ElasticSearchController {

    protected static final Log log = LogFactory.getLog(ElasticSearchController.class);

    protected final NuxeoElasticSearchConfig config;

    public ElasticSearchController(NuxeoElasticSearchConfig config) {
        this.config = config;
    }

    public boolean start() {

        if (config.getStartupScript() != null) {
            File script = new File(config.getStartupScript());
            if (!script.exists()) {
                log.warn("Can not autostart ElasticSearch : script "
                        + config.getStartupScript() + " not found");
                return false;
            }
            return exec(config.asCommandLineArg());
        } else {
            log.warn("Can not autostart ElasticSearch without a startup script");
            return false;
        }
    }

    protected boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    protected boolean exec(String command) {
        String[] cmd;
        if (isWindows()) {
            cmd = new String[] { "cmd", "/C", command };
            cmd = (String[]) ArrayUtils.addAll(cmd, new String[] { "2>&1" });
        } else {
            cmd = new String[] { "/bin/sh", "-c", command + " 2>&1" };
        }
        String commandLine = StringUtils.join(cmd, " ");

        Process p1;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Running system command: " + commandLine);
            }
            p1 = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            return false;
        }

        ThreadedStreamGobbler out = new ThreadedStreamGobbler(
                p1.getInputStream(), (OutputStream) null);
        ThreadedStreamGobbler err = new ThreadedStreamGobbler(
                p1.getErrorStream(), SimpleLog.LOG_LEVEL_ERROR);

        err.start();
        out.start();

        int exitCode = 0;
        try {
            exitCode = p1.waitFor();
            out.join();
            err.join();
        } catch (InterruptedException e) {
            return false;
        }

        return true;
    }

}
