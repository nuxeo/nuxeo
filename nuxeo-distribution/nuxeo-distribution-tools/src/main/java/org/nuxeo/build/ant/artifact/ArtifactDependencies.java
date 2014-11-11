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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.nuxeo.build.ant.artifact.ArtifactSet.NodeFilesIterator;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.graph.AttachmentNode;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactDependencies extends DataType implements ResourceCollection {

    protected Node node;
    protected List<Node> nodes;
    public String key;
    public int depth = 1;
    public ArtifactDescriptor ad = ArtifactDescriptor.emptyDescriptor();

    public Includes includes;
    public Excludes excludes;
    
    public void setDepth(String depth) {
        if ("all".equals(depth)) {
            this.depth = Integer.MAX_VALUE;
        } else {
            this.depth = Integer.parseInt(depth);
            if (this.depth == 0) {
                throw new IllegalArgumentException("0 is not a valid value for depth"); 
            }
        }
    }
    
    public void addExcludes(Excludes excludes) {
        if (this.excludes != null) {
            throw new BuildException("Found an Excludes that is defined more than once in an artifact dependencies");
        }        
        this.excludes = excludes;
    }
    
    public void addIncludes(Includes includes) {
        if (this.includes != null) {
            throw new BuildException("Found an Includes that is defined more than once in an artifact dependencies");
        }
        this.includes = includes;
    }

    public void setKey(String pattern) {
        key = pattern;
    }

    public void setArtifactId(String artifactId) {
        this.ad.artifactId = artifactId;
    }
    
    public void setGroupId(String groupId) {
        this.ad.groupId = groupId;
    }
    
    public void setType(String type) {
        this.ad.type = type;
    }
    
    public void setVersion(String version) {
        this.ad.version = version;
    }
    

    public Node getNode() {
        if (node == null) {
            if (key != null) {
                node = MavenClientFactory.getInstance().getGraph().findFirst(key);
            } else {
                node = MavenClientFactory.getInstance().getGraph().findNode(ad);
            }
            if (node == null) {
                throw new BuildException("Artifact with pattern "+key+" was not found in graph");
            }
            if (ad.classifier != null) {
                // we need to create a virtual node that points to the attachment
                node = new AttachmentNode(node, ad.classifier);
            }
        }
        return node;
    }

    public List<Node> getNodes() {
        if (nodes == null) {
            Filter filter = null;
            if (includes != null || excludes != null) {
                AndFilter andf = new AndFilter();
                if (includes != null) {
                    andf.addFilter(includes.filter);
                }
                if (excludes != null) {
                    andf.addFilter(excludes.filter);
                }
                filter = CompositeFilter.compact(andf);
            }
            Node node = getNode();
            // make sure node is expanded        
            node.expand(depth, null); // if not already expanded this expand may not be done correctly
            nodes = new ArrayList<Node>();
            if (filter != null) {
                for (Edge edge : node.getEdgesOut()) {
                    if (filter.accept(edge.dst.getArtifact())) {
                        nodes.add(edge.dst);
                    }
                }
            } else {
                for (Edge edge : node.getEdgesOut()) {
                    nodes.add(edge.dst);
                }            
            }            
        }
        return nodes;
    }
    
    public Iterator<FileResource> iterator() {
        return new NodeFilesIterator(getNodes().iterator());
    }

    public int size() {
        return getNodes().size();
    }

    public boolean isFilesystemOnly() {
        return true;
    }
}
