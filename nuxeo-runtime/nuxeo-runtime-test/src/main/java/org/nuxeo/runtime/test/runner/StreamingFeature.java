/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
