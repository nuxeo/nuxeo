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

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.NoSuchResourceException;
import org.nuxeo.ecm.webengine.model.ObjectResource;
import org.nuxeo.ecm.webengine.model.ObjectType;
import org.nuxeo.ecm.webengine.model.Profile;
import org.nuxeo.ecm.webengine.model.TypeNotFoundException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * The default implementation for a web configuration
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProfileImpl implements Profile, FileChangeListener  {


    protected WebEngine engine;
    protected ProfileDescriptor descriptor;

    protected File root;
    protected DirectoryStack dirStack;
    // how many roots are defined by this instance (ignore inherited roots)
    // can be used to iterate only over the local roots
    protected int localRootsCount = 0; 
    // cache used for resolved files
    protected ConcurrentMap<String, ScriptFile> fileCache;

    protected final Object typeLock = new Object();

    // these two members are lazy initialized when needed
    protected TypeRegistry typeReg;
    protected TypeConfigurationProvider localTypes;


    public ProfileImpl(WebEngine engine, File root, ProfileDescriptor descriptor) throws WebException {
        this.fileCache = new ConcurrentHashMap<String, ScriptFile>();
        this.root = root;
        this.descriptor = descriptor;
        this.engine = engine;
        loadDirectoryStack();
    }


    public boolean isFragment() {
        return descriptor.fragment != null;
    }


    public ProfileDescriptor getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return descriptor.name;
    }

    public WebEngine getEngine() {
        return engine;
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
            List<String> roots = descriptor.roots;
            // first add roots defined locally
            dirStack = new DirectoryStack();
            if (descriptor.roots != null && !descriptor.roots.isEmpty()) {
                for (String root : descriptor.roots) {
                    File file =new File(engine.getRootDirectory(), root);
                    dirStack.addDirectory(file);//TODO: priority is meaningless
                }
            } else {
                dirStack.addDirectory(root);
            }
            localRootsCount = dirStack.getDirectories().size();
            // watch roots for modifications
            FileChangeNotifier notifier = engine.getFileChangeNotifier();
            if (notifier != null) {
                if (!dirStack.isEmpty()) {
                    notifier.addListener(this);
                    for (File entry : dirStack.getDirectories()) {
                        notifier.watch(entry);
                    }
                }
            }
            // then add roots from parent if any
            if (roots == null || roots.isEmpty()) {
                if (descriptor.base != null) {
                    ProfileImpl parent = (ProfileImpl)engine.getProfile(descriptor.base);
                    if (parent != null && parent.dirStack != null) {
                        dirStack.getDirectories().addAll(parent.dirStack.getDirectories());
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

    public ScriptFile getFile(String path) throws WebException {
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
        try {
            return findFile(new org.nuxeo.common.utils.Path(path).makeAbsolute().toString());
        } catch (IOException e) {
            throw WebException.wrap(e);
        }
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
    
    /**
     * This is sharing the same cache as the {@link #getFile(String)} method so that cached object files will be  
     *  visible through {@link #getFile(String)}. TODO: Do we really want this?
     *  
     * @param obj
     * @param name
     * @return
     * @throws WebException
     */
    public ScriptFile getTemplate(ObjectResource obj, String name, String mediaType) throws WebException {
        if (name == null) {
            name = "view"; 
        }
        ObjectType type = obj.getType();
        StringBuilder buf = new StringBuilder();
        buf.append('/').append(Utils.fcToLowerCase(type.getName()));
        buf.append('/').append(name);
        if (mediaType != null) {
            buf.append('.').append(mediaType);
        }
        buf.append(getTemplateExtension());
        String key = buf.toString();
        ScriptFile file = fileCache.get(key);
        if (file == null) {
            int len = type.getName().length()+1;
            do {
                buf.replace(1, len, Utils.fcToLowerCase(type.getName()));
                file = getFile(buf.toString());
                if (file != null) {
                    fileCache.put(key, file);
                    break;
                }
                type = type.getSuperType();
                len = type.getName().length()+1;
            } while (type != null);            
        }
        if (file == null) {
            throw new NoSuchResourceException("No Such Template: "+key);
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
    
    public File[] getLocalRoots() {
        File[] local = new File[localRootsCount];
        List<File> entries = dirStack.getDirectories();
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
                    List<File> entries = dirStack.getDirectories();
                    // we need to install them in reverse order so that local roots are installed at 
                    // end to overwrite inherited types
                    for (int i=entries.size()-1; i>=0; i--) {
                        File entry = entries.get(i);
                        TypeConfigurationProvider provider = globalTypes.getProvider(entry.getName());
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
        for (File dir : dirStack.getDirectories()) {
            if (dir.getPath().equals(entry.file.getPath())) {
                fileCache.clear(); // TODO optimize this do not flush entire cache
            }
        }
    }

}
