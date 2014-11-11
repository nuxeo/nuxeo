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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Messages;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * The default implementation for a web configuration
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModuleImpl extends AbstractModule {

    private static final Log log = LogFactory.getLog(ModuleImpl.class);

    protected final File root;

    protected Messages messages;

    protected DirectoryStack dirStack;

    // cache used for resolved files
    protected ConcurrentMap<String, ScriptFile> fileCache;

    public ModuleImpl(WebEngine engine, File root, ModuleDescriptor descriptor) {
        super (engine, descriptor);
        fileCache = new ConcurrentHashMap<String, ScriptFile>();
        this.root = root;
        reloadMessages();
        loadDirectoryStack();
    }

    public void flushSkinCache() {
        log.info("Flushing skin cache for module: "+getName());
        fileCache = new ConcurrentHashMap<String, ScriptFile>();
    }

    @Override
    public void flushCache() {
        flushSkinCache();
        super.flushCache();
    }

    public static File getSkinDir(File moduleDir) {
        return new File(moduleDir, "skin");
    }

    protected void loadDirectoryStack() {
        dirStack = new DirectoryStack();
        try {
            File skin = getSkinDir(root);
            if (skin.isDirectory()) {
                dirStack.addDirectory(skin);
            }
            if (superModule instanceof ModuleImpl) {
                DirectoryStack ds = ((ModuleImpl)superModule).dirStack;
                if (ds != null) {
                    dirStack.getDirectories().addAll(ds.getDirectories());
                }
            }
        } catch (IOException e) {
            WebException.wrap("Failed to load directories stack", e);
        }
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
     * @param path a normalized path (absolute path)
     * @return
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

    public ScriptFile getSkinResource(String path) throws IOException {
        File file = dirStack.getFile(path);
        if (file != null) {
            return new ScriptFile(file);
        }
        return null;
    }

    @Override
    public TypeRegistry createTypeRegistry() {
        //double s = System.currentTimeMillis();
        GlobalTypes gtypes = engine.getGlobalTypes();
        TypeRegistry typeReg = null;
        // install types from super modules
        if (superModule != null) { //TODO add type reg listener on super modules to update types  when needed?
            typeReg = new TypeRegistry(superModule.getTypeRegistry(), engine, this);
        } else {
            typeReg = new TypeRegistry(gtypes.getTypeRegistry(), engine, this);
        }
        DirectoryTypeLoader loader = new DirectoryTypeLoader(engine, typeReg, root);
        loader.load();
        typeReg.registerModuleType(this);
        //System.out.println(">>>>>>>>>>>>>"+((System.currentTimeMillis()-s)/1000));
        return typeReg;
    }

    public File getRoot() {
        return root;
    }

    public void reloadMessages() {
        messages = new Messages(superModule != null
                ? superModule.getMessages() : engine.getMessages(), this);
    }

    public Messages getMessages() {
        return messages;
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> getMessages(String language) {
        log.info("Loading i18n files");
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

}
