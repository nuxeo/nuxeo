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

package org.nuxeo.ecm.webengine.client.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.CommandLine;
import org.nuxeo.ecm.webengine.client.command.CommandRegistry;
import org.nuxeo.ecm.webengine.client.util.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class HttpClient {

    protected CommandRegistry registry;    
    protected URL url;
    protected Path cwd;
    protected List<Path> pathStack;
    
    public HttpClient(String url) throws MalformedURLException {
        this (new URL(url));
    }

    public HttpClient(URL url) {
        this.url = url;
        reset();
    }

    public void reset() {
        cwd = Path.ROOT;
        pathStack = new ArrayList<Path>();
        registry = new CommandRegistry();
    }

    /**
     * @return the registry.
     */
    public CommandRegistry getRegistry() {
        return registry;
    }
    
    public CommandLine parseCommandLine(String line) throws CommandException {
        return new CommandLine(registry, line);    
    }

    public void run(String cmd) throws Exception {
        parseCommandLine(cmd).run(this);
    }
    
    
    public void cd(String path) {
        if (path.startsWith("/")) {
            cwd = new Path(path);
        } else {
            cwd = cwd.append(path);
        }
    }

    public void pushd(String path) {
        pathStack.add(cwd);
        cd(path);
    }

    public void popd(String path) {
        if (pathStack.isEmpty()) {
            cwd = Path.ROOT;
        } else {
            cwd = pathStack.remove(pathStack.size()-1);
        }
    }

    public Path pwd() {
        return cwd;
    }

    public String[] ls() {
        return new String[] {};
    }


    
}
