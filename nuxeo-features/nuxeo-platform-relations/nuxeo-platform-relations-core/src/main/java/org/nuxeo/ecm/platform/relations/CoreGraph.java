/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
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
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.NodeFactory;
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
    public static String BLANK_NS = "-:";

    public static String DOCUMENT_NAMESPACE = RelationConstants.DOCUMENT_NAMESPACE;

    /** Without final slash (compat). */
    public static String DOCUMENT_NAMESPACE2 = DOCUMENT_NAMESPACE.substring(0,
            DOCUMENT_NAMESPACE.length() - 1);

    /** Has no final slash (compat). */
    public static final String COMMENT_NAMESPACE = "http://www.nuxeo.org/comments/uid";

    public static final String[] DOC_NAMESPACES = { DOCUMENT_NAMESPACE,
            DOCUMENT_NAMESPACE2, COMMENT_NAMESPACE };

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
     * A graph with this base session. An unrestricted session will be opened
     * based on it.
     */
    public CoreGraph(CoreSession session) {
        this.session = session;
    }

    @Override
    public void setDescription(GraphDescription graphDescription) {
        name = graphDescription.getName();
        setOptions(graphDescription.getOptions());
        namespaces = graphDescription.getNamespaces();
        namespaceList = namespaces == null ? Collections.<String> emptyList()
                : new ArrayList<String>(new LinkedHashSet<String>(
                        namespaces.values()));
    }

    protected void setOptions(Map<String, String> options) {
        for (Entry<String, String> option : options.entrySet()) {
            String key = option.getKey();
            String type = option.getValue();
            if (key.equals(OPTION_DOCTYPE)) {
                SchemaManager sm = Framework.getLocalService(SchemaManager.class);
                DocumentType documentType = sm.getDocumentType(type);
                if (documentType == null) {
                    throw new IllegalArgumentException("Unknown type: " + type
                            + " for graph: " + name);
                }
                Type[] th = documentType.getTypeHierarchy();
                String baseType = th.length == 0 ? type
                        : th[th.length - 1].getName();
                if (!REL_TYPE.equals(baseType)) {
                    throw new IllegalArgumentException("Not a Relation type: "
                            + type + " for graph: " + name);
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
        SizeFinder sizeFinder = session == null ? new SizeFinder()
                : new SizeFinder(session);
        try {
            sizeFinder.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
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
        public void run() throws ClientException {
            // TODO could use a COUNT(*) query
            IterableQueryResult it = session.queryAndFetch("SELECT "
                    + NXQL.ECM_UUID + " FROM " + docType, NXQL.NXQL);
            try {
                size = it.size();
            } finally {
                it.close();
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
        StatementAdder statementAdder = session == null ? new StatementAdder(
                statements) : new StatementAdder(statements, session);
        try {
            statementAdder.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    protected class StatementAdder extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Date now;

        protected StatementAdder(List<Statement> statements) {
            super(getDefaultRepositoryName(), "system");
            this.statements = statements;
        }

        protected StatementAdder(List<Statement> statements, CoreSession session) {
            super(session);
            this.statements = statements;
        }

        @Override
        public void run() throws ClientException {
            now = new Date();
            for (Statement statement : statements) {
                add(statement);
            }
        }

        protected void add(Statement statement) throws ClientException {
            Resource pred = statement.getPredicate();
            NodeAsString predicate = getNodeAsString(pred);
            if (predicate.uri == null) {
                throw new IllegalArgumentException(
                        "Invalid predicate in statement: " + statement);
            }

            Subject subject = statement.getSubject();
            if (subject.isLiteral()) {
                throw new IllegalArgumentException(
                        "Invalid literal subject in statement: " + statement);
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

            String title = (source.id != null ? source.id : source.uri)
                    + " "
                    + predicate.uri.substring(predicate.uri.lastIndexOf('/') + 1)
                    + " "
                    + (target.id != null ? target.id
                            : target.uri != null ? target.uri : target.string);
            int MAX_TITLE = 200;
            if (title.length() > MAX_TITLE) {
                title = title.substring(0, MAX_TITLE);
            }

            DocumentModel rel = session.createDocumentModel(null, "relation",
                    docType);
            rel.setPropertyValue(REL_PREDICATE, predicate.uri);
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
            session.createDocument(rel);
        }
    }

    @Override
    public void remove(Statement statement) {
        remove(Collections.singletonList(statement));
    }

    @Override
    public void remove(List<Statement> statements) {
        StatementRemover statementRemover = session == null ? new StatementRemover(
                statements) : new StatementRemover(statements, session);
        try {
            statementRemover.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    protected class StatementRemover extends UnrestrictedSessionRunner {

        protected List<Statement> statements;

        protected Date now;

        protected StatementRemover(List<Statement> statements) {
            super(getDefaultRepositoryName());
            this.statements = statements;
        }

        protected StatementRemover(List<Statement> statements,
                CoreSession session) {
            super(session);
            this.statements = statements;
        }

        @Override
        public void run() throws ClientException {
            now = new Date();
            for (Statement statement : statements) {
                remove(statement);
            }
        }

        protected void remove(Statement statement) throws ClientException {
            String query = "SELECT " + NXQL.ECM_UUID + " FROM " + docType;
            query = whereBuilder(query, statement);
            if (query == null) {
                return;
            }
            IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL);
            try {
                for (Map<String, Serializable> map : it) {
                    String id = (String) map.get(NXQL.ECM_UUID);
                    session.removeDocument(new IdRef(id));
                }
            } finally {
                it.close();
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
        public void run() throws ClientException {
            String query = "SELECT " + REL_PREDICATE + ", " + REL_SOURCE_ID
                    + ", " + REL_SOURCE_URI + ", " + REL_TARGET_ID + ", "
                    + REL_TARGET_URI + ", " + REL_TARGET_STRING + ", "
                    + DC_CREATED + ", " + DC_CREATOR + ", " + DC_MODIFIED
                    + ", " + DC_DESCRIPTION + " FROM " + docType;
            query = whereBuilder(query, statement);
            if (query == null) {
                statements = EMPTY_STATEMENTS;
                return;
            }
            statements = new ArrayList<Statement>();
            IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL);
            try {
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
                    Statement statement = new StatementImpl(subject, predicate,
                            object);
                    setCreationDate(statement, created);
                    setAuthor(statement, creator);
                    setModificationDate(statement, modified);
                    setComment(statement, comment);
                    statements.add(statement);
                }
            } finally {
                it.close();
            }
        }

        protected QNameResource createId(String id) {
            return NodeFactory.createQNameResource(DOCUMENT_NAMESPACE,
                    session.getRepositoryName() + '/' + id);
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
    public List<Statement> getStatements(Node subject, Node predicate,
            Node object) {
        return getStatements(new StatementImpl(subject, predicate, object));
    }

    @Override
    public List<Statement> getStatements(Statement statement) {
        StatementFinder statementFinder = session == null ? new StatementFinder(
                statement) : new StatementFinder(statement, session);
        try {
            statementFinder.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        return statementFinder.statements;
    }

    @Override
    public List<Node> getSubjects(Node predicate, Node object) {
        List<Statement> statements = getStatements(new StatementImpl(null,
                predicate, object));
        List<Node> nodes = new ArrayList<Node>(statements.size());
        for (Statement statement : statements) {
            nodes.add(statement.getSubject());
        }
        return nodes;
    }

    @Override
    public List<Node> getPredicates(Node subject, Node object) {
        List<Statement> statements = getStatements(new StatementImpl(subject,
                null, object));
        List<Node> nodes = new ArrayList<Node>(statements.size());
        for (Statement statement : statements) {
            nodes.add(statement.getPredicate());
        }
        return nodes;
    }

    @Override
    public List<Node> getObjects(Node subject, Node predicate) {
        List<Statement> statements = getStatements(new StatementImpl(subject,
                predicate, null));
        List<Node> nodes = new ArrayList<Node>(statements.size());
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
        ResourceFinder resourceFinder = session == null ? new ResourceFinder(
                resource) : new ResourceFinder(resource, session);
        try {
            resourceFinder.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
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
        public void run() throws ClientException {
            String query = "SELECT " + NXQL.ECM_UUID + " FROM " + docType;
            query = whereAnyBuilder(query, resource);
            IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL);
            try {
                found = it.iterator().hasNext();
            } finally {
                it.close();
            }
        }

        protected String whereAnyBuilder(String query, Resource resource)
                throws ClientException {
            List<Object> params = new ArrayList<Object>(3);
            List<String> clauses = new ArrayList<String>(3);

            NodeAsString nas = getNodeAsString(resource);
            if (nas.id != null) {
                // don't allow predicates that are actually doc ids
                clauses.add(REL_SOURCE_ID + " = ?");
                params.add(nas.id);
                clauses.add(REL_TARGET_URI + " = ?");
                params.add(DOCUMENT_NAMESPACE + session.getRepositoryName()
                        + '/' + nas.id);
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
            query = NXQLQueryBuilder.getQuery(query, params.toArray(), true,
                    true, null);
            return query;
        }
    }

    @Override
    public QueryResult query(String queryString, String language, String baseURI) {
        throw new UnsupportedOperationException();
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
        try {
            return Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String whereBuilder(String query, Statement statement)
            throws ClientException {
        List<Object> params = new ArrayList<Object>(3);
        List<String> clauses = new ArrayList<String>(3);

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
            NodeAsString sn = getNodeAsString(s);
            if (sn.id != null) {
                clauses.add(REL_SOURCE_ID + " = ?");
                params.add(sn.id);
            } else {
                clauses.add(REL_SOURCE_URI + " = ?");
                params.add(sn.uri);
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
            query = NXQLQueryBuilder.getQuery(query, params.toArray(), true,
                    true, null);
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
            if (DOCUMENT_NAMESPACE.equals(ns) || DOCUMENT_NAMESPACE2.equals(ns)
                    || COMMENT_NAMESPACE.equals(ns)) {
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

    protected static void setModificationDate(Statement statement,
            Calendar modified) {
        setDateProperty(statement, RelationConstants.MODIFICATION_DATE,
                modified);
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

    protected static void setStringProperty(Statement statement, Resource prop,
            String string) {
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

    protected static void setDateProperty(Statement statement, Resource prop,
            Calendar date) {
        if (date == null) {
            return;
        }
        statement.setProperty(prop, RelationDate.getLiteralDate(date));
    }

}
