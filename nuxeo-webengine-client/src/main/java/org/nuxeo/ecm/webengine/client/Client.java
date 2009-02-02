/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.webengine.client.command.Command;
import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.CommandLine;
import org.nuxeo.ecm.webengine.client.command.CommandRegistry;
import org.nuxeo.ecm.webengine.client.command.RemoteCommand;
import org.nuxeo.ecm.webengine.client.util.FileUtils;
import org.nuxeo.ecm.webengine.client.util.Path;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Client {

    protected CommandRegistry registry;
    protected URL url;
    protected Path basePath;
    protected Path cwd;
    protected List<Path> pathStack;
    protected File workingDir;
    protected List<File> wdStack;
    protected String username;
    protected String password;

    public Client() throws IOException {
        this ((URL)null, null, null, null);
    }
    
    public Client(URL baseUrl) throws IOException {
        this (baseUrl, null, null, null);
    }

    public Client(String baseUrl) throws IOException {
        this (baseUrl, null, null, null);
    }

    public Client(String baseUrl, String username, String password) throws IOException {
        this (baseUrl, null, username, password);
    }
    
    public Client(String baseUrl, String path, String username, String password) throws IOException {        
        this (baseUrl != null ? new URL(baseUrl) : null, path, username, password);
    }
    
    public Client(URL baseUrl, String path, String username, String password) throws IOException {
        if (username == null && baseUrl != null) {
            String userInfo = baseUrl.getUserInfo();
            if (userInfo != null) {
                int p = userInfo.indexOf(':');
                if (p > -1) {
                    username = userInfo.substring(0, p);
                    password = userInfo.substring(p+1);
                } else {
                    username = userInfo;
                }
            }
        }
        if (baseUrl != null) { // do URL cleanup 
            baseUrl  = new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), baseUrl.getPath()); 
        }
        this.registry = new CommandRegistry();
        this.username = username;
        this.password = password;
        this.cwd = path == null ? Path.ROOT : new Path(path);
        this.pathStack = new ArrayList<Path>();
        this.wdStack = new ArrayList<File>();
        this.workingDir = new File(".").getCanonicalFile();
        if (baseUrl != null) {
            connect(baseUrl);
        }
    }
    
    
    public void connect(String url) throws MalformedURLException {
        connect(new URL(url));
    }
    
    public void connect(URL url) {
        try {
            if (this.url != null) {
                disconnect();
            }
            this.url = url;
            this.basePath = new Path(url.getPath()).makeAbsolute();
            onConnect();
            if (!loadRemoteCommands()) {
                System.err.println("Remote host is not recognized as a compatible server");
            } else {
                Console.getDefault().updatePrompt();
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to connect to "+url);
        }
        this.url = null;
        this.basePath = null;
    }
    
    public boolean isConnected() {
        return url != null;
    }
    
    public void disconnect() {
        onDisconnect();
        cwd = Path.ROOT;
        pathStack = new ArrayList<Path>();
        registry = new CommandRegistry();
        url = null;
        basePath = null;
        Console.getDefault().updatePrompt();
    }

    /**
     * @return the registry.
     */
    public CommandRegistry getRegistry() {
        return registry;
    }


    public File getLocalWorkingDirectory() {
        return workingDir;
    }
    
    public void lls(String path) {
        if (path == null) {
            //TODO
        } else {
            //TODO
        }
    }
    
    public void lcd(String path) {
        File dir = null;
        try {
            if (path.startsWith("/")) {
                dir = new File(path).getCanonicalFile();
            } else {
                dir = new File(workingDir, path).getCanonicalFile();
            }
            if (dir.isDirectory()) {
                workingDir = dir;
            } else {
                Console.getDefault().error(path+" is not a directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void lpushd(String path) {
        wdStack.add(workingDir);
        lcd(path);
    }

    public void lpopd(String path) {
        if (pathStack.isEmpty()) {
            try {
                workingDir = new File(".").getCanonicalFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            workingDir = wdStack.remove(wdStack.size()-1);
        }
    }

    public File lpwd() {
        return workingDir;
    }
    
    public void cd(String path) {
        if (path.startsWith("/")) {
            cwd = new Path(path);
        } else {
            cwd = cwd.append(path);
        }
        cwd = cwd.makeAbsolute();
        Console.getDefault().updatePrompt();
    }

    public void pushd(String path) {
        pathStack.add(cwd);
        cd(path);
        Console.getDefault().updatePrompt();
    }

    public void popd(String path) {
        if (pathStack.isEmpty()) {
            cwd = Path.ROOT;
        } else {
            cwd = pathStack.remove(pathStack.size()-1);
        }
        Console.getDefault().updatePrompt();
    }

    public Path pwd() {
        return cwd;
    }

    public boolean loadRemoteCommands() throws CommandException, IOException {
        HttpURLConnection conn = (HttpURLConnection)getCommandServiceUrl().openConnection();
        InputStream in = conn.getInputStream();
        if (conn.getResponseCode() != 200) {
            return false;
        }
        List<String> lines = FileUtils.readLines(in);
        for (String line : lines) {
            RemoteCommand cmd = RemoteCommand.parse(line);
            registry.registerCommand(cmd);
        }
        return true;
    }
    
    public URL getCommandServiceUrl() throws CommandException {
        return buildUrl("commands");
    }
    
    public URL getCommandUrl(String cmd) throws CommandException {
        return buildUrl("commands/"+cmd);
    }
    
    public URL getTargetUrl(String cmd) throws CommandException {
        Path path = cwd.append("@commands").append(cmd);
        return buildUrl(path);
    }

    public int execute(Command cmd, CommandLine cmdLine) throws CommandException {
        return execute("GET", getTargetUrl(cmd.getName()), cmdLine);
    }

    public int execute(String method, Command cmd, CommandLine cmdLine) throws CommandException {
        return execute(method, getTargetUrl(cmd.getName()), cmdLine);
    }

    public int execute(String cmdId, CommandLine cmdLine) throws CommandException {
        return execute("GET", getTargetUrl(cmdId), cmdLine);
    }

    public int execute(String method, String cmdId, CommandLine cmdLine) throws CommandException {
        return execute(method, getTargetUrl(cmdId), cmdLine);
    }

    public int execute(URL url, CommandLine cmdLine) {
        return execute("GET", url, cmdLine);
    }
    
    public abstract int execute(String method, URL url, CommandLine cmdLine);
    public abstract void onConnect();
    public abstract void onDisconnect();

    public String getHost() {
        return url != null ? url.getHost() : null;
    }
    
    public Path getWorkingDirectory() {
        return cwd;
    }
    
    public URL buildUrl(String path) throws CommandException {
        if (url == null) {
            throw new CommandException("Not connected");
        }
        try {
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), 
                    basePath.append(path).toString());
        } catch (MalformedURLException e) {
            throw new CommandException("Malformed URL: "+url+"; path: "+basePath.append(path).toString());
        }
    }

    public URL buildUrl(Path path) throws CommandException {
        if (url == null) {
            throw new CommandException("Not connected");
        }
        try {
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), 
                    basePath.append(path).toString());
        } catch (MalformedURLException e) {
            throw new CommandException("Malformed URL: "+url+"; path: "+basePath.append(path).toString());
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName()).append(" [");
        if (isConnected()) {
            buf.append("connected: ");
        } else {
            buf.append("disconnected: ");
        }
        if (url != null) {
            buf.append(url);
        }
        if (username != null) {
            buf.append("; username: ").append(username);            
        }
        buf.append("; path: ").append(cwd).append("]");
        return buf.toString();
    }


}
