/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.DocsQueryProviderDef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;

/**
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@SuppressWarnings({"SuppressionAnnotation"})
final class DocsQueryProviderFactory {

    private static final Log log = LogFactory.getLog(DocsQueryProviderFactory.class);

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
                case TYPE_QUERY_FTS:
                    dqp = getDQPQueryFtsResult(dqpDef.getQuery(),
                            dqpDef.getStartingPath());
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

    private DocsQueryProvider getDQPQueryFtsResult(String keywords, String startingPath)
            throws ClientException {

        final StringBuffer xpathPrefix = new StringBuffer("//");
        // extract path elements and send as params
        // so they could be escaped correctly
        String[] pathElements = new String[0];
        if (startingPath != null) {
            pathElements = startingPath.split("\\/");
            for (String element : pathElements) {
                xpathPrefix.append("?/");
            }
        }

        //final String xpathQ = xpathPrefix + "element(*, ecmnt:document)[jcr:contains(.,'*" + keywords
        //        + "*')]";
        final String xpathQ = xpathPrefix + "element(*, ecmnt:document)[jcr:contains(.,'*?*')]";

        String[] params = new String[pathElements.length + 1];
        System.arraycopy(pathElements, 0, params, 0, pathElements.length);
        params[params.length - 1] = keywords;

        final Query compiledQuery;
        try {
            compiledQuery = session.getSession().createQuery(xpathQ,
                    Query.Type.XPATH, params);
        } catch (QueryException e) {
            log.error("Error executing xpath query: " + xpathQ, e);
            throw new ClientException("qe", e);
        }

        DocsQueryProvider dqProvider = new DocsQueryProvider() {
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
        return dqProvider;
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
