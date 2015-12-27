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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DummyGraphType.java 25534 2007-09-28 11:40:25Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;

public class DummyGraphType implements Graph {

    private static final long serialVersionUID = 1L;

    public String name;

    public String backend;

    public String host;

    public String port;

    public Map<String, String> namespaces;

    @Override
    public void setDescription(GraphDescription graphDescription) {
        name = graphDescription.getName();
        namespaces = graphDescription.getNamespaces();
        setOptions(graphDescription.getOptions());
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setOptions(Map<String, String> options) {
        for (Map.Entry<String, String> option : options.entrySet()) {
            String key = option.getKey();
            String value = option.getValue();
            if (key.equals("backend")) {
                this.backend = value;
            } else if (key.equals("host")) {
                this.host = value;
            } else if (key.equals("port")) {
                this.port = value;
            }
        }
    }

    @Override
    public void add(Statement statement) {
    }

    public void add(List<Statement> statements) {
    }

    public void clear() {
    }

    public List<Node> getObjects(Node subject, Node predicate) {
        return null;
    }

    public List<Node> getPredicates(Node subject, Node object) {
        return null;
    }

    public Long size() {
        return null;
    }

    public List<Statement> getStatements() {
        return null;
    }

    public List<Statement> getStatements(Node subject, Node predicate, Node object) {
        return null;
    }

    public List<Statement> getStatements(Statement statement) {
        return null;
    }

    public List<Node> getSubjects(Node predicate, Node object) {
        return null;
    }

    public boolean hasResource(Resource resource) {
        return false;
    }

    public boolean hasStatement(Statement statement) {
        return false;
    }

    public QueryResult query(String queryString, String language, String baseURI) {
        return null;
    }

    @Override
    public int queryCount(String queryString, String language, String baseURI) {
        return 0;
    }

    public boolean read(InputStream in, String lang, String base) {
        return false;
    }

    public boolean read(String path, String lang, String base) {
        return false;
    }

    public boolean write(OutputStream out, String lang, String base) {
        return false;
    }

    public boolean write(String path, String lang, String base) {
        return false;
    }

    @Override
    public void remove(Statement statement) {
    }

    public void remove(List<Statement> statements) {
    }

}
