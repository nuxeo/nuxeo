/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 */
package org.nuxeo.runtime.management.metrics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javasimon.Simon;

/**
 * @author "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 *
 */
public class SimonAttributes implements MetricAttributes {

    protected final Simon simon;

    protected SimonAttributes(Simon simon) {
        this.simon = simon;
    }

    @Override
    public void clear() {
        Iterator<String> it = simon.getAttributeNames();
        while (it.hasNext()) {
            simon.removeAttribute(it.next());
        }
    }

    @Override
    public void put(String key, Object value) {
        simon.setAttribute(key, value);
    }

    @Override
    public Map<String, String> get() {
        Map<String,String> attributes = new HashMap<String,String>();
        Iterator<String> it = simon.getAttributeNames();
        while (it.hasNext()) {
            String key = it.next();
            attributes.put(key, (String)simon.getAttribute(key));
        }
        return attributes;
    }


    @Override
    public void putAll(Map<String, Object> values) {
        // TODO Auto-generated method stub

    }
}
