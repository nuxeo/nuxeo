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

package org.nuxeo.common.utils;

import java.util.ArrayList;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathFilterSet extends ArrayList<PathFilter> implements PathFilter {

    private static final long serialVersionUID = -2967683005810353014L;

    private boolean isExclusive = true;

    public PathFilterSet() {

    }

    public PathFilterSet(boolean isExclusive) {
        this.isExclusive = isExclusive;
    }

    public boolean isExclusive() {
        return !isExclusive;
    }

    public boolean accept(Path path) {
        int inclusive = 0;
        boolean defaultValue = false;
        for (PathFilter filter : this) {
            boolean ret = filter.accept(path);
            if (ret) {
                if (!filter.isExclusive()) {
                    inclusive++;
                    defaultValue = true;
                }
            } else {
                if (filter.isExclusive()) {
                    return false;
                } else {
                    inclusive++;
                }
            }
        }
        return inclusive == 0 || defaultValue;
    }

}
