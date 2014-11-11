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
package org.nuxeo.ecm.webengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RequestConfiguration {

    /**
     * Synchronized list of configured paths
     */
    protected Set<PathDescriptor> paths;

    /**
     * Thread safe cache
     */
    protected volatile PathDescriptor[] cache;

    public RequestConfiguration() {
        paths = Collections.synchronizedSet(new HashSet<PathDescriptor>());
    }

    public void addPathDescriptor(PathDescriptor path) {
        path.createMatcher();
        paths.add(path);
        cache = null;
    }

    public void removePathDescriptor(PathDescriptor path) {
        paths.remove(path);
        cache = null;
    }

    public PathDescriptor[] getPaths() {
        PathDescriptor[] result = cache;
        if (result == null) {
            result = paths.toArray(new PathDescriptor[paths.size()]);
            Arrays.sort(result);
            cache = result;
        }
        return result;
    }

    public PathDescriptor getMatchingConfiguration(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() == 0) {
            pathInfo = "/";
        }
        for (PathDescriptor pd : getPaths()) {
            if (pd.match(pathInfo)) {
                return pd;
            }
        }
        return null;
    }

}
