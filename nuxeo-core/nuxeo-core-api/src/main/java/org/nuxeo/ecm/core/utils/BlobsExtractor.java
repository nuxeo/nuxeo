/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Extractor for all the blobs of a document.
 *
 * @author Florent Guillaume
 */
public class BlobsExtractor {

    protected final SchemaManager schemaManager;

    protected List<Field> blobFields;

    protected List<Type> interestingTypes;

    protected List<Blob> blobsFound;

    public BlobsExtractor() throws ClientException {
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        if (schemaManager == null) {
            throw new ClientException("No schema manager");
        }
    }

    /**
     * Finds all the blobs of the document.
     * <p>
     * This method is not thread-safe.
     *
     * @param doc the document
     * @return the list of blobs in the document
     */
    public List<Blob> getBlobs(DocumentModel doc) throws ClientException {
        blobFields = new LinkedList<Field>();
        interestingTypes = new LinkedList<Type>();
        blobsFound = new LinkedList<Blob>();

        // find all fields which are blobs
        // and all types containing one such field
        for (Schema schema : doc.getDocumentType().getSchemas()) {
            findInteresting(schema);
        }

        // get actual blobs
        for (String schemaName : doc.getDocumentType().getSchemaNames()) {
            Schema schema = schemaManager.getSchema(schemaName);
            if (!interestingTypes.contains(schema)) {
                continue;
            }
            findBlobs(schema, doc.getDataModel(schemaName));
        }

        // cleanup
        List<Blob> blobs = blobsFound;
        blobFields = null;
        interestingTypes = null;
        blobsFound = null;

        return blobs;
    }

    /**
     * Analyzes the document's schemas to find which fields and complex types
     * contain blobs.
     *
     * @return {@code true} if the passed complex type contains at least one
     *         blob field
     */
    protected boolean findInteresting(ComplexType ct) {
        boolean interesting = false;
        for (Field field : ct.getFields()) {
            Type type = field.getType();
            if (type.isSimpleType()) {
                continue; // not binary text
            } else if (type.isListType()) {
                Type ftype = ((ListType) type).getField().getType();
                if (ftype.isComplexType()) {
                    if (findInteresting((ComplexType) ftype)) {
                        interestingTypes.add(type);
                        interesting |= true;
                    }
                } else {
                    continue; // not binary text
                }
            } else { // complex type
                ComplexType ctype = (ComplexType) type;
                if (type.getName().equals(TypeConstants.CONTENT)) {
                    blobFields.add(field);
                    interestingTypes.add(type);
                    interesting = true;
                } else {
                    interesting |= findInteresting(ctype);
                }
            }
        }
        if (interesting) {
            interestingTypes.add(ct);
        }
        return interesting;
    }

    /**
     * Finds the blobs in a DataModel.
     */
    protected void findBlobs(ComplexType ct, DataModel data) {
        for (Field field : ct.getFields()) {
            Type type = field.getType();
            if (!interestingTypes.contains(type)) {
                continue;
            }
            Object value;
            try {
                value = data.getData(field.getName().getLocalName());
            } catch (PropertyException e) {
                continue;
            }
            findBlobs(field, value);
        }
    }

    /**
     * Finds the blobs in a value.
     */
    @SuppressWarnings("unchecked")
    protected void findBlobs(Field field, Object value) {
        if (value == null) {
            return;
        }
        if (blobFields.contains(field)) {
            blobsFound.add((Blob) value);
            return;
        }
        Type type = field.getType();
        if (!interestingTypes.contains(type)) {
            return;
        }
        if (type.isListType()) {
            for (Object o : ((List<Object>) value)) {
                findBlobs(((ListType) type).getField(), o);
            }
        } else { // complex type
            ComplexType ctype = (ComplexType) type;
            Map<String, Object> map = (Map<String, Object>) value;
            for (String k : map.keySet()) {
                findBlobs(ctype.getField(k), map.get(k));
            }
        }
    }

}
