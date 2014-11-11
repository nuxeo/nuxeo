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
 * $Id: Graph.java 25079 2007-09-18 14:49:05Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Interface for graphs.
 * <p>
 * New types of graphs will be registered using extension points.
 * <p>
 * Graphs have to be serializable has they will be kept as references in the
 * RelationService bean.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface Graph extends Serializable {

    /**
     * Sets name for the graph.
     *
     * @param name the new name
     */
    void setName(String name);

    /**
     * Sets options for the graph.
     * <p>
     * Options are typically: which backend to use, additional parameters for backend
     * (e.g. for instance SQL backend, host, port, user/password...).
     *
     * @param options map of options for the graph
     */
    void setOptions(Map<String, String> options);

    /**
     * Sets namespaces for the graph.
     * <p>
     * Namespaces are prefix/namespace bindings, as rdf for
     * http://www.w3.org/1999/02/22-rdf-syntax-ns#.
     *
     * TODO AT: maybe share same namespaces with the relation service?
     *
     * @param namespaces map of namespace bindings for the graph
     */
    void setNamespaces(Map<String, String> namespaces);

    /**
     * Returns namespaces for the graph.
     * <p>
     * Namespaces are prefix/namespace bindings, as rdf for
     * http://www.w3.org/1999/02/22-rdf-syntax-ns#.
     *
     * @returns namespaces map of namespace bindings for the graph
     */
    Map<String, String> getNamespaces();

    /**
     * Adds given list of Statement objects to the graph.
     * <p>
     * If the graph has reification support, the statement properties are also
     * added to the graph too.
     *
     * @param statements list of Statement instances to add
     */
    void add(List<Statement> statements);

    /**
     * Removes given list of Statement objects from the graph.
     * <p>
     * If the graph has reification support, the deleted statements properties
     * are removed too.
     *
     * @param statements List of Statement instances to remove
     */
    void remove(List<Statement> statements);

    /**
     * Returns all statements in the graph.
     * <p>
     * If the graph has reification support, the statements properties are
     * retrieved too.
     *
     * @return list of Statement instances
     */
    List<Statement> getStatements();

    /**
     * Returns all statements in the graph matching the pattern.
     * <p>
     * If the graph has reification support, the statements properties are
     * retrieved too.
     *
     * @param statement pattern to match, can hold null nodes as wildcards
     * @return list of Statement instances matching the pattern
     */
    List<Statement> getStatements(Statement statement);

    /**
     * Get items matching the statement pattern (null, predicate, object).
     *
     * @param predicate predicate pattern, null accepted
     * @param object object pattern, null accepted
     * @return list of subjects
     */
    List<Node> getSubjects(Node predicate, Node object);

    /**
     * Gets items matching the statement pattern (subject, null, object).
     *
     * @param subject subject pattern, null accepted
     * @param object object pattern, null accepted
     * @return list of predicates
     */
    List<Node> getPredicates(Node subject, Node object);

    /**
     * Gets items matching the statement pattern (subject, predicate, null).
     *
     * @param subject subject pattern, null accepted
     * @param predicate predicate pattern, null accepted
     * @return list of node objects
     */
    List<Node> getObjects(Node subject, Node predicate);

    /**
     * Returns true if given statement pattern is in the graph.
     *
     * @param statement statement pattern, can use null as wild cards
     * @return true or false
     */
    boolean hasStatement(Statement statement);

    /**
     * Returns true if given resource appears in any statement of the graph.
     *
     * @param resource
     * @return true or false
     */
    boolean hasResource(Resource resource);

    /**
     * Returns the number of statements in the graph.
     *
     * @return number of statements as a Long
     */
    Long size();

    /**
     * Clears the graph, removing all statements in it.
     */
    void clear();

    /**
     * Query the graph using a base URI.
     *
     * @param queryString the query string
     * @param language the query language (sparql, rdql,...)
     * @param baseURI the base URI to use for query
     * @return QueryResult instance
     */
    QueryResult query(String queryString, String language, String baseURI);

    // I/O

    /**
     * Parses source into the graph.
     *
     * @param path path on file system where to take the serialization file
     * @param lang format for the input serialization, may be "RDF/XML",
     *            "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The default value,
     *            represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute
     *            uris, may be null. If set to "", allows relative uris to be
     *            used in the model.
     * @return true on success, else false
     */
    boolean read(String path, String lang, String base);

    /**
     * Parses source into the graph.
     *
     * @param in input stream
     * @param lang format for the input serialization, may be "RDF/XML",
     *            "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The default value,
     *            represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute
     *            uris, may be null. If set to "", allows relative uris to be
     *            used in the model.
     * @return true on success, else false
     */
    boolean read(InputStream in, String lang, String base);

    /**
     * Serializes graph.
     *
     * @param path path on file system where to put the serialization file
     * @param lang format for the input serialization, may be "RDF/XML",
     *            "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The default value,
     *            represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute
     *            uris, may be null. If set to "", allows relative uris to be
     *            used in the model.
     * @return true on success, else false
     */
    boolean write(String path, String lang, String base);

    /**
     * Serializes graph.
     *
     * @param out output stream
     * @param lang format for the input serialization, may be "RDF/XML",
     *            "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The default value,
     *            represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute
     *            uris, may be null. If set to "", allows relative uris to be
     *            used in the model.
     * @return true on success, else false
     */
    boolean write(OutputStream out, String lang, String base);

}
