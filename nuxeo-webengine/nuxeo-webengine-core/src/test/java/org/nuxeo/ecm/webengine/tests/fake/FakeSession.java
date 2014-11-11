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

package org.nuxeo.ecm.webengine.tests.fake;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FakeSession implements HttpSession {

    static final Random RANDOM = new Random();

    private final Map<String, Object> attrs;
    private final String sid;
    private final long ctime = System.currentTimeMillis();


    public FakeSession() {
        long id = RANDOM.nextLong();
        sid = Long.toHexString(id);
        attrs = new HashMap<String, Object>();
    }

    public Object getAttribute(String name) {
        return attrs.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attrs.keySet());
    }

    public long getCreationTime() {
        return ctime;
    }

    public String getId() {
        return sid;
    }

    public long getLastAccessedTime() {
        return 0;
    }

    public int getMaxInactiveInterval() {
        return 0;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getValue(String name) {
        return null;
    }

    public String[] getValueNames() {
        return null;
    }

    public void invalidate() {
    }

    public boolean isNew() {
        return false;
    }

    public void putValue(String name, Object value) {
    }

    public void removeAttribute(String name) {
    }

    public void removeValue(String name) {
    }

    public void setAttribute(String name, Object value) {
    }

    public void setMaxInactiveInterval(int interval) {
    }

}
