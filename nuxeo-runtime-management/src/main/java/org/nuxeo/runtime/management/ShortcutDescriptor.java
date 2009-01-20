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
 *     matic
 */
package org.nuxeo.runtime.management;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author matic
 * 
 */
@XObject("shortcut")
public class ShortcutDescriptor {

    public ShortcutDescriptor() {
        ;
    }

    public ShortcutDescriptor(String shortName, String qualifiedName) {
        this.shortName = shortName;
        this.qualifiedName = qualifiedName;
    }

    @XNode("@name")
    private String shortName;

    @XNode("@qualifiedName")
    private String qualifiedName;

    public String getShortName() {
        return shortName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }
}
