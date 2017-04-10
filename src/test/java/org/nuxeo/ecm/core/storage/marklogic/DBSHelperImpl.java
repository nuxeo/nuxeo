/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.dbs.DBSHelper;

import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

public class DBSHelperImpl implements DBSHelper {

    private static final String HOST_PROPERTY = "nuxeo.test.marklogic.host";

    private static final String PORT_PROPERTY = "nuxeo.test.marklogic.port";

    private static final String DBNAME_PROPERTY = "nuxeo.test.marklogic.dbname";

    private static final String USER_PROPERTY = "nuxeo.test.marklogic.user";

    private static final String PASSWORD_PROPERTY = "nuxeo.test.marklogic.password";

    private static final String DEFAULT_HOST = "localhost";

    private static final String DEFAULT_PORT = "8010";

    private static final String DEFAULT_DBNAME = "unittests";

    private static final String DEFAULT_USER = "nuxeo";

    private static final String DEFAULT_PASSWORD = "nuxeo";

    @Override
    public void init() {
        String host = defaultProperty(HOST_PROPERTY, DEFAULT_HOST);
        Integer port = Integer.valueOf(defaultProperty(PORT_PROPERTY, DEFAULT_PORT));
        String dbname = defaultProperty(DBNAME_PROPERTY, DEFAULT_DBNAME);
        String user = defaultProperty(USER_PROPERTY, DEFAULT_USER);
        String password = defaultProperty(PASSWORD_PROPERTY, DEFAULT_PASSWORD);
        MarkLogicRepositoryDescriptor descriptor = new MarkLogicRepositoryDescriptor();
        descriptor.name = "test";
        descriptor.host = host;
        descriptor.port = port;
        descriptor.user = user;
        descriptor.password = password;
        descriptor.dbname = dbname;
        ContentSource client = MarkLogicRepository.newMarkLogicContentSource(descriptor);
        try (Session session = client.newSession()) {
            AdhocQuery request = session.newAdhocQuery(
                    "for $doc in doc() return xdmp:document-delete(xdmp:node-uri($doc))");
            session.submitRequest(request);
        } catch (RequestException e) {
            throw new NuxeoException("MarkLogic cleaning failed", e);
        }
    }

}
