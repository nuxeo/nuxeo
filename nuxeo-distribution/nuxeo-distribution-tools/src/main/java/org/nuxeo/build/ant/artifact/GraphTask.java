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
package org.nuxeo.build.ant.artifact;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GraphTask extends Task {

    protected List<ArtifactKey> resolves;
    protected String src;
    protected Expand expand;


    public void setResolve(String resolve) {
        if (resolves == null) {
            resolves = new ArrayList<ArtifactKey>();
        }
        resolves.add(new ArtifactKey(resolve));
    }

    public void setSrc(String file) {
        src = file;
    }

    public void addExpand(Expand expand) {
        this.expand = expand;
    }

    public void addResolve(ArtifactKey artifact) {
        if (resolves == null) {
            resolves = new ArrayList<ArtifactKey>();
        }
        resolves.add(artifact);
    }


    @Override
    public void execute() throws BuildException {
        MavenClient maven = MavenClientFactory.getInstance();
        if (src != null) {
            if (resolves == null) {
                resolves = new ArrayList<ArtifactKey>();
            }
            try {
                BufferedReader reader = new BufferedReader(new FileReader(src));
                String line = reader.readLine();
                while (line != null) {
                    line = getProject().replaceProperties(line.trim());
                    resolves.add(new ArtifactKey(line));
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new BuildException("Failed to import file: "+src, e);
            }
        }
        if (resolves != null) {
            for (ArtifactKey resolve : resolves) {
                ArtifactDescriptor ad = new ArtifactDescriptor(resolve.pattern);
                Artifact arti = readArtifact(ad);
                try {
                    Node node = maven.getGraph().getRootNode(arti);
                    if (expand != null) {
                        if (expand.filter != null) {
                            node.expand(expand.depth, CompositeFilter.compact(expand.filter));
                        } else {
                            node.expand(expand.depth, null);
                        }
                    }
                } catch (ArtifactNotFoundException e) {
                    throw new BuildException("Root artifact cannot be found: "+arti, e);
                }
            }
        }
    }

    public static Artifact readArtifact(ArtifactDescriptor artifactDescriptor) {
        return MavenClientFactory.getInstance().getArtifactFactory().createBuildArtifact(artifactDescriptor.groupId, artifactDescriptor.artifactId, artifactDescriptor.version, artifactDescriptor.type);
    }
}
