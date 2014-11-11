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
package org.nuxeo.build.maven.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.nuxeo.build.maven.filter.Filter;

/**
 * TODO: use pom settings when resolving an artifact (use remote repos specified in pom if any)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public class Node {

    protected Graph graph;
    protected String id;
    protected Artifact artifact;
    protected List<Edge> edgesIn;
    protected List<Edge> edgesOut;

    private boolean isExpanded;

    /**
     * Point to an artifact pom. When embedded in maven and using the current project pom
     * as the root this will be set by the maven loader mojo to point to the current pom 
     */
    protected MavenProject pom;


    public static String createNodeId(Artifact artifact) {
        return new StringBuilder().append(artifact.getGroupId()).append(':').append(artifact.getArtifactId()).append(':').append(artifact.getVersion()).append(':').append(artifact.getType()).append(':').toString();
    }



    public Node(Node node) {
        this.graph = node.graph;
        this.id = node.id;
        this.artifact = node.artifact;
        this.edgesIn = node.edgesIn;
        this.edgesOut = node.edgesOut;
        this.pom = node.pom;
        this.isExpanded = node.isExpanded;
    }

    public Node(Graph graph, MavenProject pom, Artifact artifact) {
        this (graph, pom, artifact, Node.createNodeId(artifact));
    }

    protected Node(Graph graph, MavenProject pom, Artifact artifact, String id) {
        this.graph = graph;
        this.id = id;
        this.artifact = artifact;
        this.pom = pom;
        edgesIn = new ArrayList<Edge>();
        edgesOut = new ArrayList<Edge>();
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public File getFile() {
        resolveIfNeeded();
        File file = artifact.getFile();
        if (file != null) {
            graph.file2artifacts.put(file.getName(), artifact);
        }
        return file;
    }

    public File getFile(String classifier) {
        resolveIfNeeded();
        Artifact ca = graph.maven.getArtifactFactory().createArtifactWithClassifier(
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), classifier);
        try {
            graph.maven.resolve(ca);
            File file = ca.getFile();
            if (file != null) {
                graph.file2artifacts.put(file.getAbsolutePath(), ca);
            }
            return file;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public boolean isRoot() {
        return edgesIn.isEmpty();
    }

    public String getId() {
        return id;
    }

    public List<Edge> getEdgesOut() {
        return edgesOut;
    }

    public List<Edge> getEdgesIn() {
        return edgesIn;
    }

    public void addEdgeIn(Edge edge) {
        edgesIn.add(edge);
    }

    public void addEdgeOut(Edge edge) {
        edgesOut.add(edge);
    }

    public MavenProject getPom() {
        resolveIfNeeded();
        return pom;
    }

    public MavenProject getPomIfAlreadyLoaded() {
        return pom;
    }

    public boolean isExpanded() {
        return isExpanded;
    }
    
    public void expand(int recurse, Filter filter) {
        if (isExpanded) {
            return;
        }
        isExpanded = true;
        resolveIfNeeded();
        if (pom == null) {
            return;
        }
        if (recurse > 0) {
            loadDependencies(recurse-1, (List<Dependency>)pom.getDependencies(), filter);
//            if ("pom".equals(artifact.getType())) {
//                loadDependencies(recurse-1, (List<Dependency>)pom.getDependencyManagement().getDependencies(), filter);
//            } else {
//                loadDependencies(recurse-1, (List<Dependency>)pom.getDependencies(), filter);
//            }
        }
    }

    public void expand(Filter filter) {
        expand(0, filter);
    }

    public void expandAll(Filter filter) {
        expand(Integer.MAX_VALUE, filter);
    }

    public List<Node> getTrail() {
        if (edgesIn.isEmpty()) {
            ArrayList<Node> result = new ArrayList<Node>();
            result.add(this);
            return result;
        }
        Edge edge = edgesIn.get(0);
        List<Node> path = edge.src.getTrail();
        path.add(this);
        return path;
    }


    public void resolveIfNeeded() {
        try {
            graph.getResolver().resolve(this);
        } catch (ArtifactNotFoundException e) {
            throw new BuildException("Artifact not found: "+artifact.getId(), e);
        }
    }

    protected void loadDependencies(int recurse, List<Dependency> deps, Filter filter)  {
        resolveIfNeeded();
        ArtifactFactory factory = graph.getMaven().getArtifactFactory();
        MavenProject pom = getPom();
        if (pom == null) {
            return;
        }
        for(Dependency d : deps) {
            if (filter != null && !filter.accept(d)) {
                continue;
            }
            // the last boolean parameter is redundant, but the version that doesn't take this
            // has a bug. See MNG-2524
            Artifact a = factory.createDependencyArtifact(
                    d.getGroupId(), d.getArtifactId(), VersionRange.createFromVersion(d.getVersion()),
                    d.getType(), d.getClassifier(), d.getScope(), false);

            // beware of Maven bug! make sure artifact got the value inherited from dependency
            assert a.getScope().equals(d.getScope());
            Node newNode = null;
            try {
                newNode = graph.getNode(a);
            } catch (ArtifactNotFoundException e) {
                throw new RuntimeException("Unable to find dependency: "+a+". Trail: "+getTrail(), e);
            }
            Edge edge = new Edge(this, newNode, d.getScope(), d.isOptional());
            addEdgeOut(edge);
            newNode.addEdgeIn(edge);
            //edge.resolve(); //TODO resolve using pom repos
            if (recurse > 0) {
                newNode.expand(recurse, filter);
            }
        }
    }

    public void collectNodes(Collection<Node> nodes, Filter filter) {
        for (Edge edge : edgesOut) {
            if (filter.accept(edge)) {
                nodes.add(edge.dst);
            }
        }
    }

    public void collectNodes(Collection<Node> nodes) {
        for (Edge edge : edgesOut) {
            nodes.add(edge.dst);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Node) {
            return ((Node)obj).id.equals(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
