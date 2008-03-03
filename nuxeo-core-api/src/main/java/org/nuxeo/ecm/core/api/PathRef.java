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

package org.nuxeo.ecm.core.api;

/**
 * A PATH reference to a document.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathRef implements DocumentRef {

    private static final long serialVersionUID = 4817248580727120854L;

    public final String value;


    public PathRef(String parentPath, String name) {
        if ("/".equals(parentPath)) {
            value = '/' + name;
        } else {
            value = parentPath + '/' + name;
        }
    }

    public PathRef(String path) {
        value = path;
    }

    public int type() {
        return PATH;
    }

    public Object reference() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PathRef) {
            return ((PathRef) obj).value.equals(value);
        }
        // it is not possible to compare a PathRef with an IdRef
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

}
