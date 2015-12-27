/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
        checkName(name);
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

    public static void checkName(String name) {
        // checks the name does not contains slash
        if (name != null && name.indexOf('/') >= 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid name '%s'. A DocumentModel's name cannot contain slash character. "
                            + "Use the parentPath to specificy the document's path.", name));
        }
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
