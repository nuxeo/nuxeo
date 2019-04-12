/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 * Graphs have to be serializable has they will be kept as references in the RelationService bean.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface Graph extends Serializable {

    /**
     * Sets the graph description.
     */
    void setDescription(GraphDescription graphDescription);

    /**
     * Returns namespaces for the graph.
     * <p>
     * Namespaces are prefix/namespace bindings, as rdf for http://www.w3.org/1999/02/22-rdf-syntax-ns#.
     *
     * @return namespaces map of namespace bindings for the graph
     */
    Map<String, String> getNamespaces();

    /**
     * Adds the statement object to the graph.
     *
     * @param statement statement to add
     * @since 5.5
     */
    void add(Statement statement);

    /**
     * Adds given list of Statement objects to the graph.
     *
     * @param statements list of Statement instances to add
     */
    void add(List<Statement> statements);

    /**
     * Removes the statement object from the graph.
     *
     * @param statement statement to remove
     * @since 5.5
     */
    void remove(Statement statement);

    /**
     * Removes given list of Statement objects from the graph.
     *
     * @param statements List of Statement instances to remove
     */
    void remove(List<Statement> statements);

    /**
     * Returns all statements in the graph.
     *
     * @return list of Statement instances
     */
    List<Statement> getStatements();

    /**
     * Returns all statements in the graph matching the pattern.
     *
     * @param statement pattern to match, can hold null nodes as wildcards
     * @return list of Statement instances matching the pattern
     */
    List<Statement> getStatements(Statement statement);

    /**
     * Returns all statements in the graph matching the pattern.
     *
     * @return list of Statement instances matching the pattern
     * @since 5.5
     */
    List<Statement> getStatements(Node subject, Node predicate, Node object);

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

    /**
     * Counts the number of results of a query.
     *
     * @param queryString the query string
     * @param language the query language (sparql, rdql,...)
     * @param baseURI the base URI to use for query
     * @return the count
     */
    int queryCount(String queryString, String language, String baseURI);

    // I/O

    /**
     * Parses source into the graph.
     *
     * @param path path on file system where to take the serialization file
     * @param lang format for the input serialization, may be "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The
     *            default value, represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute uris, may be null. If set to "", allows
     *            relative uris to be used in the model.
     * @return true on success, else false
     */
    boolean read(String path, String lang, String base);

    /**
     * Parses source into the graph.
     *
     * @param in input stream
     * @param lang format for the input serialization, may be "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The
     *            default value, represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute uris, may be null. If set to "", allows
     *            relative uris to be used in the model.
     * @return true on success, else false
     */
    boolean read(InputStream in, String lang, String base);

    /**
     * Serializes graph.
     *
     * @param path path on file system where to put the serialization file
     * @param lang format for the input serialization, may be "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The
     *            default value, represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute uris, may be null. If set to "", allows
     *            relative uris to be used in the model.
     * @return true on success, else false
     */
    boolean write(String path, String lang, String base);

    /**
     * Serializes graph.
     *
     * @param out output stream
     * @param lang format for the input serialization, may be "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The
     *            default value, represented by null, is "RDF/XML".
     * @param base base uri to be used when converting relative uris to absolute uris, may be null. If set to "", allows
     *            relative uris to be used in the model.
     * @return true on success, else false
     */
    boolean write(OutputStream out, String lang, String base);

}
