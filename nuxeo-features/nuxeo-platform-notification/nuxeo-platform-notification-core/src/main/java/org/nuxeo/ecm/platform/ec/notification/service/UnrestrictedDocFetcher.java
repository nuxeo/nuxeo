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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public class UnrestrictedDocFetcher extends UnrestrictedSessionRunner {

    private String docId;

    private DocumentModel doc;

    private List<DocumentModel> queryResult;

    private String query;

    private UnrestrictedDocFetcher() {
        super(Framework.getService(RepositoryManager.class).getDefaultRepositoryName());
    }

    @Override
    public void run() {

        if(docId != null) {
            doc = session.getDocument(new IdRef(docId));
        }
        if(query != null) {
            queryResult = session.query(query);
        }
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public static DocumentModel fetch(String docId) {
        UnrestrictedDocFetcher fetcher = new UnrestrictedDocFetcher();
        fetcher.docId = docId;
        fetcher.runUnrestricted();
        return fetcher.getDocument();
    }

    public static List<DocumentModel> query(String nxql) {
        UnrestrictedDocFetcher fetcher = new UnrestrictedDocFetcher();
        fetcher.query = nxql;
        fetcher.runUnrestricted();
        return fetcher.queryResult;
    }

}
