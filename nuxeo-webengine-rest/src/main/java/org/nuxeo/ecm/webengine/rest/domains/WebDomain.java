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

package org.nuxeo.ecm.webengine.rest.domains;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.util.DirectoryStack;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

import com.sun.jersey.api.core.HttpContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@ProduceMime({"text/html", "*/*"})
public class WebDomain<T extends DomainDescriptor> implements FileChangeListener {

    public String id;
    public WebEngine2 engine;
    public T descriptor;

    protected DirectoryStack dirStack;
    protected ConcurrentMap<String, ScriptFile> fileCache;


    public WebDomain(WebEngine2 engine, T desc) throws WebException {
        descriptor = desc;
        this.engine = engine;
        this.fileCache = new ConcurrentHashMap<String, ScriptFile>();
        loadDirectoryStack();
    }

    public void flushCache() {
        fileCache.clear();
    }

    protected void loadDirectoryStack() throws WebException {
        try {
            List<RootDescriptor> roots = descriptor.roots;
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

    protected WebObject resolve(WebContext2 ctx, String path) throws WebException {
        return null;
    }

    @Path(value="{path}", limited=false)
    public WebObject dispatch(@PathParam("path") String path, @Context HttpContext ctx) throws Exception {
        WebContext2 webCtx = (WebContext2)ctx;
        webCtx.setDomain(this);
        return resolve((WebContext2)ctx, path);
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
