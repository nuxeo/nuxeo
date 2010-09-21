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
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PathRef extends DocRef {

    public PathRef(String path) {
        super(path);
    }

    public String value() {
        return ref;
    }

    public PathRef getParent() {
        if (ref.length() == 0 || ref.equals("/")) {
            return null;
        }
        String path = ref;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int p = path.lastIndexOf('/');
        if (p == -1) {
            return new PathRef("/");
        } else {
            return new PathRef(path.substring(0, p));
        }
    }

    public PathRef getChild(String childPath) {
        StringBuilder buf = new StringBuilder(ref);
        if (ref.endsWith("/")) {
            buf.append(childPath);
        } else {
            buf.append('/').append(childPath);
        }
        return new PathRef(buf.toString());
    }

}
