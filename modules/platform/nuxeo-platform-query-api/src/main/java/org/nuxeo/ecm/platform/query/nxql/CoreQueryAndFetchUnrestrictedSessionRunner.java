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
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Unrestricted session runner providing API for retrieving the result list.
 *
 * @since 6.0
 */
public class CoreQueryAndFetchUnrestrictedSessionRunner extends UnrestrictedSessionRunner {

    protected final String query;

    protected final String language;

    protected IterableQueryResult result;

    public CoreQueryAndFetchUnrestrictedSessionRunner(CoreSession session, String query, String language) {
        super(session);
        this.query = query;
        this.language = language;
    }

    @Override
    public void run() {
        result = session.queryAndFetch(query, language);
    }

    public IterableQueryResult getResult() {
        return result;
    }

}
