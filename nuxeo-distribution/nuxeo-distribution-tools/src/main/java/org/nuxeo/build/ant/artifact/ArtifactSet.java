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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.filter.AncestorFilter;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.ArtifactIdFilter;
import org.nuxeo.build.maven.filter.ClassifierFilter;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.filter.GroupIdFilter;
import org.nuxeo.build.maven.filter.IsOptionalFilter;
import org.nuxeo.build.maven.filter.ScopeFilter;
import org.nuxeo.build.maven.filter.TypeFilter;
import org.nuxeo.build.maven.filter.VersionFilter;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactSet extends DataType  implements ResourceCollection {

    public AndFilter filter = new AndFilter();
    public String id;
    public File src;
    public Expand expand;

    public List<ArtifactFile> artifacts;
    public List<ArtifactSet> artifactSets;
    public Includes includes;
    public Excludes excludes;

    protected Collection<Node> nodes;


    public void setGroupId(String groupId) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new GroupIdFilter(groupId));
    }

    public void setArtifactId(String artifactId) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new ArtifactIdFilter(artifactId));
    }

    public void setVersion(String version) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new VersionFilter(version));
    }

    public void setClassifier(String classifier) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new ClassifierFilter(classifier));
    }

    public void setType(String type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new TypeFilter(type));
    }

    public void setScope(String scope) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new ScopeFilter(scope));
    }

    public void setOptional(boolean isOptional) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new IsOptionalFilter(isOptional));
    }

    public void setPattern(String pattern) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFiltersFromPattern(pattern);
    }

    public void setAncestor(String ancestor) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new AncestorFilter(ancestor));
    }

    public void setId(String id) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.id = id;
    }

    public void setSrc(File importFile) {
        this.src = importFile;
    }

    public void addExpand(Expand expand) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        this.expand = expand;
    }

    public void addArtifact(ArtifactFile artifact) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (artifacts == null) {
            artifacts = new ArrayList<ArtifactFile>();
        }
        artifacts.add(artifact);
    }

    public void addArtifactSet(ArtifactSet set) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (artifactSets == null) {
            artifactSets = new ArrayList<ArtifactSet>();
        }
        artifactSets.add(set);
    }

    public void addIncludes(Includes includes) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.includes != null) {
            throw new BuildException("Found an Includes that is defined more than once in an artifactSet");
        }
        this.includes = includes;
    }

    public void addExcludes(Excludes excludes) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.excludes != null) {
            throw new BuildException("Found an Excludes that is defined more than once in an artifactSet");
        }
        this.excludes =  excludes;
    }

    @Override
    public void setRefid(Reference ref) {
        super.setRefid(ref);
    }

    protected ArtifactSet getRef(Project p) {
        return (ArtifactSet) getCheckedRef(p);
    }

    protected List<Node> createInputNodeList() {
        if (includes == null && excludes == null) {
            return new ArrayList<Node>();
        }
        final AndFilter ieFilter = new AndFilter();
        if (includes != null) {
            ieFilter.addFilter(includes.filter);
        }
        if (excludes != null) {
            ieFilter.addFilter(excludes.filter);
        }
        return new ArrayList<Node>() {
            private static final long serialVersionUID = 1L;
            Filter f = CompositeFilter.compact(ieFilter);
            @Override
            public boolean add(Node node) {
                if (!f.accept(node.getArtifact())) {
                    return false;
                }
                return super.add(node);
            }
            @Override
            public boolean addAll(Collection<? extends Node> c) {
                for (Node node : c) {
                    if (f.accept(node.getArtifact())) {
                        super.add(node);
                    }
                }
                return true;
            }
        };
    }

    protected Filter buildFilter() {
        AndFilter f = new AndFilter();
        if (!filter.isEmpty()) {
            f.addFilters(filter.getFilters());
        }
        if (includes != null) {
            f.addFilter(includes.filter);
        }
        if (excludes != null) {
            f.addFilter(excludes.filter);
        }
        return f.isEmpty() ? Filter.ANY : CompositeFilter.compact(f);
    }

    protected Collection<Node> computeNodes() {
        Graph graph = MavenClientFactory.getInstance().getGraph();
        Filter finalFilter = buildFilter();
        Collection<Node> roots = new ArrayList<Node>();
        if (src != null) {
            collectImportedNodes(roots);
        }
        if (artifacts != null) {
            for (ArtifactFile arti : artifacts) {
                roots.add(arti.getNode());
            }
        }
        if (artifactSets != null) {
            for (ArtifactSet arti : artifactSets) {
                roots.addAll(arti.getNodes());
            }
        }
        if (roots.isEmpty()) {
            roots = graph.getNodes();
        }

        if (finalFilter != Filter.ANY) {
            ArrayList<Node> result = new ArrayList<Node>();
            for (Node node : roots) {
                if (finalFilter.accept(node.getArtifact())) {
                    result.add(node);
                }
            }
            roots = result;
        }
        if (expand != null) {
            ArrayList<Node> nodes = new ArrayList<Node>();
            if (expand.filter != null) {
                Filter filter = CompositeFilter.compact(expand.filter);
                for (Node root : roots) {
                    collectNodes(nodes, root, filter, expand.depth);
                }
            } else {
                for (Node root : roots) {
                    collectNodes(nodes, root, expand.depth);
                }
            }
        }
        return roots;
    }


    public Collection<Node> getNodes() {
        if (isReference()) {
            return getRef(getProject()).getNodes();
        }
        if (nodes == null) {
            nodes = computeNodes();
        }
        if (id != null) { // avoid caching if artifactSet is referencable
            Collection<Node> copy = nodes;
            nodes = null;
            return copy;
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

    public void collectImportedNodes(Collection<Node> nodes) {
        try {
            ArtifactSetParser parser = new ArtifactSetParser(getProject());
            parser.parse(src, nodes);
        } catch (IOException e) {
            throw new BuildException("Failed to import artifacts file: "+src, e);
        }
    }

    public static class NodeFilesIterator implements Iterator<FileResource> {
        protected Iterator<Node> it;
        public NodeFilesIterator(Iterator<Node> it) {
            this.it = it;
        }
        public boolean hasNext() {
            return it.hasNext();
        }
        public FileResource next() {
            File f = it.next().getFile();
            FileResource fr = new FileResource(f);
            fr.setBaseDir(f.getParentFile());
            return fr;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    public static void collectNodes(Collection<Node> nodes, Node node, Filter filter, int depth) {
        nodes.add(node);
        if (depth > 0) {
            depth--;
            for (Edge edge : node.getEdgesOut()) {
                if (filter.accept(edge)) {
                    collectNodes(nodes, edge.dst, filter, depth);
                }
            }
        }
    }

    public static void collectNodes(Collection<Node> nodes, Node node, int depth) {
        nodes.add(node);
        if (depth > 0) {
            depth--;
            for (Edge edge : node.getEdgesOut()) {
                collectNodes(nodes, edge.dst, depth);
            }
        }
    }

}
