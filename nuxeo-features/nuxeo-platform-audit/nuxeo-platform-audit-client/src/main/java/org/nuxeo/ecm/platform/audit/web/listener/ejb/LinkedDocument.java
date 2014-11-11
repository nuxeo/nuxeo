/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.web.listener.ejb;

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
