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
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Unrestricted session runner providing API for retrieving the result list.
 *
 * @since 6.0
 */
public class CoreQueryAndFetchUnrestrictedSessionRunner extends
        UnrestrictedSessionRunner {

    protected final String query;

    protected final String language;

    protected IterableQueryResult result;

    public CoreQueryAndFetchUnrestrictedSessionRunner(CoreSession session,
            String query, String language) {
        super(session);
        this.query = query;
        this.language = language;
    }

    @Override
    public void run() throws ClientException {
        result = session.queryAndFetch(query, language);
    }

    public IterableQueryResult getResult() {
        return result;
    }

}
