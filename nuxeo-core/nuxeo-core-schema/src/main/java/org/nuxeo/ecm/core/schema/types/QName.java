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

package org.nuxeo.ecm.core.schema.types;

import java.io.Serializable;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class QName implements Serializable {

    private static final long serialVersionUID = 5259846002688485463L;

    final String prefix;
    final String localName;
    final String prefixedName;

    /**
     * Creates a QName without prefix (e.g. with the default prefix : "").
     *
     * @param localName the local name
     */
    public QName(String localName) {
        this(localName, null);
    }

    /**
     * Creates a QName having the given local name and prefix.
     *
     * @param localName
     * @param prefix
     */
    public QName(String localName, String prefix) {
        this.localName = localName.intern();
        if (prefix == null || prefix.length() == 0) {
            this.prefix = "";
            prefixedName = this.localName;
        } else {
            this.prefix = prefix.intern();
            prefixedName = (prefix + ':' + localName).intern();
        }
    }

    public final String getLocalName() {
        return localName;
    }

    public final String getPrefixedName() {
        return prefixedName;
    }

    public final String getPrefix() {
        return prefix;
    }

    @Override
    public int hashCode() {
        return prefixedName.hashCode();
    }

    @Override
    public String toString() {
        return prefixedName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof QName) {
            return ((QName) obj).prefixedName.equals(prefixedName);
        }
        return false;
    }

    /**
     * Parses the given name and creates the corresponding QName.
     * <p>
     * If the given name is not prefixed then the default prefix (e.g. "")
     * will be used (i.e. the name will remain unprefixed).
     *
     * @param name the name in the prefixed form
     * @return the qname
     */
    public static QName valueOf(String name) {
        return valueOf(name, "");
    }

    /**
     * Parses the given name and create the corresponding QName.
     * <p>
     * If the given name is not prefixed then the given prefix will be used.
     *
     * @param name
     * @param prefix
     * @return
     */
    public static QName valueOf(String name, String prefix) {
        int p = name.indexOf(':');
        if (p > -1) {
            return new QName(name.substring(p + 1), name.substring(0, p));
        } else {
            return new QName(name, prefix);
        }
    }

}
