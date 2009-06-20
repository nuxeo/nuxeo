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
 */
package org.nuxeo.build.maven.filter;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.graph.Edge;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OrFilter extends CompositeFilter {

    public OrFilter() {
        super();
    }

    public OrFilter(List<Filter> filters) {
        super (filters);
    }

    public boolean accept(Dependency dep) {
        for (int i=0,len=filters.size(); i<len; i++) {
            if (this.filters.get(i).accept(dep)) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(Edge edge) {
        for (int i=0,len=filters.size(); i<len; i++) {
            if (this.filters.get(i).accept(edge)) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(Artifact artifact) {
        for (int i=0,len=filters.size(); i<len; i++) {
            if (this.filters.get(i).accept(artifact)) {
                return true;
            }
        }
        return false;
    }
}
