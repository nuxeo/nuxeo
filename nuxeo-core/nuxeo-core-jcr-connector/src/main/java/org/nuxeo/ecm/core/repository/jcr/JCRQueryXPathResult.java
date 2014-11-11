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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryResult;

public class JCRQueryXPathResult implements QueryResult {

    private final NodeIterator iterator;

    private final JCRQueryXPath query;

    private Node node;

    public JCRQueryXPathResult(JCRQueryXPath query,
            javax.jcr.query.QueryResult qr) throws RepositoryException {
        iterator = qr.getNodes();
        this.query = query;
    }

    public long count() {
        return iterator.getSize();
    }

    public boolean isEmpty() {
        if (iterator.getPosition() > 0) {
            return false;
        }
        return !iterator.hasNext();
    }

    public boolean next() {
        if (iterator.hasNext()) {
            node = iterator.nextNode();
            return true;
        }
        return false;
    }

    public long row() {
        return iterator.getPosition();
    }

    /**
     * @return the results as a List
     */
    public DocumentModelList getDocumentModels() throws QueryException {
        try {
            List<DocumentModel> list = new ArrayList<DocumentModel>();
            String[] schemas = null;
            while (next()) {
                if (node.getPath().equals("/ecm:root")) {
                    continue;
                }

                JCRDocument doc = query.getSession().newDocument(node);
                if (schemas == null) {
                    schemas = getSchemasInSelectClause(doc);
                }
                DocumentModel docModel = DocumentModelFactory
                        .createDocumentModel(doc, schemas);
                list.add(docModel);
            }
            return new DocumentModelListImpl(list);
        } catch (Exception e) {
            e.printStackTrace();
            throw new QueryException("getDocumentModels failed", e);
        }
    }

    private String[] getSchemasInSelectClause(Document doc) {
        /*
         SelectList selectElements = query.sqlQuery.select.elements;
         // if it is an 'select * from', we return all document schemas
         if(selectElements.size()==0) {
         DocumentType docType = doc.getType();
         return docType.getSchemaNames();
         }
         */

        // otherwise, we check for each schema if it contains any field in the select clause
        List<String> schemaNames = new ArrayList<String>();

        // make sure the schema "common" is always selected
        if (!schemaNames.contains("common")) {
            schemaNames.add("common");
        }

        //return schemaNames.toArray(new String[schemaNames.size()]);

        // NXP-415: fields are empty sometimes - it is not desirable to load
        // all schemas, though
        // XXX : lazy loading should work better
        return null; // force to load all schemas for documents...
    }

    public Object getObject() throws QueryException {
        JCRDocument document;
        try {
            document = getOwnerDocument(node);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        // we have the document with the found node as on of its children
        if (true) {
            return document;
        }

        // FIXME: unreacheable code
        try {
            return document.exportMap((String[]) null);
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates a JCRDocument for the parent node of the given node.
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @throws QueryException
     */
    private JCRDocument getOwnerDocument(Node node) throws RepositoryException,
            QueryException {
        //Node parent = ModelAdapter.getParentNode(node); - don't use this: will jump over direct parent
        final Node parent = node.getParent();
        if (parent == null) {
            final String nodeDesc = node.getPath();
            throw new QueryException("orphan content node detected: "
                    + nodeDesc);
        }

        return query.getSession().newDocument(node);
    }

    public boolean getBoolean(int i) throws QueryException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean getBoolean(String column) throws QueryException {
        // TODO Auto-generated method stub
        return false;
    }

    public double getDouble(int i, double defaultValue) throws QueryException {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getDouble(String column, double defaultValue)
            throws QueryException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getLong(int i, long defaultValue) throws QueryException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getLong(String column, long defaultValue) throws QueryException {
        // TODO Auto-generated method stub
        return 0;
    }

    public Object getObject(String column) throws QueryException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getString(int i) throws QueryException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getString(String column) throws QueryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Iterator getDocuments(int start) {

        // skip first nodes if required
        for (int i = 0; i < start; i++) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                //break; // the iterator will have no elements
                return EmptyDocumentIterator.INSTANCE;
            }
        }

        DocumentIterator docsIt = new DocumentIterator() {

            public void remove() {
                // TODO Auto-generated method stub
            }

            public Document next() {
                JCRQueryXPathResult.this.next();
                JCRDocument doc;
                try {
                    doc = query.getSession().newDocument(node);
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return next();
                }

                return doc;
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public long getSize() {
                return iterator.getSize();
            }

        };

        return docsIt;
    }

}
