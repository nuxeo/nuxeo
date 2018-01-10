/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.relations.jena;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.relations.api.Blank;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.NodeFactory;
import org.nuxeo.ecm.platform.relations.api.impl.QueryResultImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.datasource.ConnectionHelper;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.Lock;

/**
 * Jena plugin for NXRelations.
 * <p>
 * Graph implementation using the <a href="http://jena.sourceforge.net/" target="_blank">Jena</a> framework.
 */
public class JenaGraph implements Graph {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(JenaGraph.class);

    // keep model in a private field for memory graph (only useful for tests ;
    // not thread safe)
    private transient Model memoryGraph;

    private String name;

    /**
     * Backend type, default is memory, other possible value is "sql".
     */
    private String backend = "memory";

    /**
     * Database-related options, see http://jena.sourceforge.net/DB/options.html.
     */
    private String datasource;

    private String databaseType;

    private boolean databaseDoCompressUri;

    private boolean databaseTransactionEnabled;

    private Map<String, String> namespaces = new HashMap<String, String>();

    /**
     * Class holding graph and connection so that we can close the connection after having used the graph.
     * <p>
     * It can hold the jena connection or the base connection (built from a datasource).
     */
    protected static final class GraphConnection {

        private final Connection baseConnection;

        private final DBConnection connection;

        private final Model graph;

        GraphConnection(DBConnection connection, Model graph) {
            baseConnection = null;
            this.connection = connection;
            this.graph = graph;
        }

        GraphConnection(Connection baseConnection, Model graph) {
            this.baseConnection = baseConnection;
            connection = null;
            this.graph = graph;
        }

        public Model getGraph() {
            return graph;
        }

