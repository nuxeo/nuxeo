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


/**
 * A context represents the current state of the shell application and is usually wrapping 
 * the current selected remote object. 
 * 
 * The context can be adapted to the wrapped object by calling {@link Context#as(Class)} 
 * and providing the desired type.
 * If the context cannot be represented as the given type it will return null,
 * otherwise will return the instance of the desired object.
 * 
 * A context may wrap a "folder" object (thus may contain sub contexts) or it may be a leaf context.
 * To change a context to another remote object the Context#cd
 * 
 * A context is providing several basic operations like:
 * <ul>
 * <li> ls  - list available sub contexts
 * <li> pwd - get the context absolute path
 * <li> id  - show more information about the current context
 * <li> cd  - change the context to another context given a context path. 
 * If the path starts with a '/' it will be assumed to be an absolute path otherwise it will be resolved relative to the current context  
 * </ul>
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Context {

    /**
     * Get the current application
     * @return
     */
    public Application getApplication();
    
    /**
     * Get the context path
     * @return
     */
    public Path getPath();

    /**
     * Get the context absolute path as a string
     * @return
     */
    public String pwd();
    
    /**
     * This method is listing the keys of the available sub contexts. 
     * This is used by the command line completor.
     * @return an empty array if no sub contexts are available, otherwise return the array of sub context names
     */
    public String[] entries();
    
    /**
     * List sub contexts names. The returned names are colored (may contain color code characters)
     * @return
     */
    public String[] ls(); //colored entries

    /**
     * Get a child context given its name
     * @param name
     * @return null if no such sub context exists, othrwise return the sub context
     */
    public Context getContext(String name);
    
    public Path resolvePath(String path);

    
    /**
     * Clear any cache associated with the context 
     */
    public void reset();    
    
    /**
     * Adapt the context to the given type.
     * @param <T>
     * @param type
     * @return null if the context cannot be adapted, otherwise an instance of the given type 
     */
    public <T> T as(Class<T> type);
    
    /**
     * Get a string identifying this context. (can be the object title and path or other useful information). 
     * @return
     */
    public String id();

}
