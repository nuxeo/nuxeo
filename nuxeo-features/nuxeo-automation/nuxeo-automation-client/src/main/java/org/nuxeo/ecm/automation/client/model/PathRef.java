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
