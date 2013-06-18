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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;

/**
 * Nuxeo document model abstract adapter to extend for mapping
 *
 * @since 5.7
 */
public abstract class BusinessAdapter {

    private static final Log log = LogFactory.getLog(BusinessAdapter.class);

    protected transient DocumentModel doc;

    @JsonProperty("id")
    protected String id;

    public BusinessAdapter() {
        this.doc = DocumentModelFactory.createDocumentModel("File");
        this.id = doc.getId();
    }

    public BusinessAdapter(DocumentModel document) {
        this.doc = document;
        this.id = doc.getId();
    }

    public void save(CoreSession session) {
        try {
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            log.error("Cannot save document", e);
        }
    }

    @JsonIgnore
    public DocumentModel getDocument() {
        return doc;
    }

    public String getId() {
        return doc.getId();
    }

}
