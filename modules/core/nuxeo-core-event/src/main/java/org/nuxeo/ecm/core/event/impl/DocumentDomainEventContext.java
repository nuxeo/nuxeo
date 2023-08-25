/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.event.impl;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.model.Document;

/**
 * Event Context used for Document Domain Event. Different from DocumentEventContext because lighter and targeted only
 * for domain event.
 *
 * @since 2021.44
 */
public class DocumentDomainEventContext extends EventContextImpl {

    private static final long serialVersionUID = 20230906L;

    protected Document doc;

    protected String principal;

    public DocumentDomainEventContext(NuxeoPrincipal principal, Document doc) {
        super(null, principal);
        this.doc = doc;
        this.setProperty("category", DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        setRepositoryName(doc.getRepositoryName());
    }

    public Document getDoc() {
        return doc;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public Event newEvent(String name) {
        return newEvent(name, Event.FLAG_IMMEDIATE);
    }
}
