package org.nuxeo.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.log4j.ThreadedStreamGobbler;
import org.nuxeo.runtime.api.Framework;

public class ElasticSearchControler {

    protected static final Log log = LogFactory.getLog(ElasticSearchControler.class);

    protected final NuxeoElasticSearchConfig config;

    public ElasticSearchControler(NuxeoElasticSearchConfig config) {
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
