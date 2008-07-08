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

package org.nuxeo.ecm.core.repository.jcr;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XPathQuery {
    // predicate part [...]
    StringBuilder predicate = new StringBuilder(1024);
    // element(name, type)
    String name = "*";
    String type = "*";
    // path expression: //doc1/doc2/ should contain a trailing /
    String path = null;
    StringBuilder orderBy = new StringBuilder();

    public String toString() {
        if (name == null) {
            name = "*";
        }
        if (predicate.length() > 0) {
            return new StringBuilder(256).append(path == null ? "//" : path)
            .append("element(").append(name).append(",").append(type)
            .append(")").append("[").append(predicate).append("]").append(orderBy).toString();
        } else {
            return new StringBuilder(256).append(path == null ? "//" : path)
            .append("element(").append(name).append(",").append(type)
            .append(")").append(predicate).append(orderBy).toString();
        }
    }

    public void initPath() {
        path = "//";
    }
}
