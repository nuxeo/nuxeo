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
 *     Guillaume RENARD
 */
package org.nuxeo.ecm.core.event.impl;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.event.Event;

/**
 * @since 2023
 */
public class BlobEventContext extends EventContextImpl {

    private static final long serialVersionUID = 1L;

    protected String docId;

    protected String xpath;

    protected ManagedBlob blob;

    public BlobEventContext(NuxeoPrincipal principal, String repositoryName, String docId, String xpath,
            ManagedBlob blob) {
        super(null, principal);
        this.docId = docId;
        this.xpath = xpath;
        this.blob = blob;
        setRepositoryName(repositoryName);
    }

    public BlobEventContext(String repositoryName, ManagedBlob managedBlob) {
        this(null, repositoryName, null, null, managedBlob);
    }

    public ManagedBlob getBlob() {
        return blob;
    }

    public String getDocId() {
        return docId;
    }

    public String getXpath() {
        return xpath;
    }

    public Event newEvent(String name) {
        return newEvent(name, Event.FLAG_IMMEDIATE);
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

}
