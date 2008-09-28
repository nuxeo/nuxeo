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

package org.nuxeo.ecm.webengine.rest.model;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.impl.ApplicationDescriptor;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.runtime.deploy.FileChangeListener;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebApplication extends FileChangeListener {

    void initialize(WebEngine2 engine, File root, ApplicationDescriptor descriptor) throws WebException;
    
    String getName();
    
    boolean isFragment();

    String getPath();
    boolean getPathLimited();
    boolean getPathEncode();
    
    WebEngine2 getEngine();

    ScriptFile getIndexPage();

    ScriptFile getErrorPage();

    ScriptFile getDefaultPage();

    String getScriptExtension();

    String getTemplateExtension();
    
    Object getProperty(String key);
    
    Object getProperty(String key, Object defValue);

    void flushCache();

    /**
     * Get a file using the configured directory stack. Each directory in the stack is asked for the file
     * until a file is found. If no file is found return null.
     * <p>
     * Note that the implementation may cache the results. 
     * To clear any cached data you may call the {@link #flushCache()} method
     *  
     * @param path the file path
     * @return null if no file found otherwise the file
     * @throws IOException if any error occurs
     */
    ScriptFile getFile(String path) throws IOException;    

    /**
     * Load a class given it's name. The scripting class loader will be used to load the class. 
     * @param className the class name
     * @return the class instance
     * @throws ClassNotFoundException
     */
    Class<?> loadClass(String className) throws ClassNotFoundException;

    /**
     * Get a {@link WebType} instance given it's name.
     * The web type lookup is performed in the following order:
     * <ol>
     * <li> First the annotated Groovy classes are checked. (web/ directory)  
     * <li> Then the configuration type registry corresponding 
     * </ol> 
     * @param typeName the type name
     * @return the web type instance
     * @throws TypeNotFoundException if no such web type was defined
     */
    WebType getType(String typeName) throws TypeNotFoundException;
    
}