        protected void close() {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("Could not close connection");
                }
            }
            if (baseConnection != null) {
                try {
                    baseConnection.close();
                } catch (SQLException e) {
                    log.error("Could not close base connection");
                }
            }
        }
    }

    /**
     * Generates the Jena graph using options.
     *
     * @return the Jena graph (model)
     */
    protected GraphConnection openGraph() {
        return openGraph(false);
    }

    /**
     * Gets the Jena graph using options.
     * <p>
     * The Jena "Convenient" reification style is used when opening models: it allows to ignore reification quadlets
     * when calling the statements list.
     *
     * @param forceReload boolean stating if the jena graph has to be reloaded using options
     * @return the Jena graph (model)
     */
    protected synchronized GraphConnection openGraph(boolean forceReload) {
        // create model given backend
        if (backend.equals("memory")) {
            if (memoryGraph == null || forceReload) {
                memoryGraph = ModelFactory.createDefaultModel(ModelFactory.Convenient);
                memoryGraph.setNsPrefixes(namespaces);
            }
            return new GraphConnection((Connection) null, memoryGraph);
        } else if (backend.equals("sql")) {
            if (datasource == null) {
                throw new IllegalArgumentException("Missing datasource for sql graph : " + name);
            }
            // create a database connection
            Connection baseConnection;
            try {
                baseConnection = ConnectionHelper.getConnection(datasource);
            } catch (SQLException e) {
                throw new IllegalArgumentException(String.format("SQLException while opening %s", datasource), e);
            }
            /*
             * We have to wrap the connection to disallow any commit() or setAutoCommit() on it. Jena calls these
             * methods without regard to the fact that the connection may be managed by an external transaction.
             */
            Connection wrappedConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class[] { Connection.class }, new ConnectionFixInvocationHandler(baseConnection));
            DBConnection connection = new DBConnection(wrappedConnection, databaseType);
            // check if named model already exists
            Model graph;
            if (connection.containsModel(name)) {
                ModelMaker m = ModelFactory.createModelRDBMaker(connection, ModelFactory.Convenient);
                graph = m.openModel(name);
            } else {
                // create it
                // check if other models already exist for that connection.
                if (connection.getAllModelNames().hasNext()) {
                    // other models already exist => do not set parameters
                    // on driver.
                    if (databaseDoCompressUri != connection.getDriver().getDoCompressURI()) {
                        log.warn(String.format("Cannot set databaseDoCompressUri attribute to %s "
                                + "for model %s, other models already " + "exist with value %s", databaseDoCompressUri,
                                name, connection.getDriver().getDoCompressURI()));
                    }
                    if (databaseTransactionEnabled != connection.getDriver().getIsTransactionDb()) {
                        log.warn(String.format("Cannot set databaseTransactionEnabled attribute to %s "
                                + "for model %s, other models already " + "exist with value %s",
                                databaseTransactionEnabled, name, connection.getDriver().getIsTransactionDb()));
                    }
                } else {
                    if (databaseDoCompressUri) {
                        connection.getDriver().setDoCompressURI(true);
                    }
                    if (databaseTransactionEnabled) {
                        connection.getDriver().setIsTransactionDb(true);
                    }
                }
                ModelMaker m = ModelFactory.createModelRDBMaker(connection, ModelFactory.Convenient);
                graph = m.createModel(name);
            }
            graph.setNsPrefixes(namespaces);
            // use baseConnection so that it is closed instead of the jena one
            // (to let the pool handled closure).
            if (baseConnection != null) {
                return new GraphConnection(baseConnection, graph);
            }
            return new GraphConnection(connection, graph);
        } else {
            throw new IllegalArgumentException("Unknown backend type " + backend);
        }
    }

    /**
     * Gets the Jena node for given NXRelations Node instance.
     *
     * @param nuxNode NXrelations Node instance
     * @return Jena node instance
     */
    private static com.hp.hpl.jena.graph.Node getJenaNode(Node nuxNode) {
        if (nuxNode == null) {
            return null;
        }

        com.hp.hpl.jena.graph.Node jenaNodeInst;
        if (nuxNode.isBlank()) {
            Blank blank = (Blank) nuxNode;
            String id = blank.getId();
            if (id == null) {
                jenaNodeInst = com.hp.hpl.jena.graph.Node.createAnon();
            } else {
                jenaNodeInst = com.hp.hpl.jena.graph.Node.createAnon(new AnonId(id));
            }
        } else if (nuxNode.isLiteral()) {
            Literal lit = (Literal) nuxNode;
            String value = lit.getValue();
            if (value == null) {
                throw new IllegalArgumentException(String.format("Invalid literal node %s", nuxNode));
            }
            String language = lit.getLanguage();
            String type = lit.getType();
            if (language != null) {
                jenaNodeInst = com.hp.hpl.jena.graph.Node.createLiteral(value, language, false);
            } else if (type != null) {
                jenaNodeInst = com.hp.hpl.jena.graph.Node.createLiteral(value, null, new BaseDatatype(type));
            } else {
                jenaNodeInst = com.hp.hpl.jena.graph.Node.createLiteral(value);
            }
        } else if (nuxNode.isResource()) {
            Resource resource = (Resource) nuxNode;
            String uri = resource.getUri();
            jenaNodeInst = com.hp.hpl.jena.graph.Node.createURI(uri);

        } else {
            throw new IllegalArgumentException(String.format("Invalid NXRelations node %s", nuxNode));
        }
        return jenaNodeInst;
    }

    /**
     * Gets NXRelations node instance given Jena node.
     *
     * @param jenaNodeInst
     * @return NXRelations node instance
     */
    private Node getNXRelationsNode(com.hp.hpl.jena.graph.Node jenaNodeInst) {
        if (jenaNodeInst == null) {
            return null;
        }
        Node nuxNode = null;
        if (jenaNodeInst.isBlank()) {
            AnonId anonId = jenaNodeInst.getBlankNodeId();
            String id = anonId.getLabelString();
            nuxNode = NodeFactory.createBlank(id);
        } else if (jenaNodeInst.isLiteral()) {
            LiteralLabel label = jenaNodeInst.getLiteral();
            String value = label.getLexicalForm();
            String type = jenaNodeInst.getLiteralDatatypeURI();
            String language = jenaNodeInst.getLiteralLanguage();
            if (StringUtils.isNotEmpty(type)) {
                nuxNode = NodeFactory.createTypedLiteral(value, type);
            } else if (StringUtils.isNotEmpty(language)) {
                nuxNode = NodeFactory.createLiteral(value, language);
            } else {
                nuxNode = NodeFactory.createLiteral(value);
            }
        } else if (jenaNodeInst.isURI()) {
            String uri = jenaNodeInst.getURI();
            // try to find corresponding prefix
            // TODO AT: maybe take namespaces from relation service?
            for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                String base = ns.getValue();
                if (uri.startsWith(base)) {
                    String localName = uri.substring(base.length());
                    nuxNode = NodeFactory.createQNameResource(base, localName);
                    break;
                }
            }
            if (nuxNode == null) {
                // default to resource
                nuxNode = NodeFactory.createResource(uri);
            }
        } else {
            throw new IllegalArgumentException("Cannot translate non concrete Jena node into NXRelations node");
        }
        return nuxNode;
    }

    /**
     * Gets Jena statement selector corresponding to the NXRelations statement.
     *
     * @param graph the jena graph
     * @param nuxStatement NXRelations statement
     * @return jena statement selector
     */
    private static SimpleSelector getJenaSelector(Model graph, Statement nuxStatement) {
        com.hp.hpl.jena.rdf.model.Resource subjResource = null;
        com.hp.hpl.jena.graph.Node subject = getJenaNode(nuxStatement.getSubject());
        if (subject != null && subject.isURI()) {
            subjResource = graph.getResource(subject.getURI());
        }
        Property predProp = null;
        com.hp.hpl.jena.graph.Node predicate = getJenaNode(nuxStatement.getPredicate());
        if (predicate != null && predicate.isURI()) {
            predProp = graph.getProperty(predicate.getURI());
        }
        com.hp.hpl.jena.graph.Node object = getJenaNode(nuxStatement.getObject());
        RDFNode objRDF = null;
        if (object != null) {
            objRDF = graph.asRDFNode(object);
        }
        return new SimpleSelector(subjResource, predProp, objRDF);
    }

    /**
     * Gets NXRelations statement corresponding to the Jena statement.
     * <p>
     * Reified statements may be retrieved from the Jena graph and set as properties on NXRelations statements.
     *
     * @param graph the jena graph
     * @param jenaStatement jena statement
     * @return NXRelations statement
     */
    private Statement getNXRelationsStatement(Model graph, com.hp.hpl.jena.rdf.model.Statement jenaStatement) {
        Node subject = getNXRelationsNode(jenaStatement.getSubject().asNode());
        Node predicate = getNXRelationsNode(jenaStatement.getPredicate().asNode());
        Node object = getNXRelationsNode(jenaStatement.getObject().asNode());
        Statement statement = new StatementImpl(subject, predicate, object);

        // take care of properties
        if (graph.isReified(jenaStatement)) {
            com.hp.hpl.jena.rdf.model.Resource reifiedStmt = graph.getAnyReifiedStatement(jenaStatement);
            StmtIterator it = reifiedStmt.listProperties();
            while (it.hasNext()) {
                com.hp.hpl.jena.rdf.model.Statement stmt = it.nextStatement();
                Node nuxNode = getNXRelationsNode(stmt.getPredicate().asNode());
                // ugly cast as a Resource
                Node value = getNXRelationsNode(stmt.getObject().asNode());
                statement.addProperty((Resource) nuxNode, value);
            }
        }

        return statement;
    }

    /**
     * Gets NXRelations statement list corresponding to the Jena statement list.
     *
     * @param graph the jena graph
     * @param jenaStatements jena statements list
     * @return NXRelations statements list
     */
    private List<Statement> getNXRelationsStatements(Model graph,
            List<com.hp.hpl.jena.rdf.model.Statement> jenaStatements) {
        List<Statement> nuxStmts = new ArrayList<Statement>();
        for (com.hp.hpl.jena.rdf.model.Statement jenaStmt : jenaStatements) {
            // NXP-2665: remove reified statements are they're as properties in
            // nuxeo logic
            if (!jenaStmt.getSubject().canAs(ReifiedStatement.class)) {
                nuxStmts.add(getNXRelationsStatement(graph, jenaStmt));
            }
        }
        return nuxStmts;
    }

    // Interface implementation

    @Override
    public void setDescription(GraphDescription graphDescription) {
        name = graphDescription.getName();
        setOptions(graphDescription.getOptions());
        setNamespaces(graphDescription.getNamespaces());
    }

    protected void setOptions(Map<String, String> options) {
        for (Map.Entry<String, String> option : options.entrySet()) {
            String key = option.getKey();
            String value = option.getValue();
            if (key.equals("backend")) {
                if (value.equals("memory") || value.equals("sql")) {
                    backend = value;
                } else {
                    throw new IllegalArgumentException(String.format("Unknown backend %s for Jena graph", value));
                }
            } else if (key.equals("datasource")) {
                datasource = value;
            } else if (key.equals("databaseType")) {
                databaseType = value;
            } else if (key.equals("databaseDoCompressUri")) {
                if (value.equals("true")) {
                    databaseDoCompressUri = true;
                } else if (value.equals("false")) {
                    databaseDoCompressUri = false;
                } else {
                    String format = "Illegal value %s for databaseDoCompressUri, must be true or false";
                    throw new IllegalArgumentException(String.format(format, value));
                }
            } else if (key.equals("databaseTransactionEnabled")) {
                if (value.equals("true")) {
                    databaseTransactionEnabled = true;
                } else if (value.equals("false")) {
                    databaseTransactionEnabled = false;
                } else {
                    String format = "Illegal value %s for databaseTransactionEnabled, must be true or false";
                    throw new IllegalArgumentException(String.format(format, value));
                }
            }
        }
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    @Override
    public void add(Statement statement) {
        add(Collections.singletonList(statement));
    }

    @Override
    public void add(List<Statement> statements) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.WRITE);
            for (Statement nuxStmt : statements) {
                com.hp.hpl.jena.graph.Node subject = getJenaNode(nuxStmt.getSubject());
                com.hp.hpl.jena.graph.Node predicate = getJenaNode(nuxStmt.getPredicate());
                com.hp.hpl.jena.graph.Node object = getJenaNode(nuxStmt.getObject());
                Triple jenaTriple = Triple.create(subject, predicate, object);
                com.hp.hpl.jena.rdf.model.Statement jenaStmt = graph.asStatement(jenaTriple);

                // properties
                Map<Resource, Node[]> properties = nuxStmt.getProperties();
                if (properties == null || properties.isEmpty()) {
                    // no properties
                    graph.add(jenaStmt);
                } else {
                    List<com.hp.hpl.jena.rdf.model.Statement> stmts = new ArrayList<com.hp.hpl.jena.rdf.model.Statement>();
                    stmts.add(jenaStmt);
                    // create reified statement if it does not exist
                    com.hp.hpl.jena.graph.Node reifiedStmt = graph.getAnyReifiedStatement(jenaStmt).asNode();
                    for (Map.Entry<Resource, Node[]> property : properties.entrySet()) {
                        com.hp.hpl.jena.graph.Node prop = getJenaNode(property.getKey());
                        for (Node node : property.getValue()) {
                            com.hp.hpl.jena.graph.Node value = getJenaNode(node);
                            Triple propTriple = Triple.create(reifiedStmt, prop, value);
                            stmts.add(graph.asStatement(propTriple));
                        }
                    }
                    graph.add(stmts);
                }
            }
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public void remove(Statement statement) {
        remove(Collections.singletonList(statement));
    }

    @Override
    public void remove(List<Statement> statements) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.WRITE);
            for (Statement nuxStmt : statements) {
                com.hp.hpl.jena.graph.Node subject = getJenaNode(nuxStmt.getSubject());
                com.hp.hpl.jena.graph.Node predicate = getJenaNode(nuxStmt.getPredicate());
                com.hp.hpl.jena.graph.Node object = getJenaNode(nuxStmt.getObject());
                Triple jenaTriple = Triple.create(subject, predicate, object);
                com.hp.hpl.jena.rdf.model.Statement jenaStmt = graph.asStatement(jenaTriple);
                graph.remove(jenaStmt);
                // remove properties
                RSIterator it = graph.listReifiedStatements(jenaStmt);
                while (it.hasNext()) {
                    ReifiedStatement rs = it.nextRS();
                    rs.removeProperties();
                }
                // remove quadlets
                graph.removeAllReifications(jenaStmt);
                // graph.removeReification(reifiedStmt);
            }
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public List<Statement> getStatements() {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            StmtIterator it = graph.listStatements();
            return getNXRelationsStatements(graph, it.toList());
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public List<Statement> getStatements(Node subject, Node predicate, Node object) {
        return getStatements(new StatementImpl(subject, predicate, object));
    }

    @Override
    public List<Statement> getStatements(Statement statement) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            SimpleSelector selector = getJenaSelector(graph, statement);
            StmtIterator it = graph.listStatements(selector);
            return getNXRelationsStatements(graph, it.toList());
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public List<Node> getSubjects(Node predicate, Node object) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            SimpleSelector selector = getJenaSelector(graph, new StatementImpl(null, predicate, object));
            ResIterator it = graph.listSubjectsWithProperty(selector.getPredicate(), selector.getObject());
            List<Node> res = new ArrayList<Node>();
            while (it.hasNext()) {
                res.add(getNXRelationsNode(it.nextResource().asNode()));
            }
            return res;
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public List<Node> getPredicates(Node subject, Node object) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            SimpleSelector selector = getJenaSelector(graph, new StatementImpl(subject, null, object));
            StmtIterator it = graph.listStatements(selector);
            List<Statement> statements = getNXRelationsStatements(graph, it.toList());
            List<Node> res = new ArrayList<Node>();
            for (Statement stmt : statements) {
                Node predicate = stmt.getPredicate();
                if (!res.contains(predicate)) {
                    // remove duplicates
                    res.add(predicate);
                }
            }
            return res;
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public List<Node> getObjects(Node subject, Node predicate) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            SimpleSelector selector = getJenaSelector(graph, new StatementImpl(subject, predicate, null));
            NodeIterator it = graph.listObjectsOfProperty(selector.getSubject(), selector.getPredicate());
            List<Node> res = new ArrayList<Node>();
            while (it.hasNext()) {
                res.add(getNXRelationsNode(it.nextNode().asNode()));
            }
            return res;
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public boolean hasStatement(Statement statement) {
        if (statement == null) {
            return false;
        }
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            SimpleSelector selector = getJenaSelector(graph, statement);
            return graph.contains(selector.getSubject(), selector.getPredicate(), selector.getObject());
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public boolean hasResource(Resource resource) {
        if (resource == null) {
            return false;
        }
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            com.hp.hpl.jena.graph.Node jenaNodeInst = getJenaNode(resource);
            RDFNode jenaNode = graph.asRDFNode(jenaNodeInst);
            return graph.containsResource(jenaNode);
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    /**
     * Returns the number of statements in the graph.
     * <p>
     * XXX AT: this size may not be equal to the number of statements retrieved via getStatements() because it counts
     * each statement property.
     *
     * @return integer number of statements in the graph
     */
    @Override
    public Long size() {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            return graph.size();
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public void clear() {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            graph.removeAll();
            // XXX AT: remove reification quadlets explicitly
            RSIterator it = graph.listReifiedStatements();
            List<ReifiedStatement> rss = new ArrayList<ReifiedStatement>();
            while (it.hasNext()) {
                rss.add(it.nextRS());
            }
            for (ReifiedStatement rs : rss) {
                graph.removeReification(rs);
            }
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public QueryResult query(String queryString, String language, String baseURI) {
        Model graph = null;
        GraphConnection graphConnection = null;
        QueryResult res = null;
        QueryExecution qe = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            log.debug(String.format("Running query %s", queryString));
            // XXX AT: ignore language for now
            if (language != null && !language.equals("sparql")) {
                log.warn(String.format("Unknown language %s for query, using SPARQL", language));
            }
            Query query = QueryFactory.create(queryString);
            query.setBaseURI(baseURI);
            qe = QueryExecutionFactory.create(query, graph);
            res = new QueryResultImpl(0, new ArrayList<String>(), new ArrayList<Map<String, Node>>());
            ResultSet jenaResults = qe.execSelect();
            Integer count = 0;
            List<String> variableNames = jenaResults.getResultVars();
            List<Map<String, Node>> nuxResults = new ArrayList<Map<String, Node>>();
            while (jenaResults.hasNext()) {
                QuerySolution soln = jenaResults.nextSolution();
                Map<String, Node> nuxSol = new HashMap<String, Node>();
                for (String varName : variableNames) {
                    RDFNode x = soln.get(varName);
                    nuxSol.put(varName, getNXRelationsNode(x.asNode()));
                }
                nuxResults.add(nuxSol);
                count++;
            }
            res = new QueryResultImpl(count, variableNames, nuxResults);
        } finally {
            if (qe != null) {
                // Important - free up resources used running the query
                qe.close();
            }
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
        return res;
    }

    @Override
    public int queryCount(String queryString, String language, String baseURI) {
        return query(queryString, language, baseURI).getResults().size();
    }

    @Override
    public boolean read(InputStream in, String lang, String base) {
        // XXX AT: maybe update namespaces in case some new appeared
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.READ);
            graph.read(in, base, lang);
            // default to true
            return true;
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public boolean read(String path, String lang, String base) {
        // XXX AT: maybe update namespaces in case some new appeared
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            return read(in, lang, base);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public boolean write(OutputStream out, String lang, String base) {
        Model graph = null;
        GraphConnection graphConnection = null;
        try {
            graphConnection = openGraph();
            graph = graphConnection.getGraph();
            graph.enterCriticalSection(Lock.WRITE);
            graph.write(out, lang, base);
            // default to true
            return true;
        } finally {
            if (graph != null) {
                graph.leaveCriticalSection();
            }
            if (graphConnection != null) {
                graphConnection.close();
            }
        }
    }

    @Override
    public boolean write(String path, String lang, String base) {
        OutputStream out = null;
        try {
            File file = new File(path);
            out = new FileOutputStream(file);
            return write(out, lang, base);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

}

/**
 * This invocation handler is designed to wrap a normal connection but avoid all calls to
 * <ul>
 * <li>{@link Connection#commit}</li>
 * <li>{@link Connection#setAutoCommit}</li>
 * </ul>
 * <p>
 * We have to do this because Jena calls these methods without regard to the fact that the connection may be managed by
 * an external transaction.
 *
 * @author Florent Guillaume
 */

class ConnectionFixInvocationHandler implements InvocationHandler {

    private final Connection connection;

    ConnectionFixInvocationHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String name = method.getName();
        if (name.equals("commit")) {
            return null;
        } else if (name.equals("setAutoCommit")) {
            return null;
        } else {
            try {
                return method.invoke(connection, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
