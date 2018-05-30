/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
