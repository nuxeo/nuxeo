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

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.url.URLFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.exceptions.WebDeployException;
import org.nuxeo.ecm.webengine.mapping.MappingDescriptor;
import org.nuxeo.ecm.webengine.mapping.PathMapper;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.servlet.WebServlet;
import org.nuxeo.ecm.webengine.util.DirectoryStack;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebApplication implements WebApplication, FileChangeListener {

    public static final Log log = LogFactory.getLog(WebApplication.class);

    protected final String id;
    protected final WebEngine engine;
    protected FreemarkerEngine rendering;
    protected DirectoryStack dirStack;
    protected String errorPage;
    protected String indexPage;
    protected String defaultPage;
    protected Map<String, String> typeBindings;
    protected PathMapper mapper;
    protected String repositoryName;
    protected WebApplicationDescriptor desc;

    // object binding cache
    protected ConcurrentMap<String, WebObjectDescriptor> objects;
    protected ConcurrentMap<String, ScriptFile> fileCache;
    protected ConcurrentMap<String, ScriptFile> actionCache;

    public DefaultWebApplication(WebEngine engine, WebApplicationDescriptor desc) throws WebException {
        this.engine = engine;
        id = desc.getId();
        defaultPage = desc.getDefaultPage("default.ftl");
        indexPage = desc.getIndexPage("index.ftl");
        errorPage = desc.getErrorPage("error.ftl");
        repositoryName = desc.getRepositoryName();
        if (repositoryName == null) {
            repositoryName = "default";
        }
        List<WebObjectBindingDescriptor> list = desc.getBindings();
        if (list != null && !list.isEmpty()) {
            typeBindings = new HashMap<String, String>();
            for (WebObjectBindingDescriptor obd : list) {
                typeBindings.put(obd.type, obd.objectId);
            }
        }
        List<MappingDescriptor> mappings = desc.getMappings();
        if (mappings != null && !mappings.isEmpty()) {
            mapper = new PathMapper(mappings);
        } else {
            mapper = new PathMapper();
        }
        try {
            List<RootDescriptor> roots = desc.getRoots();
            dirStack = new DirectoryStack();
            if (roots == null) {
                dirStack.addDirectory(new File(engine.getRootDirectory(), "default"), 0);
            } else {
                Collections.sort(roots);
                for (RootDescriptor rd : roots) {
                    File file =new File(engine.getRootDirectory(), rd.path);
                    dirStack.addDirectory(file, rd.priority);
                }
            }

            rendering = new FreemarkerEngine();
            rendering.setResourceLocator(this);
            rendering.setMessageBundle(engine.getMessageBundle());
            rendering.setSharedVariable("env", engine.getEnvironment());
            Map<String, Object> renderingExtensions = engine.getRenderingExtensions();
            for (Map.Entry<String, Object> entry : renderingExtensions.entrySet()) {
                rendering.setSharedVariable(entry.getKey(), entry.getValue());
            }
            objects = new ConcurrentHashMap<String, WebObjectDescriptor>();
            fileCache = new ConcurrentHashMap<String, ScriptFile>();
            actionCache = new ConcurrentHashMap<String, ScriptFile>();
            this.desc = desc;

            FileChangeNotifier notifier = engine.getFileChangeNotifier();
            if (notifier != null) {
                if (!dirStack.isEmpty()) {
                    notifier.addListener(this);
                    for (DirectoryStack.Entry entry : dirStack.getEntries()) {
                        notifier.watch(entry.file);
                    }
                }
            }

        } catch (Exception e) {
            throw new WebDeployException("Failed to create virtual directory for webapp: "+id, e);
        }
    }

    public RenderingEngine getRendering() {
        return rendering;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public WebApplicationDescriptor getDescriptor(){
        return desc;
    }

    public PathMapper getPathMapper() {
        return mapper;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public String getId() {
        return id;
    }

    public DirectoryStack getDirectoryStack() {
        return dirStack;
    }

    public String getDefaultPage() {
        return defaultPage;
    }

    /**
     * Used by tests.
     */
    public void setDefaultPage(String defaultPage) {
        this.defaultPage = defaultPage;
    }

    public String getErrorPage() {
        return errorPage;
    }

    public String getIndexPage() {
        return indexPage;
    }

    public String getTypeBinding(String type) {
        if (typeBindings != null) {
            String id = typeBindings.get(type);
            if (id != null) {
                return id;
            }
        }
        return engine.getTypeBinding(type);
    }

    public WebObjectDescriptor getDefaultObject() {
        WebObjectDescriptor obj = objects.get("Document");
        if (obj == null) {
            String id = getTypeBinding("Document");
            if (id == null) {
                throw new WebRuntimeException(
                        "Invalid configuration: The default object (the one mapped to Document type) was not declared.");
            }
            obj = engine.getObject(id);
            if (obj == null) {
                throw new WebRuntimeException(
                        "Invalid configuration: The default object (the one mapped to Document type) was not declared.");
            }
            objects.put("Document", obj);
        }
        return obj;
    }

    public synchronized WebObjectDescriptor getObjectDescriptor(Type type) {
        String typeName = type.getName();
        WebObjectDescriptor obj = objects.get(typeName);
        if (obj != null) { // in cache
            return obj;
        }
        String id = getTypeBinding(typeName);
        if (id == null) {
            Type stype = type.getSuperType();
            if (stype == null || stype.getName().equals("Document")) {// the default
                obj = getDefaultObject();
            } else {
                obj = getObjectDescriptor(stype);
            }
        } else {
            obj = engine.getObject(id);
            if (obj == null) {
                Type stype = type.getSuperType();
                if (stype == null || stype.getName().equals("Document")) {// the default
                    obj = getDefaultObject();
                } else {
                    obj = getObjectDescriptor(stype);
                }
            }
        }
        objects.put(typeName, obj);
        return obj;
    }

    public void flushCache() {
        objects.clear();
        fileCache.clear();
        actionCache.clear();
    }

    public ScriptFile getFile(String path) throws IOException {
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
        return findFile(path);
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

    public ScriptFile getActionScript(String action, DocumentType docType) throws IOException {
        String key = new StringBuilder().append(docType.getName()).append("@").append(action).toString();
        ScriptFile file = actionCache.get(key);
        if (file == null) {
            for (String ext : desc.getScriptExtensions()) {
                file = getActionScript(action, docType, ext);
                if (file != null) {
                    actionCache.put(key, file);
                    break;
                }
            }
        }
        return file;
    }

    public ScriptFile getActionScript(String action, DocumentType docType, String ext) throws IOException {
        String type = docType.getName();
        String path = "/" + type + '/' + action + ext;
        // we avoid passing through WebContext#getFile() since this is always an absolute path
        ScriptFile file = findFile(path);
        if (file == null) {
            docType = (DocumentType)docType.getSuperType();
            if (docType != null) {
                file = getActionScript(action, docType);
                if (file != null) {
                    fileCache.put(path, file);
                }
            }
        }
        return file;
    }

    public WebEngine getWebEngine() {
        return engine;
    }

    public void registerRenderingExtension(String id, Object obj) {
        rendering.setSharedVariable(id, obj);
    }

    public void unregisterRenderingExtension(String id) {
        rendering.setSharedVariable(id, null);
    }

    public URL getResourceURL(String key) {
        try {
            return URLFactory.getURL(key);
        } catch (Exception e) {
            return null;
        }
    }

    public File getResourceFile(String key) {
        try {
            // -------- workaround to support action references (any string starting ith @@)
            if (key.length() > 2 && key.charAt(0) == '@' && key.charAt(1) == '@') {
                WebContext ctx = WebServlet.getContext();
                WebObject obj = ctx.getTargetObject();
                if (obj != null) {
                    ScriptFile file = obj.getActionScript(key.substring(2));
                    if (file != null) {
                        return file.getFile();
                    }
                }
            }
            // -------- end workaround
            ScriptFile file = getFile(key);
            if (file != null) {
                return file.getFile();
            }
        } catch (IOException e) {
        }
        return null;
    }

    public void fileChanged(FileChangeNotifier.FileEntry entry, long now) {
        for (DirectoryStack.Entry dir : dirStack.getEntries()) {
            if (dir.file.getPath().equals(entry.file.getPath())) {
                fileCache.clear(); // TODO optimize this do not flush entire cache
            }
        }
    }

}
