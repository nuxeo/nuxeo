/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.relations.api.Blank;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractNode;
import org.nuxeo.ecm.platform.relations.api.impl.NodeFactory;
import org.nuxeo.ecm.platform.relations.api.impl.QueryResultImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Relation graph implementation delegating to the core.
 */
public class CoreGraph implements Graph {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CoreGraph.class);

    public static final String OPTION_DOCTYPE = "doctype";

    public static final String REL_TYPE = "Relation";

    public static final String REL_PREDICATE = "relation:predicate";

    public static final String REL_SOURCE_ID = "relation:source";

    public static final String REL_SOURCE_URI = "relation:sourceUri";

    public static final String REL_TARGET_ID = "relation:target";

    public static final String REL_TARGET_URI = "relation:targetUri";

    public static final String REL_TARGET_STRING = "relation:targetString";

    public static final String DC_CREATED = "dc:created";

    public static final String DC_CREATOR = "dc:creator";

    public static final String DC_MODIFIED = "dc:modified";

    public static final String DC_TITLE = "dc:title";

    public static final String DC_DESCRIPTION = "dc:description";

    // avoid confusion with any legal uri
    public static final String BLANK_NS = "-:";

    public static final String DOCUMENT_NAMESPACE = RelationConstants.DOCUMENT_NAMESPACE;

    /** Without final slash (compat). */
    public static final String DOCUMENT_NAMESPACE2 = DOCUMENT_NAMESPACE.substring(0, DOCUMENT_NAMESPACE.length() - 1);

    /** Has no final slash (compat). */
    public static final String COMMENT_NAMESPACE = "http://www.nuxeo.org/comments/uid";

    public static final String[] DOC_NAMESPACES = { DOCUMENT_NAMESPACE, DOCUMENT_NAMESPACE2, COMMENT_NAMESPACE };

    protected static final List<Statement> EMPTY_STATEMENTS = Collections.emptyList();

    protected static final Statement ALL = new StatementImpl(null, null, null);

    protected CoreSession session;

    protected String name;

    protected String docType = REL_TYPE;

    public Map<String, String> namespaces;

    public List<String> namespaceList = Collections.emptyList();

    /** Only one of those is filled. */
    protected static class NodeAsString {
        public String id;

        public String uri;

        public String string;
    }

    /**
     * A graph with this base session. An unrestricted session will be opened based on it.
     */
    public CoreGraph(CoreSession session) {
        this.session = session;
    }

    @Override
    public void setDescription(GraphDescription graphDescription) {
        name = graphDescription.getName();
        setOptions(graphDescription.getOptions());
        namespaces = graphDescription.getNamespaces();
        namespaceList = namespaces == null ? Collections.emptyList()
                : new ArrayList<>(new LinkedHashSet<>(namespaces.values()));
    }

    protected void setOptions(Map<String, String> options) {
        for (Entry<String, String> option : options.entrySet()) {
            String key = option.getKey();
            String type = option.getValue();
            if (key.equals(OPTION_DOCTYPE)) {
                SchemaManager sm = Framework.getService(SchemaManager.class);
                DocumentType documentType = sm.getDocumentType(type);
                if (documentType == null) {
                    throw new IllegalArgumentException("Unknown type: " + type + " for graph: " + name);
                }
                Type[] th = documentType.getTypeHierarchy();
                String baseType = th.length == 0 ? type : th[th.length - 1].getName();
                if (!REL_TYPE.equals(baseType)) {
                    throw new IllegalArgumentException("Not a Relation type: " + type + " for graph: " + name);
                }
                docType = type;
            }
        }
    }

    @Override
    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    @Override
    public Long size() {
        SizeFinder sizeFinder = session == null ? new SizeFinder() : new SizeFinder(session);
        sizeFinder.runUnrestricted();
        return Long.valueOf(sizeFinder.size);
    }

    protected class SizeFinder extends UnrestrictedSessionRunner {

        protected long size;

        protected SizeFinder() {
            super(getDefaultRepositoryName());
        }

        protected SizeFinder(CoreSession session) {
            super(session);
        }

        @Override
        public void run() {
            // TODO could use a COUNT(*) query
            try (IterableQueryResult it = session.queryAndFetch("SELECT " + NXQL.ECM_UUID + " FROM " + docType,
                    NXQL.NXQL)) {
                size = it.size();
            }
        }
    }

    @Override
    public void clear() {
        remove(Collections.singletonList(ALL));
    }

    @Override
    public void add(Statement statement) {
        add(Collections.singletonList(statement));
    }

    @Override
    public void add(List<Statement> statements) {
        StatementAdder statementAdder = session == null ? new StatementAdder(statements)
                : new StatementAdder(statements, session);
        statementAdder.runUnrestricted();
    }

    protected class StatementAdder extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Date now;

        protected StatementAdder(List<Statement> statements) {
            super(getDefaultRepositoryName());
            this.statements = statements;
        }

        protected StatementAdder(List<Statement> statements, CoreSession session) {
            super(session);
            this.statements = statements;
        }

        @Override
        public void run() {
            now = new Date();
            for (Statement statement : statements) {
                add(statement);
            }
            session.save();
        }

        protected void add(Statement statement) {
            DocumentModel rel = session.createDocumentModel(null, "relation", docType);
            rel = setRelationProperties(rel, statement);
            session.createDocument(rel);
        }

        protected DocumentModel setRelationProperties(DocumentModel rel, Statement statement) {
            Resource pred = statement.getPredicate();
            String predicateUri = pred.getUri();
            if (predicateUri == null) {
                throw new IllegalArgumentException("Invalid predicate in statement: " + statement);
            }

            Subject subject = statement.getSubject();
            if (subject.isLiteral()) {
                throw new IllegalArgumentException("Invalid literal subject in statement: " + statement);
            }
            NodeAsString source = getNodeAsString(subject);

            Node object = statement.getObject();
            NodeAsString target = getNodeAsString(object);

            String author = getAuthor(statement);
            if (author == null) {
                author = getOriginatingUsername();
            }

            Date created = getCreationDate(statement);
            if (created == null) {
                created = now;
            }

            Date modified = getModificationDate(statement);
            if (modified == null) {
                modified = now;
            }

            String comment = getComment(statement);

            String title = (source.id != null ? source.id : source.uri) + " "
                    + predicateUri.substring(predicateUri.lastIndexOf('/') + 1) + " "
                    + (target.id != null ? target.id : target.uri != null ? target.uri : target.string);
            int MAX_TITLE = 200;
            if (title.length() > MAX_TITLE) {
                title = title.substring(0, MAX_TITLE);
            }

            rel.setPropertyValue(REL_PREDICATE, predicateUri);
            if (source.id != null) {
                rel.setPropertyValue(REL_SOURCE_ID, source.id);
            } else {
                rel.setPropertyValue(REL_SOURCE_URI, source.uri);
            }
            if (target.id != null) {
                rel.setPropertyValue(REL_TARGET_ID, target.id);
            } else if (target.uri != null) {
                rel.setPropertyValue(REL_TARGET_URI, target.uri);
            } else {
                rel.setPropertyValue(REL_TARGET_STRING, target.string);
            }
            if (author != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_CREATOR, author);
            }
            if (created != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_CREATED, created);
            }
            if (modified != null) {
                // will usually get overwritten by DublinCoreListener
                // but not in tests
                rel.setPropertyValue(DC_MODIFIED, modified);
            }
            rel.setPropertyValue(DC_TITLE, title); // for debug
            if (comment != null) {
                rel.setPropertyValue(DC_DESCRIPTION, comment);
            }
            return rel;
        }
    }

    @Override
    public void remove(Statement statement) {
        remove(Collections.singletonList(statement));
    }

    @Override
    public void remove(List<Statement> statements) {
        StatementRemover statementRemover = session == null ? new StatementRemover(statements)
                : new StatementRemover(statements, session);
        statementRemover.runUnrestricted();
    }

    protected class StatementRemover extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Date now;

        protected StatementRemover(List<Statement> statements) {
            super(getDefaultRepositoryName());
            this.statements = statements;
        }

        protected StatementRemover(List<Statement> statements, CoreSession session) {
            super(session);
            this.statements = statements;
        }

        @Override
        public void run() {
            now = new Date();
            for (Statement statement : statements) {
                remove(statement);
            }
        }

        protected void remove(Statement statement) {
            String query = "SELECT " + NXQL.ECM_UUID + " FROM " + docType;
            query = whereBuilder(query, statement);
            if (query == null) {
                return;
            }
            try (IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL)) {
                for (Map<String, Serializable> map : it) {
                    String id = (String) map.get(NXQL.ECM_UUID);
                    session.removeDocument(new IdRef(id));
                }
            }
        }
    }

    protected class StatementFinder extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Statement statement;

        protected StatementFinder(Statement statement) {
            super(getDefaultRepositoryName());
            this.statement = statement;
        }

        protected StatementFinder(Statement statement, CoreSession session) {
            super(session);
            this.statement = statement;
        }

        @Override
        public void run() {
            String query = "SELECT " + REL_PREDICATE + ", " + REL_SOURCE_ID + ", " + REL_SOURCE_URI + ", "
                    + REL_TARGET_ID + ", " + REL_TARGET_URI + ", " + REL_TARGET_STRING + ", " + DC_CREATED + ", "
                    + DC_CREATOR + ", " + DC_MODIFIED + ", " + DC_DESCRIPTION + " FROM " + docType;
            query = whereBuilder(query, statement);
            if (query == null) {
                statements = EMPTY_STATEMENTS;
                return;
            }
            statements = new ArrayList<>();
            try (IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL)) {
                for (Map<String, Serializable> map : it) {
                    String pred = (String) map.get(REL_PREDICATE);
                    String source = (String) map.get(REL_SOURCE_ID);
                    String sourceUri = (String) map.get(REL_SOURCE_URI);
                    String target = (String) map.get(REL_TARGET_ID);
                    String targetUri = (String) map.get(REL_TARGET_URI);
                    String targetString = (String) map.get(REL_TARGET_STRING);
                    Calendar created = (Calendar) map.get(DC_CREATED);
                    String creator = (String) map.get(DC_CREATOR);
                    Calendar modified = (Calendar) map.get(DC_MODIFIED);
                    String comment = (String) map.get(DC_DESCRIPTION);

                    Resource predicate = NodeFactory.createResource(pred);
                    Node subject;
                    if (source != null) {
                        subject = createId(source);
                    } else {
                        subject = createUri(sourceUri);
                    }
                    Node object;
                    if (target != null) {
                        object = createId(target);
                    } else if (targetUri != null) {
                        object = createUri(targetUri);
                    } else {
                        object = NodeFactory.createLiteral(targetString);
                    }
                    Statement statement = new StatementImpl(subject, predicate, object);
                    setCreationDate(statement, created);
                    setAuthor(statement, creator);
                    setModificationDate(statement, modified);
                    setComment(statement, comment);
                    statements.add(statement);
                }
            }
        }

        protected QNameResource createId(String id) {
            return NodeFactory.createQNameResource(DOCUMENT_NAMESPACE, session.getRepositoryName() + '/' + id);
        }

        protected Node createUri(String uri) {
            if (uri.startsWith(BLANK_NS)) {
                // skolemization
                String id = uri.substring(BLANK_NS.length());
                return NodeFactory.createBlank(id.isEmpty() ? null : id);
            } else {
                for (String ns : namespaceList) {
                    if (uri.startsWith(ns)) {
                        String localName = uri.substring(ns.length());
                        return NodeFactory.createQNameResource(ns, localName);
                    }
                }
                return NodeFactory.createResource(uri);
            }
        }

    }

    @Override
    public List<Statement> getStatements() {
        return getStatements(ALL);
    }

    @Override
    public List<Statement> getStatements(Node subject, Node predicate, Node object) {
        return getStatements(new StatementImpl(subject, predicate, object));
    }

    @Override
    public List<Statement> getStatements(Statement statement) {
        StatementFinder statementFinder = session == null ? new StatementFinder(statement)
                : new StatementFinder(statement, session);
        statementFinder.runUnrestricted();
        return statementFinder.statements;
    }

    @Override
    public List<Node> getSubjects(Node predicate, Node object) {
        List<Statement> statements = getStatements(new StatementImpl(null, predicate, object));
        List<Node> nodes = new ArrayList<>(statements.size());
        for (Statement statement : statements) {
            nodes.add(statement.getSubject());
        }
        return nodes;
    }

    @Override
    public List<Node> getPredicates(Node subject, Node object) {
        List<Statement> statements = getStatements(new StatementImpl(subject, null, object));
        List<Node> nodes = new ArrayList<>(statements.size());
        for (Statement statement : statements) {
            nodes.add(statement.getPredicate());
        }
        return nodes;
    }

    @Override
    public List<Node> getObjects(Node subject, Node predicate) {
        List<Statement> statements = getStatements(new StatementImpl(subject, predicate, null));
        List<Node> nodes = new ArrayList<>(statements.size());
        for (Statement statement : statements) {
            nodes.add(statement.getObject());
        }
        return nodes;
    }

    @Override
    public boolean hasStatement(Statement statement) {
        if (statement == null) {
            return false;
        }
        // could be optimized in the null/blank case, but this method seems
        // unused
        return !getStatements(statement).isEmpty();
    }

    @Override
    public boolean hasResource(Resource resource) {
        if (resource == null) {
            return false;
        }
        ResourceFinder resourceFinder = session == null ? new ResourceFinder(resource)
                : new ResourceFinder(resource, session);
        resourceFinder.runUnrestricted();
        return resourceFinder.found;
    }

    protected class ResourceFinder extends UnrestrictedSessionRunner {

        protected boolean found;

        protected Resource resource;

        protected ResourceFinder(Resource resource) {
            super(getDefaultRepositoryName());
            this.resource = resource;
        }

        protected ResourceFinder(Resource resource, CoreSession session) {
            super(session);
            this.resource = resource;
        }

        @Override
        public void run() {
            String query = "SELECT " + NXQL.ECM_UUID + " FROM " + docType;
            query = whereAnyBuilder(query, resource);
            try (IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL)) {
                found = it.iterator().hasNext();
            }
        }

        protected String whereAnyBuilder(String query, Resource resource) {
            List<Object> params = new ArrayList<>(3);
            List<String> clauses = new ArrayList<>(3);

            NodeAsString nas = getNodeAsString(resource);
            if (nas.id != null) {
                // don't allow predicates that are actually doc ids
                clauses.add(REL_SOURCE_ID + " = ?");
                params.add(nas.id);
                clauses.add(REL_TARGET_URI + " = ?");
                params.add(DOCUMENT_NAMESPACE + session.getRepositoryName() + '/' + nas.id);
            } else if (nas.uri != null) {
                for (String ns : DOC_NAMESPACES) {
                    if (nas.uri.startsWith(ns)) {
                        String id = localNameToId(nas.uri.substring(ns.length()));
                        clauses.add(REL_SOURCE_ID + " = ?");
                        params.add(id);
                        break;
                    }
                }
                clauses.add(REL_SOURCE_URI + " = ?");
                params.add(nas.uri);
                clauses.add(REL_TARGET_URI + " = ?");
                params.add(nas.uri);
                clauses.add(REL_PREDICATE + " = ?");
                params.add(nas.uri);
            }
            query += " WHERE " + StringUtils.join(clauses, " OR ");
            query = NXQLQueryBuilder.getQuery(query, params.toArray(), true, true, null);
            return query;
        }
    }

    public static final Pattern SPARQL_SPO_PO = Pattern.compile(
            "SELECT \\?s \\?p \\?o WHERE \\{ \\?s \\?p \\?o . \\?s <(.*)> <(.*)> . \\}");

    public static final Pattern SPARQL_PO_S = Pattern.compile("SELECT \\?p \\?o WHERE \\{ <(.*)> \\?p \\?o \\}");

    @Override
    public QueryResult query(String queryString, String language, String baseURI) {
        // language is ignored, assume SPARQL
        Matcher matcher = SPARQL_SPO_PO.matcher(queryString);
        if (matcher.matches()) {
            Node predicate = NodeFactory.createResource(matcher.group(1));
            Node object = NodeFactory.createResource(matcher.group(2));
            // find subjects with this predicate and object
            List<Node> subjects = getSubjects(predicate, object);
            List<Map<String, Node>> results = new ArrayList<>();
            if (!subjects.isEmpty()) {
                // find all statements with these subjects
                List<Statement> statements = getStatements(new Subjects(subjects), null, null);
                for (Statement st : statements) {
                    Map<String, Node> map = new HashMap<>();
                    map.put("s", st.getSubject());
                    map.put("p", st.getPredicate());
                    map.put("o", st.getObject());
                    results.add(map);
                }
            }
            return new QueryResultImpl(Integer.valueOf(results.size()), Arrays.asList("s", "p", "o"), results);
        }
        matcher = SPARQL_PO_S.matcher(queryString);
        if (matcher.matches()) {
            Node subject = NodeFactory.createResource(matcher.group(1));
            // find predicates and objects with this subject
            List<Statement> statements = getStatements(new StatementImpl(subject, null, null));
            List<Map<String, Node>> results = new ArrayList<>();
            for (Statement st : statements) {
                Map<String, Node> map = new HashMap<>();
                map.put("p", st.getPredicate());
                map.put("o", st.getObject());
                results.add(map);
            }
            return new QueryResultImpl(Integer.valueOf(results.size()), Arrays.asList("p", "o"), results);
        }
        throw new UnsupportedOperationException("Cannot parse query: " + queryString);
    }

    public static final Pattern SPARQL_S_PO = Pattern.compile("SELECT \\?s WHERE \\{ \\?s <(.*)> <(.*)> \\}");

    @Override
    public int queryCount(String queryString, String language, String baseURI) {
        // language is ignored, assume SPARQL
        Matcher matcher = SPARQL_S_PO.matcher(queryString);
        if (matcher.matches()) {
            Node predicate = NodeFactory.createResource(matcher.group(1));
            Node object = NodeFactory.createResource(matcher.group(2));
            List<Node> subjects = getSubjects(predicate, object);
            return subjects.size();
        }
        throw new UnsupportedOperationException("Cannot parse query: " + queryString);
    }

    @Override
    public boolean read(String path, String lang, String base) {
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            return read(in, lang, base);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    @Override
    public boolean write(String path, String lang, String base) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(path));
            return write(out, lang, base);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    @Override
    public boolean read(InputStream in, String lang, String base) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean write(OutputStream out, String lang, String base) {
        throw new UnsupportedOperationException();
    }

    protected static String getDefaultRepositoryName() {
        return Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
    }

    /** Fake Node type used to pass down multiple nodes into whereBuilder. */
    public static class Subjects extends AbstractNode implements Subject {

        private static final long serialVersionUID = 1L;

        protected List<Node> nodes;

        public Subjects(List<Node> nodes) {
            this.nodes = nodes;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        @Override
        public NodeType getNodeType() {
            return null;
        }
    }

    protected String whereBuilder(String query, Statement statement) {
        List<Object> params = new ArrayList<>(3);
        List<String> clauses = new ArrayList<>(3);

        Resource p = statement.getPredicate();
        if (p != null) {
            NodeAsString pn = getNodeAsString(p);
            if (pn.uri == null) {
                return null;
            }
            clauses.add(REL_PREDICATE + " = ?");
            params.add(pn.uri);
        }

        Node s = statement.getSubject();
        if (s != null) {
            if (s instanceof Subjects) {
                List<Node> subjects = ((Subjects) s).getNodes();
                if (subjects.isEmpty()) {
                    throw new UnsupportedOperationException("empty subjects");
                }
                StringBuilder buf = new StringBuilder(REL_SOURCE_URI);
                buf.append(" IN (");
                for (Node sub : subjects) {
                    NodeAsString sn = getNodeAsString(sub);
                    if (sn.id != null) {
                        throw new UnsupportedOperationException("subjects ListNode with id instead of uri" + subjects);
                    }
                    buf.append("?, ");
                    params.add(sn.uri);
                }
                buf.setLength(buf.length() - 2); // remove last comma/space
                buf.append(")");
                clauses.add(buf.toString());
            } else {
                NodeAsString sn = getNodeAsString(s);
                if (sn.id != null) {
                    clauses.add(REL_SOURCE_ID + " = ?");
                    params.add(sn.id);
                } else {
                    clauses.add(REL_SOURCE_URI + " = ?");
                    params.add(sn.uri);
                }

            }
        }

        Node o = statement.getObject();
        if (o != null) {
            NodeAsString on = getNodeAsString(o);
            if (on.id != null) {
                clauses.add(REL_TARGET_ID + " = ?");
                params.add(on.id);
            } else if (on.uri != null) {
                clauses.add(REL_TARGET_URI + " = ?");
                params.add(on.uri);
            } else {
                clauses.add(REL_TARGET_STRING + " = ?");
                params.add(on.string);
            }
        }

        if (!clauses.isEmpty()) {
            query += " WHERE " + StringUtils.join(clauses, " AND ");
            query = NXQLQueryBuilder.getQuery(query, params.toArray(), true, true, null);
        }
        return query;
    }

    protected static NodeAsString getNodeAsString(Node node) {
        NodeAsString nas = new NodeAsString();
        if (node.isBlank()) {
            // skolemization
            String id = ((Blank) node).getId();
            nas.uri = BLANK_NS + (id == null ? "" : id);
        } else if (node.isLiteral()) {
            nas.string = ((Literal) node).getValue();
        } else if (node.isQNameResource()) {
            String ns = ((QNameResource) node).getNamespace();
            if (DOCUMENT_NAMESPACE.equals(ns) || DOCUMENT_NAMESPACE2.equals(ns) || COMMENT_NAMESPACE.equals(ns)) {
                nas.id = localNameToId(((QNameResource) node).getLocalName());
            } else {
                nas.uri = ((Resource) node).getUri();
            }
        } else { // node.isResource()
            String uri = ((Resource) node).getUri();
            for (String ns : DOC_NAMESPACES) {
                if (uri.startsWith(ns)) {
                    nas.id = localNameToId(uri.substring(ns.length()));
                    break;
                }
            }
            if (nas.id == null) {
                nas.uri = uri;
            }
        }
        return nas;
    }

    protected static String localNameToId(String localName) {
        String[] split = localName.split("/");
        if (split.length == 1) {
            return localName; // compat, no repository name
        } else {
            return split[1];
        }
    }

    protected static String getAuthor(Statement statement) {
        return getStringProperty(statement, RelationConstants.AUTHOR);
    }

    protected static void setAuthor(Statement statement, String author) {
        setStringProperty(statement, RelationConstants.AUTHOR, author);
    }

    protected static Date getCreationDate(Statement statement) {
        return getDateProperty(statement, RelationConstants.CREATION_DATE);
    }

    protected static void setCreationDate(Statement statement, Calendar created) {
        setDateProperty(statement, RelationConstants.CREATION_DATE, created);
    }

    protected static Date getModificationDate(Statement statement) {
        return getDateProperty(statement, RelationConstants.MODIFICATION_DATE);
    }

    protected static void setModificationDate(Statement statement, Calendar modified) {
        setDateProperty(statement, RelationConstants.MODIFICATION_DATE, modified);
    }

    protected static String getComment(Statement statement) {
        return getStringProperty(statement, RelationConstants.COMMENT);
    }

    protected static void setComment(Statement statement, String comment) {
        setStringProperty(statement, RelationConstants.COMMENT, comment);
    }

    protected static String getStringProperty(Statement statement, Resource prop) {
        Node node = statement.getProperty(prop);
        if (node == null || !node.isLiteral()) {
            return null;
        }
        return ((Literal) node).getValue();
    }

    protected static void setStringProperty(Statement statement, Resource prop, String string) {
        if (string == null) {
            return;
        }
        statement.setProperty(prop, NodeFactory.createLiteral(string));
    }

    protected static Date getDateProperty(Statement statement, Resource prop) {
        Node node = statement.getProperty(prop);
        if (node == null || !node.isLiteral()) {
            return null;
        }
        return RelationDate.getDate((Literal) node);
    }

    protected static void setDateProperty(Statement statement, Resource prop, Calendar date) {
        if (date == null) {
            return;
        }
        statement.setProperty(prop, RelationDate.getLiteralDate(date));
    }

}
