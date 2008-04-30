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

    @XNode("@type")
    protected String type;

    @XNode("@extends")
    protected String base;

    @XNode("requestHandler")
    protected Class<RequestHandler> requestHandlerClass;

    @XNodeMap(value="actions/action", key="@id", type=HashMap.class, componentType=ActionDescriptor.class)
    protected Map<String, ActionDescriptor> actions;

    protected RequestHandler requestHandler;


    public ObjectDescriptor() {}

    public ObjectDescriptor(String id, String type, String base, ActionDescriptor ... actions) {
        this.id = id;
        this.type = type;
        this.base = base;
        this.actions = new HashMap<String, ActionDescriptor>();
        if (actions != null) {
            for (ActionDescriptor action : actions) {
                this.actions.put(action.getId(), action);
            }
        }
    }

    public ObjectDescriptor(String id, String type, String base, Collection<ActionDescriptor> actions) {
        this.id = id;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public Class<RequestHandler> getRequestHandlerClass() {
        return requestHandlerClass;
    }

    public RequestHandler getRequestHandler() throws SiteException {
        if (requestHandler == null) {
            if (requestHandlerClass == null) {
                requestHandler = RequestHandler.DEFAULT;
            } else {
                try {
                    requestHandler = requestHandlerClass.newInstance();
                } catch (Exception e) {
                    throw new SiteException("Failed to instantiate request handler for object type: "+type, e);
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

    public Collection<ActionDescriptor> getEnabledActions(SiteObject obj) {
        CoreSession session = obj.getSession();
        DocumentModel doc = obj.getDocument();
        ArrayList<ActionDescriptor> ads = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.getGuard().check(session, doc)) {
                ads.add(ad);
            }
        }
        return ads;
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

}
