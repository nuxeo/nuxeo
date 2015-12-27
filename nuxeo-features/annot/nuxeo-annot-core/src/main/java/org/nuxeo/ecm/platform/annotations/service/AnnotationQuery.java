/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
