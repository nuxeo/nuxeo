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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.deployment.preprocessor.ConfigurationException;
import org.nuxeo.runtime.deployment.preprocessor.ConfigurationGenerator;
import org.nuxeo.runtime.deployment.preprocessor.PackZip;
import org.xml.sax.SAXException;

/**
 * Nuxeo server controller
 *
 * @author jcarsique
 * @since 5.4.1
 */
public class Launcher {
    private static final Log log = LogFactory.getLog(Launcher.class);

    private ConfigurationGenerator configurationGenerator;

    private String[] params;

    private Thread nuxeoThread;

    /**
     * @param args
     */
    public Launcher(String[] params) {
        this.params = params;
        configurationGenerator = new ConfigurationGenerator();
        nuxeoThread = new NuxeoThread(configurationGenerator);
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
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     *
     */
    private void run() throws URISyntaxException, ConfigurationException,
            IOException, ParserConfigurationException, SAXException {
        if (params.length == 0) {
            System.err.println("Usage: java -jar "
                    + new File(
                            getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                    + " (help|start|stop|restart|configure|console|status|startbg|pack)");
            return;
        }
        String command = params[0];

        // Setup nuxeo.log.dir
        // if JBoss, setup jboss.server.log.dir ?
        // Setup log4j.configuration
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

        // Detect host server

        // Setup server parameters

        // Check old paths

        // Method checkalive

        if ("status".equalsIgnoreCase(command)) {
            status();
        } else if ("startbg".equalsIgnoreCase(command)) {
            start();
        } else if ("start".equalsIgnoreCase(command)) {
            start();
            // TODO wait for end of start
        } else if ("console".equalsIgnoreCase(command)) {
            // TODO redirect output to console
            start();
        } else if ("stop".equalsIgnoreCase(command)) {
            stop();
        } else if ("restart".equalsIgnoreCase(command)) {
            stop();
            // TODO wait for/check end of stop
            start();
        } else if ("configure".equalsIgnoreCase(command)) {
            configure();
        } else if ("pack".equalsIgnoreCase(command)) {
            PackZip.main(Arrays.copyOfRange(params, 1, params.length));
        }
    }

    private void stop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    private void status() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    private void start() throws ConfigurationException {
        configure();
        log.debug("LOG: "+ System.getProperty(Environment.NUXEO_LOG_DIR));
        nuxeoThread.start();
    }

    private void configure() throws ConfigurationException {
        configurationGenerator.run();
    }

}
