/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        property.remove();
    }

}
