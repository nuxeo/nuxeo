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

package org.nuxeo.ecm.core.repository.jcr;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FilteredPropertyIterator implements PropertyIterator {

    private final PropertyIterator delegate; // property iterator delegate
    private final PropertyFilter filter;
    private Property next;
    private long pos = 0;

    public FilteredPropertyIterator(Node node, PropertyFilter filter)
            throws RepositoryException {
        this.filter = filter;
        delegate = node.getProperties();
        next = prefetch();
    }

    private Property prefetch() {
        while (delegate.hasNext()) {
            Property p = delegate.nextProperty();
            if (filter == null) {
                return p;
            }
            try {
                if (filter.accept(p)) {
                    return p;
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        return null;
    }

    public Property nextProperty() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Property p = next;
        next = prefetch();
        pos++;
        return p;
    }

    public Object next() {
        return nextProperty();
    }

    public boolean hasNext() {
        return next != null;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public long getSize() {
        return -1; // unknown size
    }

    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            nextProperty();
        }
    }

    public long getPosition() {
        return pos;
    }

}
