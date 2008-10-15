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

package org.nuxeo.ecm.webengine.actions;

import java.text.ParseException;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.WebObject;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("action")
public class ActionDescriptor {

    public static final String[] EMPTY_CATEGORIES = new String[0];

    @XNode("@id")
    protected String id;

    @XNode("@script")
    protected String script;

    @XNode("@handler")
    protected Class<ActionHandler> handlerClass;

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("permission")
    private GuardDescriptor pd;

    @XNodeList(value = "category", type = String[].class, componentType = String.class)
    protected String[] categories = EMPTY_CATEGORIES;

    protected Guard guard;

    protected ActionHandler handler;


    public ActionDescriptor() {
    }

    public ActionDescriptor(String id, String path, Class<ActionHandler> handler, Guard guard) {
        this.id = id;
        script = path;
        handlerClass = handler;
        this.guard = guard;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isEnabled(WebObject obj) {
        if (!isEnabled) {
            return false;
        }
        return getGuard().check(obj.getWebContext());
    }

    public String getId() {
        return id;
    }

    public Class<ActionHandler> getHandlerClass() {
        return handlerClass;
    }

    public void setHandlerClass(Class<ActionHandler> handlerClass) {
        this.handlerClass = handlerClass;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Guard getGuard() {
        if (guard == null) {
            buildGuard();
        }
        return guard;
    }

    public void setGuard(Guard guard) {
        this.guard = guard;
    }

    public void setCategories(String ... categories) {
        this.categories = categories;
    }

    public String[] getCategories() {
        return categories;
    }

    public boolean hasCategory(String name) {
        for (int i = categories.length-1; i>=0; i--) {
            if (name.equals(categories[i])) {
                return true;
            }
        }
        return false;
    }

    public ActionHandler getHandler() throws WebException {
        if (handler == null) {
            if (handlerClass == null) {
                handler = ActionHandler.NULL;
            } else {
                try {
                    handler = handlerClass.newInstance();
                } catch (Exception e) {
                    throw new WebException("Failed to instantiate action handler for action: "+id, e);
                }
            }
        }
        return handler;
    }

    /**
     * This method should be used to run actions.
     * Avoid using getHandler().run() since it is not checking permissions
     */
    public void run(WebObject obj) throws WebException {
        // check rights first
        if (!getGuard().check(obj.getWebContext())) {
            throw new WebSecurityException(id);
        }
        getHandler().run(obj);
    }

    public void setHandler(ActionHandler handler) {
        this.handler = handler;
    }

    public void copyFrom(ActionDescriptor ad) {
        id = ad.id;
        isEnabled = ad.isEnabled;
        if (ad.categories != null && ad.categories.length > 0) {
            if (categories == null) {
                categories = ad.categories;
            } else {
                String[] tmp = new String[categories.length+ad.categories.length];
                System.arraycopy(categories, 0, tmp, 0, categories.length);
                System.arraycopy(ad.categories, 0, tmp, categories.length, ad.categories.length);
                categories = tmp;
            }
        }
        if (ad.script != null) {
            script = ad.script;
        }
        if (ad.handlerClass != null) {
            handlerClass = ad.handlerClass;
        }
        if (ad.getGuard() != null) {
            guard = ad.getGuard();
        } else {
            pd.getGuards().put(".", ad.getGuard());
            guard = null;
        }
    }

    private void buildGuard() {
        if (pd == null) {
            guard = Guard.DEFAULT;
        } else {
            try {
                guard = pd.getGuard(); // compute guards now
            } catch (ParseException e) {
                e.printStackTrace(); // TODO
            }
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
