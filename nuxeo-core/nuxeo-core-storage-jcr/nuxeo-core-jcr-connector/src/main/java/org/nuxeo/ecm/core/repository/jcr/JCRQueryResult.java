/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Reference;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class JCRQueryResult implements QueryResult {

    private static final Log log = LogFactory.getLog(JCRQueryResult.class);

    private final javax.jcr.query.QueryResult jcrQueryResult;

    private final NodeIterator iterator;

    private final JCRQuery query;

    private final long totalSize;

    /** When not null, order documents models by path asc (true) or desc (false) */
    protected final Boolean orderByPath;

    protected final int limit;

    protected final int offset;

    private Node node;

    public JCRQueryResult(JCRQuery query, javax.jcr.query.QueryResult qr,
            boolean countTotal, Boolean orderByPath, long limit, long offset)
            throws RepositoryException {
        jcrQueryResult = qr;
        iterator = qr.getNodes();
        this.query = query;
        if (countTotal) {
            long count = 0;
            NodeIterator it = qr.getNodes();
            while (it.hasNext()) {
                if ("/ecm:root".equals(it.nextNode().getPath())) {
                    continue;
                }
                count++;
            }
            totalSize = count;
        } else {
            totalSize = -1;
        }
        this.orderByPath = orderByPath;
        this.limit = (int) limit;
        this.offset = (int) offset;
    }

    public javax.jcr.query.QueryResult jcrQueryResult() {
        return jcrQueryResult;
    }

    public long count() {
        return iterator.getSize();
    }

    public long getTotalSize() {
        return totalSize;
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

    private static Object getPropertyValue(Property property)
            throws RepositoryException {
        switch (property.getType()) {
        case PropertyType.LONG:
            return property.getLong();
        case PropertyType.DOUBLE:
            return property.getDouble();
        case PropertyType.BOOLEAN:
            return property.getBoolean();
        }
        return property.getString();
    }

    public Object getObject(String column) throws QueryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getObject() throws QueryException {
        Map<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
        Map<String, Object> subMap = new HashMap<String, Object>();

        int size = query.sqlQuery.select.elements.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Operand o = query.sqlQuery.select.elements.get(i);
                if (o instanceof Reference) {
                    String name = ((Reference) o).name;
                    try {
                        Property property = node.getProperty(name);
                        subMap.put(name, getPropertyValue(property));
                    } catch (ValueFormatException e) {
                        e.printStackTrace();
                    } catch (PathNotFoundException e) {
                        e.printStackTrace();
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                }
            }
            map.put("default", subMap);
            return map;
        }
        // return all the fields
        try {
            JCRDocument document = query.session.newDocument(node);
            return document.exportMap((String[]) null);
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return null;

    }

    protected final Property getProperty(int i) throws RepositoryException {
        if (query.sqlQuery.select.isEmpty()) {
            return null;
        }
        Operand o = query.sqlQuery.select.elements.get(i);
        // TODO optimize - remove instanceof
        if (o instanceof Reference) {
            String name = ((Reference) o).name;
            return node.getProperty(name);
        }
        return null;
    }

    protected final Property getProperty(String column)
            throws RepositoryException {
        if (query.sqlQuery.select.isEmpty()) {
            return node.getProperty(column);
        }
        Operand o = query.sqlQuery.select.elements.get(column);
        // TODO optimize - remove instanceof
        if (o instanceof Reference) {
            String name = ((Reference) o).name;
            return node.getProperty(name);
        }
        return null;
    }

    public String getString(String column) throws QueryException {
        try {
            Property p = getProperty(column);
            if (p != null) {
                return p.getString();
            }
            return null; // TODO
        } catch (Exception e) {
            throw new QueryException("getString failed for " + column, e);
        }
    }

    public String getString(int i) throws QueryException {
        try {
            Property p = getProperty(i);
            if (p != null) {
                return p.getString();
            }
            return null; // TODO
        } catch (Exception e) {
            throw new QueryException("getString failed for column#" + i, e);
        }
    }

    public boolean getBoolean(int i) throws QueryException {
        try {
            Property p = getProperty(i);
            if (p != null) {
                return p.getBoolean();
            }
            return false; // TODO
        } catch (Exception e) {
            throw new QueryException("getBoolean failed for column#" + i, e);
        }
    }

    public boolean getBoolean(String column) throws QueryException {
        try {
            Property p = getProperty(column);
            if (p != null) {
                return p.getBoolean();
            }
            return false; // TODO
        } catch (Exception e) {
            throw new QueryException("getBoolean failed for " + column, e);
        }
    }

    public double getDouble(int i, double defaultValue) throws QueryException {
        try {
            Property p = getProperty(i);
            if (p != null) {
                return p.getDouble();
            }
            return defaultValue;
        } catch (Exception e) {
            throw new QueryException("getDouble failed for column#" + i, e);
        }
    }

    public double getDouble(String column, double defaultValue)
            throws QueryException {
        try {
            Property p = getProperty(column);
            if (p != null) {
                return p.getDouble();
            }
            return defaultValue;
        } catch (Exception e) {
            throw new QueryException("getDouble failed for " + column, e);
        }
    }

    public long getLong(int i, long defaultValue) throws QueryException {
        try {
            Property p = getProperty(i);
            if (p != null) {
                return p.getLong();
            }
            return defaultValue;
        } catch (Exception e) {
            throw new QueryException("getDouble failed column#" + i, e);
        }
    }

    public long getLong(String column, long defaultValue) throws QueryException {
        try {
            Property p = getProperty(column);
            if (p != null) {
                return p.getLong();
            }
            return defaultValue;
        } catch (Exception e) {
            throw new QueryException("getDouble failed for " + column, e);
        }
    }

    /*
     * Return the results as a List
     */
    public DocumentModelList getDocumentModels() throws QueryException {
        try {
            List<DocumentModel> list = new ArrayList<DocumentModel>();
            String[] schemas = null;
            while (next()) {
                if ("/ecm:root".equals(node.getPath())) {
                    continue;
                }

                JCRDocument doc = query.session.newDocument(node);
                if (schemas == null) {
                    schemas = getSchemasInSelectClause(doc);
                }
                try {
                    DocumentModel docModel = DocumentModelFactory.createDocumentModel(
                            doc, schemas);
                    list.add(docModel);
                } catch (DocumentException e) {
                    log.error("Could not create document model for doc " + doc);
                }
            }
            if (orderByPath != null) {
                Collections.sort(list, new PathComparator(
                        orderByPath.booleanValue()));
            }
            if (limit != 0) {
                // do limit/offset by hand
                int size = list.size();
                list.subList(0, offset > size ? size : offset).clear();
                size = list.size();
                if (limit < size) {
                    list.subList(limit, size).clear();
                }
            }
            return new DocumentModelListImpl(list, totalSize);
        } catch (Exception e) {
            throw new QueryException("getDocumentModels failed", e);
        }
    }

    public static class PathComparator implements Comparator<DocumentModel> {

        private final int sign;

        public PathComparator(boolean asc) {
            sign = asc ? 1 : -1;
        }

        public int compare(DocumentModel doc1, DocumentModel doc2) {
            String p1 = doc1.getPathAsString();
            String p2 = doc2.getPathAsString();
            return sign * p1.compareTo(p2);
        }
    }

    public DocumentIterator getDocuments(int start) {

        // skip first nodes if required
        for (int i = 0; i < start; i++) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                // break; // the iterator will have no elements
                return EmptyDocumentIterator.INSTANCE;
            }
        }

        DocumentIterator docsIt = new DocumentIterator() {

            public void remove() {
                // TODO Auto-generated method stub
            }

            public Document next() {
                JCRQueryResult.this.next();
                JCRDocument doc;
                try {
                    doc = query.session.newDocument(node);
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

    /**
     * This function computes the list of schemas that contain the selected
     * fields in the query.
     *
     * @param doc A document that represents the current node
     * @return The schemas that contain the fields in the query select clause
     */
    private String[] getSchemasInSelectClause(Document doc) {
        // DocumentType docType = doc.getType();
        // SelectList selectElements = query.sqlQuery.select.elements;

        // use only the common schema to populate doc models
        // the schema common is garanteed to be implemented by all doc types
        return new String[] { "common" };

        // HashSet<String> schemaNames = new HashSet<String>();
        // schemaNames.add("common");
        //
        // // if it is an 'select * from', we return all document schemas
        // if(selectElements.isEmpty()) {
        // return schemaNames.toArray(new String[schemaNames.size()]);
        // }
        //
        // // otherwise, we check for each schema if it contains any field in
        // the select clause
        //
        // Collection<Schema> schemas = docType.getSchemas();
        // for(Schema schema: schemas) {
        // for(int i=0; i<selectElements.size(); i++) {
        // String selectField = selectElements.getKey(i);
        // if(schema.getField(selectField)!=null) {
        // schemaNames.add(schema.getName());
        // break;
        // }
        // }
        // }
        //
        // // make sure the schema "common" is always selected
        // if(!schemaNames.contains("common")) {
        // schemaNames.add("common");
        // }
        //
        // return schemaNames.toArray(new String[schemaNames.size()]);
    }
}
