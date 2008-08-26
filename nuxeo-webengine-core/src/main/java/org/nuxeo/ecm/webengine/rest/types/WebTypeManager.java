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

package org.nuxeo.ecm.webengine.rest.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.contribution.impl.AbstractRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebTypeManager extends AbstractRegistry<String, WebTypeDescriptor> {

    protected final Map<String, WebType> registry;
    protected WebEngine2 engine;

    public WebTypeManager(WebEngine2 engine) {
        this.engine = engine;
        registry = new HashMap<String, WebType>();
        //TODO temp code until config will be back again
        registry.put("Script", new ScriptType());
    }


    //TODO trigger a reload from its backing registry
    public void reload() {

    }

    /**
     * @return the engine.
     */
    public WebEngine2 getEngine() {
        return engine;
    }

    //TODO hot reload is not yet working: when registering existing types we must remove the register
    // to force a refresh
    public void registerType(WebType type) throws WebException {
        synchronized (registry) {
            registry.put(type.getName(), type);
        }
    }

    public void unregisterType(String type) {
        synchronized (registry) {
            registry.remove(type);
        }
    }

    public void clear() {
        synchronized (registry) {
            super.clear();
            registry.clear();
        }
    }

    public WebType getType(String name) throws TypeNotFoundException {
        WebType type = registry.get(name);
        if (type == null) { // introspect document types and generate missing types
            synchronized (registry) {
                type = registry.get(name);
                if (type ==null) {
                    SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
                    DocumentType docType = mgr.getDocumentType(name);
                    if (docType == null) { // create a dynamic type
                        type = new DynamicType(this, name);
                    } else { // a dynamic document type
                        type = new WebDocumentType(this, docType);
                    }
                    registry.put(type.getName(), type);
                }
            }
        }
        return type;
    }

    public WebObject newInstance(String name) throws WebException {
        return getType(name).newInstance();
    }



    @Override
    protected void installContribution(String key, WebTypeDescriptor object) {
        synchronized (registry) {
            try {
            DefaultWebType type = new DefaultWebType(this, object);
            if (registry.get(key) != null) {
                // remove all dynamic types
                Iterator<WebType> it = registry.values().iterator();
                while (it.hasNext()) {
                    WebType entry = it.next();
                    if (entry.isDynamic()) {
                        it.remove();
                    }
                }
            }
            registry.put(key, type);
            } catch (Exception e) {
                throw new Error("Resgitration error: "+key);
            }
        }

    }

    @Override
    protected void uninstallContribution(String key) {
        registry.remove(key);
        synchronized (registry) {
            // remove all dynamic types
            Iterator<WebType> it = registry.values().iterator();
            while (it.hasNext()) {
                WebType entry = it.next();
                if (entry.isDynamic()) {
                    it.remove();
                }
            }
        }
    }

    @Override
    protected void reinstallContribution(String key, WebTypeDescriptor object) {
        try {
        WebType type = new DefaultWebType(this, object);
        synchronized (registry) {
            registry.remove(key);
            registry.put(key, type);
        }
        } catch (Exception e) {
            throw new Error("registration error: "+key, e);
        }
    }

    @Override
    protected void applyFragment(WebTypeDescriptor object,
            WebTypeDescriptor fragment) {
        if (fragment.className != null) {
            object.className = null;
        }
        if (fragment.actions != null) {
            if (object.actions == null) {
                object.actions = new HashMap<String, ActionDescriptor>();
            }
            object.actions.putAll(fragment.actions);
        }
    }
}
