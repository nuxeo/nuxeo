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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Match patterns of the type:
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathMapper {

    List<MappingDescriptor> entries;

    final ConcurrentMap<String, Mapping> cache;

    public PathMapper() {
        this (null);
    }

    public PathMapper(List<MappingDescriptor> mappings) {
        this.entries = mappings;
        cache = new ConcurrentHashMap<String, Mapping>();
    }

    public void addMapping(MappingDescriptor mdef) {
        if (entries == null) {
            entries = new ArrayList<MappingDescriptor>();
        }
        entries.add(mdef);
    }

    public void clearMappings() {
        if (entries != null) {
            entries.clear();
        }
    }

    public final Mapping getMapping(String pathInfo) {
        if (entries == null) return null;
        for (MappingDescriptor entry: entries) {
            Mapping mapping = entry.match(pathInfo);
            if (mapping != null) {
                return mapping;
            }
        }
//        Mapping mapping = new Mapping();
//        mapping.traversalPath =
        return null;
    }

}
