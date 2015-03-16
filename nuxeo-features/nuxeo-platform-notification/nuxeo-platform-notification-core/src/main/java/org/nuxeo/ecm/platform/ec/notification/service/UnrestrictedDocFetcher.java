/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class UnrestrictedDocFetcher extends UnrestrictedSessionRunner {

    private String docId;

    private DocumentModel doc;

    public UnrestrictedDocFetcher(String docId) {
        super("default");
        this.docId = docId;
    }

    @Override
    public void run() throws ClientException {
        doc = session.getDocument(new IdRef(docId));
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public static DocumentModel fetch(String docId) {
        UnrestrictedDocFetcher fetcher = new UnrestrictedDocFetcher(docId);
        fetcher.runUnrestricted();
        return fetcher.getDocument();
    }

}
