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

package org.nuxeo.ecm.core.schema;

import java.io.Serializable;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
            throw new IllegalArgumentException(
                    "prefix cannot be not empty if the uri is empty");
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
            return ns.uri.equals(uri)
                && ns.prefix.equals(prefix);
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
