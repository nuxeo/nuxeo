/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webengine.app.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.app.ModuleHandler;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Messages;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.Validator;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.DirectoryStack;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultModule implements Module {

    public static final Log log = LogFactory.getLog(DefaultModule.class);
    
    protected ModuleHandler handler;
    protected WebEngine engine;
    protected File root;
    protected File skinDir;
    protected Messages messages;
    protected DirectoryStack dirStack;
    protected ModuleHandler base;
    
    // cache used for resolved files
    protected ConcurrentMap<String, ScriptFile> fileCache;

    
    public DefaultModule(ModuleHandler handler) {
        this.handler = handler;
        this.engine = handler.getEngine();
        this.root = new File(engine.getRootDirectory(), "modules/"+handler.getBundledApplication().getId());
        this.skinDir = new File(root, "skin"); 
        fileCache = new ConcurrentHashMap<String, ScriptFile>();
        loadSuperModule();
        loadDirectoryStack();
        loadMessages();
    }
    
    public WebEngine getEngine() {
        return engine;
    }

    public String getName() {
        return handler.getName();
    }

    
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return engine.loadClass(className);
    }

    public File getRoot() {
        return root;
    }

    public Module getSuperModule() {
        return base != null ? base.getModule() : null;                
    }

    public ScriptFile getSkinResource(String path) throws IOException {
        File file = dirStack.getFile(path);
        if (file != null) {
            return new ScriptFile(file);
        }
        return null;
    }

    public ScriptFile getFile(String path) {
        int len = path.length();
        if (len == 0) {
            return null;
        }
        char c = path.charAt(0);
        if (c == '.') { // avoid getting files outside the web root
            path = new Path(path).makeAbsolute().toString();
        } else if (c != '/') {// avoid doing duplicate entries in document stack cache
            path = new StringBuilder(len+1).append("/").append(path).toString();
        }
        try {
            return findFile(new Path(path).makeAbsolute().toString());
        } catch (IOException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param path a normalized path (absolute path).
     */
    protected ScriptFile findFile(String path) throws IOException {
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

    public File getSkinDir() {
        return skinDir;
    }
    
    protected void loadSuperModule() {
        Class<? extends WebEngineModule> superModule = handler.getBaseModule();
        if (superModule != null) {            
            ModuleHandler mh = engine.getApplicationManager().getModuleHandler(superModule.getName());
            if (mh == null) {
                log.warn("Imported module not found: "+superModule
                        +". Needed by module: "+handler.getBundledApplication().getId());
            } else {                    
                base = mh; 
            }
        }
    }
    
    protected void loadDirectoryStack() {
        dirStack = new DirectoryStack();
        try {
            if (skinDir.isDirectory()) {
                dirStack.addDirectory(skinDir);
            }
            Module superModule = getSuperModule();
            if (superModule != null) {
                DirectoryStack ds = ((DefaultModule)superModule).dirStack;
                if (!ds.isEmpty()) {
                    dirStack.getDirectories().addAll(ds.getDirectories());
                }                
            }
        } catch (IOException e) {
            throw WebException.wrap("Failed to load directories stack", e);
        }
    }

    public void loadMessages() {
        Module superModule = getSuperModule();
        messages = new Messages(superModule != null
                ? superModule.getMessages() : null, this);
    }

    public Messages getMessages() {
        return messages;
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> getMessages(String language) {
        log.info("Loading i18n files for module "+getName());
        File file = new File(root,  new StringBuilder()
                    .append("/i18n/messages_")
                    .append(language)
                    .append(".properties").toString());
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            Properties p = new Properties();
            p.load(in);
            return new HashMap(p); // HashMap is faster than Properties
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ee) {
                    log.error(ee);
                }
            }
        }
    }
    
    public List<LinkDescriptor> getActiveLinks(Resource context, String category) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("not yet impl");
    }

    public List<LinkDescriptor> getLinks(String category) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("not yet impl");
    }

    @Override
    public String toString() {
        return getName();
    }

    // the following should be removed
    @Deprecated    
    public void flushCache() {
    }

    @Deprecated
    public String getTemplateFileExt() {
        return "ftl";
    }

    @Deprecated
    public String getMediaTypeId(MediaType mt) {
        throw new UnsupportedOperationException("deprecated");        
    }

    @Deprecated 
    public String getSkinPathPrefix() {
        throw new UnsupportedOperationException("deprecated");        
    }

    @Deprecated
    public Resource getRootObject(WebContext ctx) {
        throw new UnsupportedOperationException("deprecated");
    }

    
    @Deprecated
    public AdapterType getAdapter(Resource ctx, String name) {
        throw new UnsupportedOperationException("deprecated");
    }
    @Deprecated
    public List<String> getAdapterNames(Resource ctx) {
        throw new UnsupportedOperationException("deprecated");
    }
    @Deprecated
    public AdapterType[] getAdapters() {
        throw new UnsupportedOperationException("deprecated");
    }
    @Deprecated
    public List<AdapterType> getAdapters(Resource ctx) {
        throw new UnsupportedOperationException("deprecated");
    }
    @Deprecated
    public List<String> getEnabledAdapterNames(Resource ctx) {
        throw new UnsupportedOperationException("deprecated");
    }
    @Deprecated
    public List<AdapterType> getEnabledAdapters(Resource ctx) {
        throw new UnsupportedOperationException("deprecated");
    }

    @Deprecated
    public List<ResourceBinding> getResourceBindings() {
        throw new UnsupportedOperationException("deprecated");        
    }
    
    @Deprecated
    public ResourceType getType(String typeName) {
        throw new UnsupportedOperationException("deprecated");
    }

    @Deprecated
    public ResourceType[] getTypes() {
        throw new UnsupportedOperationException("deprecated");        
    }

    @Deprecated
    public Validator getValidator(String docType) {
        throw new UnsupportedOperationException("deprecated");        
    }

    @Deprecated
    public boolean isDerivedFrom(String moduleName) {
        throw new UnsupportedOperationException("deprecated");        
    }

}
