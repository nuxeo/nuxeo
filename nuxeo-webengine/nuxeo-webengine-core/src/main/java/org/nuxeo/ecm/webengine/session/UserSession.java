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

package org.nuxeo.ecm.webengine.session;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * Used to store user session. This object is cached in a the HTTP session
 * Principal, subject and credentials are immutable per user session
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO: should be synchronized? concurrent access may happen for the same
// session
public final class UserSession extends HashMap<String, Object> {

    private static final long serialVersionUID = 260562970988817064L;

    protected static final Log log = LogFactory.getLog(UserSession.class);

    protected Map<Class<?>, ComponentMap<?>> comps = new HashMap<Class<?>, ComponentMap<?>>();

    protected HttpServletRequest request;

    protected UserSession(HttpServletRequest request) {
        this.request = request;
    }

    public static UserSession getCurrentSession(HttpServletRequest request) {
        String key = UserSession.class.getName();
        UserSession us = (UserSession)request.getAttribute(key);
        if (us == null) {
            us = new UserSession(request);
            request.setAttribute(key, us);
        }
        return us;
    }


    /**
     * Gets a core session.
     * <p>
     * If it does not already exist, it will be opened against the given
     * repository.
     *
     * @param repoName
     * @return the core session
     *
     * @deprecated use {@link SessionFactory#getSession(HttpServletRequest, String)}
     */
    public CoreSession getCoreSession(String repoName) {
        try {
            return SessionFactory.getSession(request, repoName);
        } catch (Exception e) {
            log.error(
                    "Failed to open core session for repository: " + repoName,
                    e);
            return null;
        }
    }

    /**
     * Gets a core session.
     * <p>
     * If not already opened, opens a new core session against the default
     * repository.
     *
     * @deprecated use {@link SessionFactory#getSession(HttpServletRequest)}
     */
    public CoreSession getCoreSession() {
        try {
            return SessionFactory.getSession(request);
        } catch (Exception e) {
            log.error(
                    "Failed to open core session for default repository",
                    e);
            return null;
        }
    }

    public Principal getPrincipal() {
        return request.getUserPrincipal();
    }


    /**
     * Register a cleanup handler that will be invoked when HTTP request
     * terminate. This method is not thread safe.
     */
    public static void addRequestCleanupHandler(HttpServletRequest request,
            RequestCleanupHandler handler) {
        RequestContext.getActiveContext(request).addRequestCleanupHandler(handler);
    }


    /**
     * Finds an existing component.
     * <p>
     * The component state will not be modified before being returned as in
     * {@link #getComponent(Class, String)}.
     * <p>
     * If the component was not found in that session, returns null.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T findComponent(Class<T> type,
            String name) {
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
     * If the component was not yet created in this session, it will be created
     * and registered against the session.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T getComponent(Class<T> type,
            String name) throws SessionException {
        ComponentMap<T> map = (ComponentMap<T>) comps.get(type);
        T comp;
        if (map == null) {
            map = new ComponentMap<T>();
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
            comp = type.newInstance();
        } catch (Exception e) {
            throw new SessionException("Failed to instantiate component: "
                    + type, e);
        }
        comp.initialize(this, name);
        if (name == null) {
            map.setComponent(comp);
        } else {
            map.put(name, comp);
        }
        return type.cast(comp);
    }

    public <T extends Component> T getComponent(Class<T> type)
            throws SessionException {
        return getComponent(type, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(String typeName, String name)
            throws SessionException {
        try {
            Class<T> type = (Class<T>) Class.forName(typeName);
            return getComponent(type, name);
        } catch (ClassNotFoundException e) {
            throw new SessionException("Could not find component class: "
                    + typeName, e);
        }
    }

    /**
     * Gets component by ID.
     * <p>
     * The ID is of the form <code>type#name</code> for non-null names and
     * <code>type</code> for null names.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(String id)
            throws SessionException {
        int p = id.lastIndexOf('#');
        if (p > -1) {
            return (T) getComponent(id.substring(0, p), id.substring(p + 1));
        } else {
            return (T) getComponent(id, null);
        }
    }



}
