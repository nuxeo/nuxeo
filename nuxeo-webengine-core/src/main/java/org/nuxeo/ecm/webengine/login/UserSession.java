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

package org.nuxeo.ecm.webengine.login;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Used to store user session. This object is cached in a the HTTP session
 * Principal, subject and credentials are immutable per user session
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UserSession extends HashMap<String, Object> implements Serializable, HttpSessionBindingListener {

    private static final long serialVersionUID = 260562970988817064L;

    protected static UserSession anonymous;


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
        //install(event.getSession());
        //System.out.println("bound : "+event.getName() + " = " +event.getValue());
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
        // the user session was removed from the HTTP session
        //uninstall(event.getSession());
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
    }

    protected void uninstall(HttpSession session) {
    }

}
