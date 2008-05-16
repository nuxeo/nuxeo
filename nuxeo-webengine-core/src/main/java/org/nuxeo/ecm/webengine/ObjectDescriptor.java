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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("object")
public class ObjectDescriptor {

    @XNode("@id") // internal id needed to uniquely identify the object
    protected String id;

    @XNode("@extends")
    protected String base;

    @XNode("requestHandler")
    protected Class<RequestHandler> requestHandlerClass;

    @XNodeMap(value="actions/action", key="@id", type=HashMap.class, componentType=ActionDescriptor.class)
    protected Map<String, ActionDescriptor> actions;

    protected RequestHandler requestHandler;


    public ObjectDescriptor() {}

    public ObjectDescriptor(String id, String base, ActionDescriptor ... actions) {
        this.id = id;
        this.base = base;
        this.actions = new HashMap<String, ActionDescriptor>();
        if (actions != null) {
            for (ActionDescriptor action : actions) {
                this.actions.put(action.getId(), action);
            }
        }
    }

    public ObjectDescriptor(String id, String base, Collection<ActionDescriptor> actions) {
        this.id = id;
        this.base = base;
        this.actions = new HashMap<String, ActionDescriptor>();
        if (actions != null) {
            for (ActionDescriptor action : actions) {
                this.actions.put(action.getId(), action);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getBase() {
        return base;
    }


    public Class<RequestHandler> getRequestHandlerClass() {
        return requestHandlerClass;
    }

    public RequestHandler getRequestHandler() throws WebException {
        if (requestHandler == null) {
            if (requestHandlerClass == null) {
                requestHandler = RequestHandler.DEFAULT;
            } else {
                try {
                    requestHandler = requestHandlerClass.newInstance();
                } catch (Exception e) {
                    throw new WebException("Failed to instantiate request handler for object id: "+id, e);
                }
            }
        }
        return requestHandler;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public Map<String, ActionDescriptor> getActions() {
        return actions;
    }

    public Collection<ActionDescriptor> getActions(String category) {
        List<ActionDescriptor> ads = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.hasCategory(category)) {
                ads.add(ad);
            }
        }
        return ads;
    }

    public Map<String, Collection<ActionDescriptor>> getActionsByCategory() {
        Map<String, Collection<ActionDescriptor>> result = new HashMap<String, Collection<ActionDescriptor>>();
        for (ActionDescriptor ad : actions.values()) {
            String[] cats = ad.getCategories();
            for (String cat : cats) {
                Collection<ActionDescriptor> list = result.get(cat);
                if (list == null) {
                    list = new ArrayList<ActionDescriptor>();
                    result.put(cat, list);
                }
                list.add(ad);
            }
        }
        return result;
    }

    public Collection<ActionDescriptor> getEnabledActions(WebObject obj) throws WebException {
        CoreSession session = obj.getWebContext().getCoreSession();
        DocumentModel doc = obj.getDocument();
        List<ActionDescriptor> ads = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.isEnabled() && ad.getGuard().check(session, doc)) {
                ads.add(ad);
            }
        }
        return ads;
    }

    public Collection<ActionDescriptor> getEnabledActions(WebObject obj, String category) throws WebException {
        CoreSession session = obj.getWebContext().getCoreSession();
        DocumentModel doc = obj.getDocument();
        List<ActionDescriptor> ads = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.isEnabled() && ad.hasCategory(category) && ad.getGuard().check(session, doc)) {
                ads.add(ad);
            }
        }
        return ads;
    }

    public Map<String, Collection<ActionDescriptor>> getEnabledActionsByCategory(WebObject obj) throws WebException {
        CoreSession session = obj.getWebContext().getCoreSession();
        DocumentModel doc = obj.getDocument();
        Map<String, Collection<ActionDescriptor>> result = new HashMap<String, Collection<ActionDescriptor>>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.isEnabled() && ad.getGuard().check(session, doc)) {
                String[] cats = ad.getCategories();
                for (String cat : cats) {
                    Collection<ActionDescriptor> list = result.get(cat);
                    if (list == null) {
                        list = new ArrayList<ActionDescriptor>();
                        result.put(cat, list);
                    }
                    list.add(ad);
                }
            }
        }
        return result;
    }

    public ActionDescriptor getAction(String name) {
        return actions.get(name);
    }

    public void merge(ObjectDescriptor baseObj) {
        if (requestHandlerClass == null) {
            requestHandlerClass = baseObj.requestHandlerClass;
        }
        if (baseObj.actions == null) {
            return;
        }
        // merge actions
        for (ActionDescriptor desc : baseObj.actions.values()) {
            ActionDescriptor action = actions.get(desc.getId());
            if (action == null) { // import base action
                actions.put(desc.getId(), desc);
            } else { // merge the 2 actions
                action.merge(desc);
            }
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
