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
 */
package org.nuxeo.chemistry.shell;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.chemistry.shell.command.CommandRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractApplication implements Application {

    protected final CommandRegistry registry;
    protected final Map<String, Object> dataMap;

    protected Context ctx;
    protected URL serverUrl;
    protected File wd;
    protected String username;
    protected char[] password;


    public AbstractApplication() {
        registry = new CommandRegistry();
        dataMap = new HashMap<String, Object>();
        wd = new File(".");
        ctx = getRootContext();
    }

    protected void initServerURL(URL serverUrl) {
        String userInfo = serverUrl.getUserInfo();
        if (userInfo != null) {
            int p = userInfo.indexOf(':');
            if (p > -1) {
                username = userInfo.substring(0, p);
                password = userInfo.substring(p+1).toCharArray();
            } else {
                username = userInfo;
            }
        }
        // do URL cleanup
        try {
            this.serverUrl = new URL(serverUrl.getProtocol(), serverUrl.getHost(), serverUrl.getPort(), serverUrl.getPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getHost() {
        return serverUrl.getHost();
    }

    public void connect(String uri) throws IOException {
        connect(new URL(uri));
    }

    public void connect(URL uri) throws IOException {
        initServerURL(uri);
        doConnect();
    }

    protected abstract void doConnect();

    public CommandRegistry getCommandRegistry() {
        return registry;
    }

    public File resolveFile(String path) {
        if (path.startsWith("/")) {
            return new File(path);
        } else {
            return new File(wd, path);
        }
    }

    public Context getContext() {
        return ctx;
    }

    public Object getData(String key) {
        return dataMap.get(key);
    }

    public URL getServerUrl() {
        return serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public File getWorkingDirectory() {
        return wd;
    }

    public void login(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public Context resolveContext(Path path) {
        Context c;
        if (path.isRelative()) {
            if (path.segmentCount() == 0) {
                return getContext();
            }
            boolean dotdot = false;
            while (path.segmentCount() > 0) {
                String seg = path.segment(0);
                if (seg.equals(".")) {
                    path = path.removeFirstSegments(1);
                } else if (seg.equals("..")) {
                    dotdot = true;
                    break;
                } else {
                    break;
                }
            }
            if (dotdot) {
                path = getContext().getPath().append(path);
                c = getRootContext();
            } else {
                c = getContext();
            }
        } else {
            c = getRootContext();
        }
        if (c == null) {
            return null;
        }
        for (int i=0,cnt=path.segmentCount(); i<cnt; i++) {
            c = c.getContext(path.segment(i));
            if (c == null) {
                return null;
            }
        }
        return c;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
        Console.getDefault().updatePrompt();
    }

    public void setData(String key, Object data) {
        if (data == null) {
            dataMap.remove(key);
        } else {
            dataMap.put(key, data);
        }
    }

    public void setWorkingDirectory(File file) {
        wd = file;
        Console.getDefault().updatePrompt();
    }

    //TODO
    public String getHelp(String cmdName) {
        return "TODO";
    }

}
