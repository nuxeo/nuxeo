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

import java.lang.reflect.Method;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Action {

    public static final Action NULL = new Action(null) {
        public Object invoke(WebObject obj) { return null; };
    };

    protected ActionDescriptor desc;

    public Action(ActionDescriptor desc) {
        this.desc = desc;
    }

    public ActionDescriptor getDescriptor() {
        return desc;
    }

    public void checkPermission(WebObject object) throws WebSecurityException {
        if (!isEnabled(object)) {
            throw new WebSecurityException(desc.getId());
        }
    }

    public boolean isEnabled(WebObject object) {
        return desc.getGuard().check(object);
    }

    public abstract Object invoke(WebObject obj) throws WebException;

    /**
     * Resolve the action given its descriptor
     * @param obj
     * @return the action if resolved, null otherwise
     */
    public static Action resolve(WebObject obj, ActionDescriptor desc) {
        String id = desc.getId();
        try {
            Method method = obj.getClass().getMethod(id);
            return new MethodAction(desc, method);
        } catch (Exception e) {
            ScriptFile script = obj.getActionScript(id);
            if (script != null) {
                return new ScriptAction(desc, script);
            }
        }
        return null;
    }

}
