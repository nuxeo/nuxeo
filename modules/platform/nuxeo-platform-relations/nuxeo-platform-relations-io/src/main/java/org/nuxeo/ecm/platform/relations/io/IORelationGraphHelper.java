/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * relies on Jena memory graphs to perform serialization and deserialization of memory graphs.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IORelationGraphHelper {

    protected final Map<String, String> namespaces;

    protected List<Statement> statements;

    public IORelationGraphHelper(Map<String, String> namespaces, List<Statement> statements) {
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
