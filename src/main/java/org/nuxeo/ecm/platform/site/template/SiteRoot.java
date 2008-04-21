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

package org.nuxeo.ecm.platform.site.template;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.site.mapping.Mapping;
import org.nuxeo.ecm.platform.site.mapping.PathMapper;
import org.nuxeo.ecm.platform.site.resolver.DefaultSiteResolver;
import org.nuxeo.ecm.platform.site.resolver.SiteResourceResolver;
import org.nuxeo.ecm.platform.site.template.config.Configuration;
import org.nuxeo.ecm.platform.site.template.config.MainSection;
import org.nuxeo.ecm.platform.site.template.config.MappingSection;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRoot implements FileChangeListener {

    final static Log log = LogFactory.getLog(SiteRoot.class);

    SiteManager manager;

    File root;

    boolean debug = false;

    Version version = new Version(1, 0, 0);

    PathMapper mapper;

    Configuration config = null;

    ConcurrentMap<String, Mapping> cache;

    SiteResourceResolver resolver = DefaultSiteResolver.INSTANCE;

    ConcurrentMap<String, ScriptFile> scriptFileCache;

    public SiteRoot(SiteManager manager, String name) {
        this (manager, new File(manager.getRootDirectory(), name));
    }

    public SiteRoot(SiteManager manager, File root) {
        this.manager = manager;
        this.root = root;
        this.mapper = new PathMapper();
        this.cache = new ConcurrentHashMap<String, Mapping>();
        this.config = new Configuration("main");
        config.putConfigurator("main", MainSection.INSTANCE);
        config.putConfigurator("mapping", MappingSection.INSTANCE);
    }

    public SiteManager getSiteManager() {
        return manager;
    }

    public void setDebugEnabled(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebugEnabled() {
        return this.debug;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public void setResolver(SiteResourceResolver resolver) {
        this.resolver = resolver;
    }

    public SiteResourceResolver getResolver() {
        return resolver;
    }

    public Mapping getMapping(String pathInfo) {
        return mapper.getMapping(pathInfo);
    }

    public PathMapper getPathMapper() {
        return mapper;
    }

    /**
     * Given a relative path to this site get the path relative to the web directory
     * @param path
     * @return
     */
    public String getWebFilePath(String path) {
        String name = root.getName();
        if (path.startsWith("/")) {
            return "/"+name+path;
        } else {
            return "/"+name+"/"+path;
        }
    }

    public ScriptFile getScript(String path, String type) {
        path = getWebFilePath(path);
        return new ScriptFile(manager.getRootDirectory(), path, type);
    }

    public File getFile(String path) {
        return new File(root, path);
    }

    public void loadConfiguration() throws IOException {
        config.loadConfiguration(this, new File(root, ".metadata"));
        if (debug) { // register file reloader
            FileChangeNotifier notifier = Framework.getLocalService(FileChangeNotifier.class);
            if (notifier != null) {
                notifier.addListener(this);
            }
        }
    }

    public void reload() throws IOException {
        mapper.clearMappings();
        cache.clear();
        if (debug) {
            FileChangeNotifier notifier = Framework.getLocalService(FileChangeNotifier.class);
            if (notifier != null) {
                notifier.removeListener(this);
            }
        }
        loadConfiguration();
    }



    public void fileChanged(File file, long since) {
        if (file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            if (file.getName().equals(".metadata")) {
                try {
                    reload();
                    log.info("Reloaded site configuration for: "+root.getName());
                } catch (IOException e) {
                    log.error("Failed to reload site configuration for: "+root.getName());
                }
            }
        }
    }

}
