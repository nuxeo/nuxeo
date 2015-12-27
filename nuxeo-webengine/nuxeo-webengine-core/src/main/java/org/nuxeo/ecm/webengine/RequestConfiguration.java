/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
