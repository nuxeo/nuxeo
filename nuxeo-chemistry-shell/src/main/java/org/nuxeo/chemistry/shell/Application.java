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

import org.nuxeo.chemistry.shell.command.CommandRegistry;

/**
 * An application represents the global context of the shell.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Application {

    /**
     * Login using the given account.
     *
     * @param username
     * @param password
     */
    void login(String username, char[] password);
    
    /**
     * Gets the connection URL.
     *
     * @return
     */
    URL getServerUrl();
    
    /**
     * Gets he username used for the connection.
     *
     * @return
     */
    String getUsername();
    
    /**
     * Gets the host where the application is connected.
     *
     * @return
     */
    String getHost();
    
    /**
     * Gets the working directory.
     *
     * @return
     */
    File getWorkingDirectory();
    
    /**
     * Sets the working directory (will be used to resolve relative file paths).
     *
     * @param file
     */
    void setWorkingDirectory(File file);
    
    /**
     * Gets a file given its path.
     * If the path is absolute (starts with '/') it will be resolved as an absolute path
     * otherwise it will be resolved against the current working directory.
     *
     * @param path
     * @return
     */
    File resolveFile(String path);

    /**
     * Gets the current context.
     *
     * @return
     */
    Context getContext();
    
    /**
     * Sets the current context to the given one.
     *
     * @param ctx
     */
    void setContext(Context ctx);
    
    /**
     * Resolves the given path to a context.
     *
     * @param path
     * @return
     */
    Context resolveContext(Path path);
    
    /**
     * Gets the root context.
     *
     * @return
     */
    Context getRootContext();
    
    /**
     * Gets the command registry.
     *
     * @return
     */
    CommandRegistry getCommandRegistry();
        
    /**
     * Sets a global variable. Can be used by commands to preserve their state.
     *
     * @param key
     * @param data
     */
    void setData(String key, Object data);
    
    /**
     * Gets a global variable given its key.
     *
     * @param key
     * @return
     */
    Object getData(String key);
    
    /**
     * Connects to the given url. The current context will be reset.
     *
     * @param uri
     * @throws IOException
     */
    void connect(String uri) throws IOException;
    
    /**
     * Connects to the given url. The current context will be reset.
     *
     * @param uri
     * @throws IOException
     */
    void connect(URL uri) throws IOException;
    
    /**
     * Disconnects if already connected. The current context will be reset.
     *
     */
    void disconnect();
    
    /**
     * Tests if connected.
     *
     * @return
     */
    boolean isConnected();
    
    //TODO
    String getHelp(String cmdName);
    
}
