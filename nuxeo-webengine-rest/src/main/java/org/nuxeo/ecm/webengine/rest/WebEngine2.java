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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.rest.domains.WebDomain;
import org.nuxeo.ecm.webengine.rest.scripting.Scripting;
import org.nuxeo.ecm.webengine.rest.types.WebTypeManager;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngine2 implements FileChangeListener {

    protected File root;
    protected Map<String, WebDomain<?>> domains = new HashMap<String, WebDomain<?>>();
    protected Map<String, WebDomain<?>> mappings = new HashMap<String, WebDomain<?>>();
    protected FileChangeNotifier notifier;
    protected volatile long lastMessagesUpdate = 0;
    protected Scripting scripting;
    protected WebTypeManager typeMgr;


    public WebEngine2(File root, FileChangeNotifier notifier) {
        this.root = root;
        this.notifier = notifier;
        this.scripting = new Scripting();
        this.typeMgr = new WebTypeManager();
        if (notifier != null) {
            notifier.addListener(this);
        }
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

    public void registerDomain(WebDomain<?> domain) {
        //domains.put(domain.getId(), value);
    }

    public WebDomain<?>[] getDomains() {
        return domains.values().toArray(new WebDomain[domains.size()]);
    }

    public WebDomain<?> getDomain(String id) {
        return domains.get(id);
    }

    public WebDomain<?> getDomainByPath(String path) {
        return mappings.get(path);
    }

    public File getRootDirectory() {
        return root;
    }

    public FileChangeNotifier getFileChangeNotifier() {
        return notifier;
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
        //TODO loadMessageBundle(false);
    }

}
