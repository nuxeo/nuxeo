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
 */
package org.nuxeo.ecm.automation.core.operations.business.adapter;

import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;

/**
 * Nuxeo document model abstract adapter to extend for mapping
 *
 * @since 5.7
 */
@JsonPropertyOrder({ "type", "id" })
public abstract class BusinessAdapter {

    private static final Log log = LogFactory.getLog(BusinessAdapter.class);

    @JsonProperty("id")
    protected String id;

    @JsonProperty("type")
    protected String type;

    private transient DocumentModel doc;

    /**
     * Default constructor called by jackson
     */
    public BusinessAdapter() {
    }

    public BusinessAdapter(DocumentModel document) {
        type = document.getType();
        doc = document;
        id = doc.getId();
    }

    public void save(CoreSession session) {
        try {
            session.saveDocument(doc);
        } catch (ClientException e) {
            log.error("Cannot save document", e);
        }
    }

    @JsonIgnore
    /**
     * Fetches the adapted document in a lazy way
     * @return
     * @throws ClientException
     *
     * @since TODO
     */
    public DocumentModel getDocument() throws ClientException {

        // We need a document model so that the setter don't throw
        // a NPE
        // to get a document model we need
        // * id and type for update
        // * type for creation
        // * nothing for a SimpleDocumentModel (that mocks any DM)
        // Every time there is a call to that method we try to get
        // a *better* documentModel in which we copy the datamodels
        // of the precedent one:
        // SimpleDocumentModel -> DocumentModel without id -> DocumentModel
        try {
            if (doc == null || doc instanceof SimpleDocumentModel) {
                DocumentModel oldDoc = doc;
                if (StringUtils.isBlank(id) && StringUtils.isBlank(type)) {
                    doc = new SimpleDocumentModel();
                } else if (StringUtils.isBlank(id)) {
                    doc = DocumentModelFactory.createDocumentModel(type);
                    copyDataModels(oldDoc, doc);
                } else {
                    doc = DocumentModelFactory.createDocumentModel(type, id);
                    copyDataModels(oldDoc, doc);
                }

            }
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
        return doc;
    }

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

}
