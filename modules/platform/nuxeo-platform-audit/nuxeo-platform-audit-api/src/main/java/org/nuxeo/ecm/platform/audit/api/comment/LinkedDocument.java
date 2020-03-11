/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api.comment;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

public class LinkedDocument implements Serializable {

    private static final long serialVersionUID = 1565438769754L;

    protected DocumentRef documentRef;

    protected RepositoryLocation repository;

    protected transient DocumentModel document;

    protected boolean brokenDocument = true;

    public boolean isBrokenDocument() {
        return brokenDocument;
    }

    public void setBrokenDocument(boolean brokenDocument) {
        this.brokenDocument = brokenDocument;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public void setDocument(DocumentModel document) {
        this.document = document;
    }

    public DocumentRef getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(DocumentRef documentRef) {
        this.documentRef = documentRef;
    }

    public RepositoryLocation getRepository() {
        return repository;
    }

    public void setRepository(RepositoryLocation repository) {
        this.repository = repository;
    }

}
