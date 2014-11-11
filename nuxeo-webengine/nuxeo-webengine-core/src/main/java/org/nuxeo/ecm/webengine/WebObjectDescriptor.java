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
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.runtime.deploy.Contribution;
import org.nuxeo.runtime.deploy.ExtensibleContribution;
import org.nuxeo.runtime.deploy.ManagedComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("object")
public class WebObjectDescriptor extends ExtensibleContribution {

    @XNode("@id") // internal id needed to uniquely identify the object
    @Override
    public void setContributionId(String id) { contributionId = id; }

    @XNode("@extends")
    @Override
    public void setBaseContributionId(String id) { baseContributionId = id; }

    @XNode("requestHandler")
    protected Class<RequestHandler> requestHandlerClass;

    @XNodeMap(value="actions/action", key="@id", type=HashMap.class, componentType=ActionDescriptor.class)
    protected Map<String, ActionDescriptor> actions;

    protected RequestHandler requestHandler;


    public WebObjectDescriptor() {}

    public WebObjectDescriptor(String id, String base, ActionDescriptor ... actions) {
        this.contributionId = id;
        this.baseContributionId = base;
        this.actions = new HashMap<String, ActionDescriptor>();
        if (actions != null) {
            for (ActionDescriptor action : actions) {
                this.actions.put(action.getId(), action);
            }
        }
    }

    public WebObjectDescriptor(String id, String base, Collection<ActionDescriptor> actions) {
        this.contributionId = id;
        this.baseContributionId = base;
        this.actions = new HashMap<String, ActionDescriptor>();
        if (actions != null) {
            for (ActionDescriptor action : actions) {
                this.actions.put(action.getId(), action);
            }
        }
    }

    public String getId() {
        return contributionId;
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
                    throw new WebException("Failed to instantiate request handler for object id: "+getId(), e);
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

    public Collection<ActionDescriptor> getEnabledActions(WebObject obj) {
        List<ActionDescriptor> ads = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.isEnabled() && ad.getGuard().check(obj)) {
                ads.add(ad);
            }
        }
        return ads;
    }

    public Collection<ActionDescriptor> getEnabledActions(WebObject obj, String category) {
        List<ActionDescriptor> ads = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.isEnabled() && ad.hasCategory(category) && ad.getGuard().check(obj)) {
                ads.add(ad);
            }
        }
        return ads;
    }

    public Map<String, Collection<ActionDescriptor>> getEnabledActionsByCategory(WebObject obj) {
        Map<String, Collection<ActionDescriptor>> result = new HashMap<String, Collection<ActionDescriptor>>();
        for (ActionDescriptor ad : actions.values()) {
            if (ad.isEnabled() && ad.getGuard().check(obj)) {
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

    @Override
    protected void copyOver(ExtensibleContribution contrib) {
        WebObjectDescriptor objDesc = (WebObjectDescriptor)contrib;
        if (requestHandlerClass != null) {
            objDesc.requestHandlerClass = requestHandlerClass;
        }
        if (actions == null) {
            return;
        }
        if (objDesc.actions == null) {
            objDesc.actions = new HashMap<String, ActionDescriptor>();
        }
        // merge actions
        for (ActionDescriptor desc : actions.values()) {
            ActionDescriptor action = objDesc.actions.get(desc.getId());
            if (action == null) { // import base action
                ActionDescriptor clone = new ActionDescriptor();
                clone.copyFrom(desc); // be sure we deep copy the object
                objDesc.actions.put(desc.getId(), clone);
            } else { // merge the 2 actions
                action.copyFrom(desc);
            }
        }
    }


    @Override
    public void install(ManagedComponent comp, Contribution contrib) throws Exception {
        WebEngine engine = ((WebEngineComponent)comp).getEngine();
        engine.registerObject((WebObjectDescriptor)contrib);
    }

    @Override
    public void uninstall(ManagedComponent comp, Contribution contrib) throws Exception {
        WebEngine engine = ((WebEngineComponent)comp).getEngine();
        engine.unregisterObject((WebObjectDescriptor)contrib);
    }


}
