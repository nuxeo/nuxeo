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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * TODO is this really needed?
 */
public class PropertyDiff implements Iterable<PropertyDiff> {

    public static final int MODIFIED = 1;
    public static final int ADDED = 2;
    public static final int REMOVED = 3;

    public int type;
    public Serializable value;
    public String name;

    public final Set<PropertyDiff> children;

    public PropertyDiff(int type, String name) {
        this(type, name, null);
    }

    // FIXME: not fully implemented
    public PropertyDiff(int type, String name, Serializable value) {
        children = new HashSet<PropertyDiff>();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void modify(String name, Serializable value) {
        children.add(new PropertyDiff(MODIFIED, name, value));
    }

    public void remove(String name) {
        children.add(new PropertyDiff(REMOVED, name));
    }

    public void add(String name, Serializable value) {
        children.add(new PropertyDiff(ADDED, name, value));
    }

    public Iterator<PropertyDiff> iterator() {
        return children.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyDiff) {
            return name.equals(((PropertyDiff) obj).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (name == null) ? 0 : name.hashCode();
    }

}
