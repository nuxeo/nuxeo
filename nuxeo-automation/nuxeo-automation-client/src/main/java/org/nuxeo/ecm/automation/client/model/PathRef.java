/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PathRef extends DocRef {

    private static final long serialVersionUID = 1L;

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
