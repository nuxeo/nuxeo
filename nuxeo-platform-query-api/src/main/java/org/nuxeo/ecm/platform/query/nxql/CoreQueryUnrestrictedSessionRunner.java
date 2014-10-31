/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.nxql;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Unrestricted session runner providing API for retrieving the result
 * documents list.
 *
 * @since 6.0
 */
public class CoreQueryUnrestrictedSessionRunner extends
        UnrestrictedSessionRunner {

    protected final String query;

    protected final Filter filter;

    protected final long limit;

    protected final long offset;

    protected final boolean countTotal;

    protected final long countUpTo;

    protected final boolean detachDocuments;

    protected DocumentModelList docs;

    public CoreQueryUnrestrictedSessionRunner(CoreSession session,
            String query, Filter filter, long limit, long offset,
            boolean countTotal, long countUpTo, boolean detachDocuments) {
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
    public void run() throws ClientException {
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
