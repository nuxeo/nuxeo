/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api;

/**
 * A PATH reference to a document.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PathRef implements DocumentRef {

    private static final long serialVersionUID = 4817248580727120854L;

    public final String value;

    public PathRef(String parentPath, String name) {
        if (parentPath == null) {
            value = name;
        } else if ("/".equals(parentPath)) {
            value = '/' + name;
        } else {
            value = parentPath + '/' + name;
        }
    }

    public PathRef(String path) {
        value = path;
    }

    /**
     * @since 5.6
     */
    public PathRef(PathRef parentRef, String name) {
        this(parentRef.value, name);
    }

    @Override
    public int type() {
        return PATH;
    }

    @Override
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
