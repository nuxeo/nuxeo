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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import java.io.Serializable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Namespace implements Serializable {

    public static final Namespace DEFAULT_NS = new Namespace();

    private static final long serialVersionUID = -3069469489908062592L;

    private static final String DEFAULT_PREFIX = "";

    private static final String DEFAULT_URI = "";

    public final String uri;

    public final String prefix;

    public Namespace(String uri, String prefix) {
        assert uri != null;
        if (uri.length() == 0 && prefix.length() > 0) {
            throw new IllegalArgumentException("prefix cannot be not empty if the uri is empty");
        }
        this.uri = uri;
        this.prefix = prefix == null ? "" : prefix;
    }

    private Namespace() {
        this(DEFAULT_URI, DEFAULT_PREFIX);
    }

    public boolean hasPrefix() {
        return prefix.length() > 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Namespace) {
            Namespace ns = (Namespace) obj;
            return ns.uri.equals(uri) && ns.prefix.equals(prefix);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return uri + " [" + prefix + ']';
    }

}
