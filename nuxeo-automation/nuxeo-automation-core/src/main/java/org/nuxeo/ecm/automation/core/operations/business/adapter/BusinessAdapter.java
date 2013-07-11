/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     dmetzler <dmetzler@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.business.adapter;

import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;

/**
 * Nuxeo document model abstract adapter to extend for mapping
 * This adapter can be created by the core adapter factory or
 * unmarshalled by Jackson from a JSON stream.
 *
 * @since 5.7
 */
@JsonPropertyOrder({ "type", "id" })
public abstract class BusinessAdapter {

    private static final Log log = LogFactory.getLog(BusinessAdapter.class);

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    private transient DocumentModel doc;

    /**
     * Default constructor called by jackson
     */
    public BusinessAdapter() {
        // For jackson we need to create a simple document model
        // that accepts all schema and properties.
        // When the "type" property will be set, then it
        // will be replaced by a DocumentModelImpl
        doc = new SimpleDocumentModel();
    }

    /**
     * Constructor used by the document adapter factory
     * @param document
     */
    public BusinessAdapter(DocumentModel document) {
        type = document.getType();
        id = doc.getId();
        doc = document;
    }


    public void save(CoreSession session) {
        try {
            session.saveDocument(doc);
        } catch (ClientException e) {
            log.error("Cannot save document", e);
        }
    }

    @JsonIgnore
    public DocumentModel getDocument() throws ClientException {
        return doc;
    }

    /**
     * Copy the datamodel from one doc to another.
     * The list of datamodel to use is taken from the destination
     * datamodel.
     * @param src
     * @param dst
     * @throws ClientException
     *
     * @since 5.7.2
     */
    private void copyDataModels(DocumentModel src, DocumentModel dst)
            throws ClientException {
        if (src != null) {
            for (Entry<String, DataModel> entry : dst.getDataModels().entrySet()) {
                DataModel dataModel = src.getDataModel(entry.getKey());
                entry.getValue().setMap(dataModel.getMap());
            }
        }
    }

    public String getId() {
        if (doc.getId() == null) {
            return id;
        }
        return doc.getId();
    }

    public String getType() {
        return doc.getType();
    }

    public void setId(String id) {
        this.id = id;
        if (doc instanceof DocumentModelImpl) {
            ((DocumentModelImpl) doc).setId(id);
        }
    }

    public void setType(String type) throws ClientException {
        if (!type.equals(doc.getType())) {
            DocumentModel oldDoc = doc;
            doc = DocumentModelFactory.createDocumentModel(type);
            ((DocumentModelImpl) doc).setId(id);
            copyDataModels(oldDoc, doc);
        }
    }

}
