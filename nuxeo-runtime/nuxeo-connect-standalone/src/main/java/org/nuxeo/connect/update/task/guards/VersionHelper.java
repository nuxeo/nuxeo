/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.guards;

import org.nuxeo.connect.update.Version;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class VersionHelper {

    public boolean isEqual(String v1, String v2) {
        return v1.equals(v2);
    }

    public boolean isGreaterOrEqual(String v1, String v2) {
        if (v1.equals(v2)) {
            return true;
        }
        return new Version(v1).greaterThan(new Version(v2));
    }

    public boolean isLessOrEqual(String v1, String v2) {
        if (v1.equals(v2)) {
            return true;
        }
        return new Version(v1).lessThan(new Version(v2));
    }

    public boolean isGreater(String v1, String v2) {
        return new Version(v1).greaterThan(new Version(v2));
    }

    public boolean isLess(String v1, String v2) {
        return new Version(v1).lessThan(new Version(v2));
    }

}
