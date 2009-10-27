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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.MavenClientFactory;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.GraphVizExporter;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GraphExportTask extends Task {

    public File file;
    public String nodeColor;
    public String edgeColor;
    public List<SubGraph> graphs = new ArrayList<SubGraph>();

    public void setFile(File file) {
        this.file = file;
    }

    public void addGraph(SubGraph graph) {
        graphs.add(graph);
    }

    public void setNodeColor(String color) {
        nodeColor = color;
    }

    public void setEdgeColor(String color) {
        edgeColor = color;
    }

    @Override
    public void execute() throws BuildException {
        try {
            String name = file.getName();
            if (name.endsWith(".png")) {
                exportPng();
            } else if (name.endsWith(".txt")) {

            } else if (name.endsWith(".xml")) {

            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public void exportPng() throws Exception {
        Graph graph = MavenClientFactory.getInstance().getGraph();
        FileOutputStream out = new FileOutputStream(file);
        GraphVizExporter gv = GraphVizExporter.createPng(out);
        if (nodeColor != null) {
            gv.setDefaultNodeColor(nodeColor);
        }
        if (edgeColor != null) {
            gv.setDefaultEdgeColor(nodeColor);
        }
        if (graphs.isEmpty()) {
            gv.process(graph);
        } else {
            Set<Node> nodes = new HashSet<Node>();
            for (SubGraph subg : graphs) {
                Collection<Node> subnodes = subg.getNodes();
                if (subg.nodeColor != null || subg.edgeColor != null) {
                    gv.setColors(subnodes, subg.nodeColor, subg.edgeColor);
                }
                nodes.addAll(subnodes);
            }
            gv.process(nodes);
        }
        gv.close();
    }

    public void exportXml() throws Exception {

    }

    public void exportText() throws Exception {

    }

}
