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

package org.nuxeo.ecm.webengine.fake;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public class SetEnumeration implements Enumeration<String> {

    private final List<String> keys;

    public SetEnumeration(Set<String> set) {
        keys = new ArrayList<String>(set);
    }

    public boolean hasMoreElements() {
        return !keys.isEmpty();
    }

    public String nextElement() {
        return keys.remove(0);
    }

}
