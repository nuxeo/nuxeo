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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    protected transient DocumentModel doc;

    /**
     * Default constructor called by jackson
     */
    public BusinessAdapter() {
        doc = new SimpleDocumentModel();
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
    public DocumentModel getDocument() {
        return doc;
    }

    public String getId() {
        try {
            return doc.getId();
        } catch (UnsupportedOperationException e) {
            return id;
        }
    }

    public String getType() {
        return doc.getType() == null ? type : doc.getType();
    }

}
