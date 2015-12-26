/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.webengine.gwt.codeserver;


import com.google.gwt.dev.codeserver.CodeServer;
import com.google.gwt.dev.codeserver.Options;
import com.google.gwt.dev.codeserver.WebServer;

public class CodeServerWrapper implements CodeServerLauncher {

    WebServer server;

    @Override
    public void startup(String[] args) throws Exception {
        Options options = new Options();

        if (!options.parseArgs(args)) {
            throw new RuntimeException("Cannot parse gwt code server options");
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(CodeServerWrapper.class.getClassLoader());
        try {
            server = CodeServer.start(options);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } finally {
            server = null;
        }
    }

}
