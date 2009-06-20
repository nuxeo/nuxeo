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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.graph.Edge;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScopeFilter implements Filter {

    protected SegmentMatch matcher;


    public ScopeFilter(String pattern) {
        this (SegmentMatch.parse(pattern));
    }

    public ScopeFilter(SegmentMatch matcher) {
        this.matcher = matcher;
    }

    public boolean match(String segment) {
        return matcher.match(segment);
    }

    public boolean accept(Dependency dep) {
        String scope = dep.getScope();
        if (scope == null) {
            return matcher == SegmentMatch.ANY;
        }
        return matcher.match(dep.getScope());
    }

    public boolean accept(Edge edge) {
        if (edge.scope == null) {
            return matcher == SegmentMatch.ANY;
        }
        return matcher.match(edge.scope);
    }

    public boolean accept(Artifact artifact) {
        String scope = artifact.getScope();
        if (scope == null) {
            return matcher == SegmentMatch.ANY;
        }
        return matcher.match(scope);
    }

}
