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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

/**
 * @author Alexandre Russel
 */
public class AnnotationQuery {
    public static final String BASE_QUERY = "SELECT ?uri SELECT {}";

    private final AnnotationManager manager = new AnnotationManager();

    public List<Annotation> getAnnotationsForURIs(List<URI> uris, Graph graph)
            throws AnnotationException {
        return getAnnotationsForURIs(uris, graph, Collections.<String, String> emptyMap());
    }

    public List<Annotation> getAnnotationsForURIs(List<URI> uris, Graph graph,
            Map<String, String> filters) throws AnnotationException {
        List<Annotation> annotations = new ArrayList<Annotation>();
        final String baseQuery = " SELECT ?s ?p ?o WHERE { ?s ?p ?o . }";
        for (URI uri : uris) {
            final StringBuilder query = new StringBuilder(baseQuery);
            query.insert(query.lastIndexOf("}"), " ?s <"
                    + AnnotationsConstants.A_ANNOTATES + "> <" + uri.toString()
                    + "> . ");
            if (filters != null) {
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    query.insert(query.lastIndexOf("}"), " ?s <"
                            + entry.getKey() + "> <" + entry.getValue()
                            + "> . ");
                }
            }
            QueryResult results = graph.query(query.toString(), "sparql", null);
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
        }
        return annotations;
    }
}
