/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     slacoin
 */
package org.nuxeo.runtime.test.runner;

import java.io.File;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.StreamManager;
import org.nuxeo.runtime.services.streaming.StreamManagerClient;
import org.nuxeo.runtime.services.streaming.StreamManagerServer;

import com.google.inject.Binder;

public class StreamingFeature extends SimpleFeature {

    protected StreamManagerClient client;

    protected StreamManagerServer server;

    protected File tmpDir;

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        tmpDir = new File(Framework.getRuntime().getHome(), "tmp/uploads");
        server = new StreamManagerServer("localhost", 3234, tmpDir);
        client = new StreamManagerClient("socket://127.0.0.1:3234");
        server.start();
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
       if (server != null) {
           server.stop();
       }
       tmpDir.delete();
       client = null;
       server = null;
       tmpDir= null;
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(StreamManager.class).toInstance(client);
        binder.bind(StreamManagerServer.class).toInstance(server);
        binder.bind(StreamManagerClient.class).toInstance(client);
    }

}
