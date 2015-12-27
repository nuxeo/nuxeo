/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.api;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

public class AnnotationImpl implements Annotation, Serializable {

    private static final long serialVersionUID = 1L;

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    private Graph graph;

    @Override
    public Resource getSubject() {
        QueryResult result = graph.query("SELECT ?s WHERE {?s ?p ?o}", "sparql",
                null);
        List<Map<String, Node>> results = result.getResults();
        Node node = results.get(0).get("s");
        return node.isBlank() ? null : (Resource) node;
    }

    @Override
    public void setBody(Statement body) {
        graph.add(body);
    }

    @Override
    public URI getAnnotates() {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <" + AnnotationsConstants.A_ANNOTATES + "> ?o}",
                "sparql", null);
        List<Map<String, Node>> results = result.getResults();
        if (results.isEmpty()) {
            return null;
        }
        Node node = results.get(0).get("o");
        try {
            return node.isBlank() ? null : new URI(((Resource) node).getUri());
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public URI getBody() {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <" + AnnotationsConstants.A_BODY + "> ?o}", "sparql",
                null);
        List<Map<String, Node>> results = result.getResults();
        if (results.isEmpty()) {
            return null;
        }
        Node node = results.get(0).get("o");
        try {
            return node.isBlank() ? null : new URI(((Resource) node).getUri());
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getBodyAsText() {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <" + AnnotationsConstants.A_BODY + "> ?o}", "sparql",
                null);
        List<Map<String, Node>> results = result.getResults();
        if (results.isEmpty()) {
            return null;
        }
        Node node = results.get(0).get("o");
        if (node.isLiteral()) {
            Literal literal = (Literal) node;
            return literal.getValue();
        }
        if (node.isResource()) {
            Resource resource = (Resource) node;
            return resource.getUri();
        }
        return null;
    }

    @Override
    public void setBodyText(String text) {
        QueryResult result = graph.query("SELECT ?s WHERE {?s <" + AnnotationsConstants.A_BODY + "> ?o}", "sparql",
                null);
        Node s = result.getResults().get(0).get("s");
        Resource p = new ResourceImpl(AnnotationsConstants.A_BODY);
        graph.remove(graph.getStatements(new StatementImpl(s, p, null)));
        Literal o = new LiteralImpl(text);
        Statement newStatement = new StatementImpl(s, p, o);
        graph.add(newStatement);
    }

    @Override
    public String getContext() {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <" + AnnotationsConstants.A_CONTEXT + "> ?o}", "sparql",
                null);
        List<Map<String, Node>> results = result.getResults();
        if (results.isEmpty()) {
            return null;
        }
        Node node = results.get(0).get("o");
        return node.isBlank() ? null : ((Literal) node).getValue();
    }

    @Override
    public void setContext(Statement context) {
        graph.add(context);
    }

    @Override
    public List<Statement> getStatements() {
        return graph.getStatements();
    }

    @Override
    public void setStatements(List<Statement> statements) {
        graph.add(statements);
    }

    @Override
    public void setSubject(Resource resource) {
        List<Statement> statements = new ArrayList<Statement>();
        for (Statement statement : graph.getStatements()) {
            statement.setSubject(resource);
            statements.add(statement);
        }
        graph.clear();
        graph.add(statements);
    }

    @Override
    public void setAnnotates(Statement statement) {
        graph.add(statement);
    }

    @Override
    public String getCreator() {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <" + AnnotationsConstants.D_CREATOR + "> ?o .}",
                "sparql", null);
        if (result.getCount() == 0) {
            return null;
        }
        Node node = result.getResults().get(0).get("o");
        return node.isBlank() ? null : ((Literal) node).getValue();
    }

    @Override
    public void addMetadata(String predicate, String value) {
        Statement statement = new StatementImpl(getSubject(), new ResourceImpl(predicate), new LiteralImpl(value));
        graph.add(statement);
    }

    @Override
    public String getId() {
        Resource subject = getSubject();
        String uri = subject.getUri().toString();
        return uri.substring(uri.lastIndexOf(":"));
    }
}
