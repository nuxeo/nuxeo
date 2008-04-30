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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.config.Configuration;
import org.nuxeo.ecm.webengine.config.MainSection;
import org.nuxeo.ecm.webengine.config.MappingSection;
import org.nuxeo.ecm.webengine.mapping.Mapping;
import org.nuxeo.ecm.webengine.mapping.PathMapper;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.util.FileChangeListener;
import org.nuxeo.ecm.webengine.util.FileChangeNotifier;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRoot implements FileChangeListener {

    final static Log log = LogFactory.getLog(SiteRoot.class);

    public final static String SITE_CONFIG = "site.config";

    SiteManager manager;

    File root;

    boolean debug = false;

    String defaultPage = "unknown.ftl";

    String errorPage = "error.ftl";

    Version version = new Version(1, 0, 0);

    PathMapper mapper;

    Configuration config = null;

    ConcurrentMap<String, Mapping> cache;

    DocumentResolver resolver = DefaultDocumentResolver.INSTANCE;

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
        try {
            loadConfiguration();
        } catch (IOException e) {
            log.error("Failed to load site configuration", e);
        }
        if (debug) { // register file reloader
            FileChangeNotifier notifier = Framework.getLocalService(FileChangeNotifier.class);
            if (notifier != null) {
                notifier.addListener(this);
                try {
                    notifier.watch(new File(root, SITE_CONFIG));
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    public void dispose() {
        FileChangeNotifier fcn = Framework.getLocalService(FileChangeNotifier.class);
        if (fcn != null) {
            try {
                fcn.unwatch(new File(root, SITE_CONFIG));
            } catch (IOException e) {
                // do nothing
            }
            fcn.removeListener(this);
        }
    }

    public File getBaseDir() {
        return root;
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

    /**
     * @param defaultPage the defaultPage to set.
     */
    public void setDefaultPage(String defaultPage) {
        this.defaultPage = defaultPage;
    }

    /**
     * @return the defaultPage.
     */
    public String getDefaultPage() {
        return defaultPage;
    }

    /**
     * @param errorPage the errorPage to set.
     */
    public void setErrorPage(String errorPage) {
        this.errorPage = errorPage;
    }

    /**
     * @return the errorPage.
     */
    public String getErrorPage() {
        return errorPage;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public void setResolver(DocumentResolver resolver) {
        this.resolver = resolver;
    }

    public DocumentResolver getResolver() {
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

    public ScriptFile getScript(String path, String type) throws IOException {
        path = getWebFilePath(path);
        return new ScriptFile(manager.getRootDirectory(), path, type);
    }

    public File getFile(String path) {
        return new File(root, path);
    }

    public void loadConfiguration() throws IOException {
        config.loadConfiguration(this, new File(root, SITE_CONFIG));
    }

    public void reload() throws IOException {
        mapper.clearMappings();
        cache.clear();
        loadConfiguration();
    }



    public void fileChanged(File file, long since) {
        if (file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            if (file.getName().equals(SITE_CONFIG)) {
                try {
                    reload();
                    log.info("Reloaded site configuration for: "+root.getName());
                } catch (Throwable e) {
                    log.error("Failed to reload site configuration for: "+root.getName());
                }
            }
        }
    }

}
