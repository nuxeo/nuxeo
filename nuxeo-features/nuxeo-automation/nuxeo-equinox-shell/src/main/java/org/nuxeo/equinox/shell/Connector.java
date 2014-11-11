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
 *     bstefanescu
 */
package org.nuxeo.equinox.shell;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;

import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.cmds.Interactive;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Connector {

    protected String[] commands;

    protected Socket socket;

    protected Reader in;

    protected PrintWriter out;

    public static Connector newConnector(String address) {
        int p = address.indexOf(':');
        if (p == -1) {
            throw new ShellException("Illegal address '" + address
                    + "'. Must be in format 'host:port'");
        }
        return new Connector(address.substring(0, p),
                Integer.parseInt(address.substring(p + 1)));
    }

    public Connector(String host, int port) {
        try {
            socket = new Socket(host, port);
            socket.setReuseAddress(true);
            in = new InputStreamReader(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream());
            initConnection();
        } catch (Exception e) {
            throw new ShellException("Failed to connect to " + host + ':'
                    + port);
        }
    }

    protected void initConnection() throws IOException {
        readAll();
        String r = send("help");
        String[] lines = StringUtils.split(r, '\n', true);
        ArrayList<String> cmds = new ArrayList<String>();
        for (String line : lines) {
            if (line.length() == 0 || line.startsWith("---")) {
                continue;
            }
            int i = line.indexOf(' ');
            cmds.add(line.substring(0, i));
        }
        commands = cmds.toArray(new String[cmds.size()]);
    }

    public String[] getBundles() {
        String r = send("ss");
        String[] lines = StringUtils.split(r, '\n', true);
        ArrayList<String> bundles = new ArrayList<String>();
        for (String line : lines) {
            if (line.length() == 0 || line.startsWith("id")) {
                continue;
            }
            String[] ar = line.split("\\s");
            String b = ar[ar.length - 1];
            int i = b.indexOf('_');
            if (i > -1) {
                b = b.substring(0, i);
            }
            bundles.add(b);
        }
        return bundles.toArray(new String[bundles.size()]);
    }

    public String[] getCommands() {
        return commands;
    }

    public Reader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void println(String text) {
        out.println(text);
        out.flush();
    }

    public void println() {
        out.println();
        out.flush();
    }

    public String sendCurrentCommand() {
        return send(Interactive.getCurrentCmdLine());
    }

    public String send(String command) {
        try {
            println(command);
            return readAll();
        } catch (IOException e) {
            throw new ShellException("Failed to read response", e);
        }
    }

    /**
     * Get the response and remove the ending "osgi>" if present - also trim the
     * string before returning. (CRLF is not included at the end)
     * 
     * @return
     * @throws IOException
     */
    public String readAll() throws IOException {
        StringBuilder result = new StringBuilder();
        char[] cbuf = new char[4096];
        int r = in.read(cbuf);
        while (r > 0) {
            result.append(new String(cbuf, 0, r));
            if (result.lastIndexOf("osgi> ") == result.length()
                    - "osgi> ".length()) {
                break;
            }
            r = in.read(cbuf);
        }
        String str = result.toString().trim();
        if (str.endsWith("osgi>")) {
            str = str.substring(0, str.length() - "osgi>".length());
            str = str.trim();
        }
        return str;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {
            throw new ShellException("Failed to disconnect", e);
        }
    }

    public Socket getSocket() {
        return socket;
    }

}
