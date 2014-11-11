/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.runner.JUnitCore;

/**
 * Runs a Nuxeo server based on the unit tests configuration. Wait for a
 * connection on a given port to stop it.
 * <p>
 * Args: -p SHUTDOWNPORT
 */
public class NuxeoServerRunner {

    private static final Log log = LogFactory.getLog(NuxeoServerRunner.class);

    public static int shutdownPort = 4444;

    public static void getPortFromArgs(String[] args) {
        if (args.length == 2 && "-p".equals(args[0])) {
            try {
                shutdownPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // use default port
            }
        }
        String msg = "Using server shutdown port " + shutdownPort;
        log.info(msg);
        // note that System.out is closed when running from ant spawn
        System.out.println(msg);
    }

    public static void main(String[] args) {
        try {
            getPortFromArgs(args);
            JUnitCore.runClasses(ToRun.class);
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    @Ignore
    public static class ToRun extends TXSQLRepositoryTestCase {

        @Override
        protected void deployRepositoryContrib() throws Exception {
            deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                    "OSGI-INF/test-backend-core-types-contrib.xml");

            if (database instanceof DatabaseH2) {
                String contrib = "OSGI-INF/test-server-pooling-h2-contrib.xml";
                deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
            } else {
                super.deployRepositoryContrib();
            }
        }

        // wait until connection on PORT
        public void test() throws Exception {
            ServerSocket serverSocket = new ServerSocket(shutdownPort);
            try {
                serverSocket.accept();
            } finally {
                serverSocket.close();
            }
        }
    }

    public static class Stopper {

        public static void main(String[] args) throws Exception {
            getPortFromArgs(args);
            Socket socket = new Socket("127.0.0.1", shutdownPort);
            try {
                OutputStream os = socket.getOutputStream();
                try {
                    os.write('a');
                } finally {
                    os.close();
                }
            } finally {
                socket.close();
            }
        }

    }

}
