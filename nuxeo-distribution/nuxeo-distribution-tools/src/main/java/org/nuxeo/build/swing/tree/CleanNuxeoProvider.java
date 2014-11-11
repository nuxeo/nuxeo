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
package org.nuxeo.build.swing.tree;

import org.apache.maven.artifact.Artifact;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CleanNuxeoProvider extends DefaultNuxeoProvider {

    @Override
    public boolean accept(Edge edge) {
        if ("pom".equals(edge.src.getArtifact().getType())) {
            return true;
        }
        if (edge.src.getArtifact().getArtifactId().startsWith("nuxeo-")) {
            return !edge.dst.getArtifact().getArtifactId().startsWith("nuxeo-");
        }
        return true;
    }

    @Override
    public boolean hasChildren(Node node) {
        Artifact artifact = node.getArtifact();
        if ("pom".equals(artifact.getType())) {
            return true;
        }
        return artifact.getArtifactId().startsWith("nuxeo-");
    }

}
