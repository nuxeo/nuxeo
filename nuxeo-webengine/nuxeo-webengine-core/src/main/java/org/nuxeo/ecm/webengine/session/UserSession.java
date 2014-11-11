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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Used to store user session. This object is cached in a the HTTP session
 * Principal, subject and credentials are immutable per user session
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: should be synchronized? concurrent access may happen for the same session
public abstract class UserSession extends HashMap<String, Object>  {

    private static final long serialVersionUID = 260562970988817064L;

    protected static final String WE_SESSION_KEY="nuxeo.webengine.user_session";

    private static final Log log = LogFactory.getLog(UserSession.class);

    protected Map<Class<?>, ComponentMap<?>> comps = new HashMap<Class<?>, ComponentMap<?>>();

    protected final Subject subject;

    protected final Principal principal;

    protected final Object credentials;

    protected transient CoreSession coreSession;

    public static UserSession getCurrentSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return (UserSession) request.getAttribute(WE_SESSION_KEY);
        }
        return (UserSession) session.getAttribute(WE_SESSION_KEY);
    }

    public static void register(HttpServletRequest request, UserSession us) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            request.setAttribute(WE_SESSION_KEY, us);
        }
        else {
            session.setAttribute(WE_SESSION_KEY, us);
        }
    }

    protected UserSession(Principal principal) {
        this(principal, null);
    }

    protected UserSession(Principal principal, String password) {
        this(principal, password == null ? new char[0] : password.toCharArray());
    }

    protected UserSession(Principal principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
        Set<Principal> principals = new HashSet<Principal>();
        Set<Object> publicCredentials = new HashSet<Object>();
        Set<Object> privateCredentials = new HashSet<Object>();
        principals.add(principal);
        publicCredentials.add(credentials);
        subject = new Subject(true, principals, publicCredentials, privateCredentials);
    }

    public void setCoreSession(CoreSession coreSession) {
        this.coreSession = coreSession;
    }

    /**
     * Gets a core session.
     * <p>
     * If it does not already exist, it will be opened against the
     * given repository.
     *
     * @param repoName
     * @return the core session
     */
    public CoreSession getCoreSession(String repoName) {
        if (coreSession == null) {
            synchronized (this) {
                if (coreSession == null) {
                    try {
                        coreSession = openSession(repoName);
                    } catch (Exception e) {
                        e.printStackTrace(); //TODO
                    }
                }
            }
        }
        return coreSession;
    }

    /**
     * Gets a core session.
     * <p>
     * If not already opened, opens a new core session against the default
     * repository.
     *
     * @return
     */
    public CoreSession getCoreSession() {
        return getCoreSession(null);
    }

    public Principal getPrincipal() {
        return principal;
    }

    public Object getCredentials() {
        return credentials;
    }

    public Subject getSubject() {
        return subject;
    }

    public static CoreSession openSession(String repoName) throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = null;
        if (repoName== null) {
            repo = rm.getDefaultRepository();
        } else {
            repo = rm.getRepository(repoName);
        }
        if (repo == null) {
            throw new SessionException("Unable to get " + repoName
                    + " repository");
        }
        return repo.open();
    }


    protected void install() {
        if (log.isDebugEnabled()) {
            log.debug("Installing user session");
        }
    }

    protected synchronized void uninstall() {
        if (log.isDebugEnabled()) {
            log.debug("Uninstalling user session");
        }
        // destroy all components
        for (Map.Entry<Class<?>,ComponentMap<?>> entry : comps.entrySet()) {
            try {
                entry.getValue().destroy(this);
            } catch (SessionException e) {
                log.error("Failed to destroy component: "+entry.getKey(), e);
            }
        }
        comps = new HashMap<Class<?>, ComponentMap<?>>();
        // destroy core session
        if (coreSession != null) {
            coreSession.destroy();
            coreSession = null;
        }
    }

    /**
     * Finds an existing component.
     * <p>
     * The component state will not be modified before being returned
     * as in {@link #getComponent(Class, String)}.
     * <p>
     * If the component was not found in that session, returns null.
     *
     * @param <T>
     * @param type
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T findComponent(Class<T> type, String name) {
        ComponentMap<T> map = (ComponentMap<T>)comps.get(type);
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
     * If the component was not yet created in this session, it will
     * be created and registered against the session.
     *
     * @param <T>
     * @param type
     * @param name
     * @return
     * @throws SessionException
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T getComponent(Class<T> type, String name) throws SessionException {
        ComponentMap<T> map = (ComponentMap<T>) comps.get(type);
        T comp = null;
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
            throw new SessionException("Failed to instantiate component: "+type, e);
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
    public <T extends Component> T getComponent(String  typeName, String name) throws SessionException {
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
     * The ID is of the form <code>type#name</code> for non-null names
     * and <code>type</code> for null names.
     *
     * @param <T>
     * @param id
     * @return
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

    public abstract void terminateRequest(HttpServletRequest request);

}
