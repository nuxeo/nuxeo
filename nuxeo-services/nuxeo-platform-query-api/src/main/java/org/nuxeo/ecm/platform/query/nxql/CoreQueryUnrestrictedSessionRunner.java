/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.nxql;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Unrestricted session runner providing API for retrieving the result documents list.
 *
 * @since 6.0
 */
public class CoreQueryUnrestrictedSessionRunner extends UnrestrictedSessionRunner {

    protected final String query;

    protected final Filter filter;

    protected final long limit;

    protected final long offset;

    protected final boolean countTotal;

    protected final long countUpTo;

    protected final boolean detachDocuments;

    protected DocumentModelList docs;

    public CoreQueryUnrestrictedSessionRunner(CoreSession session, String query, Filter filter, long limit,
            long offset, boolean countTotal, long countUpTo, boolean detachDocuments) {
        super(session);
        this.query = query;
        this.filter = filter;
        this.limit = limit;
        this.offset = offset;
        this.countTotal = countTotal;
        this.countUpTo = countUpTo;
        this.detachDocuments = detachDocuments;
    }

    @Override
    public void run() {
        if (countTotal) {
            docs = session.query(query, filter, limit, offset, countTotal);
        } else {
            docs = session.query(query, filter, limit, offset, countUpTo);
        }
        if (docs != null && detachDocuments) {
            for (DocumentModel doc : docs) {
                doc.detach(true);
            }
        }
    }

    public DocumentModelList getDocs() {
        return docs;
    }

}
