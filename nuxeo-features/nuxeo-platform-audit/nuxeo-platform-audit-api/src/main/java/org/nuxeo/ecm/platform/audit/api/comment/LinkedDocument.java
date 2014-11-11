/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
