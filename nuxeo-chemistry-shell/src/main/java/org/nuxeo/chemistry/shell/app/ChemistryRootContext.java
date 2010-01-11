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
package org.nuxeo.chemistry.shell.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Repository;
import org.apache.chemistry.atompub.client.APPConnection;
import org.apache.chemistry.atompub.client.ContentManager;
import org.apache.chemistry.atompub.client.connector.APPContentManager;
import org.nuxeo.chemistry.shell.AbstractContext;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.console.ColorHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ChemistryRootContext extends AbstractContext {

    protected Map<String, Repository> repos;
    protected String[] keys;
    protected String[] ls;

    public ChemistryRootContext(ChemistryApp app) {
        super(app, Path.ROOT);
    }

    public APPContentManager getContentManager() {
        return ((ChemistryApp) app).getContentManager();
    }

    @Override
    public ChemistryApp getApplication() {
        return (ChemistryApp) app;
    }

    public <T> T as(Class<T> type) {
        return null;
    }

    public Context getContext(String name) {
        load();
        ContentManager cm = getContentManager();
        if (cm == null) {
            Console.getDefault().error("Not connected: cannot browse repository");
            return null;
        }
        Repository r = repos.get(name); // TODO  atompub client is using IDs to get repositories ...
        Repository repo = cm.getRepository(r.getId());
        if (repo != null) {
            APPConnection conn = (APPConnection)repo.getConnection(null);
            CMISObject entry = conn.getRootFolder();
            return new ChemistryContext((ChemistryApp)app, path.append(name), conn, entry);
        }
        return null;
    }

    public String[] ls() {
        if (load()) {
            return ls;
        }
        return new String[0];
    }

    public String[] entries() {
        if (load()) {
            return keys;
        }
        return new String[0];
    }

    protected boolean load() {
        if (keys == null) {
            ContentManager cm = getContentManager();
            if (cm == null) {
                Console.getDefault().error("Not connected: cannot browse repository");
                return false;
            }
            Repository[] repos = cm.getRepositories();
            this.repos = new HashMap<String, Repository>();
            keys = new String[repos.length];
            ls = new String[repos.length];
            for (int i=0; i<repos.length; i++) {
                keys[i] = repos[i].getName();
                this.repos.put(repos[i].getName(), repos[i]);
                ls[i] = ColorHelper.decorateNameByType(repos[i].getName(), "Repository");
            }
        }
        return true;
    }

    public void reset() {
        keys = null;
        ls = null;
        APPContentManager cm = getContentManager();
        if (cm != null) {
            cm.refresh();
        }
    }

    public String id() {
        return "CMIS server: "+app.getServerUrl();
    }

}
