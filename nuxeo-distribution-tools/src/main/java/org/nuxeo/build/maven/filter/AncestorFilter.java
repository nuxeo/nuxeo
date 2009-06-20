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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.graph.Edge;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AncestorFilter implements Filter {

    protected ArtifactDescriptor ad;
    protected List<EdgeFilter> filters;

    public AncestorFilter(String pattern) {
        filters = new ArrayList<EdgeFilter>();
        if (ad.groupId != null && !ad.groupId.equals("*")) {
            addFilter(new GroupIdFilter(ad.groupId));
        }
        if (ad.artifactId != null && !ad.artifactId.equals("*")) {
            addFilter(new ArtifactIdFilter(ad.artifactId));
        }
        if (ad.version != null && !ad.version.equals("*")) {
            addFilter(new VersionFilter(ad.version));
        }
        if (ad.type != null && !ad.type.equals("*")) {
            addFilter(new TypeFilter(ad.type));
        }
        if (ad.classifier != null && !ad.classifier.equals("*")) {
            addFilter(new GroupIdFilter(ad.classifier));
        }
        if (ad.scope != null && !ad.scope.equals("*")) {
            addFilter(new GroupIdFilter(ad.scope));
        }
    }

    protected void addFilter(EdgeFilter filter) {
        filters.add(filter);
    }

    public boolean accept(Dependency dep) {
        throw new UnsupportedOperationException("Ancestor folter cannt be applied on dependency objects");
    }

    public boolean accept(Edge edge) {
        for (int i=0,len=filters.size(); i<len; i++) {
            if (!filters.get(i).accept(edge)) {
                return false;
            }
        }
        return true;
    }

    public boolean accept(Artifact artifact) {
        throw new UnsupportedOperationException("Ancestor folter cannt be applied on artifact objects");
    }

}
