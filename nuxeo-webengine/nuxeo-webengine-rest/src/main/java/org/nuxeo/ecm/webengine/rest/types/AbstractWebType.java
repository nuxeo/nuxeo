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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractWebType implements WebType {

    private final static Log log = LogFactory.getLog(WebType.class);

    protected Map<String, Action> actions;


    public AbstractWebType() {
        actions = new ConcurrentHashMap<String, Action>();
    }

    public boolean isDynamic() {
        return false;
    }

    protected Class<? extends WebObject> resolveObjectClass(WebTypeManager mgr) throws WebException {
        return resolveObjectClass(mgr, null);
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends WebObject> resolveObjectClass(WebTypeManager mgr, String className) throws WebException {
        Class<? extends WebObject> klass = null;
        if (className == null) {
            className = "org.nuxeo.ecm.webengine.rest.adapters."+getName()+"Object";
            try {
                klass = (Class<? extends WebObject>)mgr.engine.getScripting().loadClass(className);
            } catch (ClassNotFoundException e) {
                log.warn("Type not found: "+className);
            } catch (Exception e) {
                throw WebException.wrap("Failed to load class: "+className, e);
            }
        } else {
            try {
                klass = (Class<? extends WebObject>)mgr.engine.getScripting().loadClass(className);
            } catch (Exception e) {
                throw new TypeNotFoundException(getName());
            }
        }
        if (klass == null) {
            klass = getSuperType().getObjectClass();
        }
        return klass;
    }


    @SuppressWarnings("unchecked")
    public WebObject newInstance() throws WebException {
        try {
            Constructor ctor = getObjectClass().getConstructor(new Class[] {WebType.class});
            return (WebObject)ctor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
            throw WebException.wrap("Failed to instantiate type: "+getName()
                    +". Class name: "+getObjectClass(), e);
        }
    }

    public Action getAction(String action) {
        Action act = actions.get(action);
        if (act == null) {
            if (getSuperType() != null) {
                act = getSuperType().getAction(action);
            }
            if (action != null) {
                actions.put(action, act);
            }
        }
        return act;
    }

}
