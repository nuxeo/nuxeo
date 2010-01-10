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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.Repository;
import org.apache.chemistry.atompub.client.APPConnection;
import org.apache.chemistry.atompub.client.connector.APPContentManager;
import org.nuxeo.chemistry.shell.AbstractContext;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.console.ColorHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ChemistryContext extends AbstractContext {

    //public static final String CONN_KEY = "chemistry.connection";
    
    protected final APPContentManager cm;
    protected final APPConnection conn;
    protected final CMISObject entry;

    protected String[] keys;
    protected String[] ls;
    protected Map<String,CMISObject> children;
    
    public ChemistryContext(ChemistryApp app, Path path, APPConnection conn, CMISObject entry) {
        super(app, path);
        this.cm = app.getContentManager();
        this.conn = conn;
        this.entry = entry;
    }
    
    public ChemistryApp getApplication() {
        return (ChemistryApp)app;
    }

    public APPConnection getConnection() {
        return conn;
    }
    
    public CMISObject getEntry() {
        return entry;
    }

    public APPContentManager getContentManager() {
        return cm;
    }
    
    public Repository getRepository() {
        return conn.getRepository();
    }
    
    public Context getContext(String name) {
        load();
        CMISObject e = children.get(name);
        if (e != null) {
            return new ChemistryContext((ChemistryApp)app, path.append(name), conn, e);
        }
        return null;
    }

    public String[] ls() {
        load();
        return ls;
    }

    public String[] entries() {
        load();
        return keys;
    }
    
    public void reset() {
        children = null;
        keys = null;
        ls = null;
    }

    public boolean isFolder() {
        return entry instanceof Folder;
    }
    
    protected void load() {
        if (children == null) {
            if (!isFolder()) {
                return;
            }
            Folder folder = (Folder)entry; 
            List<CMISObject> feed =  folder.getChildren();
            children = new LinkedHashMap<String, CMISObject>();
            keys = new String[feed.size()];
            ls = new String[keys.length];
            int i = 0;
            for (CMISObject entry : feed) {
                children.put(entry.getName(), entry);
                keys[i] = entry.getName();
                ls[i++] = ColorHelper.decorateNameByType(entry.getName(), entry.getTypeId());
            }
        }
    }
    
    public <T> T as(Class<T> type) {
        if (type.isAssignableFrom(entry.getClass())) {
            return type.cast(entry);
        }
        return null;
    }

    public CMISObject getObjectByAbsolutePath(String path) {
        ObjectEntry entry = conn.getObjectByPath(path, null);
        if (entry!=null) {
            return conn.getObject(entry);
        } else {
            return null;
        }
    }

    public CMISObject resolveObject(String path) {
        Path p = resolvePath(path);
        ObjectEntry entry = conn.getObjectByPath(p.toString(), null);
        if (entry!=null) {
            return conn.getObject(entry);
        } else {
            return null;
        }
    }

    public String id() {
        return "Object "+entry.getId()+" of type "+entry.getTypeId();
    }
    
}
