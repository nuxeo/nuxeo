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
package org.nuxeo.ecm.webengine.jaxrs.context.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathMappingRegistry extends DefaultComponent {

    public final static String XP_MAPPING = "mappings";

    protected List<PathMapping> mappings;
    protected volatile PathMapping[] cache;

    public PathMappingRegistry() {
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        mappings = new ArrayList<PathMapping>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        mappings = null;
    }

    public PathMapping[] getMappings() {
        PathMapping[] _cache = cache;
        if (_cache == null) {
            synchronized (this) {
                _cache = mappings.toArray(new PathMapping[mappings.size()]);
                Arrays.sort(_cache);
                cache = _cache;
            }
        }
        return _cache;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        mappings.add((PathMapping)contribution);
        cache = null;
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        mappings.remove((PathMapping)contribution);
        // we are in a synchronized block
        cache = null;
    }

}
