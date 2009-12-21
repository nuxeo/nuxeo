/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IORelationGraphHelper.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.jena.JenaGraph;

/**
 * Relation graph importer/exporter.
 * <p>
 * relies on Jena memory graphs to perform serialization and deserialization of
 * memory graphs.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class IORelationGraphHelper {

    protected final Map<String, String> namespaces;

    protected List<Statement> statements;

    public IORelationGraphHelper(Map<String, String> namespaces,
            List<Statement> statements) {
        this.namespaces = namespaces;
        this.statements = statements;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    protected Graph createMemoryGraph() {
        JenaGraph graph = new JenaGraph();
        graph.setNamespaces(namespaces);
        return graph;
    }

    public void write(OutputStream out) {
        Graph graph = createMemoryGraph();
        if (statements != null) {
            graph.add(statements);
        }
        graph.write(out, null, null);
    }

    public void read(InputStream in) {
        Graph graph = createMemoryGraph();
        if (statements != null) {
            graph.add(statements);
        }
        graph.read(in, null, null);
        // update current statements with new ones
        statements = graph.getStatements();
    }

    public Graph getGraph() {
        Graph graph = createMemoryGraph();
        if (statements != null) {
            graph.add(statements);
        }
        return graph;
    }

}
