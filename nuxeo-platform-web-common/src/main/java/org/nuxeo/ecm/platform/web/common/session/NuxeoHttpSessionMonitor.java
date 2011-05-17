/*
 *
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.management.counters.CounterHelper;

/**
 *
 * Singleton used to keep track of all HttpSessions. This Singleton is
 * populated/updated either via the HttpSessionListener or via directedly via
 * the Authentication filter
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class NuxeoHttpSessionMonitor {

    protected static Log log = LogFactory.getLog(NuxeoHttpSessionMonitor.class);

    protected static NuxeoHttpSessionMonitor instance = new NuxeoHttpSessionMonitor();

    public static final String REQUEST_COUNTER = "org.nuxeo.web.requests";

    public static final String SESSION_COUNTER = "org.nuxeo.web.sessions";

    public static final long REQUEST_COUNTER_STEP = 5;

    protected long globalRequestCounter;

    public static NuxeoHttpSessionMonitor instance() {
        return instance;
    }

    protected Map<String, SessionInfo> sessionTracker = new ConcurrentHashMap<String, SessionInfo>();

    protected void increaseRequestCounter() {
        globalRequestCounter+=1;
        if (globalRequestCounter==1 || globalRequestCounter%REQUEST_COUNTER_STEP==0) {
            CounterHelper.setCounterValue(REQUEST_COUNTER, globalRequestCounter);
        }
    }

    public SessionInfo addEntry(HttpSession session) {
        if (session == null || session.getId()==null) {
            return null;
        }
        SessionInfo si = new SessionInfo(session.getId());
        sessionTracker.put(session.getId(), si);
        return si;
    }

    public SessionInfo associatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getId()!=null) {
            SessionInfo si = sessionTracker.get(session.getId());
            if (si == null) {
                si = addEntry(session);
            }
            if (request.getUserPrincipal() != null && si.getLoginName()==null) {
                si.setLoginName(request.getUserPrincipal().getName());
                CounterHelper.increaseCounter(SESSION_COUNTER);
            }
            si.setLastAccessUrl(request.getRequestURI());
            increaseRequestCounter();
            return si;
        }
        return null;
    }

    public SessionInfo associatedUser(HttpSession session, String userName) {
        if (session == null || session.getId()==null) {
            return null;
        }
        SessionInfo si = sessionTracker.get(session.getId());
        if (si == null) {
            si = addEntry(session);
        }
        if (si.getLoginName()==null) {
            si.setLoginName(userName);
            CounterHelper.increaseCounter(SESSION_COUNTER);
        }
        return si;
    }

    public SessionInfo updateEntry(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getId()!=null) {
            SessionInfo si = sessionTracker.get(session.getId());
            if (si != null) {
                si.updateLastAccessTime();
                si.setLastAccessUrl(request.getRequestURI());
                increaseRequestCounter();
                return si;
            } else {
                return addEntry(session);
            }
        }
        return null;
    }

    public void removeEntry(String sid) {
        SessionInfo si = sessionTracker.remove(sid);
        if (si!=null && si.getLoginName()!=null) {
            CounterHelper.decreaseCounter(SESSION_COUNTER);
        }
    }

    public Collection<SessionInfo> getTrackedSessions() {
        return sessionTracker.values();
    }

    public List<SessionInfo> getSortedSessions() {

        List<SessionInfo> sortedSessions = new ArrayList<SessionInfo>();
        for (SessionInfo si : getTrackedSessions()) {
            if (si.getLoginName()!=null) {
                sortedSessions.add(si);
            }
        }
        Collections.sort(sortedSessions);
        return sortedSessions;
    }

    public List<SessionInfo> getSortedSessions(long maxInactivity) {

        List<SessionInfo> sortedSessions = new ArrayList<SessionInfo>();
        for (SessionInfo si : getTrackedSessions()) {
            if (si.getLoginName()!=null && si.getInactivityInS() < maxInactivity) {
                sortedSessions.add(si);
            }
        }
        Collections.sort(sortedSessions);
        return sortedSessions;

    }

    public long getGlobalRequestCounter() {
        return globalRequestCounter;
    }

}
