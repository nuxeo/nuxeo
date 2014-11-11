/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.session.impl;

import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.CoreSessionProvider;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionRef;
import org.nuxeo.ecm.webengine.jaxrs.session.impl.PerSessionCoreProvider.Ref;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PerSessionCoreProvider extends CoreSessionProvider<Ref> implements HttpSessionBindingListener {

    private static final Log log = LogFactory.getLog(PerSessionCoreProvider.class);

    public static class Ref implements SessionRef {
        protected CoreSession session;
        protected ReentrantLock lock;

        public Ref(CoreSession session) {
            this.session = session;
            this.lock = new ReentrantLock();
        }

        @Override
        public CoreSession get() {
            lock.lock();
            return session;
        }

        @Override
        public void unget() {
            // unlock only if the current thread holds the lock otherwise ignore.
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                //do nothing
            }
        }

        @Override
        public void destroy() {
            try {
                session.destroy();
            } finally {
                session = null;
                lock = null;
            }
        }

    }

    public static synchronized void install(HttpServletRequest request) {
        HttpSession s = request.getSession(true);
        if (s.getAttribute(SessionFactory.SESSION_FACTORY_KEY) == null) {
            s.setAttribute(SessionFactory.SESSION_FACTORY_KEY, new PerSessionCoreProvider());
        }
    }


    @Override
    protected Ref createSessionRef(CoreSession session) {
        return new Ref(session);
    }

    @Override
    public void onRequestDone(HttpServletRequest request) {
        // unlock sessions if any was locked
        for (SessionRef ref : getSessions()) {
            ref.unget();
        }
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        // do nothing
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        // destroy all sessions
        if (!hasSessions()) {
            destroy();
            return;
        }

        LoginContext lc = null;
        try {
            lc = Framework.login();
            destroy();
        } catch (Throwable e) {
            log.error(e);
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                }
            }
        }
    }

    @Override
    public synchronized SessionRef[] getSessions() {
        return super.getSessions();
    }

    @Override
    public synchronized SessionRef getSessionRef(HttpServletRequest request, String repoName) {
        return super.getSessionRef(request, repoName);
    }

    @Override
    public synchronized boolean hasSessions() {
        return super.hasSessions();
    }

    @Override
    protected synchronized void destroy() {
        super.destroy();
    }

}
