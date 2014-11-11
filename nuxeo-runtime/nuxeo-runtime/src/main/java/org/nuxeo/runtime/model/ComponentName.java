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

package org.nuxeo.runtime.model;

import java.io.Serializable;

/**
 * A component name.
 * <p>
 * Component names are strings of the form <code>type:name</code>
 * The type part is optional - when missing the type is assumed to be "service".
 * <p>
 * Example of valid component names:
 *
 * <ul>
 * <li> repository:my.repo
 * <li> service:my.service
 * <li> my.component
 * </ul>
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentName implements Serializable {

    public static final String DEFAULT_TYPE = "service";

    private static final long serialVersionUID = -7686792831111487156L;

    private final String type;
    private final String name;
    private final String rawName;

    /**
     * Constructs a component name from its string representation.
     *
     * @param rawName the string representation of this name
     */
    public ComponentName(String rawName) {
        int p = rawName.indexOf(':');
        if (p > -1) {
            type = rawName.substring(0, p).intern();
            name = rawName.substring(p + 1);
            this.rawName = rawName.intern();
        } else {
            type = DEFAULT_TYPE;
            name = rawName;
            this.rawName = (type + ':' + name).intern();
        }
    }

    /**
     * Constructs a component name from its two parts: type and name.
     *
     * @param type the type part of the component name
     * @param name the name part of the component name
     */
    public ComponentName(String type, String name) {
        this.type = type.intern();
        this.name = name;
        rawName = (type + ':' + name).intern();
    }

    /**
     * Gets the type part of the component name.
     *
     * @return the type part
     */
    public final String getType() {
        return type;
    }

    /**
     * Gets the name part of the component name.
     *
     * @return the name part
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the qualified component name.
     *
     * @return the qualified component name
     */
    public final String getRawName() {
        return rawName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ComponentName) {
            return rawName.equals(((ComponentName) obj).rawName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return rawName.hashCode();
    }

    @Override
    public String toString() {
        return rawName;
    }

}
