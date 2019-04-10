/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.wi.filter;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WISession implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BACKEND_KEY = "org.nuxeo.ecm.platform.wi.backend";

    public static final String CORESESSION_KEY = "org.nuxeo.ecm.platform.wi.coresession";

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private String key;

    private long creationTime;

    private long accessTime;

    public WISession(String key) {
        this.creationTime = System.currentTimeMillis();
        access();
        this.key = key;
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public Collection<Object> getAttributes() {
        return attributes.values();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void access() {
        this.accessTime = System.currentTimeMillis();
    }

    public boolean isValid() {
        long time = System.currentTimeMillis();
        if (time > creationTime + 20 * 60 * 1000
                && time > accessTime + 2 * 60 * 1000) {
            return false;
        } else {
            return true;
        }
    }
}
