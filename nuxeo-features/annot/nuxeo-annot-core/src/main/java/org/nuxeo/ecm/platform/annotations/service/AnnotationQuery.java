/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

public class AnnotationQuery {

    private final AnnotationManager manager = new AnnotationManager();

    public List<Annotation> getAnnotationsForURIs(URI uri, Graph graph) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . " //
                + "?s <" + AnnotationsConstants.A_ANNOTATES + "> <" + uri.toString() + "> . }";
        QueryResult results = graph.query(query, "sparql", null);
        Map<String, List<Statement>> mapann = new HashMap<String, List<Statement>>();
        for (Map<String, Node> map : results.getResults()) {
            Node subject = map.get("s");
            Node predicate = map.get("p");
            Node object = map.get("o");
            List<Statement> statements = mapann.get(subject.toString());
            if (statements == null) {
                statements = new ArrayList<Statement>();
                mapann.put(subject.toString(), statements);
            }
            statements.add(new StatementImpl(subject, predicate, object));
        }
        for (List<Statement> stats : mapann.values()) {
            annotations.add(manager.getAnnotation(stats));
        }
        return annotations;
    }

    public int getAnnotationsCountForURIs(URI uri, Graph graph) {
        String query = "SELECT ?s WHERE { ?s <" + AnnotationsConstants.A_ANNOTATES + "> <" + uri.toString() + "> }";
        return graph.queryCount(query, "sparql", null);
    }

}
