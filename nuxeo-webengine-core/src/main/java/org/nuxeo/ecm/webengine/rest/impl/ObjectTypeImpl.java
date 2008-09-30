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
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.ObjectResource;
import org.nuxeo.ecm.webengine.rest.model.ObjectType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ObjectTypeImpl implements ObjectType {
        
    protected String name;
    protected ObjectTypeImpl superType;
    protected ConcurrentMap<String, ActionTypeImpl> actions;
    protected Class<ObjectResource> clazz;

    public ObjectTypeImpl(ObjectTypeImpl superType, String name, Class<ObjectResource> clazz, Map<String, ActionTypeImpl> actions) {
        this.superType = superType;
        this.name = name;
        this.clazz = clazz;
        this.actions = new ConcurrentHashMap<String, ActionTypeImpl>();
        if (actions != null) { // add own defined actions
            this.actions.putAll(actions);
        }
    }
    
    public ObjectType getSuperType() {
        return superType;
    }
    
    public Class<ObjectResource> getObjectType() {
        return this.clazz; 
    }
    
    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }
    
    public Class<ObjectResource> getResourceClass() {
        return clazz;
    }
    
    public ObjectResource newInstance() throws WebException {
        try {            
            return clazz.newInstance();
        } catch (Exception e) {
            throw WebException.wrap("Failed to instantiate web object: "+clazz, e);
        }
    }
    
    public ActionTypeImpl getAction(String name) {
        return actions.get(name);
    }    
    
    public ActionTypeImpl addAction(ActionTypeImpl action) {
        return actions.put(action.name, action);
    }

    public void removeAction(String name) {
        actions.remove(name);
    }
    
    public ActionTypeImpl[] getActions() {
        ArrayList<ActionTypeImpl> result = new ArrayList<ActionTypeImpl>();
        collectActions(result);
        return result.toArray(new ActionTypeImpl[result.size()]);
    }
    
    public ActionTypeImpl[] getActions(String category) {
        ArrayList<ActionTypeImpl> result = new ArrayList<ActionTypeImpl>();
        collectActions(result, category);
        return result.toArray(new ActionTypeImpl[result.size()]);
    }    

    public ActionTypeImpl[] getEnabledActions(WebContext2 ctx) {
        ArrayList<ActionTypeImpl> result = new ArrayList<ActionTypeImpl>();
        collectEnabledActions(result, ctx);
        return result.toArray(new ActionTypeImpl[result.size()]);
    }

    public ActionTypeImpl[] getEnabledActions(String category, WebContext2 ctx) {
        ArrayList<ActionTypeImpl> result = new ArrayList<ActionTypeImpl>();
        collectEnabledActions(result, category, ctx);
        return result.toArray(new ActionTypeImpl[result.size()]);
    }
    

    public ActionTypeImpl[] getLocalActions() {
        return actions.values().toArray(new ActionTypeImpl[actions.size()]);
    }
        
    protected void collectActions(List<ActionTypeImpl> result) {
        if (superType != null) {
            superType.collectActions(result);
        }
        ActionTypeImpl[] actions = getLocalActions();
        for (ActionTypeImpl action : actions) {
            result.add(action);
        }    
    }

    protected void collectEnabledActions(List<ActionTypeImpl> result, WebContext2 ctx) {
        if (superType != null) {
            superType.collectEnabledActions(result, ctx);
        }
        ActionTypeImpl[] actions = getLocalActions();
        for (ActionTypeImpl action : actions) {
            if (action.isEnabled(ctx)) {
                result.add(action);
            }
        }    
    }

    protected void collectActions(List<ActionTypeImpl> result, String category) {
        if (superType != null) {
            superType.collectActions(result, category);
        }
        ActionTypeImpl[] actions = getLocalActions();
        for (ActionTypeImpl action : actions) {
            if (action.categories.contains(category)) {
                result.add(action);
            }
        }        
    }

    protected void collectEnabledActions(List<ActionTypeImpl> result, String category, WebContext2 ctx) {
        if (superType != null) {
            superType.collectEnabledActions(result, category, ctx);
        }
        ActionTypeImpl[] actions = getLocalActions();
        for (ActionTypeImpl action : actions) {
            if (action.categories.contains(category) && action.isEnabled(ctx)) {
                result.add(action);
            }
        }        
    }

}
