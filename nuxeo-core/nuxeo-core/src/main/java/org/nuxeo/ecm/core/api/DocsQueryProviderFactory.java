/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.api.impl.DocsQueryProviderDef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;

/**
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
final class DocsQueryProviderFactory {

    private final AbstractSession session;

    DocsQueryProviderFactory(AbstractSession session) {
        this.session = session;
    }

    public DocsQueryProvider getDQLbyType(DocsQueryProviderDef dqpDef)
            throws ClientException {
        final DocsQueryProvider dqp;
        try {
            switch (dqpDef.getType()) {
                case TYPE_CHILDREN:
                    dqp = getDQPChildren(dqpDef.getParent());
                    break;
                case TYPE_CHILDREN_NON_FOLDER:
                    dqp = getDQPChildrenNonFolder(dqpDef.getParent());
                    break;
                case TYPE_CHILDREN_FOLDERS:
                    dqp = getDQPChildrenFolder(dqpDef.getParent());
                    break;
                case TYPE_QUERY:
                    dqp = getDQPQueryResult(dqpDef.getQuery());
                    break;
                default:
                    throw new IllegalArgumentException("cannot get here");
            }
        } catch (DocumentException e) {
            throw new ClientException("DocumentException", e);
        }
        return dqp;
    }

    private DocsQueryProvider getDQPQueryResult(String query)
            throws ClientException {
        final Query compiledQuery;
        try {
            compiledQuery = session.getSession().createQuery(query,
                    Query.Type.NXQL);
        } catch (QueryException e1) {
            throw new ClientException("qe", e1);
        }

        return new DocsQueryProvider() {
            @Override
            public DocumentIterator getDocs(int start) throws ClientException {
                DocumentIterator documents;
                try {
                    documents = (DocumentIterator) compiledQuery.execute().getDocuments(
                            start);
                } catch (QueryException e) {
                    throw new ClientException("Query exception, ", e);
                }
                return documents;
            }

            @Override
            public boolean accept(Document child) {
                return true;
            }
        };
    }

    private DocsQueryProvider getDQPChildren(DocumentRef parent)
            throws DocumentException, ClientException {
        final Document doc = session.resolveReference(parent);
        session.checkPermission(doc, SecurityConstants.READ_CHILDREN);

        DocsQueryProvider dqProvider = new DocsQueryProvider() {
            @Override
            public DocumentIterator getDocs(int start) throws ClientException {
                DocumentIterator children;
                try {
                    children = doc.getChildren(start);
                } catch (DocumentException e) {
                    throw new ClientException("DocumentException", e);
                }
                return children;
            }

            @Override
            public boolean accept(Document child) {
                return true;
            }
        };

        return dqProvider;
    }

    private DocsQueryProvider getDQPChildrenNonFolder(DocumentRef parent)
            throws DocumentException, ClientException {
        final Document doc = session.resolveReference(parent);
        session.checkPermission(doc, SecurityConstants.READ_CHILDREN);

        DocsQueryProvider dqProvider = new DocsQueryProvider() {
            @Override
            public DocumentIterator getDocs(int start) throws ClientException {
                DocumentIterator children;
                try {
                    children = doc.getChildren(start);
                } catch (DocumentException e) {
                    throw new ClientException("DocumentException", e);
                }
                return children;
            }

            @Override
            public boolean accept(Document child) throws ClientException {
                try {
                    return !child.isFolder()
                            && session.hasPermission(
                                    child, SecurityConstants.READ);
                } catch (DocumentException e) {
                    throw new ClientException("DocumentException", e);
                }
            }
        };

        return dqProvider;
    }

    private DocsQueryProvider getDQPChildrenFolder(DocumentRef parent)
            throws DocumentException, ClientException {

        final Document doc = session.resolveReference(parent);
        session.checkPermission(doc, SecurityConstants.READ_CHILDREN);

        DocsQueryProvider dqProvider = new DocsQueryProvider() {
            @Override
            public DocumentIterator getDocs(int start) throws ClientException {
                DocumentIterator childrenIt;
                try {
                    childrenIt = doc.getChildren(start);
                } catch (DocumentException e) {
                    throw new ClientException("DocumentException", e);
                }
                return childrenIt;
            }

            @Override
            public boolean accept(Document child) throws ClientException {
                try {
                    return child.isFolder()
                            && session.hasPermission(
                                    child, SecurityConstants.READ);
                } catch (DocumentException e) {
                    throw new ClientException("DocumentException", e);
                }
            }
        };

        return dqProvider;
    }

}
