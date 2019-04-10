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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.client.methods.*;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Jackrabbit includes a WebDAV client library. Let's use it to test our
 * server.
 */
public class ParallelBench implements Runnable {

    // Nuxeo / JBoss
    private static final String ROOT_URI = "http://localhost:8080/nuxeo/site/dav/default-domain/workspaces/";
    private static final String LOGIN = "Administrator";
    private static final String PASSWD = "Administrator";

    // JackRabbit
    //private static final String ROOT_URI = "http://localhost:8080/repository/default/";

    // Zope
    //private static final String ROOT_URI = "http://localhost:8080/";
    //private static final String LOGIN = "admin";
    //private static final String PASSWD = "admin";

    private static final int PORT = 8080;
    private static final int NUM_THREADS = 100;
    private static final int NUM_DOCS = 10;

    private static volatile boolean keepLooping = true;

    private Random random;
    private final HttpClient client;

    public ParallelBench() {
        random = new Random();
        client = createClient();
    }

    public static void main(String[] argv) throws InterruptedException {
        while (keepLooping) {
            long startTime = System.currentTimeMillis();
            Thread[] threads = new Thread[NUM_THREADS];
            for (int i = 0; i < NUM_THREADS; i++) {
                threads[i] = new Thread(new ParallelBench());
                threads[i].start();
            }
            for (int i = 0; i < NUM_THREADS; i++) {
                threads[i].join();
            }
            long elapsed = System.currentTimeMillis() - startTime;
            long numOps = NUM_THREADS * NUM_DOCS;
            System.out.println(String.format("Completed %d operations in %f sec: %f ops/sec",
                     numOps, elapsed / 1000.0, numOps * 1000.0 / elapsed));
            break;
        }
    }

    @Override
    public void run() {
        // Setup code
        try {
            String folderUri = ROOT_URI + "test" + random.nextInt(1000000000);

            createFolder(folderUri);
            fillFolder(folderUri);
            deleteObject(folderUri);
        } catch (Exception e) {
            e.printStackTrace();
            keepLooping = false;
            throw new RuntimeException("Error in thread");
        }
    }

    private HttpClient createClient() {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", PORT);

        //HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManager connectionManager = new SimpleHttpConnectionManager(true);
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxConnectionsPerHost(hostConfig, 10);
        connectionManager.setParams(params);

        HttpClient client = new HttpClient(connectionManager);
        client.setHostConfiguration(hostConfig);

        Credentials creds = new UsernamePasswordCredentials(LOGIN, PASSWD);
        client.getState().setCredentials(AuthScope.ANY, creds);

        return client;
    }

    private void createFolder(String folderUri) throws IOException {
        HttpMethod mkcol = new MkColMethod(folderUri);
        client.executeMethod(mkcol);

        int status = mkcol.getStatusCode();
        assertEquals(201, status);
    }

    private void fillFolder(String folderUri) throws IOException {
        for (int i = 0; i < NUM_DOCS; i++) {
            String uri = folderUri + "/" + i;
            HttpMethod mkcol = new MkColMethod(uri);
            //System.out.println("creating " + uri);
            client.executeMethod(mkcol);

            int status = mkcol.getStatusCode();
            assertEquals(201, status);
        }
    }

    private void deleteObject(String folderUri) throws IOException {
        HttpMethod del = new DeleteMethod(folderUri);
        client.executeMethod(del);

        int status = del.getStatusCode();
        assertEquals(204, status);
    }

}
