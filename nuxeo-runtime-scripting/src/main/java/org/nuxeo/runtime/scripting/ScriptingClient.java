/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.scripting;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.remoting.RemotingService;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingClient {

    private final ScriptingServer server;
    private ScriptContext ctx = new SimpleScriptContext();

    public ScriptingClient(String host, int port) throws Exception {
        String serverLocator = RemotingService.getServerURI(host, port);
        server = (ScriptingServer) TransporterClient.createTransporterClient(
                new InvokerLocator(serverLocator), ScriptingServer.class);
    }

    public ScriptingServer getServer() {
        return server;
    }

    public RemoteScript loadScript(File file) throws IOException {
        return loadScript(file.getAbsolutePath(), new FileReader(file));
    }

    public RemoteScript loadScript(URL url) throws IOException {
        return loadScript(url.toExternalForm(),
                new InputStreamReader(url.openStream()));
    }

    public RemoteScript loadScript(String name, String content) {
        return new RemoteScript(this, name, content);
    }

    public RemoteScript loadScript(String name, Reader reader) throws IOException {
        return new RemoteScript(this, name, readScriptContent(reader));
    }

    private static String readScriptContent(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            int read;
            char[] buffer = new char[1024 * 32];
            while ((read = reader.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, read));
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    public void setScriptContext(ScriptContext ctx) {
      this.ctx = ctx;
    }

    public ScriptContext getScriptContext() {
        return ctx;
    }

    /**
     * Runs a remote script. Example of usage:
     * <p>
     * <code>
     * java -cp ... org.nuxeo.runtime.scripting.ScriptingClient localhost:62474 test.js
     * </code>
     * @param args a 2 length array containing on index 0 the nuxeo runtime host info
     * and on index 1 the script file path to execute on that host
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println(
                    "Usage: java -cp ... org.nuxeo.runtime.scripting.ScriptingClient localhost:62474 test.js");
            System.exit(1);
        }

        String[] addr = args[0].split(":");
        String host = addr[0];
        int port = 62474;
        if (addr.length == 2) {
            try {
                port = Integer.parseInt(addr[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number: " + addr[1]);
                System.exit(2);
            }
        }

        try {
            ScriptingClient client = new ScriptingClient(host, port);
            RemoteScript script = client.loadScript(new File(args[1]));
            script.eval();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
