/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.theme.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class OrderedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    private final List<Object> order;

    public OrderedProperties() {
        order = new ArrayList<Object>();
    }

    @Override
    public Enumeration<Object> propertyNames() {
        return Collections.enumeration(order);
    }

    @Override
    public Object put(Object key, Object value) {
        if (order.contains(key)) {
            order.remove(key);
        }
        order.add(key);
        return super.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        order.remove(key);
        return super.remove(key);
    }

}
