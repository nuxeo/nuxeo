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
 */
package org.nuxeo.ecm.webengine.debug;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleImpl;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadManager {

    private static final Log log = LogFactory.getLog(ReloadManager.class);

    protected final WebEngine engine;

    protected final FileEntry deploy; // track deploy/undeploy modules

    private final Timer timer = new Timer("ReloadManager");

    public ReloadManager(WebEngine engine) {
        this.engine = engine;
        deploy = new FileEntry(engine.getDeploymentDirectory());
    }

    public void start() {
        String interval = Framework.getProperty(
                "org.nuxeo.ecm.webengine.reloadInterval", "2000");
        start(4000, Integer.parseInt(interval));
    }

    public void start(int startAfter, int interval) {
        timer.scheduleAtFixedRate(new Task(), startAfter, interval);
    }

    public void stop() {
        timer.cancel();
        timer.purge();
    }

    private class Task extends TimerTask {
        @Override
        public void run() {
            ModuleConfiguration[] modules = engine.getModuleManager().getModules();
            for (ModuleConfiguration mc : modules) {
                if (mc.isLoaded()) {
                    ModuleImpl module = (ModuleImpl) mc.get();
                    module.getTracker().run();
                }
            }
            // handle hot deployment
            if (deploy.check()) { // deployment directory changed
                ModuleManager mgr = engine.getModuleManager();
                ModuleConfiguration[] ar = mgr.getModules();
                Set<File> set = new HashSet<File>();
                for (ModuleConfiguration mc : ar) {
                    set.add(mc.file);
                }
                File root = engine.getDeploymentDirectory();
                if (root.isDirectory()) {
                    for (File file : root.listFiles()) {
                        if (file.isDirectory()) {
                            File f = new File(file, "module.xml");
                            if (f.isFile()) {
                                if (!set.remove(f)) { // a new module
                                    try {
                                        log.info("auto-deploying module: " + f);
                                        engine.getWebLoader().addClassPathElement(
                                                file);
                                        mgr.loadModule(f);
                                    } catch (Exception e) {
                                        log.error(
                                                "Failed to load module: " + f,
                                                e);
                                    }
                                }
                            }
                        }
                    }
                }
                String rootPath = root.getAbsolutePath();
                for (File f : set) { // these are modules to undeploy
                    if (f.getAbsolutePath().startsWith(rootPath)) {
                        ModuleConfiguration mc = mgr.getModuleByConfigFile(f);
                        if (mc != null) {
                            log.info("auto-undeploying module: " + mc.name);
                            mgr.unregisterModule(mc.name);
                            engine.getWebLoader().flushCache();
                        }
                    }
                }
            }
        }
    }

}
