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

package org.nuxeo.ecm.webengine.rest.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.model.TypeNotFoundException;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.WebType;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.util.DirectoryStack;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * The default implementation for a web configuration
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebApplication implements WebApplication {

    protected WebEngine2 engine;
    protected ApplicationDescriptor descriptor;

    protected File root;
    protected DirectoryStack dirStack;
    protected ConcurrentMap<String, ScriptFile> fileCache;

    protected final Object typeLock = new Object();
    
    // these two members are lazy initialized when needed
    protected TypeRegistry typeReg;
    protected TypeConfigurationProvider localTypes;


    public DefaultWebApplication() {
        this.fileCache = new ConcurrentHashMap<String, ScriptFile>();
    }    
    
    public void initialize(WebEngine2 engine, File root, ApplicationDescriptor descriptor) throws WebException {
        this.root = root;
        this.descriptor = descriptor;
        this.engine = engine;
        loadDirectoryStack();
    }

    public boolean isFragment() {
        return descriptor.fragment != null;
    }
    
    public String getPath() {
        return descriptor.path.path;
    }

    public boolean getPathLimited() {
        return descriptor.path.limited;
    }

    public boolean getPathEncode() {
        return descriptor.path.encode;
    }

    public Object getProperty(String key) {
        return descriptor.properties.get(key);
    }

    public Object getProperty(String key, Object defValue) {
        Object val = getProperty(key);
        return val == null ? defValue : val;
    }

    public ApplicationDescriptor getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return descriptor.name;
    }

    public WebEngine2 getEngine() {
        return engine;
    }

    public ScriptFile getDefaultPage() {
        try {
            return getFile(descriptor.defaultPage);
        } catch (IOException e) {
            return null;
        }
    }

    public ScriptFile getErrorPage() {
        try {
            return getFile(descriptor.errorPage);
        } catch (IOException e) {
            return null;
        }
    }

    public ScriptFile getIndexPage() {
        try {
            return getFile(descriptor.indexPage);
        } catch (IOException e) {
            return null;
        }
    }


    public void flushCache() {
        fileCache.clear();
        synchronized (typeLock) {
            typeReg = null; // type registry will be recreated on first access
            localTypes = null;
        }
        engine.getScripting().flushCache();
    }

    protected void loadDirectoryStack() throws WebException {
        try {
            List<RootDescriptor> roots = descriptor.roots;
            // if no roots are defined import base roots and add application directory
            if (roots == null || roots.isEmpty()) {  
                if (descriptor.base != null) { 
                    DefaultWebApplication parent = (DefaultWebApplication)engine.getApplication(descriptor.base);
                    if (parent != null && parent.dirStack != null) {                        
                        dirStack = new DirectoryStack(parent.dirStack.getEntries());
                        int size  = dirStack.getEntries().size();
                        int p = 0;
                        if (size > 0) {
                            DirectoryStack.Entry entry = dirStack.getEntries().get(size-1);
                            p = entry.priority+1;
                        }
                        dirStack.addDirectory(root, p);
                    }
                } else {
                    dirStack = new DirectoryStack();
                    if (descriptor.roots != null && !descriptor.roots.isEmpty()) {
                        for (RootDescriptor rd : descriptor.roots) {
                            File file =new File(engine.getRootDirectory(), rd.path);
                            dirStack.addDirectory(file, rd.priority);
                        }
                        Collections.sort(dirStack.getEntries()); //TODO priority is meaningless
                    } else {
                        dirStack.addDirectory(root, 0);
                    }
                }
            }

            // watch roots for modifications
            FileChangeNotifier notifier = engine.getFileChangeNotifier();
            if (notifier != null) {
                if (!dirStack.isEmpty()) {
                    notifier.addListener(this);
                    for (DirectoryStack.Entry entry : dirStack.getEntries()) {
                        notifier.watch(entry.file);
                    }
                }
            }
        } catch (IOException e) {
            WebException.wrap("Failed to load directories stack", e);
        }
    }

    public String getScriptExtension() {
        return descriptor.scriptExtension;
    }

    public String getTemplateExtension() {
        return descriptor.templateExtension;
    }

    public ScriptFile getFile(String path) throws IOException {
        int len = path.length();
        if (len == 0) {
            return null;
        }
//        char c = path.charAt(0);
//        if (c == '.') { // avoid getting files outside the web root
//            path = new org.nuxeo.common.utils.Path(path).makeAbsolute().toString();
//        } else if (c != '/') {// avoid doing duplicate entries in document stack cache
//            path = new StringBuilder(len+1).append("/").append(path).toString();
//        }
        return findFile(new org.nuxeo.common.utils.Path(path).makeAbsolute().toString());
    }

    /**
     * @param path a normalized path (absolute path)
     * @return
     */
    private ScriptFile findFile(String path) throws IOException {
        ScriptFile file = fileCache.get(path);
        if (file == null) {
            File f = dirStack.getFile(path);
            if (f != null) {
                file = new ScriptFile(f);
                fileCache.put(path, file);
            }
        }
        return file;
    }

    protected void loadConfiguredTypes() {
        localTypes = new TypeConfigurationProvider();
        // load declared types and actions        
        localTypes.types = new ArrayList<TypeDescriptor>();
        if (descriptor.types != null) {
            localTypes.types.addAll(descriptor.types); 
        }
        localTypes.actions = new ArrayList<ActionDescriptor>();
        if (descriptor.actions != null) {
            descriptor.actions.addAll(descriptor.actions);
        }
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return engine.getScripting().loadClass(className);
    }

    public WebType getType(String typeName) throws TypeNotFoundException {
        if (typeReg == null) { // create type registry if not already created
            synchronized (typeLock) {
                if (typeReg == null) {
                    typeReg = new TypeRegistry();
                    // install global types
                    GlobalTypesLoader globalTypes = engine.getGlobalTypes();
                    globalTypes.getMainProvider().install(typeReg);
                    // install types defined in script classes for each entry in directory stack 
                    for (DirectoryStack.Entry entry : dirStack.getEntries()) {
                        TypeConfigurationProvider provider = globalTypes.getProvider(entry.file.getName());
                        if (provider != null) {
                            provider.install(typeReg);
                        }
                    }
                    // install local configured types (in XML configuration)
                    loadConfiguredTypes();
                    localTypes.install(typeReg);
                }
            }
        }
        return typeReg.getType(typeName);
    }

    /**
     * A tracked file changed.
     * Flush directory stack cache
     */
    public void fileChanged(FileEntry entry, long now) throws Exception {
        for (DirectoryStack.Entry dir : dirStack.getEntries()) {
            if (dir.file.getPath().equals(entry.file.getPath())) {
                fileCache.clear(); // TODO optimize this do not flush entire cache
            }
        }
    }


}
