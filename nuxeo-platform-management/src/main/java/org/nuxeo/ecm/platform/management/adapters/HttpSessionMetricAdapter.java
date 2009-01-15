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
 *     matic
 */
package org.nuxeo.ecm.platform.management.adapters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author matic
 * 
 */
public class HttpSessionMetricAdapter implements HttpSessionMetricMBean {

    public static HttpSessionMetricAdapter instance;

    protected static final String SESSION_KEY = HttpSessionMetricAdapter.class.getSimpleName();

    protected Long createdSessionCount = 0L;

    protected Long destroyedSessionCount = 0L;

    public void addSessionListener(HttpServletRequest request) {
        HttpSession session= request.getSession();
        HttpSessionMetricListener listener = new HttpSessionMetricListener(this);
        if (session == null) {
            request.setAttribute(SESSION_KEY, listener);
        } else {
            session.setAttribute(SESSION_KEY, listener);
        }
    }

    public Long getActiveSessionCount() {
        return createdSessionCount - destroyedSessionCount;
    }

    public Long getDestroyedSessionCount() {
        return destroyedSessionCount;
    }

    public Long getCreatedSessionCount() {
        return createdSessionCount;
    }
}
