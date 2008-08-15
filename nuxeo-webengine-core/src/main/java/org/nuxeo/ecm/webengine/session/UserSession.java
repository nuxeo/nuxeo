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

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * TODO: should be synchronized? concurrent access may happen for the same session
 *
 * Used to store user session. This object is cached in a the HTTP session
 * Principal, subject and credentials are immutable per user session
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UserSession extends HashMap<String, Object> implements Serializable, HttpSessionBindingListener {

    private static final long serialVersionUID = 260562970988817064L;

    private static Log log = LogFactory.getLog(UserSession.class);

    protected static UserSession anonymous;

    protected Map<Class<?>, ComponentMap<?>> comps = new HashMap<Class<?>, ComponentMap<?>>();


    public static UserSession getCurrentSession(HttpSession session) {
        return (UserSession)session.getAttribute("nuxeo.webengine.user_session");
    }

    public static void setCurrentSession(HttpSession session, UserSession us) {
//        UserSession currentUs = (UserSession)session.getAttribute("nuxeo.webengine.user_session");
//        if (currentUs != null) {
//
//        }
        session.setAttribute("nuxeo.webengine.user_session", us);
    }

    public static UserSession getAnonymousSession(UserManager mgr) throws ClientException {
        if (anonymous == null) {
            anonymous = createAnonymousSession(mgr);
        }
        return anonymous;
    }

    public static UserSession createAnonymousSession(UserManager mgr) throws ClientException {
        String userId = mgr.getAnonymousUserId();
        if (userId == null) {
            throw new IllegalStateException("User anonymous cannot be created");
        }
        return new UserSession(mgr.getPrincipal(userId), userId);
    }

    public static void destroyAnonynousSession() {
        if (anonymous != null && anonymous.coreSession != null) {
            anonymous.coreSession.destroy();
        }
        anonymous.coreSession = null;
    }

    protected final Subject subject;
    protected final Principal principal;
    protected final Object credentials;

    protected transient CoreSession coreSession;

    public UserSession(Principal principal) {
        this(principal, null);
    }

    public UserSession(Principal principal, String password) {
        this(principal, password == null ? new char[0] : password.toCharArray());
    }

    public UserSession(Principal principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
        Set<Principal> principals = new HashSet<Principal>();
        Set<Object> publicCredentials = new HashSet<Object>();
        Set<Object> privateCredentials = new HashSet<Object>();
        principals.add(principal);
        publicCredentials.add(credentials);
        this.subject = new Subject(true, principals, publicCredentials, privateCredentials);
    }

    public void setCoreSession(CoreSession coreSession) {
        this.coreSession = coreSession;
    }

    public CoreSession getCoreSession() {
        return coreSession;
    }

    /**
     * Whether or not this is the shared anonymous session.
     */
    public boolean isAnonymous() {
        return this == anonymous;
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

    public void valueBound(HttpSessionBindingEvent event) {
        // the user session was bound to the HTTP session
        install(event.getSession());
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
        // the user session was removed from the HTTP session
        uninstall(event.getSession());
       // System.out.println("unbound: "+event.getName() + " = " +event.getValue());
        //HttpSess
//        CoreSession cs = (CoreSession)session.getAttribute(DefaultWebContext.CORESESSION_KEY);
//        if (cs != null) {
//            if (!DefaultWebContext.isAnonymousSession(cs)) {
//                propagate(currentIdentity);
//                cs.destroy();
//            }
//        }
    }

    protected void install(HttpSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Installing user session");
        }
    }

    protected synchronized void uninstall(HttpSession session) {
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
        comps = null;
        // destroy core session
        if (coreSession != null) {
            coreSession.destroy();
            coreSession = null;
        }
    }


    /**
     * Find an existing component. The component state will not be modified before being returned
     * as in {@link #getComponent(Class, String)}
     * <p>
     * If the component was not found in that session returns null
     *
     * @param <T>
     * @param type
     * @param name
     * @return
     * @throws SessionException
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T findComponent(Class<T> type, String name) throws SessionException {
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
     * Get a component given it's class and an optional name.
     * If the component was not yet created in this session it will
     * be created and registered against the session.
     * @param <T>
     * @param type
     * @param name
     * @return
     * @throws SessionException
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Component> T getComponent(Class<T> type, String name) throws SessionException {
        ComponentMap<T> map = (ComponentMap<T>)comps.get(type);
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
            comp = (T)type.newInstance();
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
            Class<T> type = (Class<T>)Class.forName(typeName);
            return getComponent(type, name);
        } catch (ClassNotFoundException e) {
            throw new SessionException("Could not find component class: "+typeName, e);
        }
    }

    /**
     * Get component by ID.
     * The ID is of the form <code>type#name</code> for not null names
     * and <code>type</code> for null names
     *
     * @param <T>
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(String  id) throws SessionException {
        int p = id.lastIndexOf('#');
        if (p > -1) {
            return (T)getComponent(id.substring(0, p), id.substring(p+1));
        } else {
            return (T)getComponent(id, null);
        }
    }

}