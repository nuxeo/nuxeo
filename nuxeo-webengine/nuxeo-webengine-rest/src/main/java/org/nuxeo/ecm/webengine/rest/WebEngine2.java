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

package org.nuxeo.ecm.webengine.rest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.ResourceBundle;

import org.nuxeo.ecm.core.url.URLFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.WebClassLoader;
import org.nuxeo.ecm.webengine.nl.ResourceComposite;
import org.nuxeo.ecm.webengine.rest.domains.DefaultWebDomain;
import org.nuxeo.ecm.webengine.rest.jersey.WebContextImpl;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.scripting.Scripting;
import org.nuxeo.ecm.webengine.rest.types.WebTypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngine2 implements FileChangeListener, ResourceLocator {

    protected File root;
    protected Map<String, DefaultWebDomain<?>> domains = new HashMap<String, DefaultWebDomain<?>>();
    protected Map<String, DefaultWebDomain<?>> mappings = new HashMap<String, DefaultWebDomain<?>>();
    protected FileChangeNotifier notifier;
    protected volatile long lastMessagesUpdate = 0;
    protected Scripting scripting;
    protected WebTypeManager typeMgr;
    protected RenderingEngine rendering;
    protected Map<String, Object> env;
    protected ResourceBundle messages;
    protected Map<String, Object> renderingExtensions;
    protected boolean isDebug = false;

    public WebEngine2(File root, FileChangeNotifier notifier) throws IOException {
        isDebug = Boolean.parseBoolean(Framework.getProperty("debug", "false"));
        this.root = root;
        this.notifier = notifier;
        this.scripting = new Scripting(isDebug);
        String cp = System.getProperty("groovy.classpath");
        if (cp == null) {
            cp = new File(root, "classes").getAbsolutePath();
        }
        scripting.addClassPath(cp);
        this.typeMgr = new WebTypeManager(this);
        this.renderingExtensions = new Hashtable<String, Object>();
        this.env = new HashMap<String, Object>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        env.put("version", "1.0.0.b1"); //TODO this should be put in the MANIFEST

        rendering = new FreemarkerEngine();
        rendering.setResourceLocator(this);
        rendering.setSharedVariable("env", getEnvironment());
        Map<String, Object> renderingExtensions = getRenderingExtensions();
        for (Map.Entry<String, Object> entry : renderingExtensions.entrySet()) {
            rendering.setSharedVariable(entry.getKey(), entry.getValue());
        }
        loadMessageBundle(true);
        if (notifier != null) {
            notifier.addListener(this);
        }
    }

    private void loadMessageBundle(boolean watch) throws IOException {
        File file = new File(root, "i18n");
        WebClassLoader cl = new WebClassLoader();
        cl.addFile(file);
        //messages = ResourceBundle.getBundle("messages", Locale.getDefault(), cl);
        messages = new ResourceComposite(cl);
        rendering.setMessageBundle(messages);
        // make a copy to avoid concurrent modifs
        if (watch && notifier != null) {
            notifier.watch(file);
            for (File f : file.listFiles()) {
                notifier.watch(f);
            }
        }
    }

    public boolean isDebug() {
        return isDebug;
    }

    /**
     * @param isDebug the isDebug to set.
     */
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public Map<String, Object> getRenderingExtensions() {
        return renderingExtensions;
    }

    public Object getRenderingExtension(String id) {
        return renderingExtensions.get(id);
    }

    public void registerRenderingExtension(String id, Object obj) {
        renderingExtensions.put(id, obj);
    }

    public void unregisterRenderingExtension(String id) {
        renderingExtensions.remove(id);
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    public ResourceBundle getMessageBundle() {
        return messages;
    }

    public WebTypeManager  getWebTypeManager() {
        return typeMgr;
    }

    /**
     * @return the scripting.
     */
    public Scripting getScripting() {
        return scripting;
    }

    public void registerDomain(DefaultWebDomain<?> domain) {
        //domains.put(domain.getId(), value);
    }

    public DefaultWebDomain<?>[] getDomains() {
        return domains.values().toArray(new DefaultWebDomain[domains.size()]);
    }

    public DefaultWebDomain<?> getDomain(String id) {
        return domains.get(id);
    }

    public DefaultWebDomain<?> getDomainByPath(String path) {
        return mappings.get(path);
    }

    public File getRootDirectory() {
        return root;
    }

    public FileChangeNotifier getFileChangeNotifier() {
        return notifier;
    }

    public RenderingEngine getRendering() {
        return rendering;
    }

    /**
     * Reload configuration
     */
    public void reload() {
        //TODO
        //defaultApplication = null;
        //for (WebApplication app : apps.values()) {
        //    app.flushCache();
        //}
    }

    public void destroy() {
        if (notifier != null) {
            notifier.removeListener(this);
            notifier = null;
        }
    }

    public void fileChanged(FileEntry entry, long now) throws Exception {
        if (lastMessagesUpdate == now) {
            return;
        }
        String path = entry.file.getAbsolutePath();
        String rootPath = root.getAbsolutePath();
        if (!path.startsWith(rootPath)) {
            return;
        }
        lastMessagesUpdate = now;
        loadMessageBundle(false);
    }

    /** ResourceLocator API */

    public URL getResourceURL(String key) {
        try {
            return URLFactory.getURL(key);
        } catch (Exception e) {
            return null;
        }
    }

    public File getResourceFile(String key) {
        try {
            WebContext2 ctx = WebContextImpl.getCurrent();
            ScriptFile file = ctx.getFile(key);
            if (file != null) {
                return file.getFile();
            }
        } catch (IOException e) {
        }
        return null;
    }
}
