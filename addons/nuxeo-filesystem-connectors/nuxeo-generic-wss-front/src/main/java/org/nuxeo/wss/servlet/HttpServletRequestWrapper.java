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
 *     Vitalii Siryi
 */
package org.nuxeo.wss.servlet;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

public class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {

    protected final Map<String, String> headers = new HashMap<String, String>();

    public HttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        for (Enumeration<?> e = request.getHeaderNames(); e.hasMoreElements();) {
            String headerName = String.valueOf(e.nextElement());

            if (StringUtils.isNotEmpty(headerName)) {
                String value = request.getHeader(headerName);
                headers.put(headerName.toLowerCase(), value);
            }
        }
    }

    @Override
    public String getHeader(String name) {
        if (StringUtils.isNotEmpty(name)) {
            return headers.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    public void setHeader(String name, String value) {
        if (StringUtils.isNotEmpty(name)) {
            headers.put(name.toLowerCase(), value);
        }
    }

    @Override
    public Enumeration<?> getHeaderNames() {
        return new SetEnumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Set<String> set = new HashSet<String>();
        String value = headers.get(name);
        if(StringUtils.isNotEmpty(value)){
            set.add(value);
        }
        return new SetEnumeration(set);
    }

    class SetEnumeration implements Enumeration<String> {

        private final Iterator<String> iterator;

        public SetEnumeration(Set<String> set) {
            iterator = new ArrayList<String>(set).iterator();
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public String nextElement() {
            return iterator.next();
        }

    }


}
