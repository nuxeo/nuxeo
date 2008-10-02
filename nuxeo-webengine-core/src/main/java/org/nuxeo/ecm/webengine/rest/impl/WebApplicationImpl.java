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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.model.ObjectType;
import org.nuxeo.ecm.webengine.rest.model.TypeNotFoundException;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.util.DirectoryStack;
import org.nuxeo.ecm.webengine.util.DirectoryStack.Entry;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * The default implementation for a web configuration
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebApplicationImpl implements WebApplication, FileChangeListener  {


    protected WebEngine2 engine;
    protected ApplicationDescriptor descriptor;

    protected File root;
    protected DirectoryStack dirStack;
    // how many roots are defined by this instance (ignore inherited roots)
    // can be used to iterate only over the local roots
    protected int localRootsCount = 0; 
    protected ConcurrentMap<String, ScriptFile> fileCache;

    protected final Object typeLock = new Object();

    // these two members are lazy initialized when needed
    protected TypeRegistry typeReg;
    protected TypeConfigurationProvider localTypes;


    public WebApplicationImpl(WebEngine2 engine, File root, ApplicationDescriptor descriptor) throws WebException {
        this.fileCache = new ConcurrentHashMap<String, ScriptFile>();
        this.root = root;
        this.descriptor = descriptor;
        this.engine = engine;
        loadDirectoryStack();
    }


    public boolean isFragment() {
        return descriptor.fragment != null;
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
            // first add roots defined locally
            dirStack = new DirectoryStack();
            if (descriptor.roots != null && !descriptor.roots.isEmpty()) {
                for (RootDescriptor rd : descriptor.roots) {
                    File file =new File(engine.getRootDirectory(), rd.path);
                    dirStack.addDirectory(file, rd.priority);//TODO: priority is meaningless
                }
            } else {
                dirStack.addDirectory(root, 0);
            }
            localRootsCount = dirStack.getEntries().size();
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
            // then add roots from parent if any
            if (roots == null || roots.isEmpty()) {
                if (descriptor.base != null) {
                    WebApplicationImpl parent = (WebApplicationImpl)engine.getApplication(descriptor.base);
                    if (parent != null && parent.dirStack != null) {
                        dirStack.getEntries().addAll(parent.dirStack.getEntries());
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
        char c = path.charAt(0);
        if (c == '.') { // avoid getting files outside the web root
            path = new org.nuxeo.common.utils.Path(path).makeAbsolute().toString();
        } else if (c != '/') {// avoid doing duplicate entries in document stack cache
            path = new StringBuilder(len+1).append("/").append(path).toString();
        }
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
        localTypes.actions = new ArrayList<ActionTypeImpl>();
        if (descriptor.actions != null) {
            descriptor.actions.addAll(descriptor.actions);
        }
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return engine.getScripting().loadClass(className);
    }
    
    public DirectoryStack.Entry[] getLocalRoots() {
        DirectoryStack.Entry[] local = new DirectoryStack.Entry[localRootsCount];
        List<Entry> entries = dirStack.getEntries();
        assert localRootsCount <= entries.size();
        for (int i=0; i<localRootsCount; i++) {
            local[i] = entries.get(i);
        }
        return local;
    }

    public ObjectType getType(String typeName) throws TypeNotFoundException {
        if (typeReg == null) { // create type registry if not already created
            synchronized (typeLock) {
                if (typeReg == null) {
                    typeReg = new TypeRegistry();
                    // install global types
                    GlobalTypesLoader globalTypes = engine.getGlobalTypes();
                    globalTypes.getMainProvider().install(typeReg);
                    // install types defined in script classes for each entry in local directory stack
                    List<Entry> entries = dirStack.getEntries();
                    // we need to install them in reverse order so that local roots are installed at 
                    // end to overwrite inherited types
                    for (int i=entries.size()-1; i>=0; i--) {
                        DirectoryStack.Entry entry = entries.get(i);
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
        ObjectType type = typeReg.getType(typeName);
        if (type == null) {
            throw new TypeNotFoundException(typeName);
        }
        return type;
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
