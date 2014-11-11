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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.client.Path;
import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.CommandLine;
import org.nuxeo.ecm.webengine.client.command.CommandRegistry;
import org.nuxeo.ecm.webengine.client.command.RemoteCommand;
import org.nuxeo.ecm.webengine.client.util.FileUtils;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Client {

    protected CommandRegistry registry;
    protected URL baseUrl;
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
            if (this.baseUrl != null) {
                disconnect();
            }
            this.baseUrl = url;
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
        this.baseUrl = null;
        this.basePath = null;
    }
    
    public boolean isConnected() {
        return baseUrl != null;
    }
    
    public void disconnect() {
        onDisconnect();
        cwd = Path.ROOT;
        pathStack = new ArrayList<Path>();
        registry = new CommandRegistry();
        baseUrl = null;
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


    public String id() throws CommandException, IOException {
        Result result = null; //get("id"); TODO
        if (result.isOk()) {
            return result.getContentAsString();
        } else {
            throw new CommandException("Operation failed with error code: "+result.getStatus());
        }
    }
    
    public String id(String path) throws CommandException, IOException {
        pushd(path, false);
        try {
            Map<String,Object> map = new HashMap<String, Object>();
            map.put("file", path);
            Result result = null; //get("id"); TODO
            if (result.isOk()) {
                return result.getContentAsString();
            } else {
                throw new CommandException("Operation failed with error code: "+result.getStatus());
            }
        } finally {
            popd(false);
        }
    }
    
    public List<String> ls() throws CommandException, IOException {
        Result result = null; //get("ls");
        if (result.isOk()) {
            return result.getContentLines();
        } else {
            throw new CommandException("Operation failed with error code: "+result.getStatus());
        }
    }
    
    public List<String> ls(String path) throws CommandException, IOException {
        pushd(path, false);
        try {
            Map<String,Object> map = new HashMap<String, Object>();
            map.put("file", path);
            Result result = get(new Path(path), null); //TODO
            if (result.isOk()) {
                return result.getContentLines();
            } else {
                throw new CommandException("Operation failed with error code: "+result.getStatus());
            }
        } finally {
            popd(false);
        }
    }
    
    public void cd(String path) {
        cd(path, true);
    }
    
    public void cd(String path, boolean updateConsole) {
        if (path.startsWith("/")) {
            cwd = new Path(path);
        } else {
            cwd = cwd.append(path);
        }
        cwd = cwd.makeAbsolute();
        if (updateConsole) {
            Console.getDefault().updatePrompt();
        }
    }

    public void pushd(String path) {
        pushd(path, true);
    }
    
    public void pushd(String path, boolean updateConsole) {
        pathStack.add(cwd);
        cd(path);
    }

    public void popd() {
        popd(true);
    }
    
    public void popd(boolean updateConsole) {
        if (pathStack.isEmpty()) {
            cwd = Path.ROOT;
        } else {
            cwd = pathStack.remove(pathStack.size()-1);
        }
        if (updateConsole) {
            Console.getDefault().updatePrompt();
        }
    }

    public Path pwd() {
        return cwd;
    }

    
    protected boolean loadRemoteCommands() throws CommandException, IOException {
        URL url = buildUrl(cwd.append("@commands"));
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        InputStream in = conn.getInputStream();
        if (conn.getResponseCode() != 200) {
            in.close();
            return false;
        }        
        List<String> lines = FileUtils.readLines(in);
        in.close();
        for (String line : lines) {
            RemoteCommand cmd = RemoteCommand.parse(line);
            registry.registerCommand(cmd);
        }
        return true;
    }    
        
    public String getHelp(String cmdId) throws CommandException, IOException {
        URL url = buildUrl(getAbsolutePath(new Path("@commands").append("help").append(cmdId)));
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        InputStream in = conn.getInputStream();
        if (conn.getResponseCode() != 200) {
            in.close();
            throw new CommandException("Failed to get help. Error is "+conn.getResponseCode());
        }
        String help = FileUtils.read(in);
        in.close();
        return help;
    }


    public Result delete(Path path, Map<String,Object> args) throws CommandException {
        if (baseUrl == null) {
            throw new CommandException("Not Connected");
        }
        path = getAbsolutePath(path);
        return doDelete(path, args);
    }
    
    public Result head(Path path, Map<String,Object> args) throws CommandException {
        if (baseUrl == null) {
            throw new CommandException("Not Connected");
        }
        path = getAbsolutePath(path);
        return doHead(path, args);        
    }
    
    public Result get(Path path, Map<String,Object> args) throws CommandException {
        if (baseUrl == null) {
            throw new CommandException("Not Connected");
        }
        path = getAbsolutePath(path);
        return doGet(path, args);    
    }
    
    public Result post(Path path, Map<String,Object> args) throws CommandException {
        if (baseUrl == null) {
            throw new CommandException("Not Connected");
        }
        path = getAbsolutePath(path);
        return doPost(path, args);
    }
    
    public Result put(Path path, Map<String,Object> args) throws CommandException {
        if (baseUrl == null) {
            throw new CommandException("Not Connected");
        }
        path = getAbsolutePath(path);
        return doPut(path, args);    
    }
    
    /**
     * Relative paths are converted to absolute paths
     * @param path
     * @param args
     * @return
     */
    public abstract Result doDelete(Path path, Map<String,Object> args);    
    public abstract Result doHead(Path path, Map<String,Object> args);
    public abstract Result doGet(Path path, Map<String,Object> args);
    public abstract Result doPost(Path path, Map<String,Object> args);
    public abstract Result doPut(Path path, Map<String,Object> args);

    public abstract void onConnect();
    public abstract void onDisconnect();

    public String getHost() {
        return baseUrl != null ? baseUrl.getHost() : null;
    }
    
    public Path getWorkingDirectory() {
        return cwd;
    }
    

    public File getFile(String path) {
        if (path.startsWith("/")) {
            return new File(path);
        } else {
            return new File(workingDir, path);
        }
    }
    
    public Path getAbsolutePath(Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        if (cwd.segmentCount() == 0) {
            return basePath.append(cwd).append(path);
        }
        return basePath.append(path);
    }

    
    
    public URL buildUrl(Path path) throws CommandException {
        return buildUrl(baseUrl.getProtocol(),baseUrl.getHost(), baseUrl.getPort(),
                path.toString(), null);
    }

    public URL buildUrl(Path path, Map<String,Object>args) throws CommandException {
        return buildUrl(baseUrl.getProtocol(),baseUrl.getHost(), baseUrl.getPort(),
                path.toString(), args);
    }

    
    public static URL buildUrl(String protocol, String host, int port, String path, Map<String,Object>args) throws CommandException {
        try {            
            if (args != null && !args.isEmpty()) {
                StringBuilder buf = new StringBuilder();
                buf.append(path).append('?');
                try {
                    for (Map.Entry<String,Object> entry : args.entrySet()) {
                        buf.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue().toString(), "UTF-8")).append("&");
                    }
                    buf.setLength(buf.length()-1);
                } catch (Exception e) {
                    throw new CommandException("Failed to encode URL query: "+args, e);
                }
                path = buf.toString();
            }
            return new URL(protocol, host, port, path);
        } catch (MalformedURLException e) {
            throw new CommandException("Malformed URL: "+protocol+"://"+host+":"+port+"; path: "+path.toString());
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
        if (baseUrl != null) {
            buf.append(baseUrl);
        }
        if (username != null) {
            buf.append("; username: ").append(username);            
        }
        buf.append("; path: ").append(cwd).append("]");
        return buf.toString();
    }


}
