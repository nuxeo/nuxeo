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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.nuxeo.build.ant.artifact.GraphTask;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClient;
import org.nuxeo.build.maven.filter.Filter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Graph {

    protected MavenClient maven;

    protected LinkedList<Node> roots = new LinkedList<Node>();
    protected TreeMap<String, Node> nodes = new TreeMap<String, Node>();
    protected Resolver resolver = new Resolver(this);
    protected Map<String, Artifact> file2artifacts = new HashMap<String, Artifact>();

    public Graph(MavenClient maven) {
        this.maven = maven;
    }

    public MavenClient getMaven() {
        return maven;
    }

    public List<Node> getRoots() {
        return roots;
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Artifact getArtifactByFile(String fileName) {
        return file2artifacts.get(fileName);
    }

    public void collectNodes(Collection<Node> nodes) {
        for (Node node : roots) {
            node.collectNodes(nodes);
        }
    }

    public void collectNodes(Collection<Node> nodes, Filter filter) {
        for (Node node : roots) {
            node.collectNodes(nodes, filter);
        }
    }

    public Node[] getNodesArray() {
        return nodes.values().toArray(new Node[nodes.size()]);
    }

    public TreeMap<String, Node> getNodesTree() {
        return nodes;
    }

    public Node findFirst(String pattern) {
        return findFirst(pattern, false);
    }

    public Node findFirst(String pattern, boolean stopIfNotUnique) {
        SortedMap<String, Node> map = nodes.subMap(pattern+':', pattern+((char)(':'+1)));
        int size = map.size();
        if (size == 0) {
            return null;
        }
        if (stopIfNotUnique && size > 1) {
            throw new BuildException("Pattern '"+pattern+"' cannot be resolved to a unique node. Matching nodes are: "+map.values());
        }
        return map.get(map.firstKey());
    }

    public Collection<Node> find(String pattern) {
        SortedMap<String, Node> map = nodes.subMap(pattern+':', pattern+((char)(':'+1)));
        return map.values();
    }


//    public List<Node> getNodes(NodeFilter filter) {
//        Node[] allNodes = getNodes();
//        ArrayList<Node> result = new ArrayList<Node>();
//
//    }
//

    /**
     * Add a root node given an artifact pom. This can be used by the embedder maven mojo
     * to initialize the graph with the current pom.
     */
    public Node addRootNode(MavenProject pom) throws ArtifactNotFoundException {
        Artifact artifact = pom.getArtifact();
        String key = Node.createNodeId(artifact);
        Node node = nodes.get(key);
        if (node == null) {
            //node = getResolver().resolve(artifact);
            node = new Node(this, pom, artifact, key);
            nodes.put(node.getId(), node);
            roots.add(node);
        }
        return node;
    }

    public Node addRootNode(String key) throws ArtifactNotFoundException {
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        Artifact artifact = GraphTask.readArtifact(ad);
        return getRootNode(artifact);
    }

    public Node getRootNode(Artifact artifact) throws ArtifactNotFoundException {
        String key = Node.createNodeId(artifact);
        Node node = nodes.get(key);
        if (node == null) {
            //node = getResolver().resolve(artifact);
            node = new Node(this, null, artifact, key);
            nodes.put(node.getId(), node);
            roots.add(node);
        }
        return node;
    }

    public Node getNode(Artifact artifact) throws ArtifactNotFoundException {
        String key = Node.createNodeId(artifact);
        Node node = nodes.get(key);
        if (node == null) {
            //node = getResolver().resolve(artifact);
            node = new Node(this, null, artifact, key);
            nodes.put(node.getId(), node);
        }
        return node;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public Node lookup(String id) {
        return nodes.get(id);
    }

    public Node lookup(Artifact artifact) {
        return lookup(Node.createNodeId(artifact));
    }

    public Node findNode(ArtifactDescriptor ad) {
        String key = ad.getNodeKeyPattern();
        Collection<Node> nodes = null;
        if (key == null) {
            nodes = getNodes();
        } else {
            nodes = find(key);
        }
        for (Node node : nodes) {
            Artifact arti = node.getArtifact();
            if (ad.artifactId != null && !ad.artifactId.equals(arti.getArtifactId())) {
                continue;
            }
            if (ad.groupId != null && !ad.groupId.equals(arti.getGroupId())) {
                continue;
            }
            if (ad.version != null && !ad.version.equals(arti.getVersion())) {
                continue;
            }
            if (ad.type != null && !ad.type.equals(arti.getType())) {
                continue;
            }
            //            if (ad.classifier != null && !ad.classifier.equals(arti.getClassifier())) {
            //                continue;
            //            }
            return node;
        }

        return null;
    }


    public MavenProject loadPom(Artifact artifact) {
        if ("system".equals(artifact.getScope())) return null;
        try {
            return maven.getProjectBuilder().buildFromRepository(
                    // this create another Artifact instance whose type is 'pom'
                    maven.getArtifactFactory().createProjectArtifact(artifact.getGroupId(),artifact.getArtifactId(), artifact.getVersion()),
                    maven.getRemoteRepositories(),
                    maven.getLocalRepository());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) throws Exception {
        TreeMap<String,String> map = new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        map.put("org.nuxeo:core:", "org.nuxeo:core");
        map.put("org.nuxeo:core:e", "org.nuxeo:coree");
        map.put("org.nuxeo:coree", "org.nuxeo:core:test");
        map.put("org.nuxeo:common", "org.nuxeo:common");
        map.put("org.nuxeo:clear", "org.nuxeo:clear");
        map.put("org.nuxeos", "org.nuxeos");
        map.put("com", "com");
        map.put("pom", "pom");
        map.put("a:b:c:d", "a:b:c:d");
        map.put("a:b:d", "a:b:d");

        map.put("b", "b");
        System.out.println(map);
        SortedMap<String, String> smap = map.subMap("org.nuxeo:core:", "org.nuxeo:core:\0");
        System.out.println(smap.size()+" - "+smap);
//        System.out.println(smap.firstKey());
//        System.out.println(smap.lastKey());


    }
}
