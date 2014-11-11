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

package org.nuxeo.ecm.core.api.model.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyRuntimeException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DirtyPropertyIterator implements Iterator<Property> {

    private final Iterator<Property> it;
    private Property property; // the current property - null if no intialized
    private Property next; // the last seen property by hasNext() - null if no initialized

    public DirtyPropertyIterator(Iterator<Property> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        while (it.hasNext()) {
            next = it.next();
            if (next.isDirty()) {
                return true;
            }
        }
        next = null;
        return false;
    }

    @Override
    public Property next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements to iterate over");
        }
        property = next;
        next = null;
        return property;
    }

    @Override
    public void remove() {
        if (property == null) {
            throw new IllegalStateException("Cannot call remove on a non initialized iterator");
        }
        try {
            property.remove();
        } catch (PropertyException e) {
            throw new PropertyRuntimeException("Failed to remove property", e);
        }
    }

}
