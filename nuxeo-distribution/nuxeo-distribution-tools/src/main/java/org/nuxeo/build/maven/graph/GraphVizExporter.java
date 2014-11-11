/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.build.maven.graph;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.build.util.IOUtils;

/**
 * Generates a dependency diagram by using GraphViz.
 * @author Bogdan Stefanescu
 */
public class GraphVizExporter extends AbstractGraphVisitor {

    protected final PrintWriter out;
    protected final Map<Object/*Node,Edge*/,String> colors = new HashMap<Object,String>();
    protected String nodeColor = "black";
    protected String edgeColor = "black";


    /**
     * Unique IDs given to GraphViz for each node.
     */
    private Map<Node,String> ids = new HashMap<Node, String>();

    public GraphVizExporter(PrintWriter out) {
        this.out = out;
        out.println("digraph G {");
    }

    public GraphVizExporter(OutputStream out) {
        this(new PrintWriter(out));
    }

    public void setDefaultEdgeColor(String edgeColor) {
        this.edgeColor = edgeColor;
    }

    public void setDefaultNodeColor(String nodeColor) {
        this.nodeColor = nodeColor;
    }


    public void setColors(Collection<Node> nodes, final String nodeColor) {
        for (Node node : nodes) {
            colors.put(node, nodeColor);
        }
    }

    /**
     * Paint all edges and nodes that belong to the given subgraph by using the specified color.
     */
    public void setColors(Collection<Node> nodes, final String nodeColor, final String edgeColor) {
        for (Node node : nodes) {
            if (nodeColor != null) {
                colors.put(node, nodeColor);
            }
            if (edgeColor != null) {
                for (Edge edge : node.getEdgesOut()) {
                    colors.put(edge, edgeColor);
                }
            }
        }
    }

    public void setColor(Object key, String color) {
        colors.put(key, color);
    }

    public String getEdgeColor(Edge edge) {
        String color = colors.get(edge);
        return color != null ? color : edgeColor;
    }

    public String getNodeColor(Node node) {
        String color = colors.get(node);
        return color != null ? color : nodeColor;
    }

    public void close() {
        out.println("}");
        out.close();
    }

    public boolean visitEdge(Edge edge) {
        Map<String,String> attrs = new HashMap<String, String>();

        if(!"compile".equals(edge.scope))   // most of dependencies are compile, so skip them for brevity
            attrs.put("label",edge.scope);
        if(edge.isOptional)
            attrs.put("style","dotted");
        attrs.put("color", getEdgeColor(edge));
        if(edge.src.artifact.getGroupId().equals(edge.dst.artifact.getGroupId()))
            attrs.put("weight","10");

        out.printf("%s -> %s ", id(edge.src), id(edge.dst));
        writeAttributes(attrs);
        return true;
    }

    public boolean visitNode(Node node) {
        Map<String,String> attrs = new HashMap<String, String>();
        attrs.put("label",node.artifact.getGroupId()+':'+node.artifact.getArtifactId());
        attrs.put("color", getNodeColor(node));

        out.print(id(node)+' ');
        writeAttributes(attrs);
        return true;
    }

    private void writeAttributes(Map<String,String> attributes) {
        out.print('[');
        boolean first=true;
        for (Map.Entry<String,String> e : attributes.entrySet()) {
            if(e.getValue()==null)  continue;   // skip

            out.printf("%s=\"%s\"",e.getKey(),e.getValue());
            if(!first)
                out.print(',');
            else
                first = false;
        }
        out.println("];");
    }

    private String id(Node n) {
        String id = ids.get(n);
        if(id==null) {
            id = "n"+ids.size();
            ids.put(n,id);
        }
        return id;
    }

    public void process(Graph graph, OutputStream out) {
        process(graph.getRoots());
    }

    public void process(Collection<Node> nodes, OutputStream out) throws IOException {
        GraphVizExporter viz = GraphVizExporter.createPng(out);
        super.process(nodes);
        viz.close();
    }


    /**
     * Returns a {@link GraphVizExporter} that generates a PNG file.
     */
    public static GraphVizExporter createPng(final OutputStream out) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("/usr/local/bin/dot","-Tpng");
        final Process proc = pb.start();

        final Thread stdoutCopier = new Thread() {
            public void run() {
                try {
                    try {
                        IOUtils.copy(proc.getInputStream(), out);
                    } finally {
                        IOUtils.safeClose(out);
                    }
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        };

        stdoutCopier.start();

        // copy stderr
        new Thread() {
            public void run() {
                try {
                    IOUtils.copy(proc.getErrorStream(), System.err);
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }.start();

        return new GraphVizExporter(proc.getOutputStream()) {
            @Override
            public void close() {
                super.close();
                try {
                    stdoutCopier.join();
                } catch (InterruptedException e) {
                    // handle interruption later
                    Thread.currentThread().interrupt();
                }
            }
        };
    }
}
