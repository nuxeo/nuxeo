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

package org.nuxeo.ecm.platform.annotations.api;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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
        QueryResult result = graph.query("SELECT ?s WHERE {?s <"
                + AnnotationsConstants.A_BODY + "> ?o}", "sparql", null);
        Node node = result.getResults().get(0).get("s");
        return node.isBlank() ? null : (Resource) node;
    }

    @Override
    public void setBody(Statement body) {
        graph.add(body);
    }

    @Override
    public URI getAnnotates() throws AnnotationException {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <"
                + AnnotationsConstants.A_ANNOTATES + "> ?o}", "sparql", null);
        Node node = result.getResults().get(0).get("o");
        try {
            return node.isBlank() ? null : new URI(((Resource) node).getUri());
        } catch (URISyntaxException e) {
            throw new AnnotationException(e);
        }
    }

    @Override
    public URI getBody() throws AnnotationException {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <"
                + AnnotationsConstants.A_BODY + "> ?o}", "sparql", null);
        Node node = result.getResults().get(0).get("o");
        try {
            return node.isBlank() ? null : new URI(((Resource) node).getUri());
        } catch (URISyntaxException e) {
            throw new AnnotationException(e);
        }
    }

    @Override
    public String getBodyAsText() {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <"
                + AnnotationsConstants.A_BODY + "> ?o}", "sparql", null);
        Node node = result.getResults().get(0).get("o");
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
        QueryResult result = graph.query("SELECT ?s WHERE {?s <"
                + AnnotationsConstants.A_BODY + "> ?o}", "sparql", null);
        Node s = result.getResults().get(0).get("s");
        Resource p = new ResourceImpl(AnnotationsConstants.A_BODY);
        graph.remove(graph.getStatements(new StatementImpl(s, p, null)));
        Literal o = new LiteralImpl(text);
        Statement newStatement = new StatementImpl(s, p, o);
        graph.add(newStatement);
    }

    @Override
    public String getContext() throws AnnotationException {
        QueryResult result = graph.query("SELECT ?o WHERE {?s <"
                + AnnotationsConstants.A_CONTEXT + "> ?o}", "sparql", null);
        Node node = result.getResults().get(0).get("o");
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
        QueryResult result = graph.query("SELECT ?o WHERE {?s <"
                + AnnotationsConstants.D_CREATOR + "> ?o .}", "sparql", null);
        if (result.getCount() == 0) {
            return null;
        }
        Node node = result.getResults().get(0).get("o");
        return node.isBlank() ? null : ((Literal) node).getValue();
    }

    @Override
    public void addMetadata(String predicate, String value) {
        Statement statement = new StatementImpl(getSubject(), new ResourceImpl(
                predicate), new LiteralImpl(value));
        graph.add(statement);
    }

    @Override
    public String getId() {
        Resource subject = getSubject();
        String uri = subject.getUri().toString();
        return uri.substring(uri.lastIndexOf(":"));
    }
}
