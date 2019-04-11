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

package org.nuxeo.common.utils;

import java.util.ArrayList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PathFilterSet extends ArrayList<PathFilter> implements PathFilter {

    private static final long serialVersionUID = -2967683005810353014L;

    private boolean isExclusive = true;

    public PathFilterSet() {

    }

    public PathFilterSet(boolean isExclusive) {
        this.isExclusive = isExclusive;
    }

    @Override
    public boolean isExclusive() {
        return !isExclusive;
    }

    @Override
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
