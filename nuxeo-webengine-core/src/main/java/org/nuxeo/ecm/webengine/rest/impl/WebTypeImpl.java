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

package org.nuxeo.ecm.webengine.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.WebAction;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebTypeImpl implements WebType {
        
    protected String name;
    protected WebTypeImpl superType;
    protected ConcurrentMap<String, ActionDescriptor> actions;
    protected Class<WebObject> clazz;

    public WebTypeImpl(WebTypeImpl superType, String name, Class<WebObject> clazz, Map<String, ActionDescriptor> actions) {
        this.superType = superType;
        this.name = name;
        this.clazz = clazz;
        this.actions = new ConcurrentHashMap<String, ActionDescriptor>();
        if (actions != null) { // add own defined actions
            this.actions.putAll(actions);
        }
    }
    
    public WebType getSuperType() {
        return superType;
    }
    
    public Class<WebObject> getObjectType() {
        return this.clazz; 
    }
    
    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }
    
    public WebObject newInstance() throws WebException {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate web object: "+clazz, e);
        }
    }
    
    public ActionDescriptor getAction(String name) {
        return actions.get(name);
    }
    
    public WebAction getActionInstance(WebContext2 ctx, String name) throws WebException {
        ActionDescriptor action = getAction(name);
        if (action != null) {
            try {
                if (!action.getGuard().check(ctx)) {
                    throw new WebSecurityException("Failed to get action: "+action.name+". Action is not accessible in the current context", action.name);
                }
                return (WebAction)action.klass.newInstance();
            } catch (Exception e) {
                throw WebException.wrap(e);
            }
        }
        return null;
    }
    
    public ActionDescriptor addAction(ActionDescriptor action) {
        return actions.put(action.name, action);
    }

    public void removeAction(String name) {
        actions.remove(name);
    }
    
    public ActionDescriptor[] getActions() {
        ArrayList<ActionDescriptor> result = new ArrayList<ActionDescriptor>();
        collectActions(result);
        return result.toArray(new ActionDescriptor[result.size()]);
    }
    
    public ActionDescriptor[] getActions(String category) {
        ArrayList<ActionDescriptor> result = new ArrayList<ActionDescriptor>();
        collectActions(result, category);
        return result.toArray(new ActionDescriptor[result.size()]);
    }    

    public ActionDescriptor[] getEnabledActions(WebContext2 ctx) {
        ArrayList<ActionDescriptor> result = new ArrayList<ActionDescriptor>();
        collectEnabledActions(result, ctx);
        return result.toArray(new ActionDescriptor[result.size()]);
    }

    public ActionDescriptor[] getEnabledActions(String category, WebContext2 ctx) {
        ArrayList<ActionDescriptor> result = new ArrayList<ActionDescriptor>();
        collectEnabledActions(result, category, ctx);
        return result.toArray(new ActionDescriptor[result.size()]);
    }
    

    public ActionDescriptor[] getLocalActions() {
        return actions.values().toArray(new ActionDescriptor[actions.size()]);
    }
        
    protected void collectActions(List<ActionDescriptor> result) {
        if (superType != null) {
            superType.collectActions(result);
        }
        ActionDescriptor[] actions = getLocalActions();
        for (ActionDescriptor action : actions) {
            result.add(action);
        }    
    }

    protected void collectEnabledActions(List<ActionDescriptor> result, WebContext2 ctx) {
        if (superType != null) {
            superType.collectEnabledActions(result, ctx);
        }
        ActionDescriptor[] actions = getLocalActions();
        for (ActionDescriptor action : actions) {
            if (action.isEnabled(ctx)) {
                result.add(action);
            }
        }    
    }

    protected void collectActions(List<ActionDescriptor> result, String category) {
        if (superType != null) {
            superType.collectActions(result, category);
        }
        ActionDescriptor[] actions = getLocalActions();
        for (ActionDescriptor action : actions) {
            if (action.categories.contains(category)) {
                result.add(action);
            }
        }        
    }

    protected void collectEnabledActions(List<ActionDescriptor> result, String category, WebContext2 ctx) {
        if (superType != null) {
            superType.collectEnabledActions(result, category, ctx);
        }
        ActionDescriptor[] actions = getLocalActions();
        for (ActionDescriptor action : actions) {
            if (action.categories.contains(category) && action.isEnabled(ctx)) {
                result.add(action);
            }
        }        
    }

}
