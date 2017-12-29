/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.business.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
        session.saveDocument(doc);
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
