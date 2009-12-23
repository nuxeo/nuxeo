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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DummyGraphType.java 25534 2007-09-28 11:40:25Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;

@SuppressWarnings({ "PublicField" })
public class DummyGraphType implements Graph {

    private static final long serialVersionUID = 1L;

    public String name;

    public String backend;

    public String host;

    public String port;

    public final Map<String, String> namespaces = new HashMap<String, String>();

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces.putAll(namespaces);
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

    public void remove(List<Statement> statements) {
    }

}
