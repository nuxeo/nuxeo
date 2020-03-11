/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.session;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.RequestCleanupHandler;
import org.nuxeo.ecm.platform.web.common.RequestContext;

/**
 * Used to store user session. This object is cached in a the HTTP session Principal, subject and credentials are
 * immutable per user session
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: should be synchronized? concurrent access may happen for the same
// session
public final class UserSession extends HashMap<String, Object> {

    private static final long serialVersionUID = 260562970988817064L;

    protected static final Log log = LogFactory.getLog(UserSession.class);

    protected Map<Class<?>, ComponentMap<?>> comps = new HashMap<>();

    protected HttpServletRequest request;

    protected UserSession(HttpServletRequest request) {
        this.request = request;
    }

    public static UserSession getCurrentSession(HttpServletRequest request) {
        String key = UserSession.class.getName();
        HttpSession session = request.getSession(false);
        UserSession us = null;
        if (session != null) {
            us = (UserSession) session.getAttribute(key);
        }
        if (us == null) {
            us = (UserSession) request.getAttribute(key);
        }
        if (us == null) {
            us = new UserSession(request);
            if (session != null) {
                session.setAttribute(key, us);
            } else {
                request.setAttribute(key, us);
            }
        }
        return us;
    }

    public Principal getPrincipal() {
        return request.getUserPrincipal();
    }

    /**
     * Register a cleanup handler that will be invoked when HTTP request terminate. This method is not thread safe.
     */
    public static void addRequestCleanupHandler(HttpServletRequest request, RequestCleanupHandler handler) {
        RequestContext.getActiveContext(request).addRequestCleanupHandler(handler);
    }

    /**
     * Finds an existing component.
     * <p>
     * The component state will not be modified before being returned as in {@link #getComponent(Class, String)}.
     * <p>
     * If the component was not found in that session, returns null.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T findComponent(Class<T> type, String name) {
        ComponentMap<T> map = (ComponentMap<T>) comps.get(type);
        if (map == null) {
            return null;
        }
        if (name == null) {
            return map.getComponent();
        } else {
            return type.cast(map.get(name));
        }
    }

    /**
     * Gets a component given its class and an optional name.
     * <p>
     * If the component was not yet created in this session, it will be created and registered against the session.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T getComponent(Class<T> type, String name) throws SessionException {
        ComponentMap<T> map = (ComponentMap<T>) comps.get(type);
        T comp;
        if (map == null) {
            map = new ComponentMap<>();
            comps.put(type, map);
        } else {
            if (name == null) {
                comp = map.getComponent();
            } else {
                comp = type.cast(map.get(name));
            }
            if (comp != null) {
                return comp;
            }
        }
        // component not found
        try {
            comp = type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SessionException("Failed to instantiate component: " + type, e);
        }
        comp.initialize(this, name);
        if (name == null) {
            map.setComponent(comp);
        } else {
            map.put(name, comp);
        }
        return type.cast(comp);
    }

    public <T extends Component> T getComponent(Class<T> type) throws SessionException {
        return getComponent(type, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(String typeName, String name) throws SessionException {
        try {
            Class<T> type = (Class<T>) Class.forName(typeName);
            return getComponent(type, name);
        } catch (ClassNotFoundException e) {
            throw new SessionException("Could not find component class: " + typeName, e);
        }
    }

    /**
     * Gets component by ID.
     * <p>
     * The ID is of the form <code>type#name</code> for non-null names and <code>type</code> for null names.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(String id) throws SessionException {
        int p = id.lastIndexOf('#');
        if (p > -1) {
            return (T) getComponent(id.substring(0, p), id.substring(p + 1));
        } else {
            return (T) getComponent(id, null);
        }
    }

}
